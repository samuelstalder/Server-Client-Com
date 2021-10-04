package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;
import ch.zhaw.pm2.multichat.protocol.Config;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class is a controller
 * Manages communication between view and model
 */
public class ChatWindowController {

    private static final Logger LOGGER = Logger.getLogger(ChatWindowController.class.getName());
    private final Pattern messagePattern = Pattern.compile("^(?:@(\\w*))?\\s*(.*)$");

    private ClientConnectionHandler connectionHandler;
    ExecutorService executorService;
    Thread threadReceive;

    private WindowCloseHandler windowCloseHandler = new WindowCloseHandler();

    @FXML
    private Pane rootPane;
    @FXML
    private TextField serverAddressField;
    @FXML
    private TextField serverPortField;
    @FXML
    private TextField userNameField;
    @FXML
    private TextField messageField;
    @FXML
    private TextArea messageArea;
    @FXML
    private Button connectButton;
    @FXML
    private Button sendButton;

    public ChatWindowController() {
        LOGGER.setLevel(Level.ALL);
    }

    /**
     * Method sets default host and port in UI
     */
    @FXML
    private void initialize() {
        serverAddressField.setText(NetworkHandler.DEFAULT_ADDRESS.getCanonicalHostName());
        serverPortField.setText(String.valueOf(NetworkHandler.DEFAULT_PORT));
        stateChanged(Config.State.NEW);
    }

    private void applicationClose() {
        connectionHandler.setState(Config.State.DISCONNECTED);
    }

    @FXML
    private void toggleConnection() {
        if (connectionHandler == null || connectionHandler.getState() != Config.State.CONNECTED) {
            connect();
        } else {
            disconnect();
        }
    }

    private void connect() {
        if (isConnectionInputCorrect()) {
            try {
                startConnectionHandler();
                connectionHandler.connect();
            } catch (ChatProtocolException | IOException e) {
                writeError(e.getMessage());
            }
        }
    }

    private void disconnect() {
        if (connectionHandler == null) {
            writeError("No connection handler");
            return;
        }
        try {
            connectionHandler.disconnect();
        } catch (ChatProtocolException e) {
            writeError(e.getMessage());
        }
    }

    @FXML
    private void message() {
        if (connectionHandler == null) {
            writeError("No connection handler, please connect first");
            return;
        }
        String messageString = messageField.getText().strip();
        Matcher matcher = messagePattern.matcher(messageString);
        if (matcher.find()) {
            String receiver = matcher.group(1);
            String message = matcher.group(2);
            if (receiver == null || receiver.isBlank()) receiver = Config.USER_ALL;
            try {
                connectionHandler.message(receiver, message);
            } catch (ChatProtocolException e) {
                writeError(e.getMessage());
            }
        } else {
            writeError("Not a valid message format.");
        }
    }

    private void startConnectionHandler() throws IOException {
        String userName = userNameField.getText();
        String serverAddress = serverAddressField.getText();
        if (serverPortField.getText().equals("")) {
            serverPortField.setText(String.valueOf(NetworkHandler.DEFAULT_PORT));
        }
        int serverPort = Integer.parseInt(serverPortField.getText());
        executorService = Executors.newSingleThreadExecutor();
        connectionHandler = new ClientConnectionHandler(
                NetworkHandler.openConnection(serverAddress, serverPort), userName, this);
        threadReceive = connectionHandler.threadReceiving();
        executorService.submit(threadReceive);
        // register window close handler
        rootPane.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseHandler);
    }

    private void terminateConnectionHandler() {
        // unregister window close handler
        executorService.shutdownNow();
        rootPane.getScene().getWindow().removeEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseHandler);
        if (connectionHandler != null) {
            connectionHandler.stopReceiving();
            connectionHandler = null;
        }
    }

    /**
     * Method toggles connection state. Connected <-> Disconnected
     *
     * @param newState
     */
    public void stateChanged(Config.State newState) {
        // update UI (need to be run in UI thread: see Platform.runLater())
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                connectButton.setText((newState == Config.State.CONNECTED || newState == Config.State.CONFIRM_DISCONNECT) ? "Disconnect" : "Connect");
            }
        });
        if (newState == Config.State.DISCONNECTED) {
            terminateConnectionHandler();
        }
    }

    /**
     * Method sets the username in UI
     *
     * @param userName
     */
    public void setUserName(String userName) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userNameField.setText(userName);
            }
        });
    }

    /**
     * Method sets server address in UI
     *
     * @param serverAddress
     */
    public void setServerAddress(String serverAddress) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverAddressField.setText(serverAddress);
            }
        });
    }

    /**
     * Method sets server port in UI
     *
     * @param serverPort
     */
    public void setServerPort(int serverPort) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverPortField.setText(Integer.toString(serverPort));
            }
        });
    }

    /**
     * Method writes  error messages to the message area in UI
     *
     * @param message
     */
    public void writeError(String message) {
        this.messageArea.appendText(String.format("[ERROR] %s\n", message));
    }

    /**
     * Method writes info messages to the message are in UI
     *
     * @param message
     */
    public void writeInfo(String message) {
        this.messageArea.appendText(String.format("[INFO] %s\n", message));
    }

    /**
     * Method writes received messages to the message area in UI
     *
     * @param sender
     * @param reciever * = all
     * @param payload  message
     */
    public void writeMessage(String sender, String reciever, String payload) {
        this.messageArea.appendText(String.format("[%s -> %s] %s\n", sender, reciever, payload));
    }

    private boolean isConnectionInputCorrect() {
        boolean isCorrect = true;
        String regexName = "[a-zA-Z]*$";
        String regexAddress = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
        String regexPort = "[0-6][0-5][0-5][0-4][0-5]";
        if (!(Pattern.matches(regexName, userNameField.getText()) || userNameField.getText().equals(""))) {
            LOGGER.log(Level.WARNING, "Name is not allowed");
            writeInfo("Name is not allowed");
            isCorrect = false;
        }
        if (!Pattern.matches(regexAddress, serverAddressField.getText()) || serverAddressField.getText().equals("")) {
            LOGGER.log(Level.WARNING, "Address is not allowed");
            writeInfo("Address is not allowed");
            isCorrect = false;
        }
        if (!Pattern.matches(regexPort, serverPortField.getText()) || serverPortField.getText().equals("")) {
            LOGGER.log(Level.WARNING, "Port is not allowed");
            writeInfo("Port is not allowed");
            isCorrect = false;
        }
        return isCorrect;
    }

    class WindowCloseHandler implements EventHandler<WindowEvent> {
        public void handle(WindowEvent event) {
            applicationClose();
        }

    }

}
