package handler;

import core.SubReactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author fzk
 * @datetime 2023-01-10 11:58:06
 */

public class ChannelContext {
    public final SocketChannel socketChannel;
    public final SelectionKey key;
    private final  ChannelHandler[] handlers;
    private int index=0;// 下次执行handler

    public ChannelContext(SocketChannel socketChannel, SelectionKey key, ChannelHandler[] handlers) {
        this.socketChannel = socketChannel;
        this.key = key;
        this.handlers = handlers;
    }

    public void next(Object nextArg) throws IOException {
        // 执行到末尾了
        if (index == handlers.length) {
            registerBack();
        }else{
            // 向后执行
            handlers[index++].ChannelRead(nextArg,this);
        }
    }

    /**
     * 目的在于将channel重新注册回去
     */
    public void registerBack() {
        SubReactor subReactor = (SubReactor) key.attachment();
        subReactor.updateInterestOps(key, SelectionKey.OP_READ);
    }
}
