package example;

import config.ConfigReader;
import config.HttpServerConfig;
import core.HttpServer;
import handler.ChannelContext;
import handler.ChannelHandler;
import handler.DefaultHttpMessageConvertor;
import handler.HttpHandler;
import log.MyLogger;
import lombok.Data;
import model.MyHttpRequest;
import model.MyHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author fzk
 * @datetime 2023-01-10 14:21:37
 */
public class HttpDemo {
    public static void main(String[] args) {
        HttpServerConfig config = ConfigReader.getConfig();
        MyLogger.info(config.toString());

        try {
            new HttpServer(config, () -> {
                ArrayList<ChannelHandler> list = new ArrayList<>();
                list.add(new HttpHandler());
                list.add(new MyHttpHandler());
                return list;
            }).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class MyHttpHandler implements ChannelHandler<MyHttpRequest> {
        @Override
        public void ChannelRead(MyHttpRequest request, ChannelContext ctx) throws IOException {
            // 构造响应
            MyHttpResponse response = new MyHttpResponse(200, ctx);
            try {
                // 读取请求体
                BodyModel bodyModel = new DefaultHttpMessageConvertor<>(BodyModel.class).convert(request);
                System.out.println(request.getMethod() + " " + request.getUri() + " body=" + bodyModel);

                // 正常响应
                response.write("hello".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // 错误响应
                response.code = 500;
                String errMsg = "服务端错误: " + Arrays.toString(e.getStackTrace());
                MyLogger.severe(() -> errMsg);

                response.write(errMsg.getBytes(StandardCharsets.UTF_8));
            } finally {
                response.flush();// 必须刷入channel
                // 向后执行
                ctx.next(response);
            }
        }
    }

    @Data
    public static class BodyModel {
        private String k1;
        private int[] k2;
    }
}
