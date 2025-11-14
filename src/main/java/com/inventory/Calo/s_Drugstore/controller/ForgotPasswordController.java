package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.repository.UserRepository;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private Button sendResetButton;

    @FXML
    private Hyperlink backToLoginLink;

    @FXML
    private Label messageLabel;

    @Autowired
    private UserRepository userRepository;

    @FXML
    public void initialize() {
        if (messageLabel != null) {
            messageLabel.setVisible(false);
        }
    }

    @FXML
    private void handleSendReset() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address");
            return;
        }

        // Check if email exists
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            // In a real application, you would:
            // 1. Generate a reset token
            // 2. Send an email with the reset link
            // 3. Store the token in database with expiration

            showSuccess("Password reset instructions have been sent to " + email);

            // For demo purposes, show the user info
            User user = userOpt.get();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Demo Mode");
            alert.setHeaderText("Password Reset Demo");
            alert.setContentText("In production, an email would be sent to: " + email +
                    "\n\nFor demo purposes, here's your account:\n" +
                    "Username: " + user.getUsername() +
                    "\n\nPlease contact the administrator to reset your password.");
            alert.showAndWait();

        } else {
            showError("No account found with that email address");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backToLoginLink.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error returning to login: " + e.getMessage());
        }
    }

    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setVisible(true);
        }
    }

    private void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setVisible(true);
        }
    }
}