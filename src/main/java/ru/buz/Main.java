package ru.buz;

import ru.buz.server.*;
import ru.buz.service.AuthorizationServiceImpl;

public class Main {
    public static void main(String[] args) {

        ServerRegistryImpl serverRegistry = new ServerRegistryImpl(new ServerFactoryImpl(new AuthorizationServiceImpl()));
        GlobalMessageSender globalMessageSender = new GlobalMessageSenderImpl(serverRegistry);
        serverRegistry.setGlobalMessageSender(globalMessageSender);
        ServerBalancer serverBalancer = new ServerBalancer(serverRegistry);
        serverBalancer.start();


    }
}
