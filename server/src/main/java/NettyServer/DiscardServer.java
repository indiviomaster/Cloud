package NettyServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
public class DiscardServer {
    private int port;
    public DiscardServer ( int port) {
        this .port = port;
    }
    public void run () throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //настройки сервера
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)                                     //указание пулов потоков
                    .channel(NioServerSocketChannel.class)                      //канал нио сервер
                    .childHandler( new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel (SocketChannel ch) throws Exception {                //обработчик входящих подключений при каждом подключении
                            //Обрабатываем входящие данные
                            ch.pipeline().addLast( new DiscardServerHandler()); //в конец конвеера установить дискард
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128 )
                    .childOption(ChannelOption.SO_KEEPALIVE, true );

            ChannelFuture f = b.bind(port).sync(); //сервер слушает нужный порт и запускаем задачу
            f.channel().closeFuture().sync();  //ждем пока канал не закроется по любой причине.
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    public static void main (String[] args) throws Exception {
        int port;
        if (args.length > 0 ) {
            port = Integer.parseInt(args[ 0 ]);
        } else {
            port = 8189 ;
        }
        new DiscardServer(port).run();
    }
}