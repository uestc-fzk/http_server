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
    public final Supplier<List<ChannelHandler>> supplier;
    private final HttpServerConfig config;

    private final MainReactor mainReactor;

    public HttpServer(HttpServerConfig config, Supplier<List<ChannelHandler>> supplier) throws IOException {
        this.supplier=supplier;
        this.config = config;
        this.mainReactor = new MainReactor(config,supplier);
    }

    public void start() {
        mainReactor.start();
        MyLogger.info("http server start successfully");
    }

    @Override
    public void close() {
        mainReactor.close();
        MyLogger.info("http server closed");
    }
}
