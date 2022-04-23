package ru.buz.service;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import ru.buz.model.Client;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ClientsHolderImpl implements ClientsHolder {
    private static final ClientsHolder clientsHolder = new ClientsHolderImpl();
    private final Logger LOG = Logger.getLogger(ClientsHolderImpl.class);

    List<Client> clientList;

    private ClientsHolderImpl() {
        clientList = getClientListFromFile();
    }

    private List<Client> getClientListFromFile() {
        Gson gson = new Gson();
        List<Client> clientList = new ArrayList<>();

        try(InputStream inputStream = ServerProperties.class.getClassLoader().getResourceAsStream("Clients.json");
            ByteArrayOutputStream result = new ByteArrayOutputStream()
        ) {
            assert inputStream != null;

            result.write(inputStream.readAllBytes());
            String clientsJSON = result.toString(StandardCharsets.UTF_8);
            Client[] clients = gson.fromJson(clientsJSON,Client[].class);
            clientList.addAll(Arrays.asList(clients));

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return clientList;
    }

    @Override
    public Optional<Client> getClientByKey(String key) {
        return clientList.stream().filter(client -> client.getPersonalKey().equals(key)).findFirst();
    }


    public static ClientsHolder getInstance() {
        return clientsHolder;
    }
}
