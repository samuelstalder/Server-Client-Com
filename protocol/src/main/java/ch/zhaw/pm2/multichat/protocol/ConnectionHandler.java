package ch.zhaw.pm2.multichat.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class provides fundamental communication methods for client and user
 */
public abstract class ConnectionHandler {

    private static final Logger LOGGER = Logger.getLogger(ConnectionHandler.class.getName());
    private final NetworkHandler.NetworkConnection<Data> connection;
    private Config.State state = Config.State.NEW;
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);
    private final int connectionId = connectionCounter.incrementAndGet();
    private String userName = Config.USER_NONE;

    /**
     * Constructor for client based connectionHandler
     *
     * @param connection
     * @param userName
     */
    public ConnectionHandler(NetworkHandler.NetworkConnection<Data> connection, String userName)  {
        this.connection = connection;
        this.userName = this.userName = (userName == null || userName.isBlank()) ? Config.USER_NONE : userName;
    }

    /**
     * Constructor for server based connectionHandler
     *
     * @param connection
     */

    public ConnectionHandler(NetworkHandler.NetworkConnection<Data> connection)  {
        this.connection = connection;
        this.userName = "Anonymous-" + connectionId;
    }


    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {this.userName = userName;}
    protected NetworkHandler.NetworkConnection<Data> getConnection() {
        return this.connection;
    }

    public Config.State getState() {
        return this.state;
    }

    public void setState(Config.State newState) {
        this.state = newState;
    }

    /**
     * Methods returns a working thread who listen for new messages
     *
     * @return Thread
     */
    public abstract Thread threadReceiving();

    /**
     * Methods closes connection between server and client
     */
    public void stopReceiving() {
        LOGGER.info("Closing Connection Handler for " + getUserName());
        try {
            LOGGER.log(Level.FINE, "Stop receiving data...");
            connection.close();
            LOGGER.log(Level.FINE, "Stopped receiving data.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to close connection." + e.getMessage());
        }
        LOGGER.info("Closed Connection Handler for " + getUserName());
    }

    protected abstract void processData(Data data);

    /**
     * Methods sends all types of messages between server and client
     *
     * @param sender   certain user
     * @param receiver certain user or * (all)
     * @param type
     * @param payload  message
     */
    public void sendData(String sender, String receiver, String type, String payload) {
        if (connection.isAvailable()) {
            Data data = new Data(sender, receiver, type, payload);
            try {
                connection.send(data);
            } catch (SocketException e) {
                LOGGER.log(Level.INFO,"Connection closed: " + e.getMessage(), e);
            } catch (EOFException e) {
                LOGGER.log(Level.INFO, "Connection terminated by remote", e);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Communication error: " + e.getMessage(), e);
            }
        }
    }
}
