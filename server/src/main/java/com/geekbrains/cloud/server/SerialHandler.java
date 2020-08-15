package com.geekbrains.cloud.server;

import com.geekbrains.cloud.common.CommandMessage;
import com.geekbrains.cloud.common.FileMessage;
import com.geekbrains.cloud.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class SerialHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(SerialHandler.class);
    private static String authUser="";
    String storagePath = "./ServerFiles/";
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof FileRequest) {
            FileRequest fileRequest = (FileRequest) msg;
            if (Files.exists(Paths.get(storagePath+authUser+"/" + fileRequest.getFilename()))) {
                FileMessage fileMessage = new FileMessage(Paths.get(storagePath +authUser+"/" + fileRequest.getFilename()));
                LOGGER.info("Пришел запрос на файл "+fileRequest.getFilename());
                ctx.writeAndFlush(fileMessage);
            }
        }else if (msg instanceof FileMessage) {
            FileMessage fmsg = (FileMessage)msg;
            Path pin = Paths.get(storagePath+authUser+"/"+fmsg.getFilename());
            if(!Files.exists(pin))
                Files.createFile(pin);
            File fout = new File(String.valueOf(pin));
            FileOutputStream fileOutputStream = new FileOutputStream(fout);
            fileOutputStream.write(fmsg.getData());
            fileOutputStream.close();
            LOGGER.info("Пришел файл "+fmsg.getFilename());

            StringBuilder stringBuilder = new StringBuilder();
            Files.list(Paths.get(storagePath+authUser+"/"))
                    .filter(path -> !Files.isDirectory(path))
                    .map(file->file.getFileName().toString())
                    .forEach(filename->stringBuilder.append(";"+filename));
            CommandMessage commandMessage  = new CommandMessage("/list"+String.valueOf(stringBuilder));
            ctx.writeAndFlush(commandMessage);

        }else if( msg instanceof CommandMessage){
            String cmsg = ((CommandMessage) msg).getCommand();
            if(cmsg.startsWith("/list")){
                LOGGER.info("Пришел запрос списка файлов");

                StringBuilder stringBuilder = new StringBuilder();
                if(Files.notExists(Paths.get(storagePath+authUser+"/"))){
                    Files.createDirectory(Paths.get(storagePath+authUser+"/"));
                }
                System.out.println(storagePath+authUser+"/");
                Files.list(Paths.get(storagePath+authUser+"/"))
                    .filter(path -> !Files.isDirectory(path))
                    .map(file->file.getFileName().toString())
                    .forEach(filename->stringBuilder.append(";"+filename));
                CommandMessage commandMessage  = new CommandMessage("/list"+stringBuilder);
                ctx.writeAndFlush(commandMessage);
            }else if(cmsg.startsWith("/delete")){
                StringBuilder stringBuilder = new StringBuilder();
                String[] tokens = cmsg.split(";");

                LOGGER.info("Пришел запрос удалить файл = "+tokens[1]);

                Path p = Paths.get(storagePath+authUser+"/"+tokens[1]);
                System.out.println(p);
                if(Files.exists(p)){

                    Files.delete(Paths.get(storagePath+authUser+"/"+tokens[1]));
                    LOGGER.info("Файл "+tokens[1]+" удален");

                    Files.list(Paths.get(storagePath+authUser+"/"))
                            .filter(path -> !Files.isDirectory(path))
                            .map(file->file.getFileName().toString())
                            .forEach(filename->stringBuilder.append(";"+filename));
                    CommandMessage commandMessage  = new CommandMessage("/list"+stringBuilder);
                    ctx.writeAndFlush(commandMessage);
                }
            }else if(cmsg.startsWith("/authuser")){
                String[] tokens = cmsg.split(";");


                //добавить работу с БД
                authUser = tokens[1];
                ctx.writeAndFlush(new CommandMessage("/userok"+";"+authUser));
                LOGGER.info("Клиент "+tokens[1]+" авторизован");

            }else if(cmsg.startsWith("/close")){
                ctx.close();
                LOGGER.info("Клиент отключился");
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
