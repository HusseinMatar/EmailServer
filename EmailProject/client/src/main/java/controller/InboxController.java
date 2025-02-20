package main.java.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import main.java.model.Email;
import main.java.model.EmailOperations;
import main.java.util.ClientUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class InboxController {
    @FXML private Label noEmailsLabel;
    @FXML private Label username;
    @FXML private Button logoutButton;
    @FXML private Button receivedEmailButton;
    @FXML private Button sentEmailButton;
    @FXML private ListView<Email> emailListView;

    @FXML private Button replyButton;

    @FXML private Label sentDateLabel;
    @FXML private Label subjectLabel;
    @FXML private Label senderValueLabel;
    @FXML private Label recipientValueLabel;
    @FXML private TextArea bodyArea;
    @FXML private VBox emailDetailsView;

    private static String CURRENT_USERNAME = null;

    private ObservableList<Email> observableSentEmails = FXCollections.observableArrayList();
    private ObservableList<Email> observableReceivedEmails = FXCollections.observableArrayList();

    private Timer timerForSentEmails;
    private Timer timerForReceivedEmails;

    private static final boolean[] mailOperations;
    private static boolean isSendNewEmail;

    static {
        mailOperations = new boolean[3];
        // by default all 3 values are false
        // mailOperations[0] = false; reply
        // mailOperations[1] = false; reply all
        // mailOperations[2] = false; forward
    }

    public void initialize() {
        receivedEmailButton.setStyle("-fx-background-color: #FF5733;");
        sentEmailButton.setStyle(null);

        emailListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Email> call(ListView<Email> listView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Email item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            // Customize to display specific fields

                            setText("Sent Date: " + item.getSentDate() + "\t\t\t Subject: " + item.getSubject());
                        }
                    }
                };
            }
        });
        Platform.runLater(this::showReceivedEmails);

        startEmailPolling();
    }

    private void startEmailPolling() {
        timerForSentEmails = new Timer();

        timerForSentEmails.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                requestNewSentEmailsPeriodically();
            }
        }, 0, 5000);

        timerForReceivedEmails = new Timer();

        timerForReceivedEmails.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                requestNewReceivedEmailsPeriodically();
            }
        }, 0, 5000);
    }

    private void stopEmailPolling() {
        if(timerForSentEmails != null) {
            System.out.println("Stop Polling new sent emails for user: " + CURRENT_USERNAME);
            timerForSentEmails.cancel();
        }

        if(timerForReceivedEmails != null) {
            System.out.println("Stop Polling new received emails for user: " + CURRENT_USERNAME);
            timerForReceivedEmails.cancel();
        }
    }
    private void requestNewSentEmailsPeriodically() {
        new Thread(() -> {
            System.out.println("Polling new sent emails for user: " + CURRENT_USERNAME);
            try (Socket socket = new Socket("localhost", 12345);
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                // Send request for emails
                oos.writeObject(EmailOperations.POLLING_SENT.name()); // Request to get emails
                oos.writeObject(CURRENT_USERNAME); // Send the username
                oos.flush();

                // Read sent emails
                List<Email> sentEmails = (List<Email>) ois.readObject();

                // Process emails
                Platform.runLater(() -> {
                    if(sentEmailButton.isDisable() && !sentEmails.isEmpty()) {
                        this.observableSentEmails.addAll(sentEmails);
                        emailListView.setItems(this.observableSentEmails);
                    }
                });

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Server is down; for now cannot poll new sent emails");
            }
        }).start();
    }

    private void requestNewReceivedEmailsPeriodically() {
        new Thread(() -> {
            System.out.println("Polling new received emails for user: " + CURRENT_USERNAME);
            try (Socket socket = new Socket("localhost", 12345);
                 ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                // Send request for emails
                oos.writeObject(EmailOperations.POLLING_RECEIVED.name()); // Request to get emails
                oos.writeObject(CURRENT_USERNAME); // Send the username
                oos.flush();

                // Read received emails
                List<Email> receivedEmails = (List<Email>) ois.readObject();

                // Process emails
                Platform.runLater(() -> {
                    if(!sentEmailButton.isDisable() && !receivedEmails.isEmpty()) {
                        this.observableReceivedEmails.addAll(receivedEmails);
                        emailListView.setItems(this.observableReceivedEmails);
                    }
                });

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Server is down; for now cannot poll new received emails");
            }
        }).start();
    }

    public void setUsername(String username) {
        CURRENT_USERNAME = username;
        this.username.setText("Hello " + username);
    }

    @FXML
    public void showReceivedEmails() {
        if(emailListView.getItems().isEmpty()) {
            emailListView.setItems(FXCollections.emptyObservableList());
            emailDetailsView.setVisible(false);
        }

        sentEmailButton.setStyle(null);
        receivedEmailButton.setStyle("-fx-background-color: #FF5733;");

        sentEmailButton.setDisable(false);
        receivedEmailButton.setDisable(true);


        try(Socket socket = new Socket("localhost", 12345);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            oos.writeObject(EmailOperations.RECEIVED_EMAILS.name());
            oos.writeObject(CURRENT_USERNAME);

            List<Email> receivedEmails = (ArrayList<Email>) ois.readObject();

            if(receivedEmails != null && !receivedEmails.isEmpty()) {
                this.observableReceivedEmails = FXCollections.observableList(receivedEmails);

                noEmailsLabel.setVisible(false);
                emailListView.setVisible(true);
                emailListView.setItems(observableReceivedEmails);
            } else {
                noEmailsLabel.setVisible(true);
                emailListView.setVisible(false);
            }
        } catch (IOException | ClassNotFoundException e) {
            ClientUtils.buildAlertWhenServerIsDown().showAndWait();
        }
    }

    @FXML
    public void showSentEmails() {
        if(emailListView.getItems().isEmpty()) {
            emailListView.setItems(FXCollections.emptyObservableList());
            emailDetailsView.setVisible(false);
        }


        receivedEmailButton.setStyle(null);
        sentEmailButton.setStyle("-fx-background-color: #FF5733;");

        receivedEmailButton.setDisable(false);
        sentEmailButton.setDisable(true);

        try(Socket socket = new Socket("localhost", 12345);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            oos.writeObject(EmailOperations.SENT_EMAILS.name());
            oos.writeObject(CURRENT_USERNAME);

            List<Email> sentEmails = (ArrayList<Email>) ois.readObject();

            if(sentEmails != null && !sentEmails.isEmpty()) {
                this.observableSentEmails = FXCollections.observableArrayList(sentEmails);
                noEmailsLabel.setVisible(false);
                emailListView.setVisible(true);
                emailListView.setItems(observableSentEmails);
            } else {
                noEmailsLabel.setVisible(true);
                emailListView.setVisible(false);
            }
        } catch (IOException | ClassNotFoundException e) {
            ClientUtils.buildAlertWhenServerIsDown().showAndWait();
        }
    }
    @FXML
    public void handleEmailSelection() {
        Email selectedEmail = emailListView.getSelectionModel().getSelectedItem();
        if (selectedEmail != null) {
            displayEmailDetails(selectedEmail);
        }
    }

    private void displayEmailDetails(Email email) {
        sentDateLabel.setText(email.getSentDate());
        subjectLabel.setText(email.getSubject());
        bodyArea.setText(email.getContent());
        senderValueLabel.setText(email.getSender());
        recipientValueLabel.setText(String.join(",", email.getRecipients()));

        replyButton.setDisable(email.getRecipients().size() > 1);

        emailDetailsView.setVisible(true);
    }
    @FXML
    private void handleLogout() {
        try (Socket socket = new Socket("localhost", 12345);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
             oos.writeObject(EmailOperations.LOGOUT.name());
             oos.writeObject(CURRENT_USERNAME);

            boolean logoutSuccess = (boolean) ois.readObject();
            if (logoutSuccess) {
                stopEmailPolling();
                // Load inbox UI
                loadLoginUI();
            }
        } catch (IOException | ClassNotFoundException e) {
            ClientUtils.buildAlertWhenServerIsDown().showAndWait();
        }
    }

    private void loadLoginUI() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../ui/login.fxml"));
            Parent clientRoot = loader.load();

            // Switch to the login scene
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(clientRoot));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openNewEmailModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../ui/newEmail.fxml"));
            Parent root = loader.load();

            NewEmailController emailController = loader.getController();
            emailController.initialize(CURRENT_USERNAME, !isSendNewEmail ? emailListView.getSelectionModel().getSelectedItem() : null, mailOperations);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("New Email");
            stage.initModality(Modality.APPLICATION_MODAL); // Makes the window modal
            stage.setMinHeight(400);
            stage.setMinWidth(600);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void reply() {
        isSendNewEmail = false;
        mailOperations[0] = true;
        mailOperations[1] = false;
        mailOperations[2] = false;
        openNewEmailModal();
    }

    @FXML
    public void replyAll() {
        isSendNewEmail = false;
        mailOperations[0] = false;
        mailOperations[1] = true;
        mailOperations[2] = false;
        openNewEmailModal();
    }

    @FXML
    public void forwardEmail() {
        isSendNewEmail = false;
        mailOperations[0] = false;
        mailOperations[1] = false;
        mailOperations[2] = true;
        openNewEmailModal();
    }

    @FXML
    public void deleteEmail() {
        Email selectedEmail = emailListView.getSelectionModel().getSelectedItem();
        if(selectedEmail != null) {
            try(Socket socket = new Socket("localhost", 12345);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                oos.writeObject(EmailOperations.DELETE_EMAIL.name());
                oos.writeObject(CURRENT_USERNAME);
                oos.writeObject(selectedEmail);

                boolean isDeleted = (boolean) ois.readObject();

                if(isDeleted) {
                    emailDetailsView.setVisible(false);
                    emailListView.getItems().remove(selectedEmail);
                } else {
                    ClientUtils.buildAlertWhenServerUpAndFails("Deleting email failed", "Retry deleting the email").showAndWait();
                }
            } catch (IOException | ClassNotFoundException e) {
                ClientUtils.buildAlertWhenServerIsDown().showAndWait();
            }
        }
    }

    @FXML
    public void sendNewEmail() {
        isSendNewEmail = true;
        openNewEmailModal();
    }
}

