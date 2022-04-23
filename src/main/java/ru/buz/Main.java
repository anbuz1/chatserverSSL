package ru.buz;

import ru.buz.server.*;
import ru.buz.service.AuthorizationServiceImpl;
import ru.buz.service.ClientsHolderImpl;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

//        ServerRegistryImpl serverRegistry = new ServerRegistryImpl(new ServerFactoryImpl(new AuthorizationServiceImpl()));
//        GlobalMessageSender globalMessageSender = new GlobalMessageSenderImpl(serverRegistry);
//        serverRegistry.setGlobalMessageSender(globalMessageSender);
//        ServerBalancer serverBalancer = new ServerBalancer(serverRegistry);
//        serverBalancer.start();

       new Server(new AuthorizationServiceImpl(), ClientsHolderImpl.getInstance()).start();
    }
}
