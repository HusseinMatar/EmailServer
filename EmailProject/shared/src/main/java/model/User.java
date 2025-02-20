package main.java.model;

import java.io.Serializable;

public class User implements Serializable {
    private final String username;
    private final String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
