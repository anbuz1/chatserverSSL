package ru.buz.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.buz.service.ServerProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static ru.buz.service.ServerProperties.getServerProperties;


public class ServerBalancer {

    private final Logger LOG = LoggerFactory.getLogger(ServerBalancer.class);
    private final int SERVER_PORT;
    private final ServerRegistry serverRegistry;
    private boolean stopTheWork;

    public ServerBalancer( ServerRegistry serverRegistry) {
        ServerProperties serverProperties = getServerProperties();
        SERVER_PORT = Integer.parseInt(serverProperties.getProperty("server.port","8080"));
        this.serverRegistry = serverRegistry;
    }

    public void start() {

        try (var serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(SERVER_PORT));

            try (var selector = Selector.open()) {
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                while (!stopTheWork) {
                    LOG.info("waiting for client connection");
                    selector.select(this::performIO);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void stopTheWork(boolean stopTheWork) {
        this.stopTheWork = stopTheWork;
    }

    private void performIO(SelectionKey selectedKey) {
        try {
            LOG.info("something happened, key:{}", selectedKey);
            SocketChannel socketChannel = acceptConnection(selectedKey);
            registerChannelOnServer(socketChannel);

        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        }
    }

    private void registerChannelOnServer(SocketChannel socketChannel) {
        Server freeServer = serverRegistry.getFreeServer();
        freeServer.channelRegistration(socketChannel);
    }


    private SocketChannel acceptConnection(SelectionKey key) throws IOException {
        Selector selector = key.selector();
        LOG.info("accept client connection, key:{}, selector:{}", key, selector);
        // selector=sun.nio.ch.EPollSelectorImpl - Linux epoll based Selector implementation
        var serverSocketChannel = (ServerSocketChannel) key.channel();
        var socketChannel = serverSocketChannel.accept(); //The socket channel for the new connection

        socketChannel.configureBlocking(false);
//        socketChannel.register(selector, SelectionKey.OP_READ);
//        LOG.info("socketChannel:{}", socketChannel);
        return socketChannel;
    }


}


