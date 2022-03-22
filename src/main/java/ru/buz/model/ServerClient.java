package ru.buz.model;

public class ServerClient extends Client{
    public ServerClient(String name, String personalKey) {
        super(name, personalKey);
    }

    public ServerClient() {
        super("","");
    }
}
