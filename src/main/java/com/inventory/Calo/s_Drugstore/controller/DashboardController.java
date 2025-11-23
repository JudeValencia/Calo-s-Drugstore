package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.util.IconUtil;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.DashboardService;
import com.inventory.Calo.s_Drugstore.service.UserManagementService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;


@Controller
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
    private Button staffBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    private LineChart<String, Number> salesTrendsChart;

    @FXML
    private BarChart<String, Number> inventoryDistributionChart;

    @Autowired
    private DashboardService dashboardService;  // Gets dashboard data

    @Autowired
    private UserManagementService userManagementService;  // Manages staff accounts

    @Autowired
    private ApplicationContext springContext;  // Needed for loading other pages

    @FXML
    private VBox recentActivityContainer;


    private User currentUser;  // Stores the logged-in user's info


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

        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }

        refreshDashboard();
    }

    @FXML
    public void initialize() {
        // Load all dashboard data
        loadDashboardData();
        loadRecentActivity();
        refreshDashboard();

        // Set dashboard button as active (green highlight)
        setActiveButton(dashboardBtn);
    }

    public void refreshDashboard() {
        loadDashboardData();
        loadSalesTrendsChart();
        loadInventoryDistributionChart();
        loadRecentActivity();
    }

    private void loadRecentActivity() {
        if (recentActivityContainer == null) return;

        try {
            List<Map<String, Object>> activities = dashboardService.getRecentActivity();

            // Clear existing activities
            recentActivityContainer.getChildren().clear();

            for (Map<String, Object> activity : activities) {
                HBox activityItem = createActivityItem(activity);
                recentActivityContainer.getChildren().add(activityItem);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createActivityItem(Map<String, Object> activity) {
        HBox item = new HBox(15);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setStyle("-fx-padding: 12px 0;");

        // Status dot
        javafx.scene.layout.StackPane dot = new javafx.scene.layout.StackPane();
        dot.setPrefSize(10, 10);
        dot.setMaxSize(10, 10);
        dot.setMinSize(10, 10);

        String status = (String) activity.get("status");
        String dotColor = status.equals("completed") ? "#4CAF50" : "#FF9800";
        dot.setStyle("-fx-background-color: " + dotColor + "; -fx-background-radius: 5px;");

        // Activity details
        VBox details = new VBox(4);
        javafx.scene.layout.HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);

        Label description = new Label((String) activity.get("description"));
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-font-weight: 600;");

        Label itemDetails = new Label((String) activity.get("details"));
        itemDetails.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        details.getChildren().addAll(description, itemDetails);

        // Right side (amount and timestamp)
        VBox rightSide = new VBox(4);
        rightSide.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label amount = new Label((String) activity.get("amount"));
        String amountColor = status.equals("completed") ? "#4CAF50" : "#FF9800";
        amount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + amountColor + ";");

        Label timestamp = new Label((String) activity.get("timestamp"));
        timestamp.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        rightSide.getChildren().addAll(amount, timestamp);

        item.getChildren().addAll(dot, details, rightSide);

        return item;
    }

    private void loadDashboardData() {
        // Load KPI metrics
        Map<String, Object> metrics = dashboardService.getDashboardMetrics();

        // Update KPI Card 1: Total Sales Today
        if (totalSalesTodayLabel != null) {
            totalSalesTodayLabel.setText(String.format("₱%,.0f", metrics.get("totalSalesToday")));
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

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        // Already on dashboard, no need to navigate
    }


    @FXML
    private void handleInventory() {
        setActiveButton(inventoryBtn);
        navigateToPage("/fxml/inventory.fxml", "/css/inventory.css");
    }


    @FXML
    private void handleSales() {
        setActiveButton(salesBtn);
        navigateToPage("/fxml/sales.fxml", "/css/sales.css");
    }

    @FXML
    private void handleReports() {
        setActiveButton(reportsBtn);
        navigateToPage("/fxml/reports.fxml", "/css/reports.css");
    }

    @FXML
    private void handleStaff() {
        setActiveButton(staffBtn);
        navigateToPage("/fxml/staff.fxml", "/css/staff.css");
    }

    @FXML
    private void handleLogout() {
        setActiveButton(logoutBtn);
        boolean confirmed = showLogoutConfirmation();

        if (confirmed) {
            performLogout();
        }
    }

    // Replace the setActiveButton method in all controllers with this version:
    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        staffBtn.getStyleClass().remove("active");

        // Add active class to the selected button
        activeButton.getStyleClass().add("active");

        // Force JavaFX to refresh the buttons and their graphics
        Platform.runLater(() -> {
            dashboardBtn.requestLayout();
            inventoryBtn.requestLayout();
            salesBtn.requestLayout();
            reportsBtn.requestLayout();
            staffBtn.requestLayout();

            // Specifically refresh the dashboard button's graphic
            if (dashboardBtn.getGraphic() != null) {
                dashboardBtn.getGraphic().setVisible(false);
                dashboardBtn.getGraphic().setVisible(true);
            }
        });
    }


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

    private void navigateToPage(String fxmlPath, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Pass current user to the next controller
            if (fxmlPath.contains("inventory")) {
                InventoryController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("dashboard")) {
                DashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("sales")) {
                SalesController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("reports")) {
                ReportsController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("staff")) {
                StaffController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }


            Stage stage = (Stage) dashboardBtn.getScene().getWindow();
            Scene currentScene = stage.getScene();

            // SAVE CURRENT WINDOW SIZE AND POSITION
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            double currentX = stage.getX();
            double currentY = stage.getY();
            boolean isMaximized = stage.isMaximized();

            // Create new scene
            Scene newScene = new Scene(root);
            newScene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            // Set initial opacity to 0 for fade-in effect
            root.setOpacity(0);

            // Create fade-out animation for current scene
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(30),
                    currentScene.getRoot()
            );
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                // Switch to new scene
                stage.setScene(newScene);

                // RESTORE WINDOW SIZE AND POSITION
                if (isMaximized) {
                    stage.setMaximized(true);
                } else {
                    stage.setWidth(currentWidth);
                    stage.setHeight(currentHeight);
                    stage.setX(currentX);
                    stage.setY(currentY);
                }

                // Create fade-in animation for new scene
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(30),
                        root
                );
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();

        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error", "Failed to load page: " + e.getMessage());
        }
    }

    private void performLogout() {
        try {
            // Load login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) dashboardBtn.getScene().getWindow();
            Scene currentScene = stage.getScene();

            // Create new scene
            Scene newScene = new Scene(root);

            // Try to load CSS if it exists (handle null gracefully)
            try {
                java.net.URL cssUrl = getClass().getResource("/css/styles.css");
                if (cssUrl != null) {
                    newScene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    // Try alternative CSS paths
                    cssUrl = getClass().getResource("/css/login.css");
                    if (cssUrl != null) {
                        newScene.getStylesheets().add(cssUrl.toExternalForm());
                    }
                }
            } catch (Exception cssEx) {
                // CSS loading failed, continue without it
                System.err.println("Warning: Could not load CSS for login page: " + cssEx.getMessage());
            }

            // Set initial opacity to 0 for fade-in effect
            root.setOpacity(0);

            // Create fade-out animation for current scene
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(30),
                    currentScene.getRoot()
            );
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                // Clear current user
                this.currentUser = null;

                // Switch to login scene
                stage.setScene(newScene);

                // Reset window size to login page size
                stage.setWidth(800);
                stage.setHeight(600);
                stage.centerOnScreen();
                stage.setMaximized(false);

                // Create fade-in animation for login scene
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(30),
                        root
                );
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();

        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void showStyledAlert(Alert.AlertType type, String title, String message) {
        // Create custom dialog
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        // Message
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-line-spacing: 3px;");

        // Button
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button okButton = new Button("OK");

        // Button color based on alert type
        String buttonColor = "#4CAF50"; // Success - Green
        String buttonHoverColor = "#45a049";

        if (type == Alert.AlertType.ERROR) {
            buttonColor = "#dc3545"; // Red
            buttonHoverColor = "#c82333";
        } else if (type == Alert.AlertType.WARNING) {
            buttonColor = "#FF9800"; // Orange
            buttonHoverColor = "#f57c00";
        } else if (type == Alert.AlertType.INFORMATION) {
            buttonColor = "#4CAF50"; // Green
            buttonHoverColor = "#45a049";
        }

        final String finalButtonColor = buttonColor;
        final String finalHoverColor = buttonHoverColor;

        okButton.setStyle(
                "-fx-background-color: " + buttonColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 40px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );

        okButton.setOnAction(e -> dialogStage.close());

        okButton.setOnMouseEntered(e -> okButton.setStyle(
                okButton.getStyle().replace(finalButtonColor, finalHoverColor)
        ));
        okButton.setOnMouseExited(e -> okButton.setStyle(
                okButton.getStyle().replace(finalHoverColor, finalButtonColor)
        ));

        buttonBox.getChildren().add(okButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private boolean showLogoutConfirmation() {
        // Create custom dialog
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Logout");
        dialogStage.setResizable(false);
        dialogStage.setUserData(false);

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        // Title
        Label titleLabel = new Label("Are you sure you want to logout?");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        // Message
        Label messageLabel = new Label("You will be returned to the login screen and will need to log in again to access the system.");
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-line-spacing: 3px;");

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> {
            dialogStage.setUserData(false);
            dialogStage.close();
        });

        Button logoutButton = getButton(dialogStage);

        buttonBox.getChildren().addAll(cancelButton, logoutButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
    }

    private static Button getButton(Stage dialogStage) {
        Button logoutButton = new Button("Logout");
        logoutButton.setStyle(
                "-fx-background-color: #dc3545; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );
        logoutButton.setOnAction(e -> {
            dialogStage.setUserData(true);
            dialogStage.close();
        });

        logoutButton.setOnMouseEntered(e -> logoutButton.setStyle(
                logoutButton.getStyle().replace("#dc3545", "#c82333")
        ));
        logoutButton.setOnMouseExited(e -> logoutButton.setStyle(
                logoutButton.getStyle().replace("#c82333", "#dc3545")
        ));
        return logoutButton;
    }
}