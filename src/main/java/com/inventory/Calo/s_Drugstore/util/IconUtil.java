package com.inventory.Calo.s_Drugstore.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class IconUtil {
    private static Image appIcon;

    public static void setApplicationIcon(Stage stage) {
        try {
            if (appIcon == null) {
                appIcon = new Image(Objects.requireNonNull(IconUtil.class.getResourceAsStream("/icons/pharmatrack-icon.png")));
            }
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
    }
}