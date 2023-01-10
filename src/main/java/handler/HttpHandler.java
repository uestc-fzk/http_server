package handler;

import codec.HttpDecoder;
import model.MyHttpRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author fzk
 * @datetime 2023-01-09 20:11:26
 */
public class HttpHandler implements ChannelHandler<ByteBuffer> {
//    private final Consumer<HttpModel> consumer;
//    public HttpHandler(Consumer<HttpModel> consumer) {
//         this.consumer=consumer;
//    }

    @Override
    public void ChannelRead(ByteBuffer buffer, ChannelContext ctx) throws IOException {
        MyHttpRequest request = HttpDecoder.decodeHttp(buffer);

        // 调用用户自定义处理逻辑
//        consumer.accept(model);
        ctx.next(request);
    }
}
