package ru.buz.service;

import org.apache.log4j.Logger;
import ru.buz.model.ConnectionMeta;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;

public class MessageHandler {

    private final Logger LOG = Logger.getLogger(MessageHandler.class);

    public byte[] getRawBytes(SocketChannel socketChannel) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var buffer = ByteBuffer.allocate(200);
            int countRead = 1;
            int totalReadByte = 0;
            while (countRead > 0) {
                countRead = socketChannel.read(buffer);
                totalReadByte += countRead;
                buffer.flip();
                outputStream.write(buffer.array());
                buffer.flip();
                buffer.clear();
            }
            byte[] tempResultBytes = outputStream.toByteArray();
            return Arrays.copyOf(tempResultBytes, totalReadByte);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            try {
                socketChannel.close();
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
            }

        }
        return null;
    }

    private String prepareMessage(String clientMessage, ConnectionMeta connectionMeta) {
        String name = connectionMeta.getClient().getName();
        return name + ": " + clientMessage;
    }

    public String requestToString(SocketChannel socketChannel, ConnectionMeta connectionMeta) {
        String result = null;
        try {

            Cipher serverCipher = connectionMeta.getDecServerCipher();
            byte[] recovered = serverCipher.doFinal(Objects.requireNonNull(getRawBytes(socketChannel)));
            result = new String(recovered);

        } catch (IllegalBlockSizeException | BadPaddingException | NullPointerException e) {
            LOG.error(e.getMessage());
            try {
                socketChannel.close();
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
            }
        }
        LOG.info("requestFromClient: " + result);
        return result;
    }
}
