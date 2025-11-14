package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.AuthenticationService;
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
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signInButton;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Label errorLabel;

    @Autowired
    private AuthenticationService authenticationService;

    @FXML
    public void initialize() {
        // Add Enter key listener for password field
        passwordField.setOnAction(event -> handleSignIn());

        // Hide error label initially
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    @FXML
    private void handleSignIn() {
        String usernameOrEmail = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (usernameOrEmail.isEmpty()) {
            showError("Please enter your username or email");
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            return;
        }

        // Authenticate
        Optional<User> userOpt = authenticationService.authenticate(usernameOrEmail, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            hideError();

            // Show success message
            showInfo("Welcome, " + user.getFullName() + "!");

            // TODO: Navigate to main dashboard
            // For now, just print success
            System.out.println("Login successful!");
            System.out.println("User: " + user.getUsername());
            System.out.println("Role: " + user.getRole());

            // Load dashboard (placeholder)
            loadDashboard(user);
        } else {
            showError("Invalid username/email or password");
            passwordField.clear();
        }
    }

    @FXML
    private void handleForgotPassword() {
        showInfo("Password reset feature coming soon!");
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #e74c3c;");
            errorLabel.setVisible(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    private void showInfo(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #27ae60;");
            errorLabel.setVisible(true);
        }
    }

    private void loadDashboard(User user) {
        try {
            // TODO: Load the actual dashboard FXML when ready
            // For now, just show an alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Login Successful");
            alert.setContentText("Welcome, " + user.getFullName() + "!\n\n" +
                    "Role: " + user.getRole() + "\n" +
                    "Dashboard will be loaded here.");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
        }
    }
}