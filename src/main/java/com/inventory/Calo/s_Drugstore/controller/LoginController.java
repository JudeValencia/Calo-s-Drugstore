package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.AuthenticationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import java.util.Optional;

@Controller
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private StackPane passwordContainer;

    @FXML
    private Button signInButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Label capsLockLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private StackPane rootPane;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ApplicationContext springContext;

    @FXML
    public void initialize() {
        // AUTO-FOCUS: Set focus on username field when page loads
        Platform.runLater(() -> usernameField.requestFocus());

        // Initially show PasswordField, hide TextField
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        // Hide loading indicator and caps lock warning
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        if (capsLockLabel != null) {
            capsLockLabel.setVisible(false);
        }

        // Bind the text between PasswordField and TextField
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());

        // Add listener to checkbox
        showPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Show password
                passwordTextField.setVisible(true);
                passwordTextField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordTextField.requestFocus();
            } else {
                // Hide password
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                passwordTextField.setVisible(false);
                passwordTextField.setManaged(false);
                passwordField.requestFocus();
            }
        });

        // CAPS LOCK WARNING: Add key event handlers for both password fields
        passwordField.setOnKeyPressed(this::handleCapsLock);
        passwordField.setOnKeyReleased(this::handleCapsLock);
        passwordTextField.setOnKeyPressed(this::handleCapsLock);
        passwordTextField.setOnKeyReleased(this::handleCapsLock);

        // Add Enter key listener for both password fields
        passwordField.setOnAction(event -> handleSignIn());
        passwordTextField.setOnAction(event -> handleSignIn());

        // Hide error label initially
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }

        // Setup fullscreen shortcut (F11)
        setupFullscreenShortcut();
    }

    private void handleCapsLock(KeyEvent event) {
        if (capsLockLabel != null) {
            capsLockLabel.setVisible(event.getCode() != KeyCode.CAPS &&
                    (event.isShiftDown() ? !event.getCode().isLetterKey() :
                            event.getCode().isLetterKey() &&
                                    Character.isUpperCase(event.getText().charAt(0))));
            // Simple check: if caps lock might be on
            try {
                boolean capsOn = java.awt.Toolkit.getDefaultToolkit().getLockingKeyState(
                        java.awt.event.KeyEvent.VK_CAPS_LOCK);
                capsLockLabel.setVisible(capsOn);
            } catch (Exception e) {
                // Fallback if AWT not available - just hide the warning
                capsLockLabel.setVisible(false);
            }
        }
    }

    private void setupFullscreenShortcut() {
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                KeyCombination f11 = new KeyCodeCombination(KeyCode.F11);
                newScene.getAccelerators().put(f11, this::toggleFullscreen);
            }
        });
    }

    @FXML
    private void toggleFullscreen() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    private void handleSignIn() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty()) {
            showError("Please enter your username");
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            return;
        }

        // LOADING INDICATOR: Show loading and disable button
        showLoading(true);
        signInButton.setDisable(true);
        hideError();

        // Perform authentication in background thread
        Task<Optional<User>> authTask = new Task<>() {
            @Override
            protected Optional<User> call() {
                // Simulate slight delay for better UX (optional)
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return authenticationService.authenticate(username, password);
            }
        };

        authTask.setOnSucceeded(event -> {
            showLoading(false);
            signInButton.setDisable(false);

            Optional<User> userOpt = authTask.getValue();
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                showInfo("Welcome, " + user.getFullName() + "!");

                System.out.println("Login successful!");
                System.out.println("User: " + user.getUsername());
                System.out.println("Role: " + user.getRole());

                loadDashboard(user);
            } else {
                showError("Invalid username or password");
                passwordField.clear();
                passwordTextField.clear();
                passwordField.requestFocus();
            }
        });

        authTask.setOnFailed(event -> {
            showLoading(false);
            signInButton.setDisable(false);
            showError("Authentication error: " + authTask.getException().getMessage());
        });

        // Start the authentication task
        new Thread(authTask).start();
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
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
//            // TEMPORARY: Redirect to Product Management for testing
//            String fxmlPath = "/fxml/product-management.fxml";
//            String cssPath = "/css/inventory.css";
//
//          // TODO: Restore original dashboard redirect after testing

            String fxmlPath;
            String cssPath;

             if (user.getRole().equalsIgnoreCase("ADMIN")) {
                 fxmlPath = "/fxml/dashboard.fxml";
                 cssPath = "/css/dashboard.css";
             } else {
                 fxmlPath = "/fxml/staff-dashboard.fxml";
                 cssPath = "/css/dashboard.css";
             }

            // Load the appropriate FXML
            java.net.URL dashboardUrl = getClass().getResource(fxmlPath);
            if (dashboardUrl == null) {
                showError("Error: " + fxmlPath + " not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(dashboardUrl);
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

//            // TEMPORARY: Set current user for Product Management Controller
//            ProductManagementController productManagementController = loader.getController();
//            if (productManagementController != null) {
//                productManagementController.setCurrentUser(user);
//            }

//             TODO: Restore original controller assignment after testing
//             Original code:
             if (user.getRole().equalsIgnoreCase("ADMIN")) {
                 DashboardController dashboardController = loader.getController();
                 if (dashboardController != null) {
                     dashboardController.setCurrentUser(user);
                     dashboardController.refreshDashboard();
                 }
             } else {
                 StaffDashboardController staffDashboardController = loader.getController();
                 if (staffDashboardController != null) {
                     staffDashboardController.setCurrentUser(user);
                 }
             }

            Stage stage = (Stage) signInButton.getScene().getWindow();

            if (stage == null) {
                showError("Cannot get stage reference");
                return;
            }

            Scene scene = new Scene(root);

            // Load CSS
            java.net.URL cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setWidth(1200);
            stage.setHeight(700);
            stage.setMaximized(false);
            stage.setFullScreen(false);
            stage.centerOnScreen();

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
        }
    }
}