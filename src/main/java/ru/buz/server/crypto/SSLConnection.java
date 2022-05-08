package ru.buz.server.crypto;

import org.apache.log4j.Logger;
import ru.buz.model.ConnectionMeta;
import ru.buz.server.Server;
import ru.buz.service.MessageHandler;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class SSLConnection {
    private final Logger LOG = Logger.getLogger(Server.class);
    private final MessageHandler messageHandler = new MessageHandler();
    private SecretKeySpec serverAesKey;


    private byte[] getPubKey(byte[] entryKey) {

        KeyFactory keyFactory;
        KeyPairGenerator keyPairGen;
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(entryKey);

        try {
            keyFactory = KeyFactory.getInstance("DH");
            PublicKey clientPubKey = keyFactory.generatePublic(x509KeySpec);

            assert clientPubKey != null;
            DHParameterSpec dhParamFromClientPubKey = ((DHPublicKey) clientPubKey).getParams();

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

    public void establishSSLConnection(SocketChannel socketChannel, ConnectionMeta connectionMeta) {
        int partOfSSL = connectionMeta.getPartOfSSL();

        switch (partOfSSL) {
            case 1: {
                byte[] pubKey = getPubKey(messageHandler.getRawBytes(socketChannel));
                try {
                    assert pubKey != null;
                    socketChannel.write(ByteBuffer.wrap(pubKey));
                    connectionMeta.setPartOfSSL(2);
                } catch (IOException | NullPointerException e) {
                    LOG.error(e.getMessage());
                    closeSocketChannel(socketChannel);
                }
            }
            break;
            case 2: {
                try {
                    AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
                    aesParams.init(messageHandler.getRawBytes(socketChannel));
                    Cipher encServerCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

                    try {

                        encServerCipher.init(Cipher.ENCRYPT_MODE, serverAesKey);
                        encServerCipher.doFinal("".getBytes());
                        connectionMeta.setEncServerCipher(encServerCipher);
                        byte[] encodedParamsFromServer = encServerCipher.getParameters().getEncoded();
                        socketChannel.write(ByteBuffer.wrap(encodedParamsFromServer));
                    } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
                        LOG.error(e.getMessage());
                    }

                    Cipher decServerCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    decServerCipher.init(Cipher.DECRYPT_MODE, serverAesKey, aesParams);
                    connectionMeta.setDecServerCipher(decServerCipher);
                    connectionMeta.setConnectionEstablish(true);


                    LOG.info(String.format("Connection establish: %s", socketChannel.getRemoteAddress()));
                } catch (IOException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                    LOG.error(e.getMessage());
                    LOG.info("Close socket channel");
                    closeSocketChannel(socketChannel);
                } catch (NullPointerException e) {
                    LOG.error(e.getMessage());
                    LOG.info("Socket channel is down!");
                }
            }
            break;
        }
    }

    private void closeSocketChannel(SocketChannel socketChannel) {
        try {
            LOG.info("Close socket channel");
            socketChannel.close();
        } catch (IOException ioException) {
            LOG.error(ioException.getMessage());
        }
    }
}
