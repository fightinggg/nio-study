package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        String HOST = "127.0.0.1"; // 服务器的ip
        int PORT = 9999; //服务器端口

        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));//连接服务器
        socketChannel.configureBlocking(false);//设置非阻塞
        socketChannel.register(selector, SelectionKey.OP_READ);//将channel 注册到selector

        new Thread(() -> {
            while (true) {
                try {
                    int readChannels = selector.select();
                    if (readChannels > 0) {//有可以用的通道
                        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();
                            if (key.isReadable()) {
                                SocketChannel sc = (SocketChannel) key.channel();//得到相关的通道
                                ByteBuffer buffer = ByteBuffer.allocate(1024);//得到一个Buffer
                                int read = sc.read(buffer);//读取
                                String msg = new String(buffer.array(), 0, read); //把读到的缓冲区的数据转成字符串
                                System.out.println(msg);
                            }
                        }
                        iterator.remove(); //删除当前的selectionKey, 防止重复操作
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //发送数据给服务器端
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            String username = socketChannel.getLocalAddress().toString().substring(1);
            String info = username + " 说：" + s;
            try {
                socketChannel.write(ByteBuffer.wrap(info.getBytes()));//向服务器发送消息
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

