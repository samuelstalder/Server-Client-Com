package ch.zhaw.pm2.multichat.client;

import javafx.application.Application;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Class launches main method to initialize logManager and start UI
 */
public class Client {
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    public static void main(String[] args) {
        // Initialize LogManager: must only be done once at application startup
        try {
            InputStream config = Client.class.getClassLoader().getResourceAsStream("log.properties");
            LogManager.getLogManager().readConfiguration(config);
        } catch (IOException e) {

            LOGGER.log(Level.CONFIG, "No log.properties", e);

        }
        // Start UI
        LOGGER.info("Starting Client Application");
        Application.launch(ClientUI.class, args);
        LOGGER.info("Client Application ended");
    }
}

