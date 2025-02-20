package main.java.controller;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import main.java.model.Email;
import main.java.model.User;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerController {
    // this map represents the users in DB
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, ObservableList<Email>> userSentMails = new ConcurrentHashMap<>();
    private final Map<String, ObservableList<Email>> userReceivedMails = new ConcurrentHashMap<>();

    public static final Set<String> ONLINE_USERS = new HashSet<>();
    private final ConcurrentHashMap<String, List<Email>> newSentEmails = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Email>> newReceivedEmails = new ConcurrentHashMap<>();

    public ServerController() {
        loadUsersFromFile(new File("").getAbsolutePath() + "/server/src/main/database/users.txt");
        // load sent and received emails for user
        for (String username : users.keySet()) {
            loadUsersEmails(username);
        }

    }

    private void loadUsersEmails(String username) {
        if(userSentMails.get(username) == null) {
            userSentMails.put(username, FXCollections.observableArrayList());
        }

        if(userReceivedMails.get(username) == null) {
            userReceivedMails.put(username, FXCollections.observableArrayList());
        }

        try(BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + "/server/src/main/database/emails.json"))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;

            while((line = br.readLine()) != null) {
                jsonBuilder.append(line);
            }

            String json = jsonBuilder.toString();

            // Remove brackets at the start and end
            json = json.substring(1, json.length() - 1).trim();

            // Split by "},{" to separate the objects
            String[] jsonObjects = json.split("},");

            for (String jsonObject : jsonObjects) {
                // Clean up the JSON object string
                jsonObject = jsonObject.trim();
                if (!jsonObject.endsWith("}")) {
                    jsonObject += "}";
                }

                // Extract field values
                String id = extractValue(jsonObject, "id");
                String sender = extractValue(jsonObject, "sender");
                List<String> recipients = getRecipients(jsonObject);
                String subject = extractValue(jsonObject, "subject");
                String content = extractValue(jsonObject, "content");
                String sentDate = extractValue(jsonObject, "sentDate");
                Email email = new Email(id, sender, recipients, subject, content, sentDate);
                if(username.equals(sender)) {
                    userSentMails.get(username).add(email);
                } else if(!recipients.isEmpty() && recipients.contains(username)) {
                    userReceivedMails.get(username).add(email);
                }

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> getRecipients(String json) {
        List<String> recipients = new ArrayList<>();

        // Find the start and end of the "recipients" array
        int startIndex = json.indexOf("\"recipients\":") + "\"recipients\":".length();
        int arrayStart = json.indexOf("[", startIndex);
        int arrayEnd = json.indexOf("]", arrayStart);

        if (arrayStart != -1 && arrayEnd != -1) {
            // Extract the array content as a substring
            String recipientsArray = json.substring(arrayStart + 1, arrayEnd);

            // Split by commas, trim whitespace and quotes, and add to the list
            for (String recipient : recipientsArray.split(",")) {
                recipients.add(recipient.trim().replaceAll("^\"|\"$", "")); // Remove surrounding quotes
            }
        }

        return recipients;
    }

    private static String extractValue(String jsonObject, String key) {
        String keyValue = "\"" + key + "\":";
        int startIndex = jsonObject.indexOf(keyValue) + keyValue.length();
        int endIndex = jsonObject.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = jsonObject.indexOf("}", startIndex);
        }

        if(endIndex == -1) {
            return null;
        }
        return jsonObject.substring(startIndex, endIndex).replace("\"", "").trim();
    }

    private void loadUsersFromFile(String filePath) {
        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split(",");
                if (parts.length == 2) {
                    users.put(parts[0], new User(parts[0], parts[1]));
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("users.txt file not found.");
        }
    }

    public boolean validateLogin(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }

    public List<Email> getSentEmails(String username) {
        if(username == null || userSentMails.get(username) == null) return new ArrayList<>();
        return new ArrayList<>(userSentMails.get(username));
    }

    public List<Email> getReceivedEmails(String username) {
        if(username == null || userReceivedMails.get(username) == null) return new ArrayList<>();
        return new ArrayList<>(userReceivedMails.get(username));
    }

    public Email sendEmail(String sender, List<String> recipients, String subject, String content) {
        Email email = new Email(UUID.randomUUID().toString(), sender, recipients, subject, content, Email.getLocalDateTimeFormatted(LocalDateTime.now()));
        boolean allMatch = recipients.stream().allMatch(userReceivedMails::containsKey);
        if(!userSentMails.containsKey(sender) || !allMatch) return null;

        userSentMails.get(sender).add(email);
        addNewSentEmail(sender, email);

        for(String recipient : recipients) {
            userReceivedMails.get(recipient).add(email);
            if(ONLINE_USERS.contains(recipient)) {
                addNewReceivedEmail(recipient, email);
            }
        }


        // save to database
        saveNewEmailToDB(email);


        return email;
    }


    private final Object emailLock = new Object();
    public List<Email> getNewSentEmails(String username) {
        synchronized (emailLock) {
            return new ArrayList<>(newSentEmails.getOrDefault(username, new ArrayList<>()));
        }
    }
    public List<Email> getNewReceivedEmails(String recipient) {
        synchronized (emailLock) {
            return new ArrayList<>(newReceivedEmails.getOrDefault(recipient, new ArrayList<>()));
        }
    }

    // Add email to the sent emails list for a user
    public void addNewSentEmail(String username, Email email) {
        synchronized (emailLock) {
            newSentEmails.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(email);
            System.out.println("New Sent Emails are " + newSentEmails.get(username));
        }
    }

    // Add email to the received emails list for a user
    public void addNewReceivedEmail(String recipient, Email email) {
        synchronized (emailLock) {
            newReceivedEmails.computeIfAbsent(recipient, k -> new CopyOnWriteArrayList<>()).add(email);
            System.out.println("New Received Emails are " + newReceivedEmails.get(recipient));
        }
    }

    // Reset newSentEmails after polling
    public void resetNewSentEmails(String username) {
        synchronized (emailLock) {
            newSentEmails.remove(username);
        }
    }

    // Reset newReceivedEmails after polling
    public void resetNewReceivedEmails(String username) {
        synchronized (emailLock) {
            newReceivedEmails.remove(username);
        }
    }

    private static void saveNewEmailToDB(Email email) {
        List<Email> emails = new ArrayList<>();
        final Gson gson = new GsonBuilder()
                .setPrettyPrinting().create();

        String filePath = new File("").getAbsolutePath() + "/server/src/main/database/emails.json";

        // Read existing file content
        try (Reader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<ArrayList<Email>>() {}.getType();
            emails = gson.fromJson(reader, listType);

            if(emails == null) {
                emails = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add the new object to the list
        emails.add(email);

        // Write the updated list back to the file
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(emails, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteEmail(Email email, String username) {
        final Gson gson = new GsonBuilder()
                .setPrettyPrinting().create();
        List<Email> objectList = new ArrayList<>();

        String filePath = new File("").getAbsolutePath() + "/server/src/main/database/emails.json";

        // Read the existing JSON list from the file
        try (FileReader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<List<Email>>() {}.getType();
            objectList = gson.fromJson(reader, listType);

            if (objectList == null) {
                objectList = new ArrayList<>(); // Initialize if file is empty
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Filter out the object with the matching ID
        objectList.removeIf(obj -> email.getId().equals(obj.getId()));

        if(username.equals(email.getSender())) {
            userSentMails
                    .forEach((key, value) -> value.removeIf(email1 -> email.getId().equals(email1.getId())));
        } else {
            userReceivedMails
                    .forEach((key, value) -> value.removeIf(email1 -> email.getId().equals(email1.getId())));
        }

        // Write the updated list back to the file
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(objectList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


