package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Config;
import ch.zhaw.pm2.multichat.protocol.ConnectionHandler;
import ch.zhaw.pm2.multichat.protocol.Data;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServerConnectionHandler extends ConnectionHandler {

    private static final Logger LOGGER = Logger.getLogger(ServerConnectionHandler.class.getName());
    private final HashMap<String, ServerConnectionHandler> connectionRegistry;

    public ServerConnectionHandler(NetworkHandler.NetworkConnection<Data> connection,
                                   HashMap<String, ServerConnectionHandler> registry) {

        super(connection);
        try {
            Objects.requireNonNull(connection);
        } catch (NullPointerException e) {
            LOGGER.log(Level.SEVERE, "Connection must not be null", e);
        }

        if (registry == null) {
            LOGGER.log(Level.SEVERE, "Registry must not be null");
            throw new NullPointerException("Registry must not be null");
        }

        this.connectionRegistry = registry;
    }

    /**
     * Methods returns a working thread who listen for new messages from the client
     *
     * @return Thread
     */
    public Thread threadReceiving() {
        Thread thread;
        return thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Starting Connection Handler for " + getUserName());
                try {
                    LOGGER.log(Level.FINE, "Start receiving data...");
                    while (getConnection().isAvailable()) {
                        Data data = getConnection().receive();
                        processData(data);
                    }
                    LOGGER.log(Level.FINE, "Stopped recieving data");
                } catch (SocketException e) {
                    LOGGER.log(Level.FINE, "Connection terminated locally");
                    connectionRegistry.remove(getUserName());
                    LOGGER.log(Level.FINE, "Unregistered because client connection terminated: " + getUserName() + " " + e.getMessage(), e);
                } catch (EOFException e) {
                    LOGGER.log(Level.FINE, "Connection terminated by remote");
                    connectionRegistry.remove(getUserName());
                    LOGGER.log(Level.FINE, "Unregistered because client connection terminated: " + getUserName() + " " + e.getMessage(), e);
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Communication error: " + e);
                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.FINE, "Received object of unknown type: " + e.getMessage(), e);
                }
                LOGGER.info("Stopping Connection Handler for " + getUserName());
            }
        });
    }

    protected void processData(Data data) {
        try {
            String sender = data.getSender();
            String receiver = data.getReceiver();
            String type = data.getType();
            String payload = data.getPayload();
            if (sender == null) {
                LOGGER.log(Level.FINE, "No Sender found");
                throw new ChatProtocolException("No Sender found");
            }
            if (receiver == null) {
                LOGGER.log(Level.FINE, "No receiver found");
                throw new ChatProtocolException("No receiver found");
            }
            if (type == null) {
                LOGGER.log(Level.FINE, "No Type found");
                throw new ChatProtocolException("No Type found");
            }

            // dispatch operation based on type parameter
            if (type.equals(Config.DATA_TYPE_CONNECT)) {

                if (this.getState() != Config.State.NEW) {
                    throw new ChatProtocolException("Illegal state for connect request: " + getState());
                }
                if (sender == null || sender.isBlank()) {
                    sender = getUserName();
                }
                if (connectionRegistry.containsKey(sender)) {
                    LOGGER.info("User name already taken: " + sender);

                    throw new ChatProtocolException("User name already taken: " + sender);
                }
                setUserName(sender);
                connectionRegistry.put(getUserName(), this);
                sendData(Config.USER_NONE, getUserName(), Config.DATA_TYPE_CONFIRM, "Registration successfull for " + getUserName());
                this.setState(Config.State.CONNECTED);
            } else if (type.equals(Config.DATA_TYPE_CONFIRM)) {
                LOGGER.log(Level.WARNING, "Not expecting to receive a CONFIRM request from client");
            } else if (type.equals(Config.DATA_TYPE_DISCONNECT)) {
                if (getState() == Config.State.DISCONNECTED) {
                    LOGGER.log(Level.FINE, "Illegal state for disconnect request: " + getState());
                    throw new ChatProtocolException("Illegal state for disconnect request: " + getState());
                }
                if (getState() == Config.State.CONNECTED) {
                    connectionRegistry.remove(getUserName());
                }
                sendData(Config.USER_NONE, getUserName(), Config.DATA_TYPE_CONFIRM, "Confirm disconnect of " + getUserName());
                this.setState(Config.State.DISCONNECTED);
                this.stopReceiving();
            } else if (type.equals(Config.DATA_TYPE_MESSAGE)) {

                if (getState() != Config.State.CONNECTED) {
                    LOGGER.log(Level.WARNING, "Illegal state for message request: " + getState());
                    throw new ChatProtocolException("Illegal state for message request: " + getState());
                }

                if (Config.USER_ALL.equals(receiver)) {
                    for (ServerConnectionHandler handler : connectionRegistry.values()) {
                        handler.sendData(sender, receiver, type, payload);
                    }
                } else {
                    ServerConnectionHandler handler = connectionRegistry.get(receiver);
                    if (handler != null) {
                        handler.sendData(sender, receiver, type, payload);
                    } else {
                        this.sendData(Config.USER_NONE, getUserName(), Config.DATA_TYPE_ERROR, "Unknown User: " + receiver);
                    }
                }
            } else if (type.equals(Config.DATA_TYPE_ERROR)) {
                LOGGER.log(Level.FINE, "Received error from client (" + sender + "): " + payload);
            } else {
                LOGGER.log(Level.FINE, "Unknown data type received: " + type);

            }

        } catch (ChatProtocolException e) {
            LOGGER.log(Level.FINE, "Error while processing data" + e.getMessage());

            sendData(Config.USER_NONE, getUserName(), Config.DATA_TYPE_ERROR, e.getMessage());
        }
    }


}
