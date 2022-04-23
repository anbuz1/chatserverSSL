package ru.buz.service;


import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerProperties extends Properties {
    private static final Logger LOG = Logger.getLogger(ServerProperties.class);
    private static final String SERVER_PROPERTIES = "server.properties";
    private static final ServerProperties serverProperties = new ServerProperties();

    public static ServerProperties getServerProperties() {
        if (serverProperties.size() == 0) {
            try (InputStream inputStream = ServerProperties.class.getClassLoader().getResourceAsStream(SERVER_PROPERTIES)) {
                serverProperties.load(inputStream);
                LOG.info("load property successfully");
            } catch (IOException e) {
                LOG.error(e.toString());
            }
        }
        return serverProperties;
    }
}

