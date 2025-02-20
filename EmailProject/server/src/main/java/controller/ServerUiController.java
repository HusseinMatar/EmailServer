package main.java.controller;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import main.java.network.ServerSocketHandler;
public class ServerUiController {
    @FXML private ListView<String> logListView;
    @FXML private Button startServerButton;
    @FXML private Button stopServerButton;

    private ServerSocketHandler serverSocketHandler;
    private ObservableList<String> logMessages;

    public void initialize() {
        logMessages = FXCollections.observableArrayList();
        logListView.setItems(logMessages);
    }

    @FXML
    private void startServer() {
        logMessages.add("Starting server...");
        serverSocketHandler = new ServerSocketHandler(this);
        new Thread(serverSocketHandler::startServer).start();
        startServerButton.setDisable(true);
        stopServerButton.setDisable(false);
    }

    @FXML
    private void stopServer() {
        logMessages.add("Stopping server...");
        if (serverSocketHandler != null) {
            serverSocketHandler.stopServer();
        }
        startServerButton.setDisable(false);
        stopServerButton.setDisable(true);
    }

    public void logEvent(String message) {
        Platform.runLater(() -> logMessages.add(message));
    }
}
