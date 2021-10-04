package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Config;
import ch.zhaw.pm2.multichat.protocol.ConnectionHandler;
import ch.zhaw.pm2.multichat.protocol.Data;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handles connection und communication with the server
 */
public class ClientConnectionHandler extends ConnectionHandler {
    private static final Logger LOGGER = Logger.getLogger(ClientConnectionHandler.class.getName());
    private final ChatWindowController controller;

    public ClientConnectionHandler(NetworkHandler.NetworkConnection<Data> connection,
                                   String userName,
                                   ChatWindowController controller) {
        super(connection, userName);
        this.controller = controller;
    }


    /**
     * Method sets one of the given connection states
     * updates connection state UI
     *
     * @param newState
     */
    @Override
    public void setState(Config.State newState) {
        super.setState(newState);
        controller.stateChanged(newState);
    }

    /**
     * Methods returns a working thread who listen for new messages from the server
     *
     * @return Thread
     */
    public Thread threadReceiving() {
        Thread thread;
        return thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Starting Connection Handler");
                try {
                    LOGGER.log(Level.FINE, "Start receiving data...");
                    while (getConnection().isAvailable()) {
                        Data data = getConnection().receive();
                        processData(data);
                    }
                    LOGGER.log(Level.FINE, "Stopped receiving data");
                } catch (SocketException e) {
                    LOGGER.log(Level.FINE, "Connection terminated locally");
                    setState(Config.State.DISCONNECTED);
                    LOGGER.log(Level.WARNING, "Unregistered because connection terminated" + e.getMessage(), e);
                } catch (EOFException e) {
                    LOGGER.log(Level.FINE, "Connection terminated by remote");
                    setState(Config.State.DISCONNECTED);

                    LOGGER.log(Level.WARNING, "Unregistered because connection terminated" + e.getMessage(), e);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Communication error" + e, e);
                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.WARNING, "Received object of unknown type" + e.getMessage(), e);

                }
                LOGGER.log(Level.WARNING, "Stopped Connection Handler");
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
                throw new ChatProtocolException("No Sender found");
            }
            if (receiver == null) {
                throw new ChatProtocolException("No receiver found");
            }
            if (type == null) {
                throw new ChatProtocolException("No Type found");
            }
            // dispatch operation based on type parameter
            if (type.equals(Config.DATA_TYPE_CONNECT)) {
                LOGGER.log(Level.FINE, "Illegal connect request from server");
            } else if (type.equals(Config.DATA_TYPE_CONFIRM)) {
                if (getState() == Config.State.CONFIRM_CONNECT) {
                    controller.setUserName(getUserName());
                    controller.setServerPort(getConnection().getRemotePort());
                    controller.setServerAddress(getConnection().getRemoteHost());
                    controller.writeInfo(payload);
                    LOGGER.log(Level.FINE, "CONFIRM: " + payload);
                    this.setState(Config.State.CONNECTED);
                } else if (getState() == Config.State.CONFIRM_DISCONNECT) {
                    controller.writeInfo(payload);
                    LOGGER.log(Level.FINE, "CONFIRM: " + payload);
                    this.setState(Config.State.DISCONNECTED);
                } else {
                    LOGGER.log(Level.FINE, "Got unexpected confirm message: " + payload);
                }
            } else if (type.equals(Config.DATA_TYPE_DISCONNECT)) {
                if (getState() == Config.State.DISCONNECTED) {
                    LOGGER.log(Level.FINE, "DISCONNECT: Already in disconnected: " + payload);
                    return;
                }
                controller.writeInfo(payload);
                LOGGER.log(Level.FINE, "DISCONNECT: " + payload);
                this.setState(Config.State.DISCONNECTED);
            } else if (type.equals(Config.DATA_TYPE_MESSAGE)) {
                if (getState() != Config.State.CONNECTED) {
                    LOGGER.log(Level.FINE, "MESSAGE: Illegal state " + getState() + " for message: " + payload);
                    return;
                }
                controller.writeMessage(sender, receiver, payload);

                LOGGER.log(Level.FINE, "MESSAGE: From " + sender + " to " + receiver + ": " + payload);

            } else if (type.equals(Config.DATA_TYPE_ERROR)) {
                controller.writeError(payload);
                LOGGER.log(Level.FINE, "ERROR: " + payload);
            } else {
                LOGGER.log(Level.FINE, "Unknown data type received: " + type);
            }
        } catch (ChatProtocolException e) {
            LOGGER.log(Level.FINE, "Error while processing data: " + e.getMessage(), e);
            sendData(Config.USER_NONE, getUserName(), Config.DATA_TYPE_ERROR, e.getMessage());
        }
    }

    /**
     * Methods sends username, server address and server port to the server for a stable connection
     * updates connection state in UI
     *
     * @throws ChatProtocolException
     */
    public void connect() throws ChatProtocolException {

        if (getState() != Config.State.NEW) {
            throw new ChatProtocolException("Illegal state for connect: " + getState());
        }
        this.sendData(getUserName(), Config.USER_NONE, Config.DATA_TYPE_CONNECT, null);

        this.setState(Config.State.CONFIRM_CONNECT);
    }

    /**
     * Methods send information to the server to disconnect a certain user
     *
     * @throws ChatProtocolException
     */
    public void disconnect() throws ChatProtocolException {

        if (getState() != Config.State.NEW && getState() != Config.State.CONNECTED) {
            throw new ChatProtocolException("Illegal state for disconnect: " + getState());
        }
        this.sendData(getUserName(), Config.USER_NONE, Config.DATA_TYPE_DISCONNECT, null);

        this.setState(Config.State.CONFIRM_DISCONNECT);
    }

    /**
     * Methods sends received message information to the controller -> writes message to message area
     *
     * @param receiver * = all
     * @param message  = payload
     * @throws ChatProtocolException
     */
    public void message(String receiver, String message) throws ChatProtocolException {

        if (getState() != Config.State.CONNECTED) {
            throw new ChatProtocolException("Illegal state for message: " + getState());
        }
        this.sendData(getUserName(), receiver, Config.DATA_TYPE_MESSAGE, message);

    }

}
