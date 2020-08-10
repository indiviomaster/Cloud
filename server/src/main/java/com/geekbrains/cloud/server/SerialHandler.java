package com.geekbrains.cloud.server;

import com.geekbrains.cloud.common.CommandMessage;
import com.geekbrains.cloud.common.FileMessage;
import com.geekbrains.cloud.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;

public class SerialHandler extends ChannelInboundHandlerAdapter {
    String storagePath = "./server/src/main/resources/";
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Пришел запрос");
        if (msg instanceof FileRequest) {
            FileRequest fileRequest = (FileRequest) msg;
            if (Files.exists(Paths.get(storagePath + fileRequest.getFilename()))) {
                FileMessage fileMessage = new FileMessage(Paths.get(storagePath + fileRequest.getFilename()));
                ctx.writeAndFlush(fileMessage);
            }
        }else if (msg instanceof FileMessage) {
            System.out.println("Пришел файл");
            FileMessage fmsg = (FileMessage)msg;
            Path pin = Paths.get(storagePath+fmsg.getFilename());
            if(!Files.exists(pin))
                Files.createFile(pin);
            File fout = new File(String.valueOf(pin));
            FileOutputStream fileOutputStream = new FileOutputStream(fout);
            fileOutputStream.write(fmsg.getData());
            fileOutputStream.close();
            StringBuilder stringBuilder = new StringBuilder();
            Files.list(Paths.get(storagePath))
                    .filter(path -> !Files.isDirectory(path))
                    .map(file->file.getFileName().toString())
                    .forEach(filename->stringBuilder.append(" "+filename));
            System.out.println(String.valueOf(stringBuilder));
            CommandMessage commandMessage  = new CommandMessage("/list"+String.valueOf(stringBuilder));
            ctx.writeAndFlush(commandMessage);

        }else if( msg instanceof CommandMessage){
            String cmsg = ((CommandMessage) msg).getCommand();
            System.out.println("Пришел запрос команды");
            if(cmsg.startsWith("/list")){
            StringBuilder stringBuilder = new StringBuilder();
            Files.list(Paths.get(storagePath))
                    .filter(path -> !Files.isDirectory(path))
                    .map(file->file.getFileName().toString())
                    .forEach(filename->stringBuilder.append(" "+filename));
            System.out.println(String.valueOf(stringBuilder));
            CommandMessage commandMessage  = new CommandMessage("/list"+String.valueOf(stringBuilder));
            ctx.writeAndFlush(commandMessage);
            }else if(cmsg.startsWith("/delete")){
                StringBuilder stringBuilder = new StringBuilder();
                String[] tokens = cmsg.split("\\s");

                System.out.println("Пришло удалить файл "+tokens[1].toString());
                Path p = Paths.get(storagePath+tokens[1].toString());
                System.out.println(p);
                if(Files.exists(p)){

                    Files.delete(Paths.get(storagePath+tokens[1].toString()));
                    System.out.println("Файл"+tokens[1]+"удален");
                    Files.list(Paths.get(storagePath))
                            .filter(path -> !Files.isDirectory(path))
                            .map(file->file.getFileName().toString())
                            .forEach(filename->stringBuilder.append(" "+filename));
                    System.out.println(String.valueOf(stringBuilder));
                    CommandMessage commandMessage  = new CommandMessage("/list"+String.valueOf(stringBuilder));
                    ctx.writeAndFlush(commandMessage);
                }
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
