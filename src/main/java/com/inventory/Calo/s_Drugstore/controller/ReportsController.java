package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.Sale;
import com.inventory.Calo.s_Drugstore.entity.SaleItem;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.ProductService;
import com.inventory.Calo.s_Drugstore.service.SalesService;
import com.inventory.Calo.s_Drugstore.util.IconUtil;
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
import java.time.temporal.TemporalAdjusters;
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
    @FXML private Button staffBtn;
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

    // Top Selling Medicines table
    @FXML private TableView<Map<String, Object>> topSellingTable;
    @FXML private TableColumn<Map<String, Object>, String> rankCol;
    @FXML private TableColumn<Map<String, Object>, String> topMedicineNameCol;
    @FXML private TableColumn<Map<String, Object>, String> categoryCol;
    @FXML private TableColumn<Map<String, Object>, String> quantitySoldCol;

    // Current view mode for sales chart
    private String currentViewMode = "DAILY";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveButton(reportsBtn);
        setupExpiringMedicinesTable();
        setupTopSellingTable();
        setupTableColumns();
        loadDashboardData();
        loadSalesTrendsChart();
        loadCategoryChart();
        loadExpiringMedicines();
        loadTopSellingMedicines();
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

    private void setupTopSellingTable() {
        // Setup rank column with green numbering
        rankCol.setCellValueFactory(cellData -> {
            Integer rank = (Integer) cellData.getValue().get("rank");
            return new SimpleStringProperty("#" + rank);
        });
        rankCol.setStyle("-fx-alignment: CENTER-LEFT;");

        rankCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
            }
        });

        // Setup medicine name column
        topMedicineNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("medicineName")));
        topMedicineNameCol.setStyle("-fx-alignment: CENTER-LEFT;");

        // Setup category column with pill-style badges
        categoryCol.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("category")));
        categoryCol.setStyle("-fx-alignment: CENTER-LEFT;");

        categoryCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label categoryLabel = new Label(item);
                    categoryLabel.setStyle(
                            "-fx-background-color: #f5f7fa; " +
                                    "-fx-padding: 5px 12px; " +
                                    "-fx-background-radius: 12px; " +
                                    "-fx-text-fill: #2c3e50; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-font-weight: 600;"
                    );
                    setGraphic(categoryLabel);
                    setText(null);
                }
            }
        });

        // Setup quantity column
        quantitySoldCol.setCellValueFactory(cellData -> {
            Integer quantity = (Integer) cellData.getValue().get("quantitySold");
            return new SimpleStringProperty(String.valueOf(quantity));
        });
        quantitySoldCol.setStyle("-fx-alignment: CENTER-LEFT;");

        quantitySoldCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                }
            }
        });
    }

    private void loadTopSellingMedicines() {
        try {
            // Get sales from last 7 days
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            LocalDateTime now = LocalDateTime.now();

            List<Sale> weekSales = salesService.getSalesBetweenDates(weekAgo, now);

            // Count quantities by medicine
            Map<String, Map<String, Object>> medicineStats = new HashMap<>();

            for (Sale sale : weekSales) {
                List<SaleItem> items = sale.getItems();
                for (SaleItem item : items) {
                    String medicineName = item.getMedicineName();

                    if (!medicineStats.containsKey(medicineName)) {
                        // Find the product to get category
                        Optional<Product> productOpt = productService.getProductByMedicineId(item.getMedicineId());
                        String category = productOpt.map(Product::getCategory).orElse("Unknown");

                        Map<String, Object> stats = new HashMap<>();
                        stats.put("medicineName", medicineName);
                        stats.put("category", category);
                        stats.put("quantitySold", 0);
                        medicineStats.put(medicineName, stats);
                    }

                    Map<String, Object> stats = medicineStats.get(medicineName);
                    Integer currentQty = (Integer) stats.get("quantitySold");
                    stats.put("quantitySold", currentQty + item.getQuantity());
                }
            }

            // Sort by quantity and take top 5
            List<Map<String, Object>> topMedicines = medicineStats.values().stream()
                    .sorted((a, b) -> Integer.compare(
                            (Integer) b.get("quantitySold"),
                            (Integer) a.get("quantitySold")
                    ))
                    .limit(5)
                    .collect(Collectors.toList());

            // Add rank numbers
            for (int i = 0; i < topMedicines.size(); i++) {
                topMedicines.get(i).put("rank", i + 1);
            }

            topSellingTable.setItems(FXCollections.observableArrayList(topMedicines));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading top selling medicines: " + e.getMessage());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }

    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        staffBtn.getStyleClass().remove("active");

        // Add active class to the clicked button
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void loadDashboardData() {
        try {
            // Get current month sales
            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = LocalDate.now().withDayOfMonth(
                    LocalDate.now().lengthOfMonth()).atTime(23, 59, 59);

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

    @FXML
    private void handleDailyView() {
        currentViewMode = "DAILY";
        loadSalesTrendsChart();
    }

    @FXML
    private void handleWeeklyView() {
        currentViewMode = "WEEKLY";
        loadSalesTrendsChart();
    }

    @FXML
    private void handleMonthlyView() {
        currentViewMode = "MONTHLY";
        loadSalesTrendsChart();
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
            if ("DAILY".equals(currentViewMode)) {
                loadDailySalesTrend(series);
            } else if ("WEEKLY".equals(currentViewMode)) {
                loadWeeklySalesTrend(series);
            } else if ("MONTHLY".equals(currentViewMode)) {
                loadMonthlySalesTrend(series);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Use sample data if error
            String[] labels = {"Period 1", "Period 2", "Period 3", "Period 4", "Period 5"};
            for (String label : labels) {
                series.getData().add(new XYChart.Data<>(label, 0));
            }
        }

        lineChart.getData().add(series);
        salesChartContainer.getChildren().clear();
        salesChartContainer.getChildren().add(lineChart);
    }

    private void loadDailySalesTrend(XYChart.Series<String, Number> series) {
        // Last 7 days
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
    }

    private void loadWeeklySalesTrend(XYChart.Series<String, Number> series) {
        // Last 8 weeks
        LocalDate today = LocalDate.now();
        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);

            LocalDateTime startOfWeek = weekStart.atStartOfDay();
            LocalDateTime endOfWeek = weekEnd.atTime(23, 59, 59);

            List<Sale> weekSales = salesService.getSalesBetweenDates(startOfWeek, endOfWeek);
            BigDecimal weekTotal = weekSales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String weekLabel = "Week " + weekStart.format(DateTimeFormatter.ofPattern("MMM dd"));
            series.getData().add(new XYChart.Data<>(weekLabel, weekTotal.doubleValue()));
        }
    }

    private void loadMonthlySalesTrend(XYChart.Series<String, Number> series) {
        // Last 6 months
        LocalDate today = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            LocalDate monthStart = month.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate monthEnd = month.with(TemporalAdjusters.lastDayOfMonth());

            LocalDateTime startOfMonth = monthStart.atStartOfDay();
            LocalDateTime endOfMonth = monthEnd.atTime(23, 59, 59);

            List<Sale> monthSales = salesService.getSalesBetweenDates(startOfMonth, endOfMonth);
            BigDecimal monthTotal = monthSales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String monthLabel = month.format(DateTimeFormatter.ofPattern("MMM"));
            series.getData().add(new XYChart.Data<>(monthLabel, monthTotal.doubleValue()));
        }
    }

    private void loadCategoryChart() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("");
        pieChart.setLegendVisible(true);
        pieChart.setAnimated(true);

        try {
            // Get all sales
            // Get sales from last 30 days
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();
            List<Sale> allSales = salesService.getSalesBetweenDates(thirtyDaysAgo, now);

            // Count quantities by category
            Map<String, Integer> categoryCount = new HashMap<>();

            for (Sale sale : allSales) {
                for (SaleItem item : sale.getItems()) {
                    Optional<Product> productOpt = productService.getProductByMedicineId(item.getMedicineId());
                    if (productOpt.isPresent()) {
                        String category = productOpt.get().getCategory();
                        categoryCount.put(category, categoryCount.getOrDefault(category, 0) + item.getQuantity());
                    }
                }
            }

            // Create pie chart data
            for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                pieChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Use sample data if error
            pieChart.getData().addAll(
                    new PieChart.Data("Pain Relief", 35),
                    new PieChart.Data("Antibiotics", 25),
                    new PieChart.Data("Vitamins", 20),
                    new PieChart.Data("Cold & Flu", 15),
                    new PieChart.Data("Others", 5)
            );
        }

        categoryChartContainer.getChildren().clear();
        categoryChartContainer.getChildren().add(pieChart);
    }

    private void setupExpiringMedicinesTable() {
        medicineIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedicineId()));
        medicineNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        expirationDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getExpirationDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));

        daysLeftCol.setCellValueFactory(cellData -> {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), cellData.getValue().getExpirationDate());
            return new SimpleStringProperty(String.valueOf(daysLeft));
        });

        stockCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getStock())));

        statusCol.setCellValueFactory(cellData -> {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), cellData.getValue().getExpirationDate());
            if (daysLeft < 0) return new SimpleStringProperty("Expired");
            else if (daysLeft <= 7) return new SimpleStringProperty("Critical");
            else if (daysLeft <= 30) return new SimpleStringProperty("Warning");
            else return new SimpleStringProperty("Good");
        });

        // Style the status column
        statusCol.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(item);
                    String style = "-fx-padding: 5px 12px; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;";

                    switch (item) {
                        case "Expired":
                            statusLabel.setStyle(style + " -fx-background-color: #c62828; -fx-text-fill: white;");
                            break;
                        case "Critical":
                            statusLabel.setStyle(style + " -fx-background-color: #f57c00; -fx-text-fill: white;");
                            break;
                        case "Warning":
                            statusLabel.setStyle(style + " -fx-background-color: #fbc02d; -fx-text-fill: #2c3e50;");
                            break;
                        default:
                            statusLabel.setStyle(style + " -fx-background-color: #2e7d32; -fx-text-fill: white;");
                    }

                    setGraphic(statusLabel);
                    setText(null);
                }
            }
        });
    }

    private void loadExpiringMedicines() {
        try {
            // getExpiringProducts() already returns products expiring within 30 days
            List<Product> expiringProducts = productService.getExpiringProducts();

            expiringMedicinesTable.setItems(FXCollections.observableArrayList(expiringProducts));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Navigation methods
    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
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
        // Already on reports page
    }

    @FXML
    private void handleStaff() {
        setActiveButton(staffBtn);
        navigateToPage("/fxml/staff.fxml", "/css/staff.css");
    }

    private void showComingSoon(String feature) {
        showStyledAlert(Alert.AlertType.INFORMATION, "Coming Soon",
                feature + " module is under development and will be available soon!");
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

        buttonBox.getChildren().addAll(cancelButton, logoutButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
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
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-line-spacing: 3px;");

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button okButton = new Button("OK");

        String buttonColor = "#4CAF50";
        String buttonHoverColor = "#45a049";

        if (type == Alert.AlertType.ERROR) {
            buttonColor = "#dc3545";
            buttonHoverColor = "#c82333";
        } else if (type == Alert.AlertType.WARNING) {
            buttonColor = "#FF9800";
            buttonHoverColor = "#f57c00";
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
                controller.refreshDashboard();
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

                // RESTORE WINDOW SIZE AND POSITION
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
            showStyledAlert(Alert.AlertType.ERROR, "Error", "Failed to load page: " + e.getMessage());
        }
    }
}