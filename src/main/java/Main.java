import config.ConfigReader;
import core.HttpServer;
import log.MyLogger;

import java.io.IOException;
import java.util.Properties;

/**
 * @author fzk
 * @datetime 2023-01-04 21:41
 */
public class Main {
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";

    public static void main(String[] args) throws IOException {
        Properties properties = ConfigReader.getConfigProperties();
        MyLogger.info(properties.toString());
        String host=properties.getProperty(HOST_KEY);
        int port = Integer.parseInt(properties.getProperty(PORT_KEY));

        try (HttpServer httpServer = new HttpServer(host,port);){
            httpServer.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
