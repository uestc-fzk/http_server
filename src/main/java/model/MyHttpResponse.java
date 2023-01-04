package model;

import lombok.Data;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fzk
 * @datetime 2023-01-05 0:02
 */
@Data
public class MyHttpResponse<T> {
    private String version;
    private int code;// 响应码
    private String status;// 短语信息
    private Map<String, String> headers;// 响应头
    private T body;// 响应体

    /**
     * HTTP/1.1 200
     * Server: nginx/1.20.1
     * Date: Wed, 04 Jan 2023 16:19:01 GMT
     * Content-Type: application/json
     * Transfer-Encoding: chunked
     * Connection: keep-alive
     * \r\n
     * {"k1": "v1"}
     */
    public MyHttpResponse(int code, T body) {
        this.code = code;
        this.body = body;
        this.version = "HTTP/1.1";
        this.status="ok";
        this.headers=new HashMap<>();
        this.headers.put("Server", "nginx/1.20.1");
        this.headers.put("Date", Calendar.getInstance().getTime().toString());
        this.headers.put("Content-Type", "application/json;charset=UTF-8");
        this.headers.put("Transfer-Encoding", "chunked");
        this.headers.put("Connection", "keep-alive");
    }
}
