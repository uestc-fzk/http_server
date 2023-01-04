package codec;

import model.MyHttpRequest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * @author fzk
 * @datetime 2023-01-04 23:09
 */
public class HttpDecoder {
    //        POST /hello HTTP/1.1
    //        Content-Type: application/json
    //        User-Agent: PostmanRuntime/7.30.0
    //        Accept: */*
    //        Postman-Token: d8b2f089-d369-462a-9499-1e88342f8ab5
    //        Host: localhost:8080
    //        Accept-Encoding: gzip, deflate, br
    //        Connection: keep-alive
    //        Content-Length: 35
    //
    //        {
    //            "k1": 1,
    //            "k2": [1,2]
    //        }
    public MyHttpRequest decodeHttp(ByteBuffer buffer) throws IOException {
        MyHttpRequest request = new MyHttpRequest();
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer.array())));
        // 1.解析请求行
        String lineData = br.readLine();
        String[] lineArr = lineData.split(" ", 3);

        request.setMethod(lineArr[0]);
        request.setUri(URI.create(lineArr[1]));
        request.setVersion(lineArr[2]);

        // 2.解析请求头
        String headerData = br.readLine();
        while (headerData != null && !headerData.equals("")) {
            String[] headerArr = headerData.split(":\\s+", 2);
            request.getHeaders().put(headerArr[0], headerArr[1]);
            headerData = br.readLine();
        }
        // 第三行空行不解析，第四行请求体
        StringBuilder sb = new StringBuilder();
        String bodyData = br.readLine();
        while (bodyData != null && !bodyData.equals("")) {
            sb.append(bodyData);
            bodyData = br.readLine();
        }

        request.setBody(sb.toString());
        return request;
    }
}
