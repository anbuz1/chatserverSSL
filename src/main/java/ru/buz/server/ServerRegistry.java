package ru.buz.server;

import java.util.Collection;

public interface ServerRegistry {
    Server getFreeServer();

    Server getServerByName(String name);

    Collection<Server> getAllServers();
}
