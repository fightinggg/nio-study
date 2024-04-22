package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {
    public static void main(String[] args) throws IOException {
        final int port = 9999;

        ServerSocketChannel ssChannel = ServerSocketChannel.open();// 1、获取通道
        ssChannel.configureBlocking(false);// 2、切换为非阻塞模式
        ssChannel.bind(new InetSocketAddress(port));// 3、绑定连接的端口
        Selector selector = Selector.open();// 4、获取选择器Selector
        ssChannel.register(selector, SelectionKey.OP_ACCEPT);// 5、将通道都注册到选择器上去，并且开始指定监听接收事件

        System.out.println("监听线程:" + Thread.currentThread().getName());
        while (selector.select() > 0) {
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            iterator.forEachRemaining(sk -> {// 8、开始遍历这些准备好的事件
                processSelect(sk, ssChannel, selector);
                iterator.remove();
            });
        }
    }

    private static void processSelect(SelectionKey sk, ServerSocketChannel ssChannel, Selector selector) {
        if (sk.isAcceptable()) {// 9、判断这个事件具体是什么
            try {
                SocketChannel schannel = ssChannel.accept();// 10、直接获取当前接入的客户端通道
                schannel.configureBlocking(false);// 11 、切换成非阻塞模式
                System.out.println(schannel.getRemoteAddress() + " 上线 ");// 12、将本客户端通道注册到选择器
                schannel.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (sk.isReadable()) {
            SocketChannel channel = null;
            try {
                channel = (SocketChannel) sk.channel(); //得到channel
                ByteBuffer buffer = ByteBuffer.allocate(1024);//创建buffer
                int read = channel.read(buffer);
                if (read > 0) {//根据count的值做处理
                    String msg = new String(buffer.array(), 0, read);//把缓存区的数据转成字符串
                    System.out.println("来自客户端---> " + msg);//输出该消息
                    //转发消息给其它客户(通道)
                    for (SelectionKey key : selector.keys()) {
                        Channel targetChannel = key.channel();//通过 key  取出对应的 SocketChannel
                        if (targetChannel instanceof SocketChannel && targetChannel != channel) {//排除自己
                            ((SocketChannel) targetChannel).write(ByteBuffer.wrap(msg.getBytes()));//将msg 存储到buffer 将buffer 的数据写入 通道
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    System.out.println(channel.getRemoteAddress() + " 离线了..");
                    e.printStackTrace();
                    //取消注册
                    sk.cancel();
                    //关闭通道
                    channel.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }

    }
}

