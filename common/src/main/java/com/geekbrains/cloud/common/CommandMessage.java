package com.geekbrains.cloud.common;

public class CommandMessage extends AbstractMessage{
    public CommandMessage(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }



    private String command;

}
