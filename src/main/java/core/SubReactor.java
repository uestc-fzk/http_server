package core;

import codec.HttpDecoder;
import codec.HttpEncoder;
import log.MyLogger;
import model.MyHttpRequest;
import model.MyHttpResponse;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 从Reactor负责监听连接读事件，并交给线程池处理
 *
 * @author fzk
 * @datetime 2023-01-06 11:05
 */
class SubReactor implements Runnable, Closeable {
    public static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    public final String name;
    private volatile int runState = 0;// 运行状态, 0未启动, 1运行中, 2关闭中, 3已关闭
    public Thread thread;
    private final Selector subSelector;

    // 多线程处理业务逻辑
    private final ExecutorService executorService;
    private final HttpEncoder encoder;
    private final HttpDecoder decoder;

    SubReactor(String name) throws IOException {
        this.name = name;
        this.subSelector = Selector.open();
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.encoder = new HttpEncoder();
        this.decoder = new HttpDecoder();
    }

    public void start() {
        runState = 1;
        thread.start();
        MyLogger.info(String.format("%s start",this.name));
    }

    // 注册chanel，关注读事件
    public void registerChannel(SocketChannel sc) throws IOException {
        sc.configureBlocking(false);
        sc.register(subSelector, SelectionKey.OP_READ);
        wakeup();// 必须唤醒Selector才能检测到新加入的SocketChannel
    }

    /**
     * wakeup的目的在于唤醒Selector，原因如下：
     * 1.新加入的SocketChannel必须让Selector重新调用select()方法才能检测到
     * 2.SocketChannel修改兴趣集也必须让Selector重新调用select()方法才能检测到新兴趣集
     * 因此必须调用Selector#wakeup()让其立刻从select()阻塞调用中唤醒并重新发起select()监听
     */
    public void wakeup() {
        this.subSelector.wakeup();
    }

    public void updateInterestOps(SelectionKey key, int interest) {
        key.interestOps(interest);
        wakeup();// 修改兴趣集必须唤醒Selector让其重新发起select()监听才能监听到新兴趣集
    }

    @Override
    public void run() {
        while (!Thread.interrupted() && runState == 1) {
            try {
                if (subSelector.select() > 0) {
                    Set<SelectionKey> keys = subSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        // 监听到可读事件，进行处理
                        if (key.isReadable()) {
                            // 暂时将其兴趣集设置为0，防止多次触发可读操作
                            updateInterestOps(key, 0);
                            processRead(key);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 线程中断或异常退出
        if (runState != 1) {
            close();
        }
    }

    private void processRead(SelectionKey key) {
        // 线程池异步处理
        executorService.execute(() -> {
            try {
                // 前置检查: key是否有效
                if (!key.isValid()) {
                    cancelKey(key);
                    return;
                }
                // 前置检查: Channel是否连接
                SocketChannel cliChannel = (SocketChannel) key.channel();
                if (cliChannel == null || !cliChannel.isConnected()) {
                    cancelKey(key);
                    return;
                }

                ByteBuffer buf = ByteBuffer.allocateDirect(1024);
                StringBuilder sb = new StringBuilder();
                // 注意：这里必须是-1，当没有内容时返回-1不是0!
                if (cliChannel.read(buf) == -1) {
                    // 没有内容说明连接中断
                    cancelKey(key);
                    return;
                }

                // 读内容
                do {
                    buf.flip();
                    sb.append(StandardCharsets.UTF_8.decode(buf));
                    buf.clear();
                } while (cliChannel.read(buf) > 0);
                MyLogger.info(String.format("new content from %s", cliChannel.getRemoteAddress()));

                MyHttpRequest request = decoder.decodeHttp(
                        ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8)));
                System.out.println(request);

                // 响应
                MyHttpResponse<Map<String, String>> response = new MyHttpResponse<>(200, Map.of("k1", "v1"));
                ByteBuffer responseBuf = encoder.encode(response);

                while (responseBuf.hasRemaining()) {
                    cliChannel.write(responseBuf);
                }
                responseBuf.clear();

                System.out.println(new String(responseBuf.array(), StandardCharsets.UTF_8));

                // 将兴趣集恢复，监听读事件
                updateInterestOps(key, SelectionKey.OP_READ);
            } catch (IOException e) {
                MyLogger.warning(String.format("%s handle read occurs error: %s", this.name, e));
            }
        });
    }

    private void cancelKey(SelectionKey key) {
        if (key != null) {
            key.cancel();
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel != null) {
                try {
                    channel.close();
                    MyLogger.info(String.format("close connection from %s", channel.getRemoteAddress()));
                } catch (IOException e) {
                    MyLogger.warning(String.format("%s close SocketChannel occurs error: %s", this.name, e));
                }
            }
        }
    }

    @Override
    public void close() {
        if (runState != 1) return;

        runState = 2;// 关闭中
        MyLogger.info(String.format("%s is closing", this.name));
        // 关闭线程池
        executorService.shutdown();
        // 关闭Selector并关闭注册的Channel
        subSelector.keys().forEach(selectionKey -> {
            selectionKey.cancel();
            try {
                selectionKey.channel().close();
            } catch (IOException e) {
                MyLogger.warning(String.format("%s close subSelector occurs error: %s", this.name, e));
            }
        });
        try {
            subSelector.close();
        } catch (IOException e) {
            MyLogger.warning(this.name + " close subSelector occurs error: " + e);
        }

        runState = 3;// 关闭成功
        MyLogger.info(String.format("%s close successfully", this.name));
    }
}