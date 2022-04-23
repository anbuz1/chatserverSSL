package ru.buz.service;

import org.apache.log4j.Logger;
import ru.buz.model.Client;

import java.util.Optional;

import static ru.buz.service.ServerProperties.getServerProperties;

public class AuthorizationServiceImpl implements AuthorizationService {
    private final Logger LOG = Logger.getLogger(AuthorizationServiceImpl.class);
    private final ServerProperties serverProperties = getServerProperties();
    private final ClientsHolder clientsHolder;
    private static final String SERVER_KEY = "3h5N!jfl09-sDvF23Kl%jk3i545";

    public AuthorizationServiceImpl() {
        clientsHolder = ClientsHolderImpl.getInstance();
    }


    @Override
    public Optional<Client> authorizeOnServer(String clientMessage) {

        var keys = clientMessage.split("<@>");
        if(!keys[0].equals(SERVER_KEY)){
            return Optional.empty();
        }
        return clientsHolder.getClientByKey(keys[1]);
    }
}
