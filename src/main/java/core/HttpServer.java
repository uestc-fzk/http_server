package core;

import config.HttpServerConfig;
import handler.ChannelHandler;
import log.MyLogger;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author fzk
 * @datetime 2023-01-04 22:05
 */
public class HttpServer implements Closeable {
    private volatile int runState = 0;// 运行状态, 0未启动, 1运行中, 2关闭中, 3已关闭
    public final Supplier<List<ChannelHandler<?>>> supplier;
    private final HttpServerConfig config;

    private final MainReactor mainReactor;

    public HttpServer(HttpServerConfig config, Supplier<List<ChannelHandler<?>>> supplier) throws IOException {
        this.supplier = supplier;
        this.config = config;
        this.mainReactor = new MainReactor(config, supplier);
    }

    public void start() {
        runState = 1;
        mainReactor.start();
        MyLogger.logger.info("http server start successfully");
    }

    @Override
    public void close() {
        if (runState != 1) return;
        runState = 2;
        mainReactor.close();
        runState = 3;
        MyLogger.logger.info("http server closed");
    }
}
