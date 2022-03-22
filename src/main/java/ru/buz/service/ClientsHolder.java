package ru.buz.service;

import ru.buz.model.Client;

import java.util.Optional;

public interface ClientsHolder {
    Optional<Client> getClientByKey(String key);
}
