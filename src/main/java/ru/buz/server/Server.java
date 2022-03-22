package ru.buz.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.buz.model.Client;
import ru.buz.model.ConnectionMeta;
import ru.buz.model.ServerClient;
import ru.buz.service.AuthorizationService;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class Server extends Thread implements Serializable {

    private final List<SocketChannel> channelList;
    private final Logger LOG = LoggerFactory.getLogger(Server.class);
    private final int countServerLimit;
    private final String serverName;
    private final AuthorizationService authorizationService;
    private final GlobalMessageSender globalMessageSender;
    private int countChannels;
    private Selector selector;
    private boolean closeFlag = true;
    private boolean permissionToSend;

    public Server(int countServerLimit, String serverName, AuthorizationService authorizationService, GlobalMessageSender globalMessageSender) {
        this.countServerLimit = countServerLimit;
        this.serverName = serverName;
        this.authorizationService = authorizationService;
        this.globalMessageSender = globalMessageSender;
        this.channelList = new ArrayList<>();

    }


    @Override
    public void run() {
        try (var selector = Selector.open()) {
            this.selector = selector;
            while (closeFlag) {
                LOG.info("waiting for client");
                selector.select(this::performIO);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            selector.close();
            closeFlag = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int serverCapacity() {
        return countServerLimit - selector.keys().size();
    }

    public void channelRegistration(SocketChannel socketChannel) {
        ConnectionMeta connectionMeta = new ConnectionMeta(LocalDateTime.now(), new ServerClient());
        try {
            socketChannel.register(selector, SelectionKey.OP_READ, connectionMeta);
            LOG.info("Register channel: {} on Server: {}", socketChannel, serverName);
            clientAuthorization(connectionMeta, socketChannel);
        } catch (ClosedChannelException e) {
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

        if (selectedKey.isReadable()) {
            var connectionMeta = (ConnectionMeta) selectedKey.attachment();
            var socketChannel = (SocketChannel) selectedKey.channel();
            if (!connectionMeta.isAuthorize()) {
                clientAuthorization(connectionMeta, socketChannel);
            } else {
                handleRequest(socketChannel,connectionMeta);
            }
        }
    }

    private void handleRequest(SocketChannel socketChannel, ConnectionMeta connectionMeta){
        var clientMessage = requestToString(socketChannel);
        // TODO: 22.03.2022 проверить не содержит ли сообщение специальный ключ

        globalMessageSender.send(prepareMessage(clientMessage,connectionMeta));

    }

    private String prepareMessage(String clientMessage, ConnectionMeta connectionMeta) {
        String name = connectionMeta.getClient().getName();
        return name + ": " + clientMessage;
    }

    private String requestToString(SocketChannel socketChannel) {
        var buffer = ByteBuffer.allocate(50);
        var inputBuffer = new StringBuilder(100);
        try {
            while (socketChannel.read(buffer) > 0) {
                buffer.flip();
                var input = StandardCharsets.UTF_8.decode(buffer).toString();
                LOG.info("from client: {} ", input);

                buffer.flip();
                inputBuffer.append(input);
            }

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        String requestFromClient = inputBuffer.toString().replace("\n", "").replace("\r", "");
        LOG.info("requestFromClient: {} ", requestFromClient);
        return requestFromClient;
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


    private void clientAuthorization(ConnectionMeta connectionMeta, SocketChannel socketChannel) {
        var clientMessage = requestToString(socketChannel);
        Optional<Client> client = authorizationService.authorizeOnServer(clientMessage);
        if (client.isEmpty()) {
            sendResponse(socketChannel, "connection refuse");
            try {
                socketChannel.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
            return;
        } else {
            connectionMeta.setAuthorize(true);
            sendResponse(socketChannel, "authorization success");
        }
    }
}
