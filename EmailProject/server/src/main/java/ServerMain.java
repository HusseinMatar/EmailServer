package main.java;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.java.controller.ServerUiController;

public class ServerMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ui/serverUi.fxml"));
        Parent root = loader.load();

        ServerUiController serverUIController = loader.getController();
        serverUIController.initialize();

        primaryStage.setTitle("Email Server");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

