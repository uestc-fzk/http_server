package model;

import codec.HttpEncoder;
import core.SubReactor;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 对MyHttpResponse的包装，使其具有发送能力
 *
 * @author fzk
 * @datetime 2023-01-06 16:42:50
 */
public class MyResponseWrapper implements Closeable {
    private volatile boolean isClosed = false;
    public final MyHttpResponse response;
    public final SelectionKey key;
    public final SocketChannel cliChannel;
    public final ByteBuffer buffer_body;// 缓冲区
    private final HttpEncoder encoder;

    public MyResponseWrapper(MyHttpResponse response, SelectionKey key, SocketChannel cliChannel, ByteBuffer buffer, HttpEncoder encoder) {
        this.response = response;
        this.key = key;
        this.cliChannel = cliChannel;
        this.buffer_body = buffer;
        this.encoder = encoder;
    }

    /**
     * 写到内存缓冲区
     *
     * @return 写入内存缓冲区的字节数
     */
    public int write(byte[] buf) {
        buffer_body.put(buf, buffer_body.position(), buf.length);
        return buf.length;
    }

    /**
     * 将缓冲区数据写入channel
     *
     * @return 从缓冲区写入channel的字节数
     */
    public int flush() throws IOException {
        buffer_body.put((byte)'\n');// 加个换行符表示响应体结束捏
        buffer_body.flip();// 翻转待读
        int count = buffer_body.remaining();
        response.setBodyData(buffer_body);

        // 编码响应行/头
        ByteBuffer buffer_header = encoder.encode(response);
        // 将响应行/头/体写入channel
        cliChannel.write(new ByteBuffer[]{buffer_header, buffer_body});
        buffer_body.clear();
        buffer_header.clear();
        return count;
    }

    /**
     * 关闭此响应，目的在于将channel重新注册回去
     */
    public void close() {
        if (isClosed) return;
        isClosed = true;
        SubReactor subReactor = (SubReactor) key.attachment();
        subReactor.updateInterestOps(key, SelectionKey.OP_READ);
    }
}
