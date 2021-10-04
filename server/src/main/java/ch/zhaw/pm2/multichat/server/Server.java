package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.Data;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Class launches and manages the server part
 */
public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int NUMBER_OF_SUPPORTED_USER_CONNECTION = 8;
    ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_SUPPORTED_USER_CONNECTION);
    private static final boolean ISRUNNING = true;
    private static final int SHUTDOWN_TIME = 200;

    // Server connection
    private NetworkHandler.NetworkServer<Data> networkServer;

    private Thread threadReceive;

    // Connection registry
    private HashMap<String, ServerConnectionHandler> connections = new HashMap<>();

    /**
     * Main method initializes new server with certain port
     *
     * @param args
     */
    public static void main(String[] args) {
        // Initialize LogManager: must only be done once at application startup
        try {
            InputStream config = Server.class.getClassLoader().getResourceAsStream("log.properties");
            LogManager.getLogManager().readConfiguration(config);
        } catch (IOException e) {

            LOGGER.log(Level.CONFIG, "No log.properties", e);

        }
        // Parse arguments for server port.
        try {
            int port;
            switch (args.length) {
                case 0:
                    port = NetworkHandler.DEFAULT_PORT;
                    break;
                case 1:
                    port = Integer.parseInt(args[0]);
                    break;
                default:
                    LOGGER.info("Illegal number of arguments:  [<ServerPort>]");
                    return;
            }
            // Initialize server
            final Server server = new Server(port);

            // This adds a shutdown hook running a cleanup task if the JVM is terminated (kill -HUP, Ctrl-C,...)
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(SHUTDOWN_TIME);
                        LOGGER.info("Shutdown initiated...");
                        server.terminate();
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.WARNING, "Shutdown interrupted.", e);
                    } finally {
                        LOGGER.info("Shutdown complete.");
                    }
                }
            });

            // Start server
            server.start();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while starting server." + e.getMessage(), e);
        }
    }

    /**
     * New server who listen on certain server port
     *
     * @param serverPort
     * @throws IOException
     */
    public Server(int serverPort) throws IOException {
        LOGGER.setLevel(Level.ALL);
        // Open server connection
        LOGGER.info("Create server connection");
        networkServer = NetworkHandler.createServer(serverPort);
        LOGGER.info("Listening on " + networkServer.getHostAddress() + ":" + networkServer.getHostPort());
    }

    private void start() {
        LOGGER.info("Server started.");
        try {
            while (ISRUNNING) {
                 NetworkHandler.NetworkConnection<Data> connection = networkServer.waitForConnection();
                 ServerConnectionHandler connectionHandler = new ServerConnectionHandler(connection, connections);
                threadReceive = connectionHandler.threadReceiving();
                executorService.submit(threadReceive);

                LOGGER.info(String.format("Connected new Client %s with IP:Port <%s:%d>",
                        connectionHandler.getUserName(),
                        connection.getRemoteHost(),
                        connection.getRemotePort()
                ));
            }
        } catch (SocketException e) {
            LOGGER.log(Level.FINE, "Server connection terminated");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Communication error", e);
        }
        // close server
        LOGGER.info("Server Stopped.");
    }

    /**
     * Method closes all receiving threads as well as the connection to the client
     */
    public void terminate() {
        try {
            executorService.shutdownNow();
            LOGGER.info("Close server port.");
            networkServer.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to close server connection", e);
        }
    }

}
