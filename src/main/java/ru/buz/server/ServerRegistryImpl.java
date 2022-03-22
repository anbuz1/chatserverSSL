package ru.buz.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServerRegistryImpl implements ServerRegistry {

    private final Map<String, Server> serverMap;
    private final ServerFactory serverFactory;


    public ServerRegistryImpl(ServerFactory serverFactory) {
        this.serverFactory = serverFactory;
        this.serverMap = new HashMap<>();

    }
    public void setGlobalMessageSender(GlobalMessageSender globalMessageSender) {
        serverFactory.setGlobalMessageSender(globalMessageSender);
    }


    @Override
    public Server getFreeServer() {
        if (serverMap.isEmpty()) {
            Server server = serverFactory.createServer();
            serverMap.put(server.getName(),server);
            new Thread(server).start();
            return server;
        } else {
            Optional<Server> first = getAllServers().stream().filter(server -> server.serverCapacity() > 0).findFirst();
            Server server = first.orElseGet(() -> {
                Server server1 = serverFactory.createServer();
                new Thread(server1).start();
                return server1;
            });
            serverMap.put(server.getName(),server);
            return server;
        }
    }

    @Override
    public Server getServerByName(String name) {
        return serverMap.get(name);
    }

    @Override
    public Collection<Server> getAllServers() {
        return serverMap.values();
    }
}
