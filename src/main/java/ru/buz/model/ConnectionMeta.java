package ru.buz.model;

import javax.crypto.Cipher;
import java.time.LocalDateTime;

public class ConnectionMeta {

    private final LocalDateTime startConnectionTime;
    private final Client client;
    private boolean isAuthorize;
    private boolean isConnectionEstablish;
    private int partOfSSL = 1;
    private Cipher EncServerCipher;
    private Cipher DecServerCipher;
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

    public boolean isConnectionEstablish() {
        return isConnectionEstablish;
    }

    public void setConnectionEstablish(boolean connectionEstablish) {
        isConnectionEstablish = connectionEstablish;
    }

    public int getPartOfSSL() {
        return partOfSSL;
    }

    public void setPartOfSSL(int partOfSSL) {
        this.partOfSSL = partOfSSL;
    }

    public Cipher getEncServerCipher() {
        return EncServerCipher;
    }

    public void setEncServerCipher(Cipher encServerCipher) {
        this.EncServerCipher = encServerCipher;
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


    public Cipher getDecServerCipher() {
        return DecServerCipher;
    }

    public void setDecServerCipher(Cipher decServerCipher) {
        DecServerCipher = decServerCipher;
    }
}
