package core;

import log.MyLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

/**
 * @author fzk
 * @datetime 2023-01-04 22:05
 */
public class HttpServer implements Closeable {
    private final int port;
    private final String host;
    public final ServerSocketChannel serverChannel;

    private final MainReactor mainReactor;

    public HttpServer(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(new InetSocketAddress(host, port));
        this.mainReactor = new MainReactor(serverChannel);
    }

    public void start() {
        mainReactor.start();
        MyLogger.info("http server start successfully");
    }

    @Override
    public void close() {
        try {
            mainReactor.close();
            serverChannel.close();
        } catch (IOException e) {
            MyLogger.warning("close http server occurs error: " + e);
        }
    }
}
