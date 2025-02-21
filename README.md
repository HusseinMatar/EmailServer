# 📧 Java Email Server & Client (JavaFX + Sockets)

This project implements a **distributed email system** using Java, JavaFX, and Java Sockets. It consists of a **Mail Server** that manages user mailboxes and multiple **Mail Clients** that interact with the server to send, receive, and manage emails.

## 📌 Features

### **Mail Server**
- Manages multiple email accounts with **persistent storage** (TXT or binary files).
- Handles **sending, receiving, and forwarding emails**.
- Logs all client interactions (connections, email deliveries, and errors).
- Uses **multi-threading** for efficient request handling.
- Implements **mutual exclusion** for safe access to resources.

### **Mail Clients**
- **Compose, send, read, reply, reply-all, forward, and delete emails**.
- Displays an **updated mailbox** with real-time notifications for new messages.
- **Detects invalid email addresses** before sending.
- Automatically **reconnects** if the server goes offline.
- Uses **JavaFX** for a user-friendly **GUI**.

## ⚙️ Technical Details
- **JavaFX** for the graphical user interface (GUI).
- **Java Sockets** for communication between the server and clients.
- **Observer-Observable pattern** for UI updates.
- **Multithreading** for handling multiple clients concurrently.
- **File-based persistence** (No database usage).

## 🚀 Setup & Usage

### **1. Clone the Repository**
```sh
git clone https://github.com/HusseinMatar/EmailServer.git
cd EmailServer

java -jar MailServer.jar

java -jar MailClient.jar

Interact with the GUI
	•	Send emails to multiple recipients.
	•	Receive and manage emails.
	•	Get real-time notifications for new messages.

📌 Notes
	•	The project is designed to be scalable beyond the initial three users.
	•	Each client is pre-assigned to a mailbox (no account creation needed).
	•	Follows software engineering best practices for concurrency and distributed systems.

🛠️ Tools & Technologies
	•	Java 17+
	•	JavaFX 21+
	•	Java Sockets
	•	Multithreading
	•	File-based storage (TXT/Binary)
	•	Observer Pattern (JavaFX Properties & Bindings)
