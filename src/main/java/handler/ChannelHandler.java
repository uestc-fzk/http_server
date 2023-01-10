package handler;

import java.io.IOException;

/**
 * 通道事件处理器接口
 *
 * @author fzk
 * @datetime 2023-01-09 19:42:17
 */
public interface ChannelHandler<T> {
     void ChannelRead(T t,   ChannelContext ctx) throws IOException;
//    void ChannelWrite(T t);
}
