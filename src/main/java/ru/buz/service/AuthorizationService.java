package ru.buz.service;


import ru.buz.model.Client;

import java.util.Optional;

public interface AuthorizationService {
    Optional<Client> authorizeOnServer(String clientMessage);
}
