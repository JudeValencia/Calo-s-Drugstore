package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.service.UserManagementService;
import com.inventory.Calo.s_Drugstore.util.IconUtil;
import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.Sale;
import com.inventory.Calo.s_Drugstore.entity.SaleItem;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.ProductService;
import com.inventory.Calo.s_Drugstore.service.SalesService;
import javafx.application.Platform;
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
import javafx.scene.layout.Priority;
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
import java.time.chrono.ChronoLocalDate;
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

    // Transaction Management table
    @FXML private TableView<Sale> transactionsTable;
    @FXML private TableColumn<Sale, String> txnIdCol;
    @FXML private TableColumn<Sale, String> txnDateCol;
    @FXML private TableColumn<Sale, String> txnItemsCol;
    @FXML private TableColumn<Sale, String> txnTotalCol;
    @FXML private TableColumn<Sale, String> txnStaffCol;
    @FXML private TableColumn<Sale, Void> txnActionsCol;

    @Autowired
    private UserManagementService userManagementService; // Add this autowired dependency

    // Current view mode for sales chart
    private String currentViewMode = "DAILY";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveButton(reportsBtn);
        setupExpiringMedicinesTable();
        setupTopSellingTable();
        setupTableColumns();
        setupTransactionsTable();
        loadDashboardData();
        loadSalesTrendsChart();
        loadCategoryChart();
        loadExpiringMedicines();
        loadTopSellingMedicines();

        Platform.runLater(this::loadAllTransactions);
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
        topSellingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
            if (daysLeft > 0) return new SimpleStringProperty(String.valueOf(daysLeft));
            else return new SimpleStringProperty(String.valueOf(0));
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

//    private void showComingSoon(String feature) {
//        showStyledAlert(Alert.AlertType.INFORMATION, "Coming Soon",
//                feature + " module is under development and will be available soon!");
//    }

    @FXML
    private void handleLogout() {
        setActiveButton(logoutBtn);
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

    private void setupTransactionsTable() {
        txnIdCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTransactionId()));
        txnIdCol.setStyle("-fx-alignment: CENTER-LEFT;");

        txnDateCol.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. d, yyyy h:mm a");
            return new SimpleStringProperty(data.getValue().getSaleDate().format(formatter));
        });
        txnDateCol.setStyle("-fx-alignment: CENTER-LEFT;");

        txnItemsCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getTotalItems())));
        txnItemsCol.setStyle("-fx-alignment: CENTER;");
        txnItemsCol.setCellFactory(column -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item + " items");
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                    setAlignment(Pos.CENTER);
                }
            }
        });

        txnTotalCol.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + String.format("%.2f", data.getValue().getTotalAmount())));
        txnTotalCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        txnTotalCol.setCellFactory(column -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });

        txnStaffCol.setCellValueFactory(data -> {
            Long userId = data.getValue().getUserId();
            Optional<User> userOpt = userManagementService.getUserById(userId);
            return new SimpleStringProperty(userOpt.isPresent() ? userOpt.get().getFullName() : "Unknown");
        });
        txnStaffCol.setStyle("-fx-alignment: CENTER-LEFT;");

        txnActionsCol.setCellFactory(column -> new TableCell<Sale, Void>() {
            private final Button viewBtn = new Button("👁");
            private final Button editBtn = new Button("🔧");
            private final Button deleteBtn = new Button("🗑");
            private final HBox buttons = new HBox(8);

            {
                buttons.setAlignment(Pos.CENTER_LEFT);

                String viewStyle =
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #2196F3; " +
                                "-fx-font-size: 12px; " +
                                "-fx-padding: 6px 12px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #2196F3; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;";

                String editStyle =
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #FF9800; " +
                                "-fx-font-size: 12px; " +
                                "-fx-padding: 6px 12px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #FF9800; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;";

                String deleteStyle =
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #F44336; " +
                                "-fx-font-size: 12px; " +
                                "-fx-padding: 6px 12px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #F44336; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;";

                viewBtn.setStyle(viewStyle);
                editBtn.setStyle(editStyle);
                deleteBtn.setStyle(deleteStyle);

                buttons.getChildren().addAll(viewBtn, editBtn, deleteBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                Sale sale = getTableRow() != null ? getTableRow().getItem() : null;

                if (empty || sale == null) {
                    setGraphic(null);
                } else {
                    viewBtn.setOnAction(e -> handleViewTransaction(sale));
                    editBtn.setOnAction(e -> handleEditTransaction(sale));
                    deleteBtn.setOnAction(e -> handleDeleteTransaction(sale));

                    setGraphic(buttons);
                }
            }
        });
    }

    @FXML
    private void handleRefreshTransactions() {
        loadAllTransactions();
    }

    private void loadAllTransactions() {
        try {
            // Get last 30 days of transactions
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();

            List<Sale> transactions = salesService.getSalesBetweenDates(thirtyDaysAgo, now);

            // Sort by date descending (newest first)
            transactions.sort((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()));

            transactionsTable.setItems(FXCollections.observableArrayList(transactions));

            if (transactions.isEmpty()) {
                transactionsTable.setPlaceholder(new Label("No transactions found in the last 30 days"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error", "Failed to load transactions: " + e.getMessage());
        }
    }

    private void handleViewTransaction(Sale sale) {
        if (sale == null) return;

        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Transaction Details");
        dialogStage.setResizable(true);
        dialogStage.setMinWidth(650);
        dialogStage.setMinHeight(400);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");
        mainContainer.setPrefWidth(650);
        mainContainer.setMaxWidth(650);

        // Header
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 32px;");

        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Transaction: " + sale.getTransactionId());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' h:mm a");
        Label dateLabel = new Label(sale.getSaleDate().format(formatter));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        // Add staff info
        Optional<User> userOpt = userManagementService.getUserById(sale.getUserId());
        Label staffLabel = new Label("Staff: " + (userOpt.isPresent() ? userOpt.get().getFullName() : "Unknown"));
        staffLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6;");

        titleBox.getChildren().addAll(titleLabel, dateLabel, staffLabel);
        headerBox.getChildren().addAll(iconLabel, titleBox);

        Separator separator1 = new Separator();

        // Items Title
        Label itemsTitle = new Label("Items Purchased:");
        itemsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Scrollable Items List
        VBox itemsList = new VBox(12);
        itemsList.setStyle("-fx-background-color: #F8F9FA; -fx-padding: 15; -fx-background-radius: 8px;");

        for (SaleItem item : sale.getItems()) {
            VBox itemBox = new VBox(5);

            HBox itemNameRow = new HBox();
            itemNameRow.setAlignment(Pos.CENTER_LEFT);

            Label itemName = new Label(item.getMedicineName());
            itemName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            itemName.setWrapText(true);
            HBox.setHgrow(itemName, Priority.ALWAYS);

            Label itemTotal = new Label("₱" + String.format("%.2f", item.getSubtotal()));
            itemTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            itemNameRow.getChildren().addAll(itemName, itemTotal);

            Label itemDetails = new Label(item.getQuantity() + " × ₱" +
                    String.format("%.2f", item.getUnitPrice()) + " each");
            itemDetails.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

            itemBox.getChildren().addAll(itemNameRow, itemDetails);
            itemsList.getChildren().add(itemBox);
        }

        // Wrap items list in ScrollPane
        ScrollPane scrollPane = new ScrollPane(itemsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPrefHeight(300);
        scrollPane.setMaxHeight(400);
        scrollPane.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background: transparent; " +
                        "-fx-border-color: transparent;"
        );
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Separator separator2 = new Separator();

        // Total
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-background-color: #E8F5E9; -fx-padding: 15; -fx-background-radius: 8px;");

        Label totalLabel = new Label("Total Amount:");
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(totalLabel, Priority.ALWAYS);

        Label totalAmount = new Label("₱" + String.format("%.2f", sale.getTotalAmount()));
        totalAmount.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        totalRow.getChildren().addAll(totalLabel, totalAmount);

        // Button
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Close");
        closeButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 40px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> dialogStage.close());
        buttonBox.getChildren().add(closeButton);

        mainContainer.getChildren().addAll(
                headerBox,
                separator1,
                itemsTitle,
                scrollPane,
                separator2,
                totalRow,
                buttonBox
        );

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void handleEditTransaction(Sale sale) {
        LocalDate today = LocalDate.now();
        LocalDate transactionDate = sale.getSaleDate().toLocalDate();

        if (today.isEqual(transactionDate)) {
            showTransactionDetailsDialog(sale, true);
        } else
            showTransactionDetailsDialog(sale, false);
    }

    private void showTransactionDetailsDialog(Sale sale, boolean editable) {
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(editable ? "Edit Transaction" : "View Transaction");
        dialogStage.setResizable(true);
        dialogStage.setMinWidth(650);
        dialogStage.setMinHeight(450);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");
        mainContainer.setPrefWidth(650);

        // STORE ORIGINAL QUANTITIES AT THE START
        Map<String, Integer> originalQuantities = new HashMap<>();
        for (SaleItem item : sale.getItems()) {
            originalQuantities.put(item.getMedicineId(), item.getQuantity());
        }

        // Header
        Label titleLabel = new Label(editable ? "Edit Transaction" : "Transaction Details");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Transaction Info
        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-background-color: #F8F9FA; -fx-padding: 15; -fx-background-radius: 8px;");

        Label txnIdLabel = new Label("Transaction ID: " + sale.getTransactionId());
        txnIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. d, yyyy h:mm a");
        Label dateLabel = new Label("Date: " + sale.getSaleDate().format(formatter));
        dateLabel.setStyle("-fx-font-size: 14px;");

        Optional<User> userOpt = userManagementService.getUserById(sale.getUserId());
        Label staffLabel = new Label("Staff: " + (userOpt.isPresent() ? userOpt.get().getFullName() : "Unknown"));
        staffLabel.setStyle("-fx-font-size: 14px;");

        infoBox.getChildren().addAll(txnIdLabel, dateLabel, staffLabel);

        // Items Table
        Label itemsLabel = new Label("Transaction Items:");
        itemsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<SaleItem> itemsTable = new TableView<>();
        itemsTable.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-background-radius: 8px;"
        );
        itemsTable.setPrefHeight(250);

        // Medicine Column
        TableColumn<SaleItem, String> itemNameCol = new TableColumn<>("Medicine");
        itemNameCol.setPrefWidth(250);
        itemNameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMedicineName()));
        itemNameCol.setStyle("-fx-alignment: CENTER-LEFT;");

        // Quantity Column with +/- controls
        TableColumn<SaleItem, Integer> itemQtyCol = new TableColumn<>("Quantity");
        itemQtyCol.setPrefWidth(editable ? 180 : 100);
        itemQtyCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        itemQtyCol.setStyle("-fx-alignment: CENTER;");

        if (editable) {
            itemQtyCol.setCellFactory(column -> new TableCell<SaleItem, Integer>() {
                private HBox controls;
                private Button minusBtn;
                private Label qtyLabel;
                private Button plusBtn;

                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);

                    SaleItem saleItem = getTableRow() != null ? getTableRow().getItem() : null;

                    if (empty || saleItem == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        if (controls == null) {
                            controls = new HBox(10);
                            controls.setAlignment(Pos.CENTER);

                            minusBtn = new Button("−");
                            qtyLabel = new Label();
                            plusBtn = new Button("+");

                            // Button styling
                            String btnStyle =
                                    "-fx-background-color: white; " +
                                            "-fx-text-fill: #2c3e50; " +
                                            "-fx-font-size: 14px; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-min-width: 32px; " +
                                            "-fx-min-height: 32px; " +
                                            "-fx-max-width: 32px; " +
                                            "-fx-max-height: 32px; " +
                                            "-fx-background-radius: 6px; " +
                                            "-fx-border-color: #E0E0E0; " +
                                            "-fx-border-width: 1.5px; " +
                                            "-fx-border-radius: 6px; " +
                                            "-fx-cursor: hand;";

                            minusBtn.setStyle(btnStyle);
                            plusBtn.setStyle(btnStyle);

                            qtyLabel.setStyle(
                                    "-fx-font-size: 15px; " +
                                            "-fx-font-weight: 600; " +
                                            "-fx-text-fill: #2c3e50; " +
                                            "-fx-min-width: 40px; " +
                                            "-fx-alignment: CENTER;"
                            );

                            controls.getChildren().addAll(minusBtn, qtyLabel, plusBtn);
                        }

                        qtyLabel.setText(String.valueOf(saleItem.getQuantity()));

                        minusBtn.setOnAction(e -> {
                            if (saleItem.getQuantity() > 1) {
                                saleItem.setQuantity(saleItem.getQuantity() - 1);
                                // Recalculate subtotal
                                saleItem.setSubtotal(saleItem.getUnitPrice().multiply(
                                        BigDecimal.valueOf(saleItem.getQuantity())));
                                itemsTable.refresh();
                                updateTotalDisplay(itemsTable.getItems(), (Label) dialogStage.getUserData());
                            }
                        });

                        plusBtn.setOnAction(e -> {
                            // Calculate available stock considering original quantity
                            String medicineId = saleItem.getMedicineId();
                            int originalQty = originalQuantities.getOrDefault(medicineId, 0);

                            List<Product> allProducts = productService.getAllProducts();
                            Optional<Product> productOpt = allProducts.stream()
                                    .filter(p -> p.getMedicineId().equals(medicineId))
                                    .findFirst();

                            if (productOpt.isPresent()) {
                                Product product = productOpt.get();
                                // Available = current stock + what was originally taken
                                int availableStock = product.getStock() + originalQty;

                                if (saleItem.getQuantity() < availableStock) {
                                    saleItem.setQuantity(saleItem.getQuantity() + 1);
                                    // Recalculate subtotal
                                    saleItem.setSubtotal(saleItem.getUnitPrice().multiply(
                                            BigDecimal.valueOf(saleItem.getQuantity())));
                                    itemsTable.refresh();
                                    updateTotalDisplay(itemsTable.getItems(), (Label) dialogStage.getUserData());
                                } else {
                                    showStyledAlert(Alert.AlertType.WARNING, "Stock Limit",
                                            "Cannot exceed available stock: " + availableStock +
                                                    " (Current inventory: " + product.getStock() +
                                                    " + Original quantity: " + originalQty + ")");
                                }
                            }
                        });

                        setGraphic(controls);
                    }
                }
            });
        } else {
            itemQtyCol.setCellFactory(column -> new TableCell<SaleItem, Integer>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.valueOf(item));
                        setAlignment(Pos.CENTER);
                    }
                }
            });
        }

        // Unit Price Column
        TableColumn<SaleItem, String> itemPriceCol = new TableColumn<>("Unit Price");
        itemPriceCol.setPrefWidth(120);
        itemPriceCol.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + String.format("%.2f", data.getValue().getUnitPrice())));
        itemPriceCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Subtotal Column
        TableColumn<SaleItem, String> itemSubtotalCol = new TableColumn<>("Subtotal");
        itemSubtotalCol.setPrefWidth(120);
        itemSubtotalCol.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + String.format("%.2f", data.getValue().getSubtotal())));
        itemSubtotalCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        itemSubtotalCol.setCellFactory(column -> new TableCell<SaleItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });

        if (editable) {
            // Actions Column (Remove button)
            TableColumn<SaleItem, Void> itemActionsCol = new TableColumn<>("Actions");
            itemActionsCol.setPrefWidth(100);
            itemActionsCol.setCellFactory(column -> new TableCell<SaleItem, Void>() {
                private final Button deleteBtn = new Button("Remove");

                {
                    deleteBtn.setStyle(
                            "-fx-background-color: #F44336; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-padding: 6px 12px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-cursor: hand;"
                    );
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);

                    SaleItem saleItem = getTableRow() != null ? getTableRow().getItem() : null;

                    if (empty || saleItem == null) {
                        setGraphic(null);
                    } else {
                        deleteBtn.setOnAction(e -> {
                            if (itemsTable.getItems().size() > 1) {
                                itemsTable.getItems().remove(saleItem);
                                updateTotalDisplay(itemsTable.getItems(), (Label) dialogStage.getUserData());
                            } else {
                                showStyledAlert(Alert.AlertType.WARNING, "Cannot Remove",
                                        "Transaction must have at least one item.");
                            }
                        });
                        setGraphic(deleteBtn);
                        setAlignment(Pos.CENTER);
                    }
                }
            });

            itemsTable.getColumns().addAll(itemNameCol, itemQtyCol, itemPriceCol, itemSubtotalCol, itemActionsCol);
        } else {
            itemsTable.getColumns().addAll(itemNameCol, itemQtyCol, itemPriceCol, itemSubtotalCol);
        }

        itemsTable.setItems(FXCollections.observableArrayList(sale.getItems()));

        // Wrap table in ScrollPane
        ScrollPane tableScrollPane = new ScrollPane(itemsTable);
        tableScrollPane.setFitToWidth(true);
        tableScrollPane.setFitToHeight(true);
        tableScrollPane.setPrefHeight(250);
        tableScrollPane.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background: transparent; " +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(tableScrollPane, Priority.ALWAYS);

        // Total
        HBox totalBox = new HBox(10);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setStyle(
                "-fx-padding: 15px 20px; " +
                        "-fx-background-color: #E8F5E9; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: #81C784; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10px;"
        );

        Label totalLabel = new Label("Total Amount:");
        totalLabel.setStyle(
                "-fx-font-size: 15px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #1B5E20;"
        );

        Label totalValue = new Label("₱" + String.format("%.2f", sale.getTotalAmount()));
        totalValue.setStyle(
                "-fx-font-size: 32px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #2E7D32;"
        );

        // Store reference for updates
        dialogStage.setUserData(totalValue);

        totalBox.getChildren().addAll(totalLabel, totalValue);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button(editable ? "Cancel" : "Close");
        closeButton.setStyle(
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
        closeButton.setOnAction(e -> dialogStage.close());

        if (editable) {
            Button saveButton = new Button("Save Changes");
            saveButton.setStyle(
                    "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 12px 30px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-cursor: hand;"
            );

            saveButton.setOnAction(e -> {
                if (itemsTable.getItems().isEmpty()) {
                    showStyledAlert(Alert.AlertType.WARNING, "Empty Transaction",
                            "Transaction must have at least one item.");
                    return;
                }

                if (showConfirmation("Save Changes",
                        "Are you sure you want to save changes to this transaction?\n" +
                                "This will update inventory levels accordingly.")) {
                    try {
                        // Use the originalQuantities map we created at the start
                        // ⭐ THIS IS THE KEY - we're using the map created when dialog opened

                        // Create new list of items from table
                        List<SaleItem> updatedItems = new ArrayList<>(itemsTable.getItems());

                        // Update sale with new items list
                        sale.getItems().clear();
                        for (SaleItem item : updatedItems) {
                            sale.addItem(item);
                        }

                        // Save changes with inventory update
                        salesService.updateTransactionWithInventory(sale, originalQuantities);

                        showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                                "Transaction updated successfully!\nInventory has been adjusted.");
                        loadAllTransactions();
                        dialogStage.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showStyledAlert(Alert.AlertType.ERROR, "Error",
                                "Failed to update transaction: " + ex.getMessage());
                    }
                }
            });

            buttonBox.getChildren().addAll(closeButton, saveButton);
        } else {
            buttonBox.getChildren().add(closeButton);
        }

        mainContainer.getChildren().addAll(titleLabel, infoBox, itemsLabel, tableScrollPane, totalBox, buttonBox);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void updateTotalDisplay(List<SaleItem> items, Label totalValueLabel) {
        BigDecimal newTotal = items.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Platform.runLater(() -> {
            totalValueLabel.setText("₱" + String.format("%.2f", newTotal));
        });
    }

    private boolean showConfirmation(String title, String message) {
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);
        dialogStage.setUserData(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-line-spacing: 3px;");

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

        Button confirmButton = new Button("Confirm");
        confirmButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );
        confirmButton.setOnAction(e -> {
            dialogStage.setUserData(true);
            dialogStage.close();
        });

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
    }

    private void handleDeleteTransaction(Sale sale) {
        LocalDate today = LocalDate.now();
        LocalDate transactionDate = sale.getSaleDate().toLocalDate();

        if (today.isEqual(transactionDate)) {
            if (showConfirmation("Delete Transaction",
                    "Are you sure you want to delete transaction " + sale.getTransactionId() + "?\n\n" +
                            "⚠ WARNING: This action cannot be undone!\n\n" +
                            "The following will happen:\n" +
                            "• Transaction will be permanently deleted\n" +
                            "• Inventory will be restored for all items\n" +
                            "• This affects your sales reports and statistics")) {
                try {
                    salesService.deleteTransaction(sale.getId());
                    showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                            "Transaction deleted successfully!\nInventory has been restored.");
                    loadAllTransactions();
                } catch (Exception e) {
                    e.printStackTrace();
                    showStyledAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to delete transaction: " + e.getMessage());
                }
            }
        } else {
            showStyledAlert(Alert.AlertType.ERROR,"Error","Cannot delete transactions that are not within the day!");
        }
    }
}