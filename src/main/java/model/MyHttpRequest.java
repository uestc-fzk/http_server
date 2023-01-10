package model;

import lombok.Data;

import java.io.BufferedReader;
import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * POST / HTTP/1.1
 * username: zhangsan
 * cache-control: no-cache
 * Postman-Token: 63dde61d-efbf-4aa7-9b7a-732f1c608603
 * Content-Type: text/plain
 * User-Agent: PostmanRuntime/7.1.1
 * Accept: **
 * Host:127.0.0.1:9999
 * accept-encoding:gzip,deflate
 * content-length:34
 * Connection:keep-alive
 * <p>
 * {"level":5,"age":23,"name":"lisi"}
 * <p>
 * Http请求模型
 *
 * @author fzk
 * @datetime 2023-01-04 23:15
 */
@Data
public class MyHttpRequest {
    private Map<String, String> headers=new HashMap<>();
    private URI uri;
    private volatile Proxy proxy; // ensure safe publishing
    //    private final InetSocketAddress authority; // only used when URI not specified
    private String method;
    //    final HttpRequest.BodyPublisher requestPublisher;
    boolean secure;
    boolean expectContinue;
    private volatile boolean isWebSocket = false;
    //    private  Duration timeout;  // may be null
    private  String version;

    private BufferedReader body;
}
