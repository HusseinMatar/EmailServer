package main.java.util;

import javafx.scene.control.Alert;

import java.util.List;
import java.util.regex.Pattern;

public final class ClientUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean areValidEmails(List<String> emails) {
        return emails.stream().allMatch(ClientUtils::isValidEmail);
    }
    public static Alert buildAlertWhenServerIsDown() {
        return buildAlert(Alert.AlertType.ERROR,"Generic Error", "Server is down.. Please try again later!");
    }

    public static Alert buildAlertWhenServerUpAndFails(String title, String headerText) {
        return buildAlert(Alert.AlertType.ERROR,title, headerText);
    }

    public static Alert buildAlert(Alert.AlertType alertType, String title, String headerText) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);

        return alert;
    }
}
