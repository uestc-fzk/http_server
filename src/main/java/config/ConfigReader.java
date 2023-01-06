package config;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author fzk
 * @datetime 2023-01-04 21:43
 */
public class ConfigReader {

    public static final String ConfigFilePath = "http_config.properties";
    public static final HttpServerConfig config;

    static {
        Properties properties = new Properties();
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(ConfigFilePath);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String jsonStr = JSON.toJSONString(properties);
        config = JSON.parseObject(jsonStr, HttpServerConfig.class);
    }
    public static HttpServerConfig getConfig(){
        return config;
    }
}
