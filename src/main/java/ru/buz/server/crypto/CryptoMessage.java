package ru.buz.server.crypto;

public interface CryptoMessage {
    String crypt(String message);
    String decrypt(String message);
}
