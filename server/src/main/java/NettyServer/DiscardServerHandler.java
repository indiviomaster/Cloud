package NettyServer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.List;


public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
    private static int count =0;
    @Override

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {

        cause.printStackTrace();
        ctx.close();
    }

    //вызывается при получении данных
    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
        try {

            //((ByteBuf) msg).release(); //сброс сообщения
            count++;
            System.out.println("msg incoming"+count);
            ctx.write(msg); //эхо
            ctx.flush();
            System.out.println("msg send"+count);



            //обработка входящего сообщения


            System.out.println(msg.getClass());

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}