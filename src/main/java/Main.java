import config.ConfigReader;
import config.HttpServerConfig;
import core.HttpServer;
import log.MyLogger;

import java.io.IOException;

/**
 * @author fzk
 * @datetime 2023-01-04 21:41
 */
public class Main {
    public static final String ConfigFilePath="http_config.properties";

    public static void main(String[] args) throws IOException {
        HttpServerConfig config = ConfigReader.getConfig();
        MyLogger.info(config.toString());

        try {
            new HttpServer(config).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
