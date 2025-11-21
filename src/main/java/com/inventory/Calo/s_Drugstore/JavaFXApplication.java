package com.inventory.Calo.s_Drugstore;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFXApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(DrugstoreApplication.class)
                .run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());

        Image icon = new Image(getClass().getResourceAsStream("/icons/pharmatrack-icon.png"));
        primaryStage.getIcons().add(icon);

        primaryStage.setTitle("PharmaTrack - Calo's Drugstore");
        primaryStage.setScene(scene);

        // Enable resizing
        primaryStage.setResizable(true);

        // Set minimum size
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);

        // Set initial size
        primaryStage.setWidth(900);
        primaryStage.setHeight(600);



        primaryStage.show();
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}