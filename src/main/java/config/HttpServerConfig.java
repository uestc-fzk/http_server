package config;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * 配置模型
 * @author fzk
 * @datetime 2023-01-06 16:25:30
 */
@Data
public class HttpServerConfig {
    @JSONField(name="host")
    private String host;
    @JSONField(name="port")
    private int port;
    @JSONField(name="log-level")
    private String logLevel;
    @JSONField(name="log-path")
    private String logPath;
}
