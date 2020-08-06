package NettyServer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

// обработчик входящих сообщений
public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
    private static int count =0;

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {

        cause.printStackTrace();
        ctx.close();
    }

    //вызывается при получении данных, чтение сообщения
    // ctx - ссылка на контекст , канал , соединение
    // msg -то что пришло ByteBuffer
    //((ByteBuf) msg).release(); //сброс сообщения
    /* count++;
            System.out.println("msg incoming"+count);
            //((ByteBuf) msg).release() ; //
            ctx.write(msg); //эхо
            ctx.flush();
            System.out.println("msg send"+count);*/
    //обработка входящего сообщения
    //System.out.println(msg.getClass());

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg; //в стеке
        try {
            while(in.isReadable()){
                System.out.print((char)in.readByte());
            }

        } finally {
            in.release();
           // ReferenceCountUtil.release(msg);
        }
    }
}