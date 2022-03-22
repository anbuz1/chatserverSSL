package ru.buz.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.buz.service.AuthorizationService;
import ru.buz.service.ServerProperties;
import java.util.Random;
import static ru.buz.service.ServerProperties.getServerProperties;

public class ServerFactoryImpl implements ServerFactory {
    private final Logger LOG = LoggerFactory.getLogger(ServerFactoryImpl.class);
    private final ServerProperties serverProperties = getServerProperties();
    private final AuthorizationService authorizationService;
    private GlobalMessageSender globalMessageSender;

    public ServerFactoryImpl(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public Server createServer() {
        int countLimit = Integer.parseInt(serverProperties.getProperty("server.channel.limit","50"));
        Server server = new Server(countLimit,generateServerName(), authorizationService, globalMessageSender);
        LOG.info("Created server {}", server.getName());
        return server;
    }

    @Override
    public void setGlobalMessageSender(GlobalMessageSender globalMessageSender) {
        this.globalMessageSender = globalMessageSender;
    }

    private String generateServerName() {
        char[] array = new char[8];
        int rand;
        Random r = new Random();
        for (int i = 0; i < 8; i++) {
            rand = r.nextInt(127) + 1; //тут менять нужные диапазоны ((max - min) + 1) + min (см ASCII)
            array[i] = (char) rand;
        }
        return "Server[" + String.copyValueOf(array) + "]";
    }
}
