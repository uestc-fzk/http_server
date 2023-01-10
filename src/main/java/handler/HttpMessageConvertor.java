package handler;

import model.MyHttpRequest;

import java.io.IOException;

/**
 * HTTP消息转换器
 * 根据Content-type进行请求体消息转换
 *
 * @author fzk
 * @datetime 2023-01-10 14:50:19
 */
public interface HttpMessageConvertor<T> {
    public T convert(MyHttpRequest request) throws IOException;
}
