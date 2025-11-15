package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.DashboardService;
import com.inventory.Calo.s_Drugstore.service.UserManagementService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * DashboardController - Controls the main admin dashboard page
 *
 * WHAT THIS FILE DOES:
 * - Displays dashboard metrics (sales, stock, etc.)
 * - Handles navigation (clicking menu items)
 * - Shows dialogs for creating staff and resetting passwords
 * - Manages logout functionality
 *
 * CONNECTS TO:
 * - dashboard.fxml (the UI layout)
 * - DashboardService (gets the data to display)
 * - UserManagementService (creates users, resets passwords)
 */

@Controller  // This is a Spring-managed controller
public class DashboardController {

    // ========================================
    // FXML FIELDS (Connected to UI elements in dashboard.fxml)
    // ========================================

    // Welcome label at the top
    @FXML
    private Label welcomeLabel;

    // User info section
    @FXML
    private Label userNameLabel;

    @FXML
    private Label userEmailLabel;

    // KPI Card Labels (the 4 big numbers)
    @FXML
    private Label totalSalesTodayLabel;

    @FXML
    private Label salesChangeLabel;

    @FXML
    private Label lowStockAlertsLabel;

    @FXML
    private Label lowStockMessageLabel;

    @FXML
    private Label expiringMedicinesLabel;

    @FXML
    private Label expiringMessageLabel;

    @FXML
    private Label totalInventoryLabel;

    @FXML
    private Label inventoryMessageLabel;

    // Navigation Buttons (sidebar menu)
    @FXML
    private Button dashboardBtn;

    @FXML
    private Button inventoryBtn;

    @FXML
    private Button salesBtn;

    @FXML
    private Button reportsBtn;

    @FXML
    private Button suppliersBtn;

    @FXML
    private Button settingsBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    private VBox sidebarOverlay;

    @FXML
    private LineChart<String, Number> salesTrendsChart;

    @FXML
    private BarChart<String, Number> inventoryDistributionChart;


    // ========================================
    // SERVICES (Dependencies injected by Spring)
    // ========================================

    @Autowired
    private DashboardService dashboardService;  // Gets dashboard data

    @Autowired
    private UserManagementService userManagementService;  // Manages staff accounts

    @Autowired
    private ApplicationContext springContext;  // Needed for loading other pages


    // ========================================
    // CURRENT USER (who is logged in)
    // ========================================

    private User currentUser;  // Stores the logged-in user's info


    /**
     * SET CURRENT USER
     *
     * Called by LoginController after successful login
     * Stores the user info and updates the welcome message
     *
     * @param user - The logged-in user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // Update welcome message with user's name
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome back! Here's what's happening with your pharmacy today.");
        }

        // Update user info section
        if (userNameLabel != null) {
            userNameLabel.setText(user.getFullName());
        }
        if (userEmailLabel != null) {
            userEmailLabel.setText(user.getEmail());
        }
    }


    /**
     * INITIALIZE
     *
     * Called automatically when dashboard.fxml is loaded
     * Sets up the page and loads all data
     */
    @FXML
    public void initialize() {
        // Load all dashboard data
        loadDashboardData();

        // Set dashboard button as active (green highlight)
        setActiveButton(dashboardBtn);
    }


    /**
     * LOAD DASHBOARD DATA
     *
     * Fetches data from DashboardService and displays it on the UI
     * Updates all the KPI cards with current numbers
     */
    private void loadDashboardData() {
        // Load KPI metrics
        Map<String, Object> metrics = dashboardService.getDashboardMetrics();

        // Update KPI Card 1: Total Sales Today
        if (totalSalesTodayLabel != null) {
            totalSalesTodayLabel.setText(String.format("$%,.0f", metrics.get("totalSalesToday")));
        }
        if (salesChangeLabel != null) {
            salesChangeLabel.setText((String) metrics.get("salesChange"));
        }

        // Update KPI Card 2: Low Stock Alerts
        if (lowStockAlertsLabel != null) {
            lowStockAlertsLabel.setText(String.valueOf(metrics.get("lowStockAlerts")));
        }
        if (lowStockMessageLabel != null) {
            lowStockMessageLabel.setText((String) metrics.get("lowStockMessage"));
        }

        // Update KPI Card 3: Expiring Medicines
        if (expiringMedicinesLabel != null) {
            expiringMedicinesLabel.setText(String.valueOf(metrics.get("expiringMedicines")));
        }
        if (expiringMessageLabel != null) {
            expiringMessageLabel.setText((String) metrics.get("expiringMessage"));
        }

        // Update KPI Card 4: Total Inventory
        if (totalInventoryLabel != null) {
            totalInventoryLabel.setText(String.format("%,d", metrics.get("totalInventoryCount")));
        }
        if (inventoryMessageLabel != null) {
            inventoryMessageLabel.setText((String) metrics.get("inventoryMessage"));
        }

        // Load Charts
        loadSalesTrendsChart();
        loadInventoryDistributionChart();
    }


    // ========================================
    // NAVIGATION HANDLERS (Sidebar Menu Clicks)
    // ========================================

    /**
     * Handle Dashboard Button Click
     * Already on dashboard, so just highlight the button
     */
    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        // Already on dashboard, no need to navigate
    }

    /**
     * Handle Inventory Button Click
     * Will navigate to inventory page (coming soon)
     */
    @FXML
    private void handleInventory() {
        setActiveButton(inventoryBtn);
        showComingSoon("Inventory Management");
    }

    /**
     * Handle Sales Button Click
     * Will navigate to sales/POS page (coming soon)
     */
    @FXML
    private void handleSales() {
        setActiveButton(salesBtn);
        showComingSoon("Sales & POS");
    }

    /**
     * Handle Reports Button Click
     * Will navigate to reports page (coming soon)
     */
    @FXML
    private void handleReports() {
        setActiveButton(reportsBtn);
        showComingSoon("Reports & Analytics");
    }

    /**
     * Handle Suppliers Button Click
     * Will navigate to suppliers page (coming soon)
     */
    @FXML
    private void handleSuppliers() {
        setActiveButton(suppliersBtn);
        showComingSoon("Supplier Management");
    }

    /**
     * Handle Settings Button Click
     * Shows a menu with admin options:
     * - Create Staff Account
     * - Reset Password
     */
    @FXML
    private void handleSettings() {
        setActiveButton(settingsBtn);
        showSettingsMenu();
    }

    /**
     * Handle Logout Button Click
     * Asks for confirmation, then returns to login page
     */
    @FXML
    private void handleLogout() {
        // Show confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Logout");
        confirmAlert.setHeaderText("Are you sure you want to logout?");
        confirmAlert.setContentText("You will be returned to the login screen.");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        // If user clicked OK, logout
        if (result.isPresent() && result.get() == ButtonType.OK) {
            navigateToLogin();
        }
    }


    // ========================================
    // SETTINGS MENU FUNCTIONS
    // ========================================

    /**
     * SHOW SETTINGS MENU
     *
     * Displays a dialog with admin options:
     * - Create Staff Account
     * - Reset Password
     */
    private void showSettingsMenu() {
        Alert settingsAlert = new Alert(Alert.AlertType.INFORMATION);
        settingsAlert.setTitle("Settings");
        settingsAlert.setHeaderText("Admin Settings");
        settingsAlert.setContentText("Choose an action:");

        // Create custom buttons
        ButtonType createStaffBtn = new ButtonType("Create Staff Account");
        ButtonType resetPasswordBtn = new ButtonType("Reset Password");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        settingsAlert.getButtonTypes().setAll(createStaffBtn, resetPasswordBtn, cancelBtn);

        // Wait for user to click a button
        Optional<ButtonType> result = settingsAlert.showAndWait();

        // Handle the choice
        if (result.isPresent()) {
            if (result.get() == createStaffBtn) {
                showCreateStaffDialog();
            } else if (result.get() == resetPasswordBtn) {
                showResetPasswordDialog();
            }
        }
    }

    /**
     * SHOW CREATE STAFF DIALOG
     *
     * Displays a form to create a new staff account
     * Fields: Username, Email, Full Name, Password, Confirm Password, Role
     */
    private void showCreateStaffDialog() {
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Staff Account");
        dialog.setHeaderText("Enter new staff member details");

        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Create input fields
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("ADMIN", "MANAGER", "STAFF");
        roleComboBox.setValue("STAFF");  // Default to STAFF role

        // Add fields to grid
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Full Name:"), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(new Label("Confirm Password:"), 0, 4);
        grid.add(confirmPasswordField, 1, 4);
        grid.add(new Label("Role:"), 0, 5);
        grid.add(roleComboBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType createBtn = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        // Show dialog and wait for response
        Optional<ButtonType> result = dialog.showAndWait();

        // If user clicked Create, validate and create account
        if (result.isPresent() && result.get() == createBtn) {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            String role = roleComboBox.getValue();

            // Validation
            if (username.isEmpty() || email.isEmpty() || fullName.isEmpty() ||
                    password.isEmpty() || confirmPassword.isEmpty()) {
                showError("All fields are required!");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showError("Passwords do not match!");
                return;
            }

            if (password.length() < 6) {
                showError("Password must be at least 6 characters!");
                return;
            }

            // Try to create account
            try {
                userManagementService.createStaffAccount(username, email, password, fullName, role);
                showSuccess("Staff account created successfully!\n\n" +
                        "Username: " + username + "\n" +
                        "Password: " + password + "\n\n" +
                        "Please share these credentials with the staff member.");
            } catch (Exception e) {
                showError("Error creating account: " + e.getMessage());
            }
        }
    }

    /**
     * SHOW RESET PASSWORD DIALOG
     *
     * Displays a form to reset a staff member's password
     * Fields: Username, New Password, Confirm Password
     */
    private void showResetPasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Staff Password");
        dialog.setHeaderText("Enter username and new password");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType resetBtn = new ButtonType("Reset", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetBtn, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == resetBtn) {
            String username = usernameField.getText().trim();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // Validation
            if (username.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showError("All fields are required!");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showError("Passwords do not match!");
                return;
            }

            if (newPassword.length() < 6) {
                showError("Password must be at least 6 characters!");
                return;
            }

            // Try to reset password
            try {
                boolean success = userManagementService.resetPassword(username, newPassword);
                if (success) {
                    showSuccess("Password reset successfully!\n\n" +
                            "Username: " + username + "\n" +
                            "New Password: " + newPassword);
                } else {
                    showError("User not found!");
                }
            } catch (Exception e) {
                showError("Error resetting password: " + e.getMessage());
            }
        }
    }


    // ========================================
    // HELPER FUNCTIONS
    // ========================================

    /**
     * SET ACTIVE BUTTON
     *
     * Highlights the clicked menu item in green
     * Removes highlight from other buttons
     */
    private void setActiveButton(Button button) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        suppliersBtn.getStyleClass().remove("active");
        settingsBtn.getStyleClass().remove("active");

        // Add active class to clicked button
        if (!button.getStyleClass().contains("active")) {
            button.getStyleClass().add("active");
        }
    }

    /**
     * SHOW COMING SOON MESSAGE
     *
     * Displays a placeholder message for features not yet implemented
     */
    private void showComingSoon(String moduleName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(moduleName);
        alert.setContentText("This module is under development and will be available soon.");
        alert.showAndWait();
    }

    /**
     * SHOW ERROR MESSAGE
     *
     * Displays an error dialog with red X icon
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * SHOW SUCCESS MESSAGE
     *
     * Displays a success dialog with green checkmark
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Operation Successful");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * NAVIGATE TO LOGIN
     *
     * Returns to the login page (logout)
     */
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            Scene scene = new Scene(root);

            // LOAD STYLES.CSS for login page
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

            stage.setWidth(900);
            stage.setHeight(600);
            stage.centerOnScreen();

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error returning to login: " + e.getMessage());
        }
    }

    @FXML
    private void handleMenuToggle() {
        boolean isVisible = sidebarOverlay.isVisible();
        sidebarOverlay.setVisible(!isVisible);
        sidebarOverlay.setManaged(!isVisible);
    }

    /**
     * LOAD SALES TRENDS CHART
     *
     * Creates a line chart showing daily sales for the last 7 days
     */
    private void loadSalesTrendsChart() {
        if (salesTrendsChart == null) return;

        // Get sales trends data from service
        List<Map<String, Object>> trendsData = dashboardService.getSalesTrends();

        // Create a series for the line chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Sales");

        // Add data points
        for (Map<String, Object> dayData : trendsData) {
            String day = (String) dayData.get("day");
            Number sales = (Number) dayData.get("sales");
            series.getData().add(new XYChart.Data<>(day, sales));
        }

        // Add series to chart
        salesTrendsChart.getData().clear();
        salesTrendsChart.getData().add(series);

        // Style the chart
        salesTrendsChart.setLegendVisible(false);
        salesTrendsChart.setAnimated(true);
    }

    /**
     * LOAD INVENTORY DISTRIBUTION CHART
     *
     * Creates a bar chart showing inventory count by category
     */
    private void loadInventoryDistributionChart() {
        if (inventoryDistributionChart == null) return;

        // Get inventory distribution data from service
        Map<String, Integer> distributionData = dashboardService.getInventoryDistribution();

        // Create a series for the bar chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Stock Count");

        // Add data points
        distributionData.forEach((category, count) -> {
            series.getData().add(new XYChart.Data<>(category, count));
        });

        // Add series to chart
        inventoryDistributionChart.getData().clear();
        inventoryDistributionChart.getData().add(series);

        // Style the chart
        inventoryDistributionChart.setLegendVisible(false);
        inventoryDistributionChart.setAnimated(true);
    }
}