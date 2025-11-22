package com.inventory.Calo.s_Drugstore.controller;


import com.inventory.Calo.s_Drugstore.util.IconUtil;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.DashboardService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.*;

@Controller
public class StaffDashboardController implements Initializable {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;

    // FXML Components
    @FXML private Button dashboardBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button logoutBtn;

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    // KPI Labels
    @FXML private Label myTransactionsLabel;
    @FXML private Label myItemsSoldLabel;
    @FXML private Label mySalesLabel;
    @FXML private Label lowStockCountLabel;

    // Containers
    @FXML private VBox recentTransactionsContainer;
    @FXML private VBox lowStockItemsContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveButton(dashboardBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userRoleLabel.setText(user.getRole());

            // Load dashboard data
            refreshDashboard();
        }
    }

    public void refreshDashboard() {
        if (currentUser == null) return;

        loadStaffMetrics();
        loadRecentTransactions();
        loadLowStockItems();
    }

    private void loadStaffMetrics() {
        try {
            // Get staff-specific metrics
            Map<String, Object> staffMetrics = dashboardService.getStaffMetrics(currentUser.getId());
            Map<String, Object> generalMetrics = dashboardService.getDashboardMetrics();

            // Update KPI cards
            myTransactionsLabel.setText(String.valueOf(staffMetrics.get("myTransactions")));
            myItemsSoldLabel.setText(staffMetrics.get("myItemsSold") + " items sold");
            mySalesLabel.setText("₱" + String.format("%,.2f", staffMetrics.get("myRevenue")));
            lowStockCountLabel.setText(String.valueOf(generalMetrics.get("lowStockAlerts")));

        } catch (Exception e) {
            e.printStackTrace();
            // Set default values on error
            myTransactionsLabel.setText("0");
            myItemsSoldLabel.setText("0 items sold");
            mySalesLabel.setText("₱0.00");
            lowStockCountLabel.setText("0");
        }
    }

    private void loadRecentTransactions() {
        if (recentTransactionsContainer == null || currentUser == null) return;

        try {
            List<Map<String, Object>> activities = dashboardService.getStaffRecentActivity(currentUser.getId());

            // Clear existing items
            recentTransactionsContainer.getChildren().clear();

            // Filter only sales transactions
            int count = 0;
            for (Map<String, Object> activity : activities) {
                if ("sale".equals(activity.get("type")) && count < 5) {
                    HBox transactionItem = createTransactionItem(activity);
                    recentTransactionsContainer.getChildren().add(transactionItem);
                    count++;
                }
            }

            // If no transactions, show placeholder
            if (count == 0) {
                Label placeholder = new Label("No transactions today");
                placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px;");
                recentTransactionsContainer.getChildren().add(placeholder);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createTransactionItem(Map<String, Object> activity) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(
                "-fx-padding: 15px; " +
                        "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px;"
        );

        // Transaction details
        VBox details = new VBox(4);
        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);

        Label transactionId = new Label((String) activity.get("description"));
        transactionId.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label timestamp = new Label((String) activity.get("timestamp"));
        timestamp.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        details.getChildren().addAll(transactionId, timestamp);

        // Amount and items
        VBox rightSide = new VBox(4);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        Label amount = new Label((String) activity.get("amount"));
        amount.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        String itemsInfo = (String) activity.get("details");
        Label items = new Label(itemsInfo);
        items.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        rightSide.getChildren().addAll(amount, items);

        item.getChildren().addAll(details, rightSide);

        return item;
    }

    private void loadLowStockItems() {
        if (lowStockItemsContainer == null) return;

        try {
            List<Map<String, Object>> activities = dashboardService.getRecentActivity();

            // Clear existing items
            lowStockItemsContainer.getChildren().clear();

            // Filter only alert items
            int count = 0;
            for (Map<String, Object> activity : activities) {
                if ("alert".equals(activity.get("type")) && count < 5) {
                    HBox lowStockItem = createLowStockItem(activity);
                    lowStockItemsContainer.getChildren().add(lowStockItem);
                    count++;
                }
            }

            // If no low stock items, show placeholder
            if (count == 0) {
                Label placeholder = new Label("All items well stocked");
                placeholder.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px; -fx-font-weight: 600;");
                lowStockItemsContainer.getChildren().add(placeholder);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createLowStockItem(Map<String, Object> activity) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(
                "-fx-padding: 12px; " +
                        "-fx-background-color: white; " +
                        "-fx-border-color: #FFCDD2; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-background-radius: 8px;"
        );

        // Product details
        VBox details = new VBox(4);
        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);

        Label productName = new Label((String) activity.get("details"));
        productName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label medicineId = new Label("MED" + String.format("%03d", new Random().nextInt(100)));
        medicineId.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        details.getChildren().addAll(productName, medicineId);

        // Stock badge
        Label stockBadge = new Label((String) activity.get("amount"));
        stockBadge.setStyle(
                "-fx-background-color: #c62828; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6px 12px; " +
                        "-fx-background-radius: 12px;"
        );

        item.getChildren().addAll(details, stockBadge);

        return item;
    }

    private void setActiveButton(Button activeBtn) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");

        // Add active class to the clicked button
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active");
        }
    }

    // Navigation handlers
    @FXML
    private void handleDashboard() {
        // Already on dashboard
        setActiveButton(dashboardBtn);
    }

    @FXML
    private void handleInventory() {
        setActiveButton(inventoryBtn);
        navigateToPage("/fxml/staff-inventory.fxml", "/css/dashboard.css");
    }

    @FXML
    private void handleSales() {
        setActiveButton(salesBtn);
        navigateToPage("/fxml/staff-sales.fxml", "/css/sales.css");
    }

    @FXML
    private void handleReports() {
        setActiveButton(reportsBtn);
        navigateToPage("/fxml/staff-reports.fxml", "/css/staff-reports.css");
    }

    @FXML
    private void handleLogout() {
        boolean confirmed = showLogoutConfirmation();
        if (confirmed) {
            performLogout();
        }
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

    private void performLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            Scene currentScene = stage.getScene();

            Scene newScene = new Scene(root);

            // Load login CSS
            try {
                java.net.URL cssUrl = getClass().getResource("/css/styles.css");
                if (cssUrl != null) {
                    newScene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    cssUrl = getClass().getResource("/css/login.css");
                    if (cssUrl != null) {
                        newScene.getStylesheets().add(cssUrl.toExternalForm());
                    }
                }
            } catch (Exception cssEx) {
                System.err.println("Warning: Could not load CSS for login page");
            }

            root.setOpacity(0);

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(30),
                    currentScene.getRoot()
            );
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                this.currentUser = null;
                stage.setScene(newScene);
                stage.setWidth(800);
                stage.setHeight(600);
                stage.centerOnScreen();
                stage.setMaximized(false);

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
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void navigateToPage(String fxmlPath, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Pass current user to the next controller
            if (fxmlPath.contains("staff-inventory")) {
                StaffInventoryController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("staff-sales") || fxmlPath.contains("sales"))  {
                SalesController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("staff-reports")) {
                StaffReportsController controller = loader.getController();
                controller.setCurrentUser(currentUser);
                System.out.println("✅ Set user for StaffReportsController");
            }

            Stage stage = (Stage) dashboardBtn.getScene().getWindow();
            Scene currentScene = stage.getScene();

            // Save window size and position
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            double currentX = stage.getX();
            double currentY = stage.getY();
            boolean isMaximized = stage.isMaximized();

            Scene newScene = new Scene(root);
            newScene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            root.setOpacity(0);

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(30),
                    currentScene.getRoot()
            );
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                stage.setScene(newScene);

                // Restore window size and position
                if (isMaximized) {
                    stage.setMaximized(true);
                } else {
                    stage.setWidth(currentWidth);
                    stage.setHeight(currentHeight);
                    stage.setX(currentX);
                    stage.setY(currentY);
                }

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
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load page: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}