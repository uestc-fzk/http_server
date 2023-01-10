package codec;

import model.MyHttpResponse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * http编码器
 *
 * @author fzk
 * @datetime 2023-01-05 0:18
 */
public class HttpEncoder {
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
    public static ByteBuffer encode(MyHttpResponse response) {
        StringBuilder sb=new StringBuilder();
        if( response.body!=null){
            // 添加响应体长度
            response.headers.put("Content-Length",response.body.remaining());
        }

        // 1.响应行
        sb.append(response.version).append(' ').
                append(response.code).append(' ').
                append(response.status).append('\n');
        // 2.响应头
        response.headers.forEach((k, v) -> {
            sb.append(k).append(": ").append(v).append('\n');
        });
        // 3.空行
        sb.append('\n');

//        // 4.响应体
//        if (response.getBodyData() != null) {
//            // 末尾加个换行符
//            sb.append(new String(response.getBodyData(),StandardCharsets.UTF_8)).append('\n');
//        }
        return ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
