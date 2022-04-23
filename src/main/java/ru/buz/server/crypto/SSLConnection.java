package ru.buz.server.crypto;

import org.apache.log4j.Logger;
import ru.buz.server.Server;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class SSLConnection {
    private final Logger LOG = Logger.getLogger(Server.class);
    private SecretKeySpec serverAesKey;
    private PublicKey clientPubKey;;


    public byte[] getPubKey(byte[] entryKey){

        KeyFactory keyFactory;
        KeyPairGenerator keyPairGen;
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(entryKey);

        try {
            keyFactory = KeyFactory.getInstance("DH");
            clientPubKey = keyFactory.generatePublic(x509KeySpec);

            assert clientPubKey != null;
            DHParameterSpec dhParamFromClientPubKey = ((DHPublicKey)clientPubKey).getParams();

            keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(dhParamFromClientPubKey);
            KeyPair serverKeyPair = keyPairGen.generateKeyPair();


            KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
            keyAgree.init(serverKeyPair.getPrivate());
            keyAgree.doPhase(clientPubKey, true);
            byte[] bobSharedSecret = keyAgree.generateSecret();

            serverAesKey = new SecretKeySpec(bobSharedSecret, 0, 16, "AES");

            return serverKeyPair.getPublic().getEncoded();


        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    public SecretKeySpec getServerAesKey() {
        return serverAesKey;
    }

    public PublicKey getClientPubKey() {
        return clientPubKey;
    }
}
