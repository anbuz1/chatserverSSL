package ru.buz.server;

public interface ServerFactory {
    Server createServer();
    void setGlobalMessageSender(GlobalMessageSender globalMessageSender);
}