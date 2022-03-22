package ru.buz.model;

import java.time.LocalDateTime;

public class ConnectionMeta {

    private final LocalDateTime startConnectionTime;
    private final Client client;
    private boolean isAuthorize;
    private LocalDateTime lastMessageTime;

    public ConnectionMeta(LocalDateTime startConnectionTime, ServerClient client) {
        this.startConnectionTime = startConnectionTime;
        this.client = client;
    }

    public boolean isAuthorize() {
        return isAuthorize;
    }

    public void setAuthorize(boolean authorize) {
        isAuthorize = authorize;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public LocalDateTime getStartConnectionTime() {
        return startConnectionTime;
    }

    public Client getClient() {
        return client;
    }
}
