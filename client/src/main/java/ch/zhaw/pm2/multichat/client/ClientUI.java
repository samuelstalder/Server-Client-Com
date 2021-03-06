package ch.zhaw.pm2.multichat.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class extends Application from JavaFX and launches FXML
 */
public class ClientUI extends Application {
    private static final Logger LOGGER = Logger.getLogger(ClientUI.class.getName());

    @Override
    public void start(Stage primaryStage) {
        chatWindow(primaryStage);
    }

    private void chatWindow(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatWindow.fxml"));
            Pane rootPane = loader.load();
            // fill in scene and stage setup
            Scene scene = new Scene(rootPane);
            //scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            // configure and show stage
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(420);
            primaryStage.setMinHeight(250);
            primaryStage.setTitle("Multichat Client");
            primaryStage.show();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting up UI" + e.getMessage());
        }
    }
}
