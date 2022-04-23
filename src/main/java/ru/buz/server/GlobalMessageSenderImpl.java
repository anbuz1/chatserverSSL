package ru.buz.server;

public class GlobalMessageSenderImpl {

//    private final ServerRegistry serverRegistry;
//
//    public GlobalMessageSenderImpl(ServerRegistry serverRegistry) {
//        this.serverRegistry = serverRegistry;
//    }
//
//    @Override
//    public void send(String message) {
//        for (Server server : serverRegistry.getAllServers()) {
//            // TODO: 22.03.2022 сделать логику отбора недоступных серверов и отправку сообщений позднее
//            boolean wait = true;
//            while (wait){
//                wait = !server.isPermissionToSend();
//            }
//            server.sendToAllClients(message);
//        }
//
//    }
}
