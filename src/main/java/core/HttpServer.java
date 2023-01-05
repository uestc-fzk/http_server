package core;

import codec.HttpDecoder;
import codec.HttpEncoder;
import log.MyLogger;
import model.MyHttpRequest;
import model.MyHttpResponse;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author fzk
 * @datetime 2023-01-04 22:05
 */
public class HttpServer implements Closeable {
    public int port;
    public String host;
    public ServerSocketChannel serverChannel;
    public Selector selector;
    public HttpDecoder decoder;
    public HttpEncoder encoder;
    public static final int Accept_Timeout = 5000;// 监听超时, ms

    public HttpServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.decoder = new HttpDecoder();
        this.encoder = new HttpEncoder();
    }

    public void start() throws IOException, InterruptedException {
        // 1.新建服务器Channel并绑定端口
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(host, port));
        serverChannel.configureBlocking(false);// 设为非阻塞
        // 2.新建Selector
        selector = Selector.open();
        // 3.注册channel通道到选择器上
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        MyLogger.info("http server start success");

        // 4.监听连接
        while (!Thread.currentThread().isInterrupted()) {
            if (selector.select(Accept_Timeout) < 0) {
                continue;
            }

            // 5.获取到已经就绪的通道
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey next = iterator.next();
                iterator.remove();// 必须从选择器选中selected-key集合中移除已经处理的key
                // 无效说明可能channel关闭了
                if (!next.isValid()) {
                    next.cancel();// 取消该key
                    continue;
                }

                // 6.监听到有新客户端连接
                if (next.isAcceptable()) {
                    SocketChannel socketChannel = serverChannel.accept();
                    socketChannel.configureBlocking(false);// 设置为非阻塞模式
                    // 注册到选择器
                    socketChannel.register(selector, SelectionKey.OP_READ);

                    MyLogger.info(String.format("new connection from %s", socketChannel.getRemoteAddress()));
                }
                // 7.监听到客户端发新消息
                else if (next.isReadable()) {
                    // 7.1.取出发送者通道
                    next.interestOps(0);// 取消读兴趣
                    SocketChannel cliChannel = (SocketChannel) next.channel();
                    processRead(cliChannel, next);
                }
            }
        }
    }

    public void processRead(SocketChannel cliChannel, SelectionKey selectionKey) throws IOException, InterruptedException {
        // 若连接已关闭
        if (!cliChannel.isConnected()) {
            cliChannel.close();
            selectionKey.cancel();
            return;
        }

        ByteBuffer buf = ByteBuffer.allocateDirect(1024);
        StringBuilder sb = new StringBuilder();
        // 注意：这里必须是-1，当没有内容时返回-1不是0!
        if (cliChannel.read(buf) == -1) {
            // 没有内容说明连接中断
            MyLogger.info(String.format("close connection %s", cliChannel.getRemoteAddress()));
            cliChannel.close();
            selectionKey.cancel();
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
        // 关闭连接
//        Thread.sleep(1000);
//            cliChannel.close();
    }

    @Override
    public void close() throws IOException {
        try {
            selector.close();
            serverChannel.close();
        } catch (IOException e) {
            MyLogger.warning("close selector and serverChannel occurs error: " + e);
        }
    }
}
