package com.geekbrains.cloud.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.List;

public class NettyServer {
    private static final Logger LOGGER = LogManager.getLogger(NettyServer.class);
    private static Connection connection;
    private static Statement statement;
    private List<UserEntry> userEntries;


    public NettyServer() {
        String url = "jdbc:mysql://localhost:3306/cloud_db?serverTimezone=UTC";
        String username = "root";
        String password = "123456";
        System.out.println("Connecting...");

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("Connection successful!");
            LOGGER.info("База данных подключена");
            statement = connection.createStatement();
            LOGGER.info("Сервис аутентификации запущен");


            /* CREATE DATABASE  IF NOT EXISTS `cloud_db`;
               USE `cloud_db`;
               DROP TABLE `user`;
               CREATE TABLE `user` ( `id` INT UNSIGNED NOT NULL AUTO_INCREMENT, `name` VARCHAR(45) NOT NULL,   `pass` VARCHAR(45) NULL, PRIMARY KEY (`id`));
               INSERT INTO `user` (`name`, `pass`) VALUES ('user1', 'pass');
               INSERT INTO `user` (`name`, `pass`) VALUES ('user2', 'pass');
               INSERT INTO `user` (`name`, `pass`) VALUES ('user3', 'pass');*/

            ResultSet resultSet = statement.executeQuery("SELECT id, name, pass FROM user;");
            try {
                while (resultSet.next()) {
                    LOGGER.debug("пользователь id = "+resultSet.getInt("id")+" name = "+resultSet.getString("name")+" pass = "+resultSet.getString("pass"));
                    UserEntry user = new UserEntry(resultSet.getInt("id"),resultSet.getString("name"),resultSet.getString("pass"));
                    System.out.println(user.getName().toString());
                    LOGGER.debug("пользователь {} добавлен в список зарегистрированных",resultSet.getString("name"));
                }
            } catch (SQLException e) {
                LOGGER.error("Проблема с SQL",e);
            }

        } catch (SQLException e) {

            LOGGER.error("Сбой подключения",e);
        }


        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(100*1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new SerialHandler()
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(8189).sync();
            LOGGER.info("Сервер запущен!");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("Проблемы запуска сервера",e);
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {

        new NettyServer();
    }
}
