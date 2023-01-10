package core;

import log.MyLogger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MainReactor的接受者，专门监听新连接
 */
class Acceptor {
    private final Selector mainSelector;
    public static final int ACCEPT_TIMEOUT = 1000;// ms

    public Acceptor(Selector selector) {
        this.mainSelector = selector;
    }

    public List<SocketChannel> accept() throws IOException {
        ArrayList<SocketChannel> list = new ArrayList<>();
        // 接收器获取新连接并返回
        if (mainSelector.select(ACCEPT_TIMEOUT) > 0) {
            Iterator<SelectionKey> iterator = mainSelector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                if(!selectionKey.isValid()){
                    MyLogger.logger.warning("acceptor selectionKey is invalid ???");
                }
                assert selectionKey.isAcceptable();

                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) (selectionKey.channel());
                SocketChannel sc = serverSocketChannel.accept();
                sc.configureBlocking(false);
                list.add(sc);
//                sc.write(ByteBuffer.wrap("您已经连上服务器".getBytes(StandardCharsets.UTF_8)));
            }
        }
        return list;
    }
}