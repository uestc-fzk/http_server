package config;

import log.MyLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author fzk
 * @datetime 2023-01-04 21:43
 */
public class ConfigReader {
    public static final String ConfigFilePath = "http_config.properties";

    public static Properties getConfigProperties() {
        Properties properties = new Properties();
        try ( InputStream inputStream = ClassLoader.getSystemResourceAsStream(ConfigFilePath);){
            properties.load(inputStream);
        } catch (IOException e) {
            MyLogger.warning("config file read err: " + e.toString());
        }
        return properties;
    }

}
