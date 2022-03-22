package ru.buz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerProperties extends Properties {
    private static final Logger LOG = LoggerFactory.getLogger(ServerProperties.class);
    private static final String SERVER_PROPERTIES = "server.properties";
    private static final ServerProperties serverProperties = new ServerProperties();

    public static ServerProperties getServerProperties(){

        try(InputStream inputStream = ServerProperties.class.getClassLoader().getResourceAsStream(SERVER_PROPERTIES)) {
            serverProperties.load(inputStream);
            LOG.info("load property successfully");
        } catch (IOException e) {
           LOG.error(e.toString());
        }
        return serverProperties;
    }

}
