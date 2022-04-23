package ru.buz.server;

import org.apache.log4j.Logger;
import ru.buz.model.Client;
import ru.buz.model.ConnectionMeta;
import ru.buz.model.ServerClient;
import ru.buz.server.crypto.SSLConnection;
import ru.buz.service.AuthorizationService;
import ru.buz.service.ClientsHolder;
import ru.buz.service.ServerProperties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

import static ru.buz.service.ServerProperties.getServerProperties;


public class Server implements Serializable {

    private final List<SocketChannel> channelList;
    private final Logger LOG = Logger.getLogger(Server.class);
    private final AuthorizationService authorizationService;
    private final ClientsHolder clientsHolder;
    private final int SERVER_PORT;
    private int countChannels;
    private Selector selector;
    private boolean closeFlag = true;
    private boolean permissionToSend;
    private boolean stopTheWork;
    private final SSLConnection sslConnection;
    private SecretKeySpec serverAesKey;    //temp


    public Server(AuthorizationService authorizationService, ClientsHolder clientsHolder) {
        this.clientsHolder = clientsHolder;
        ServerProperties serverProperties = getServerProperties();
        SERVER_PORT = Integer.parseInt(serverProperties.getProperty("server.port", "8080"));
        this.authorizationService = authorizationService;
        this.channelList = new ArrayList<>();
        this.sslConnection = new SSLConnection();
    }

    private Server() {
        this(null, null);
    }


    public void start() {

        try (var serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(SERVER_PORT));
            try (var selector = Selector.open()) {
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                this.selector = selector;
                while (!stopTheWork) {
                    LOG.info("waiting for client connection");
                    selector.select(this::performIO);
                }
            }

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }


//    public void run() {
//
//        try (var serverSocketChannel = ServerSocketChannel.open();
//             var selector = Selector.open()) {
//            this.selector = selector;
//            serverSocketChannel.configureBlocking(false);
//            serverSocketChannel.socket().bind(new InetSocketAddress(SERVER_PORT));
//            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
//            while (closeFlag) {
//                LOG.info("waiting for client connection");
//                selector.select(this::performIO);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void close() {
        try {
            selector.close();
            stopTheWork = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void channelRegistration(SocketChannel socketChannel) {
        ConnectionMeta connectionMeta = new ConnectionMeta(LocalDateTime.now(), new ServerClient());
        try {
            socketChannel.register(selector, SelectionKey.OP_READ, connectionMeta);
            LOG.info(String.format("Register channel: %s", socketChannel));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public void sendToAllClients(String message) {
        permissionToSend = false;
        if (selector.isOpen()) {
            for (SelectionKey selectionKey : selector.keys()) {
                SocketChannel channel = (SocketChannel) selectionKey.channel();
                sendResponse(channel, message);
            }
        }
        permissionToSend = true;
    }

    public boolean isPermissionToSend() {
        return permissionToSend;
    }

    private void performIO(SelectionKey selectedKey) {
        if (selectedKey.isAcceptable()) {
            LOG.info(String.format("Trying new connection: %s", selectedKey));
            acceptConnection(selectedKey);
        }
        if (selectedKey.isReadable()) {
            var connectionMeta = (ConnectionMeta) selectedKey.attachment();
            var socketChannel = (SocketChannel) selectedKey.channel();
            if (!connectionMeta.isConnectionEstablish()) {
                establishSSLConnection(socketChannel, connectionMeta);

            } else
            /*if (!connectionMeta.isAuthorize()) {
                clientAuthorization(connectionMeta,socketChannel);
            } else*/ {
                handleRequest(socketChannel, connectionMeta);
            }
        }
    }

    private void acceptConnection(SelectionKey key) {
        LOG.info(String.format("accept client connection, key: %s, selector: %s", key, selector));
        // selector=sun.nio.ch.EPollSelectorImpl - Linux epoll based Selector implementation
        try {
            var serverSocketChannel = (ServerSocketChannel) key.channel();
            var socketChannel = serverSocketChannel.accept(); //The socket channel for the new connection
            socketChannel.configureBlocking(false);
            channelRegistration(socketChannel);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleRequest(SocketChannel socketChannel, ConnectionMeta connectionMeta) {
        LOG.info(String.format("Start handle message from: %s", connectionMeta.getClient()));
        var clientMessage = requestToString(socketChannel, connectionMeta);
        // TODO: 22.03.2022 проверить не содержит ли сообщение специальный ключ
        Cipher serverCipher = connectionMeta.getEncServerCipher();

        try {

            byte[] ciphertext = serverCipher.doFinal(clientMessage.getBytes());

            socketChannel.write(ByteBuffer.wrap(ciphertext));

        } catch (IllegalBlockSizeException | BadPaddingException | IOException e) {
            LOG.error(e.getMessage());

        } catch (NullPointerException e) {
            LOG.error(e.getMessage());
            try {
                if (!socketChannel.isConnected()) {

                    LOG.info(String.format("Socket channel was down: %s", socketChannel));
                } else {
                    LOG.info(String.format("Closed Socket channel: %s", socketChannel.getLocalAddress()));
                    socketChannel.close();
                }
            } catch (IOException ioException) {
                LOG.error(e.getMessage());
            }

        }

    }

    private String prepareMessage(String clientMessage, ConnectionMeta connectionMeta) {
        String name = connectionMeta.getClient().getName();
        return name + ": " + clientMessage;
    }

    private String requestToString(SocketChannel socketChannel, ConnectionMeta connectionMeta) {
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

    private byte[] getRawBytes(SocketChannel socketChannel) {
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

    private void sendResponse(SocketChannel socketChannel, String responseForClient) {
        var buffer = ByteBuffer.allocate(50);
        var response = responseForClient.getBytes();
        try {
            for (byte b : response) {
                buffer.put(b);
                if (buffer.position() == buffer.limit()) {
                    buffer.flip();
                    socketChannel.write(buffer);
                    buffer.flip();
                }
            }
            if (buffer.hasRemaining()) {
                buffer.flip();
                socketChannel.write(buffer);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private void establishSSLConnection(SocketChannel socketChannel, ConnectionMeta connectionMeta) {
        if (connectionMeta.getPartOfSSL() == 1) {
            byte[] pubKey = sslConnection.getPubKey(getRawBytes(socketChannel));
            serverAesKey = sslConnection.getServerAesKey(); //temp
            connectionMeta.setServerAesKey(serverAesKey);
            connectionMeta.setClientPubKey(sslConnection.getClientPubKey());

            try {
                socketChannel.write(ByteBuffer.wrap(pubKey));
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
        if (connectionMeta.getPartOfSSL() == 2) {
            try {
                AlgorithmParameters aesParams = connectionMeta.getAesParams();
                aesParams.init(getRawBytes(socketChannel));
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
//                try {
//                    socketChannel.close();
//                } catch (IOException ioException) {
//                    ioException.printStackTrace();
//                }
            }catch (NullPointerException e){
             LOG.error(e.getMessage());
             LOG.info("Socket channel is down!");
            }
        }
        connectionMeta.setPartOfSSL(2);

    }


    private void clientAuthorization(ConnectionMeta connectionMeta, SocketChannel socketChannel) {
        var clientMessage = requestToString(socketChannel, connectionMeta);
        Optional<Client> client = authorizationService.authorizeOnServer(clientMessage);
        if (client.isEmpty()) {
            sendResponse(socketChannel, "connection refuse");
            LOG.info("connection refused");
            try {
                socketChannel.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
            return;
        } else {
            connectionMeta.setAuthorize(true);
            sendResponse(socketChannel, "2ffca3b083298826");
            channelList.add(socketChannel);
        }
    }
}
