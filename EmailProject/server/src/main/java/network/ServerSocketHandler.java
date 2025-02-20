package main.java.network;


import main.java.controller.ServerController;
import main.java.controller.ServerUiController;
import main.java.model.Email;
import main.java.model.EmailOperations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ServerSocketHandler  {
    private final ServerController serverController;
    private final ServerUiController uiController;
    private ServerSocket serverSocket;

    private volatile boolean running = true;

    public ServerSocketHandler(ServerUiController uiController) {
        this.uiController = uiController;
        this.serverController = new ServerController();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(12345);
            uiController.logEvent("Server started.");
            while (running) {
                Socket clientSocket = serverSocket.accept();
                // Handle each client connection in a new thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            uiController.logEvent("Server stopped.");
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error while closing server socket");
        }
    }

    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {

            String operation = (String) ois.readObject();

            if (EmailOperations.LOGIN.name().equals(operation)) {
                uiController.logEvent("Client connected: " + clientSocket.getInetAddress());
                String username = (String) ois.readObject();
                String password = (String) ois.readObject();
                boolean success = serverController.validateLogin(username, password);
                if(success) {
                    synchronized (ServerController.ONLINE_USERS) {
                        ServerController.ONLINE_USERS.add(username);
                    }
                }
                oos.writeObject(success);
                oos.flush();
                uiController.logEvent("Client login attempt: " + username + " - " + (success ? "Success" : "Failed"));
            } else if(EmailOperations.SENT_EMAILS.name().equals(operation)){
                String username = (String) ois.readObject();
                uiController.logEvent("Fetching Sent emails for " + username);
                oos.writeObject(serverController.getSentEmails(username));
                oos.flush();
            } else if(EmailOperations.RECEIVED_EMAILS.name().equals(operation)){
                String username = (String) ois.readObject();
                uiController.logEvent("Fetching received emails for " + username);
                oos.writeObject(serverController.getReceivedEmails(username));
                oos.flush();
            } else if (EmailOperations.SEND_EMAIL.name().equals(operation)) {
                Email email = (Email) ois.readObject();
                Email saved = serverController.sendEmail(email.getSender(), email.getRecipients(), email.getSubject(), email.getContent());
                oos.writeObject(saved != null);
                oos.flush();
                uiController.logEvent("Email sent from " + email.getSender() + " to " + email.getRecipients());
            } else if(EmailOperations.DELETE_EMAIL.name().equals(operation)) {
                String username = (String) ois.readObject();
                Email email = (Email) ois.readObject();
                serverController.deleteEmail(email, username);
                oos.writeObject(true);
                oos.flush();
                uiController.logEvent("Email with id " + email.getId() + " has been deleted successfully");
            } else if(EmailOperations.POLLING_SENT.name().equals(operation)){

                String username = (String) ois.readObject();

                List<Email> newSentEmails = serverController.getNewSentEmails(username);

                System.out.println("Polling new sent emails for user: " + username);
                oos.writeObject(newSentEmails);
                oos.flush();

                serverController.resetNewSentEmails(username);

            } else if(EmailOperations.POLLING_RECEIVED.name().equals(operation)){
                String username = (String) ois.readObject();

                List<Email> newReceivedEmails = serverController.getNewReceivedEmails(username);

                System.out.println("Polling new received emails for user: " + username);
                oos.writeObject(newReceivedEmails);
                oos.flush();

                serverController.resetNewReceivedEmails(username);

            } else if(EmailOperations.LOGOUT.name().equals(operation)) {
                String username = (String) ois.readObject();
                oos.writeObject(true);
                oos.flush();
                synchronized (ServerController.ONLINE_USERS) {
                    ServerController.ONLINE_USERS.remove(username);
                }
                uiController.logEvent(username + " has logged out.");
                serverController.resetNewSentEmails(username);
                serverController.resetNewReceivedEmails(username);
            }
        } catch (IOException | ClassNotFoundException e) {
            uiController.logEvent("Client disconnected.");
        }
    }
}


