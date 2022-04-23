package ru.buz.server.crypto;

import javax.crypto.KeyGenerator;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class CryptoMessageImpl implements CryptoMessage {
    @Override
    public String crypt(String message) {
        KeyGenerator keygen = null;
        try {
            keygen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keygen.init(256);
        Key key = keygen.generateKey();
        return "";
    }

    @Override
    public String decrypt(String message) {
        return null;
    }
}
