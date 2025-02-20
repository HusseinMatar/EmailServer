package main.java.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.java.model.Email;
import main.java.model.EmailOperations;
import main.java.util.ClientUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NewEmailController {

    @FXML private Label notValidEmailLabel;
    @FXML private Label fromField;
    @FXML private Button sendNewEmailButton;
    @FXML private TextField recipientsField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyArea;

    private String CURRENT_SENDER;

    @FXML
    public void initialize(String sender, Email selectedEmail, boolean[] mailOperations){
        this.CURRENT_SENDER = sender;
        fromField.setText("FROM: " + sender);

        String startBody;
        if(selectedEmail != null) {
            if(mailOperations[0]) {
                subjectField.appendText("Reply: " + selectedEmail.getSubject());
                if(CURRENT_SENDER.equals(selectedEmail.getSender())) {
                    recipientsField.appendText(String.join(",", selectedEmail.getRecipients()));
                } else {
                    recipientsField.appendText(selectedEmail.getSender());
                }
                startBody = "Reply: ";
            } else if(mailOperations[1]) {
                subjectField.appendText("Reply All: " + selectedEmail.getSubject());

                if(CURRENT_SENDER.equals(selectedEmail.getSender())) {
                    // SENT EMAILS
                    recipientsField.appendText(String.join(",", selectedEmail.getRecipients()));
                } else {
                    // RECEIVED EMAILS
                    List<String> filteredRecipients = new ArrayList<>(selectedEmail.getRecipients()
                            .stream()
                            .filter(r -> !r.equals(CURRENT_SENDER))
                            .toList());
                    filteredRecipients.add(selectedEmail.getSender());

                    recipientsField.appendText(String.join(",", filteredRecipients));
                }

                startBody = "Reply All: ";
            } else {
                subjectField.appendText("Forward: " + selectedEmail.getSubject());
                startBody = "Forward: ";
            }

            bodyArea.appendText(startBody + selectedEmail.getContent());
        }

        sendNewEmailButton.disableProperty().bind(
                recipientsField.textProperty().isEmpty().or(
                        subjectField.textProperty().isEmpty()).or(
                                bodyArea.textProperty().isEmpty()
                )
        );
    }

    @FXML
    public void handleSendEmail() {

        List<String> recipients = Arrays.asList(recipientsField.getText().split(","));
        String subject = subjectField.getText();
        String body = bodyArea.getText();

        if(!ClientUtils.areValidEmails(recipients)){
            notValidEmailLabel.setText("Email format is invalid");
            notValidEmailLabel.setStyle("-fx-border-color: red;");
            notValidEmailLabel.setVisible(true);
        } else {
            notValidEmailLabel.setVisible(false);
            try(Socket socket = new Socket("localhost", 12345);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
            ) {
                oos.writeObject(EmailOperations.SEND_EMAIL.name());

                Email newEmail = new Email(CURRENT_SENDER, recipients, subject, body);
                oos.writeObject(newEmail);

                oos.flush();

                boolean isSent = (boolean) ois.readObject();
                if(isSent) {
                    ClientUtils.buildAlert(Alert.AlertType.INFORMATION, "OK", "Email has been sent successfully!").showAndWait();
                    closeModal();
                } else {
                    ClientUtils.buildAlertWhenServerUpAndFails("Sending email failed", "Retry sending the email").showAndWait();
                }
            } catch (IOException | ClassNotFoundException e) {
                ClientUtils.buildAlertWhenServerIsDown().showAndWait();
            }
        }
    }

    public void closeModal() {
        // Close the modal window
        ((Stage) sendNewEmailButton.getScene().getWindow()).close();
    }
}
