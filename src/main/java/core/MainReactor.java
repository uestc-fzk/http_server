package core;

import config.HttpServerConfig;
import handler.ChannelHandler;
import log.MyLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.function.Supplier;

/**
 * 主Reactor，接受连接并注册到从Reactor
 *
 * @author fzk
 * @datetime 2023-01-06 11:05
 */
public class MainReactor implements Runnable, Closeable {
    public static final int SUB_REACTOR_COUNT = Runtime.getRuntime().availableProcessors();
    private final HttpServerConfig config;
    public final String name;
    private volatile int runState = 0;// 运行状态, 0未启动, 1运行中, 2关闭中, 3已关闭
    private final Selector mainSelector;
    private final ServerSocketChannel serverChannel;
    private final Acceptor acceptor;
    private final SubReactor[] subReactors;
    private volatile int nextSubReactor = 0;
    private final Thread mainThread;
    private final ThreadGroup subTheadGroup;
    private final Supplier<List<ChannelHandler>> supplier;

    public MainReactor(HttpServerConfig config,Supplier<List<ChannelHandler>> supplier) throws IOException {
        this.config=config;
        this.supplier=supplier;
        this.name = "MainReactor";
        // 新建ServerChannel
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(new InetSocketAddress(config.getHost(),config.getPort()));
        // 新建Selector并注册
        this.mainSelector = Selector.open();
        this.serverChannel.register(mainSelector, SelectionKey.OP_ACCEPT);
        this.acceptor = new Acceptor(mainSelector);

        // 创建SubReactor
        this.subReactors = new SubReactor[SUB_REACTOR_COUNT];
        for (int i = 0; i < subReactors.length; i++) {
            subReactors[i] = new SubReactor(config,"SubReactor-" + i,supplier);
        }

        // 创建线程组
        mainThread = new Thread(this, this.name);
        subTheadGroup = new ThreadGroup("SubReactorGroup");
        for (SubReactor subReactor : subReactors) {
            subReactor.thread = new Thread(subTheadGroup, subReactor, subReactor.name);
        }
    }

    public void start() {
        runState = 1;
        // 1.启动SubReactor组
        for (SubReactor subReactor : subReactors) {
            subReactor.start();
        }
        // 2.启动MainReactor/Acceptor线程
        mainThread.start();
        MyLogger.info(String.format("%s start",this.name));
    }

    /**
     * 将SocketChannel转发到各个SubReactor中
     *
     * @param socketChannel 新连接
     */
    private void dispatch(SocketChannel socketChannel) {
        // 将SocketChannel分派到各个SubReactor中
        try {
            synchronized (subReactors) {
                MyLogger.info(String.format("监听到新连接: %s 即将分派到%s\n", socketChannel.getRemoteAddress(), "subReactor-" + nextSubReactor));
                SubReactor subReactor = subReactors[nextSubReactor++];
                if (nextSubReactor >= subReactors.length) nextSubReactor = 0;
                subReactor.registerChannel(socketChannel);
            }
        } catch (IOException e) {
            MyLogger.warning(String.format("dispatch socketChannel occurs error: " + e));
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted() && runState == 1) {
            try {
                // 监听新连接并转发
                this.acceptor.accept().forEach(cliChannel -> {
                    if (cliChannel != null) {
                        dispatch(cliChannel);
                    }
                });
            } catch (IOException e) {
                MyLogger.warning("MainReactor run occurs error: " + e);
                break;
            }
        }
        // 线程中断或异常退出
        if (runState == 1) {
            close();
        }
    }

    @Override
    public void close() {
        if (runState != 1) return;

        runState = 2;// 关闭中
        // 中断从线程组
        subTheadGroup.interrupt();
        // 1.先关闭subReactor
        for (SubReactor subReactor : subReactors) {
            subReactor.close();
        }
        // 2.再关闭serverChannel
        try {
            serverChannel.close();
        } catch (IOException e) {
            MyLogger.warning(String.format("close serverChannel occurs error: %s", e));
        }
        // 3.再关闭selector
        try {
            mainSelector.close();
        } catch (IOException e) {
            MyLogger.warning(String.format("close mainSelector occurs error: %s", e));
        }

        runState = 3;// 成功关闭
    }
}