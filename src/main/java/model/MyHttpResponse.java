package model;

import codec.HttpEncoder;
import handler.ChannelContext;
import log.MyLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 对响应的包装，使其具有发送能力
 * 注意：必须调用response.flush()，它会重新将channel注册回selector
 * 之所以不将channel注册回selector写如到finally块自动执行，目的在于让用户可以充分的掌握何时写响应以异步编程！
 * 这样就可以随时写回响应了，而不必像同步编程那样，在处理业务逻辑后 由框架自动写响应
 *
 * @author fzk
 * @datetime 2023-01-06 16:42:50
 */
public class MyHttpResponse {
    private volatile boolean isFlushed = false;
    public final ChannelContext ctx;
    public String version;
    public int code;// 响应码
    public String status;// 短语信息
    public Map<String, Object> headers;// 响应头
    public final ByteBuffer body;// 响应体 缓冲区

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

    public MyHttpResponse(int code, ChannelContext ctx) {
        this.code = code;
        this.ctx=ctx;

        this.body = ByteBuffer.allocateDirect(1024);
        this.version = "HTTP/1.1";
        this.status = "ok";
        this.headers = new HashMap<>();
        this.headers.put("Server", "nginx/1.20.1");
        this.headers.put("Date", LocalDateTime.now());
        this.headers.put("Content-Type", "application/json;charset=UTF-8");
//        this.headers.put("Transfer-Encoding", "chunked");
        this.headers.put("Connection", "keep-alive");
    }

    /**
     * 写到内存缓冲区
     *
     * @return 写入内存缓冲区的字节数
     */
    public int write(byte[] buf) {
        body.put(buf, body.position(), buf.length);
        return buf.length;
    }

    /**
     * 将缓冲区数据写入channel
     * 并将channel重新注册会selector
     *
     * @return 从缓冲区写入channel的字节数
     */
    public int flush() throws IOException {
        // 幂等处理
        if (isFlushed) {
            MyLogger.logger.warning("flush 只能调用1次");
            return 0;
        }
        body.put((byte) '\n');// 加个换行符表示响应体结束捏
        body.flip();// 翻转待读
        int count = body.remaining();

        // 编码响应行/头
        ByteBuffer buffer_header = HttpEncoder.encode(this);
        // 将响应行/头/体写入channel
        ctx.socketChannel.write(new ByteBuffer[]{buffer_header, body});
        body.clear();
        buffer_header.clear();

        isFlushed = true;
        return count;
    }
}
