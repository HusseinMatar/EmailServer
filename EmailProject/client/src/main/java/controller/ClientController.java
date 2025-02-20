package main.java.controller;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import main.java.model.EmailOperations;
import main.java.util.ClientUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientController {
    @FXML private Label notValidEmailLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;


    @FXML
    public void initialize() {
        // Bind the button's disabled property to the text properties of the fields
        loginButton.disableProperty().bind(
                usernameField.textProperty().isEmpty().or(
                        passwordField.textProperty().isEmpty())
        );
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if(!ClientUtils.isValidEmail(username)) {
            notValidEmailLabel.setText("Email format is invalid");
            notValidEmailLabel.setStyle("-fx-border-color: red;");
            notValidEmailLabel.setVisible(true);
        } else {
            notValidEmailLabel.setVisible(false);
            try (Socket socket = new Socket("localhost", 12345);
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                oos.writeObject(EmailOperations.LOGIN.name());
                oos.writeObject(username);
                oos.writeObject(password);
                boolean loginSuccess = (boolean) ois.readObject();
                if (loginSuccess) {
                    // Load inbox UI
                    loadInboxUI(username);
                } else {
                    usernameField.clear();
                    passwordField.clear();

                    usernameField.setStyle("-fx-border-color: red;");
                    passwordField.setStyle("-fx-border-color: red;");

                    ClientUtils.buildAlertWhenServerUpAndFails("Login Failed", "Invalid Credentials!").showAndWait();

                }
            } catch (IOException | ClassNotFoundException e) {
                ClientUtils.buildAlertWhenServerIsDown().showAndWait();
            }
        }
    }




    private void loadInboxUI(String username) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("../ui/inbox.fxml"));
                Parent inboxRoot = loader.load();

                InboxController inboxController = loader.getController();
                inboxController.setUsername(username); // Set the username for the inbox

                // Switch to the inbox scene
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(inboxRoot));
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}

