package core;

import config.HttpServerConfig;
import log.MyLogger;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author fzk
 * @datetime 2023-01-04 22:05
 */
public class HttpServer implements Closeable {
    private final HttpServerConfig config;

    private final MainReactor mainReactor;

    public HttpServer(HttpServerConfig config) throws IOException {
        this.config = config;
        this.mainReactor = new MainReactor(config);
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
