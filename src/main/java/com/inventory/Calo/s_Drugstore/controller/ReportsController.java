package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.Sale;
import com.inventory.Calo.s_Drugstore.entity.SaleItem;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.ProductService;
import com.inventory.Calo.s_Drugstore.service.SalesService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReportsController implements Initializable {

    @Autowired
    private SalesService salesService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;

    // FXML Components
    @FXML private Button dashboardBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button suppliersBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;

    // Chart containers
    @FXML private StackPane salesChartContainer;
    @FXML private StackPane categoryChartContainer;

    // KPI Labels
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label avgOrderValueLabel;
    @FXML private Label lowStockItemsLabel;

    // Expiring medicines table
    @FXML private TableView<Product> expiringMedicinesTable;
    @FXML private TableColumn<Product, String> medicineIdCol;
    @FXML private TableColumn<Product, String> medicineNameCol;
    @FXML private TableColumn<Product, String> expirationDateCol;
    @FXML private TableColumn<Product, String> daysLeftCol;
    @FXML private TableColumn<Product, String> stockCol;
    @FXML private TableColumn<Product, String> statusCol;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveButton(reportsBtn);
        setupExpiringMedicinesTable();
        setupTableColumns();
        loadDashboardData();
        loadSalesTrendsChart();
        loadCategoryChart();
        loadExpiringMedicines();
    }

    private void setupTableColumns() {
        // Set all column headers to CENTER_LEFT
        medicineIdCol.setStyle("-fx-alignment: CENTER-LEFT;");
        medicineNameCol.setStyle("-fx-alignment: CENTER-LEFT;");
        expirationDateCol.setStyle("-fx-alignment: CENTER-LEFT;");
        daysLeftCol.setStyle("-fx-alignment: CENTER-LEFT;");
        stockCol.setStyle("-fx-alignment: CENTER-LEFT;");
        statusCol.setStyle("-fx-alignment: CENTER-LEFT;");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }

    private void setActiveButton(Button activeBtn) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        suppliersBtn.getStyleClass().remove("active");
        settingsBtn.getStyleClass().remove("active");

        // Add active class to the clicked button
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active");
        }
    }
    private void loadDashboardData() {
        try {
            // Get ALL sales (not just today)
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = LocalDate.now().withDayOfMonth(
                    LocalDate.now().lengthOfMonth()).atTime(23, 59, 59);

            // You'll need to add this method to SalesService
            List<Sale> monthSales = salesService.getSalesBetweenDates(startOfMonth, endOfMonth);

            // Calculate metrics
            BigDecimal totalRevenue = monthSales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int totalTransactions = monthSales.size();

            BigDecimal avgOrder = totalTransactions > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(totalTransactions), 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;

            long lowStockCount = productService.getLowStockCount();

            // Update labels
            totalRevenueLabel.setText("₱" + String.format("%,.2f", totalRevenue));
            totalTransactionsLabel.setText(String.format("%,d", totalTransactions));
            avgOrderValueLabel.setText("₱" + String.format("%,.2f", avgOrder));
            lowStockItemsLabel.setText(String.valueOf(lowStockCount));

        } catch (Exception e) {
            e.printStackTrace();
            // Set default values on error
            totalRevenueLabel.setText("₱0.00");
            totalTransactionsLabel.setText("0");
            avgOrderValueLabel.setText("₱0.00");
            lowStockItemsLabel.setText("0");
        }
    }

    private void loadSalesTrendsChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);

        lineChart.setTitle("");
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sales");

        try {
            // Get last 7 days of sales
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);

                List<Sale> daySales = salesService.getSalesBetweenDates(startOfDay, endOfDay);
                BigDecimal dayTotal = daySales.stream()
                        .map(Sale::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                String dayLabel = date.format(DateTimeFormatter.ofPattern("EEE"));
                series.getData().add(new XYChart.Data<>(dayLabel, dayTotal.doubleValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Use sample data if error
            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : days) {
                series.getData().add(new XYChart.Data<>(day, 0));
            }
        }

        lineChart.getData().add(series);
        salesChartContainer.getChildren().clear();
        salesChartContainer.getChildren().add(lineChart);
    }

    private void loadCategoryChart() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("");
        pieChart.setLegendVisible(true);
        pieChart.setAnimated(true);

        try {
            // Get this month's sales
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = LocalDate.now().withDayOfMonth(
                    LocalDate.now().lengthOfMonth()).atTime(23, 59, 59);

            List<Sale> monthSales = salesService.getSalesBetweenDates(startOfMonth, endOfMonth);

            // Count sales by category
            Map<String, BigDecimal> categoryTotals = new HashMap<>();

            for (Sale sale : monthSales) {
                for (SaleItem item : sale.getItems()) {
                    // ✅ FIX: Use getMedicineId() instead of getProduct().getId()
                    String medicineId = item.getMedicineId();

                    if (medicineId != null) {
                        // Get product using the medicineId string
                        Optional<Product> productOpt = productService.getProductByMedicineId(medicineId);

                        if (productOpt.isPresent()) {
                            String category = productOpt.get().getCategory();
                            if (category != null) {
                                categoryTotals.merge(category, item.getSubtotal(), BigDecimal::add);
                            }
                        }
                    }
                }
            }

            // Calculate total
            BigDecimal total = categoryTotals.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Add to pie chart
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
                    double percentage = entry.getValue()
                            .divide(total, 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    pieChart.getData().add(new PieChart.Data(
                            entry.getKey() + " (" + String.format("%.1f%%", percentage) + ")",
                            entry.getValue().doubleValue()
                    ));
                }
            } else {
                // No data - show placeholder
                pieChart.getData().add(new PieChart.Data("No Data", 1));
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Show sample data on error
            pieChart.getData().addAll(
                    new PieChart.Data("Pain Relief (35%)", 35),
                    new PieChart.Data("Antibiotics (25%)", 25),
                    new PieChart.Data("Vitamins (20%)", 20),
                    new PieChart.Data("Cold & Flu (15%)", 15),
                    new PieChart.Data("Others (5%)", 5)
            );
        }

        categoryChartContainer.getChildren().clear();
        categoryChartContainer.getChildren().add(pieChart);
    }

    private void setupExpiringMedicinesTable() {
        medicineIdCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMedicineId()));

        medicineNameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        expirationDateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getExpirationDate().format(
                        DateTimeFormatter.ofPattern("MM/dd/yyyy"))));

        daysLeftCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Product product = getTableRow().getItem();
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), product.getExpirationDate());

                    if (daysLeft < 0) {
                        setText(Math.abs(daysLeft) + " days ago");
                        setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                    } else {
                        setText(daysLeft + " days");
                        setStyle(daysLeft <= 7 ? "-fx-text-fill: #F57C00; -fx-font-weight: bold;" : "");
                    }
                }
            }
        });
        daysLeftCol.setCellValueFactory(data -> new SimpleStringProperty(""));

        stockCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStock() + " units"));

        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Product product = getTableRow().getItem();
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), product.getExpirationDate());

                    Label badge = new Label();
                    if (daysLeft < 0) {
                        badge.setText("Expired");
                        badge.setStyle(
                                "-fx-background-color: #C62828; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-padding: 4px 12px; " +
                                        "-fx-background-radius: 12px; " +
                                        "-fx-font-size: 11px; " +
                                        "-fx-font-weight: bold;"
                        );
                    } else if (daysLeft <= 7) {
                        badge.setText("Critical");
                        badge.setStyle(
                                "-fx-background-color: #F57C00; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-padding: 4px 12px; " +
                                        "-fx-background-radius: 12px; " +
                                        "-fx-font-size: 11px; " +
                                        "-fx-font-weight: bold;"
                        );
                    } else {
                        badge.setText("Good");
                        badge.setStyle(
                                "-fx-background-color: #2E7D32; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-padding: 4px 12px; " +
                                        "-fx-background-radius: 12px; " +
                                        "-fx-font-size: 11px; " +
                                        "-fx-font-weight: bold;"
                        );
                    }

                    setGraphic(badge);
                }
            }
        });
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(""));
    }

    private void loadExpiringMedicines() {
        List<Product> expiringProducts = productService.getExpiringProducts();
        expiringMedicinesTable.setItems(FXCollections.observableArrayList(expiringProducts));

        if (expiringProducts.isEmpty()) {
            expiringMedicinesTable.setPlaceholder(new Label("No expiring medicines"));
        }
    }

    @FXML
    private void handleDailyView() {
        loadSalesTrendsChart(); // Reload with daily data
    }

    @FXML
    private void handleWeeklyView() {
        // TODO: Load weekly data
        loadSalesTrendsChart();
    }

    @FXML
    private void handleMonthlyView() {
        // TODO: Load monthly data
        loadSalesTrendsChart();
    }

    @FXML
    private void handleExportReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Report");
        alert.setHeaderText(null);
        alert.setContentText("Export feature coming soon!");
        alert.showAndWait();
    }

    // Navigation methods
    @FXML
    private void handleDashboard() {
        navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
    }

    @FXML
    private void handleInventory() {
        navigateToPage("/fxml/inventory.fxml", "/css/inventory.css");
    }

    @FXML
    private void handleSales() {
        navigateToPage("/fxml/sales.fxml", "/css/sales.css");
    }

    @FXML
    private void handleReports() {
        // Already on reports page
    }

    @FXML
    private void handleSuppliers() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(null);
        alert.setContentText("Suppliers module coming soon!");
        alert.showAndWait();
    }

    @FXML
    private void handleSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(null);
        alert.setContentText("Settings module coming soon!");
        alert.showAndWait();
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
}