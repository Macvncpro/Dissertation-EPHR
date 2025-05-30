package com.ephr;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

import com.ephr.controllers.MainEPHRController;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLoginScreen();
    }

    public static void showLoginScreen() throws IOException {
        Parent root = FXMLLoader.load(Main.class.getResource("/fxml/LoginPage.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Electronic Patient Health Record");
        primaryStage.setWidth(400);    // fixed width
        primaryStage.setHeight(700);   // fixed height
        primaryStage.show();
        primaryStage.centerOnScreen();
    }

    public static void showEPHRScreen(String email, String role) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/MainEPHR.fxml"));
        Parent root = loader.load();
    
        MainEPHRController controller = loader.getController();
        controller.setUserContext(email, role);
    
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); //  Makes window full screen on launch
        primaryStage.show();
        
    }
    
}