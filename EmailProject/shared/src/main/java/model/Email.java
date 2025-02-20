package main.java.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Email implements Serializable {

    private final String id;
    private final String sender;
    private final List<String> recipients;
    private final String subject;
    private final String content;
    private final String sentDate;

    public Email(String sender, List<String> recipients, String subject, String content) {
        this(null, sender, recipients, subject, content, LocalDateTime.now().toString());
    }

    public Email(String id, String sender, List<String> recipients, String subject, String content) {
        this(id, sender, recipients, subject, content, LocalDateTime.now().toString());
    }

    public static String getLocalDateTimeFormatted(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
    }

    public Email(String id, String sender, List<String> recipients, String subject, String content, String sentDate) {
        this.id = id;
        this.sender = sender;
        this.recipients = recipients;
        this.subject = subject;
        this.content = content;
        this.sentDate = sentDate;;
    }



    // Getters
    public String getId() {
        return id;
    }
    public String getSender() { return sender; }
    public List<String> getRecipients() { return recipients; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }

    public String getSentDate() {
        return sentDate;
    }

    @Override
    public String toString() {
        return "Email{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", recipients=" + recipients +
                ", subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                ", sentDate=" + sentDate +
                '}';
    }
}


