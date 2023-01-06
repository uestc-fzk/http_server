package buf;

import java.nio.ByteBuffer;

/**
 * 零拷贝-缓冲分配器
 *
 * @author fzk
 * @datetime 2023-01-06 12:03:40
 */
public class MyBufAllocator {
    public ByteBuffer allocate(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }
}
