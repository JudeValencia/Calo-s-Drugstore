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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.print.PrinterJob;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    @FXML private Button productsBtn;
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

    private LocalDate selectedMonth = LocalDate.now();
    private LocalDate exportMonth = LocalDate.now();
    @FXML private ComboBox<String> monthSelectorCombo;

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
    
    // Flag to track if showing voided transactions
    private boolean showVoidedTransactions = false;
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
        setupMonthSelector();

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

        // Set title based on selected month
        DateTimeFormatter titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        pieChart.setTitle("Sales by Category - " + selectedMonth.format(titleFormatter));
        pieChart.setLegendVisible(true);
        pieChart.setAnimated(true);

        try {
            // Get sales for selected month
            LocalDate monthStart = selectedMonth.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate monthEnd = selectedMonth.with(TemporalAdjusters.lastDayOfMonth());

            LocalDateTime startOfMonth = monthStart.atStartOfDay();
            LocalDateTime endOfMonth = monthEnd.atTime(23, 59, 59);

            List<Sale> monthSales = salesService.getSalesBetweenDates(startOfMonth, endOfMonth);

            // Count quantities by category
            Map<String, Integer> categoryCount = new HashMap<>();

            for (Sale sale : monthSales) {
                for (SaleItem item : sale.getItems()) {
                    Optional<Product> productOpt = productService.getProductByMedicineId(item.getMedicineId());
                    if (productOpt.isPresent()) {
                        String category = productOpt.get().getCategory();
                        categoryCount.put(category, categoryCount.getOrDefault(category, 0) + item.getQuantity());
                    }
                }
            }

            // Create pie chart data
            if (categoryCount.isEmpty()) {
                pieChart.getData().add(new PieChart.Data("No Sales", 1));
            } else {
                for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                    pieChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
                }
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

        expiringMedicinesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        medicineIdCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedicineId()));

        medicineNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBrandName()));

        // ✅ FIXED: Get expiration date from batches
        expirationDateCol.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            List<com.inventory.Calo.s_Drugstore.entity.Batch> batches = productService.getBatchesForProduct(product);

            if (!batches.isEmpty()) {
                // Get earliest expiration date
                LocalDate earliestExpiry = batches.stream()
                        .map(com.inventory.Calo.s_Drugstore.entity.Batch::getExpirationDate)
                        .filter(date -> date != null)
                        .min(LocalDate::compareTo)
                        .orElse(null);

                if (earliestExpiry != null) {
                    return new SimpleStringProperty(earliestExpiry.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                }
            }

            // Fallback to product expiration date if no batches
            if (product.getExpirationDate() != null) {
                return new SimpleStringProperty(product.getExpirationDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            }

            return new SimpleStringProperty("N/A");
        });

        // ✅ FIXED: Calculate days left from batches
        daysLeftCol.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            List<com.inventory.Calo.s_Drugstore.entity.Batch> batches = productService.getBatchesForProduct(product);

            LocalDate expiryDate = null;

            if (!batches.isEmpty()) {
                // Get earliest expiration date
                expiryDate = batches.stream()
                        .map(com.inventory.Calo.s_Drugstore.entity.Batch::getExpirationDate)
                        .filter(date -> date != null)
                        .min(LocalDate::compareTo)
                        .orElse(null);
            }

            // Fallback to product expiration date
            if (expiryDate == null) {
                expiryDate = product.getExpirationDate();
            }

            if (expiryDate != null) {
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
                return new SimpleStringProperty(String.valueOf(Math.max(0, daysLeft)));
            }

            return new SimpleStringProperty("N/A");
        });

        stockCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getStock())));

        // ✅ FIXED: Calculate status from batches
        statusCol.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            List<com.inventory.Calo.s_Drugstore.entity.Batch> batches = productService.getBatchesForProduct(product);

            LocalDate expiryDate = null;

            if (!batches.isEmpty()) {
                // Get earliest expiration date
                expiryDate = batches.stream()
                        .map(com.inventory.Calo.s_Drugstore.entity.Batch::getExpirationDate)
                        .filter(date -> date != null)
                        .min(LocalDate::compareTo)
                        .orElse(null);
            }

            // Fallback to product expiration date
            if (expiryDate == null) {
                expiryDate = product.getExpirationDate();
            }

            if (expiryDate != null) {
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
                if (daysLeft < 0) return new SimpleStringProperty("Expired");
                else if (daysLeft <= 7) return new SimpleStringProperty("Critical");
                else if (daysLeft <= 30) return new SimpleStringProperty("Warning");
                else return new SimpleStringProperty("Good");
            }

            return new SimpleStringProperty("Unknown");
        });

        // Style the status column
        statusCol.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setText(null);
                setGraphic(null);

                if (!empty && item != null) {
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
                        case "Good":
                            statusLabel.setStyle(style + " -fx-background-color: #2e7d32; -fx-text-fill: white;");
                            break;
                        default:
                            statusLabel.setStyle(style + " -fx-background-color: #9e9e9e; -fx-text-fill: white;");
                    }

                    setGraphic(statusLabel);
                }
            }
        });
    }

    private void setupMonthSelector() {
        if (monthSelectorCombo != null) {
            // Generate last 12 months
            List<String> months = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");

            for (int i = 0; i < 12; i++) {
                LocalDate month = LocalDate.now().minusMonths(i);
                months.add(month.format(formatter));
            }

            monthSelectorCombo.setItems(FXCollections.observableArrayList(months));
            monthSelectorCombo.setValue(months.get(0)); // Current month

            // Add listener for month selection
            monthSelectorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    handleMonthSelection(newVal);
                }
            });
        }
    }

        private void handleExportMonthSelection(String monthYear) {
        try {
            // Parse as "MMMM yyyy" and set to first day of month
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            YearMonth yearMonth = YearMonth.parse(monthYear, formatter);
            exportMonth = yearMonth.atDay(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error parsing export month: " + monthYear);
        }
    }

    private void handleMonthSelection(String monthYear) {
        try {
            // Parse as "MMMM yyyy" and set to first day of month
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            java.time.YearMonth yearMonth = java.time.YearMonth.parse(monthYear, formatter);
            selectedMonth = yearMonth.atDay(1);
            loadCategoryChart();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error parsing month: " + monthYear);
        }
    }

    private void loadExpiringMedicines() {
        try {
            // ✅ NOW CHECKS BATCHES - gets products with expired or expiring batches within 30 days
            List<Product> expiringProducts = productService.getProductsWithExpiringBatches(30);

            expiringMedicinesTable.setItems(FXCollections.observableArrayList(expiringProducts));

            if (expiringProducts.isEmpty()) {
                expiringMedicinesTable.setPlaceholder(new Label("No expired or expiring medicines"));
            }
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
    private void handleProducts() {
        setActiveButton(productsBtn);
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
            } else if (fxmlPath.contains("product-management")) {
                ProductManagementController controller = loader.getController();
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
        txnIdCol.setCellFactory(column -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                Sale sale = getTableRow() != null ? getTableRow().getItem() : null;
                
                if (empty || item == null || sale == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    if (sale.isVoided()) {
                        // Show void tag
                        Label idLabel = new Label(item);
                        idLabel.setStyle("-fx-text-fill: #2c3e50;");
                        
                        Label voidTag = new Label("VOID");
                        voidTag.setStyle(
                            "-fx-background-color: #F44336; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 10px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 2px 8px; " +
                            "-fx-background-radius: 3px;"
                        );
                        
                        HBox container = new HBox(8, idLabel, voidTag);
                        container.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(container);
                        setText(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                    setStyle("-fx-alignment: CENTER-LEFT;");
                }
            }
        });

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
            private final Button voidBtn = new Button("⊘ Void");
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

                String voidStyle =
                        "-fx-background-color: #F44336; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 12px; " +
                                "-fx-padding: 6px 12px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-cursor: hand;";

                viewBtn.setStyle(viewStyle);
                editBtn.setStyle(editStyle);
                voidBtn.setStyle(voidStyle);

                buttons.getChildren().addAll(viewBtn, editBtn, voidBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                Sale sale = getTableRow() != null ? getTableRow().getItem() : null;

                if (empty || sale == null) {
                    setGraphic(null);
                } else {
                    buttons.getChildren().clear();
                    
                    // Always show view button
                    viewBtn.setOnAction(e -> handleViewTransaction(sale));
                    buttons.getChildren().add(viewBtn);
                    
                    // Only show edit and void buttons for non-voided transactions
                    if (!sale.isVoided()) {
                        editBtn.setOnAction(e -> handleEditTransaction(sale));
                        voidBtn.setOnAction(e -> handleVoidTransaction(sale));
                        buttons.getChildren().addAll(editBtn, voidBtn);
                    }

                    setGraphic(buttons);
                }
            }
        });
    }

    private void loadAllTransactions() {
        try {
            // Get last 30 days of transactions
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();

            List<Sale> transactions;
            if (showVoidedTransactions) {
                // Show ALL including voided
                transactions = salesService.getAllSalesBetweenDates(thirtyDaysAgo, now);
            } else {
                // Show only active (non-voided) transactions
                transactions = salesService.getSalesBetweenDates(thirtyDaysAgo, now);
            }

            // Sort by date descending (newest first)
            transactions.sort((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()));

            transactionsTable.setItems(FXCollections.observableArrayList(transactions));

            if (transactions.isEmpty()) {
                String message = showVoidedTransactions ? 
                    "No transactions found in the last 30 days" : 
                    "No active transactions found in the last 30 days";
                transactionsTable.setPlaceholder(new Label(message));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error", "Failed to load transactions: " + e.getMessage());
        }
    }
    
    public void toggleVoidedTransactions() {
        showVoidedTransactions = !showVoidedTransactions;
        loadAllTransactions();
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

    private void handleVoidTransaction(Sale sale) {
        // Check if already voided
        if (sale.getVoided() != null && sale.getVoided()) {
            showStyledAlert(Alert.AlertType.WARNING, "Already Voided", 
                "This transaction has already been voided.");
            return;
        }

        // Check if transaction is from today
        LocalDate saleDate = sale.getSaleDate().toLocalDate();
        LocalDate today = LocalDate.now();
        if (!saleDate.equals(today)) {
            showStyledAlert(Alert.AlertType.WARNING, "Cannot Void", 
                "Only transactions made today can be voided.\n" +
                "Transaction date: " + saleDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            return;
        }

        // Show reason dialog
        String reason = showVoidReasonDialog();
        
        if (reason == null) {
            return; // User cancelled
        }

        try {
            salesService.voidTransaction(sale.getId(), reason);
            showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                    "Transaction voided successfully!\nInventory has been restored.");
            loadAllTransactions();
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to void transaction: " + e.getMessage());
        }
    }

    private String showVoidReasonDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Void Transaction");
        dialogStage.setResizable(false);

        // Set logo
        try {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/icons/pharmatrack-icon.png"));
            dialogStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Failed to load dialog icon: " + e.getMessage());
        }

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 30; -fx-background-color: white;");

        Label titleLabel = new Label("Void Transaction");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label messageLabel = new Label("Please provide a reason for voiding this transaction:");
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        messageLabel.setWrapText(true);

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Enter reason here...");
        reasonArea.setPrefRowCount(4);
        reasonArea.setPrefWidth(400);
        reasonArea.setWrapText(true);
        reasonArea.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 10px; " +
                "-fx-font-size: 13px;"
        );

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-text-fill: #2c3e50; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 10px 25px; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 8px; " +
                "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> {
            dialogStage.setUserData(null);
            dialogStage.close();
        });

        Button voidBtn = new Button("Void Transaction");
        voidBtn.setStyle(
                "-fx-background-color: #FF9800; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10px 25px; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand;"
        );
        voidBtn.setOnAction(e -> {
            String reason = reasonArea.getText().trim();
            if (reason.isEmpty()) {
                showStyledAlert(Alert.AlertType.WARNING, "Reason Required", 
                    "Please provide a reason for voiding this transaction.");
                return;
            }
            dialogStage.setUserData(reason);
            dialogStage.close();
        });

        buttonBox.getChildren().addAll(cancelBtn, voidBtn);
        content.getChildren().addAll(titleLabel, messageLabel, reasonArea, buttonBox);

        Scene scene = new Scene(content);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return (String) dialogStage.getUserData();
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

    @FXML
    private void handleExportReport() {
        // Show month selection dialog
        LocalDate selectedExportMonth = showMonthSelectionDialog();

        if (selectedExportMonth == null) {
            return; // User cancelled
        }

        // Set the export month
        exportMonth = selectedExportMonth;

        try {
            // Create file chooser
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("MMMM_yyyy");
            fileChooser.setTitle("Save Report as PDF");
            fileChooser.setInitialFileName("Sales_Report_" + exportMonth.format(fileFormatter) + ".pdf");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );

            // Show save dialog
            java.io.File file = fileChooser.showSaveDialog(dashboardBtn.getScene().getWindow());

            if (file != null) {
                generatePDFReport(file);
                showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                        "Report exported successfully to:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to export report: " + e.getMessage());
        }
    }

    @FXML
    private void handlePrintReport() {
        // Show month selection dialog
        LocalDate selectedPrintMonth = showMonthSelectionDialog();

        if (selectedPrintMonth == null) {
            return; // User cancelled
        }

        // Set the export month for printing
        exportMonth = selectedPrintMonth;

        try {
            // Show print preview dialog
            showPrintPreviewDialog();
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to show print preview: " + e.getMessage());
        }
    }

    private void showPrintPreviewDialog() throws Exception {
        // Create print preview dialog
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Print Preview - Sales Report");
        dialogStage.setResizable(true);

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 30;");

        // Header
        Label titleLabel = new Label("Print Preview");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        Label subtitleLabel = new Label("Sales Report for " + exportMonth.format(formatter));
        subtitleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");

        VBox header = new VBox(5, titleLabel, subtitleLabel);

        // Print preview content (same format as PDF)
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPrefViewportHeight(500);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox printContent = createPrintableContent();
        printContent.setStyle(
                "-fx-background-color: white; " +
                "-fx-padding: 50; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);"
        );
        printContent.setPrefWidth(800);
        printContent.setMaxWidth(800);
        printContent.setMinHeight(Region.USE_PREF_SIZE);

        // Center the content
        HBox contentWrapper = new HBox(printContent);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setStyle("-fx-padding: 20;");
        contentWrapper.setMinHeight(Region.USE_PREF_SIZE);

        scrollPane.setContent(contentWrapper);

        // Button container
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 20 0 0 0;");

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
        cancelButton.setOnAction(e -> dialogStage.close());

        Button printButton = new Button("🖨 Print");
        printButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12px 30px; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand;"
        );
        printButton.setOnAction(e -> {
            performPrint(printContent);
            dialogStage.close();
        });

        buttonBox.getChildren().addAll(cancelButton, printButton);

        mainContainer.getChildren().addAll(header, scrollPane, buttonBox);

        Scene scene = new Scene(mainContainer, 1000, 700);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private VBox createPrintableContent() {
        VBox content = new VBox(20);

        // Title
        Label title = new Label("Calo's Drugstore");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Label subtitle = new Label("Sales & Inventory Report");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setMaxWidth(Double.MAX_VALUE);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        Label dateRange = new Label(exportMonth.format(formatter));
        dateRange.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        dateRange.setAlignment(Pos.CENTER);
        dateRange.setMaxWidth(Double.MAX_VALUE);

        Label generatedDate = new Label("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")));
        generatedDate.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        generatedDate.setAlignment(Pos.CENTER);
        generatedDate.setMaxWidth(Double.MAX_VALUE);

        Separator separator1 = new Separator();
        separator1.setStyle("-fx-padding: 10 0;");

        // KPI Section
        Label kpiTitle = new Label("Key Performance Indicators");
        kpiTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Calculate KPIs
        LocalDate exportMonthStart = exportMonth.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate exportMonthEnd = exportMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startOfExportMonth = exportMonthStart.atStartOfDay();
        LocalDateTime endOfExportMonth = exportMonthEnd.atTime(23, 59, 59);

        List<Sale> exportMonthSales = salesService.getSalesBetweenDates(startOfExportMonth, endOfExportMonth);

        BigDecimal exportRevenue = exportMonthSales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int exportTransactions = exportMonthSales.size();

        BigDecimal exportAvgOrder = exportTransactions > 0
                ? exportRevenue.divide(BigDecimal.valueOf(exportTransactions), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        long exportLowStock = productService.getLowStockCount();

        // KPI Grid
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(20);
        kpiGrid.setVgap(10);
        kpiGrid.setStyle("-fx-padding: 15; -fx-background-color: #F8F9FA; -fx-background-radius: 8px;");

        kpiGrid.add(createKPILabel("Total Revenue:", true), 0, 0);
        kpiGrid.add(createKPILabel("₱" + String.format("%,.2f", exportRevenue), false), 1, 0);
        kpiGrid.add(createKPILabel("Transactions:", true), 2, 0);
        kpiGrid.add(createKPILabel(String.format("%,d", exportTransactions), false), 3, 0);
        kpiGrid.add(createKPILabel("Avg. Order Value:", true), 0, 1);
        kpiGrid.add(createKPILabel("₱" + String.format("%,.2f", exportAvgOrder), false), 1, 1);
        kpiGrid.add(createKPILabel("Low Stock Items:", true), 2, 1);
        kpiGrid.add(createKPILabel(String.valueOf(exportLowStock), false), 3, 1);

        // Top Selling Products Section
        Label topSellingTitle = new Label("Top Selling Products");
        topSellingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 20 0 10 0;");

        // Calculate top selling products
        Map<String, Map<String, Object>> medicineStats = new HashMap<>();
        for (Sale sale : exportMonthSales) {
            for (SaleItem item : sale.getItems()) {
                String medicineName = item.getMedicineName();
                if (!medicineStats.containsKey(medicineName)) {
                    Optional<Product> productOpt = productService.getProductByMedicineId(item.getMedicineId());
                    String category = productOpt.map(Product::getCategory).orElse("Unknown");
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("medicineName", medicineName);
                    stats.put("category", category);
                    stats.put("quantitySold", 0);
                    medicineStats.put(medicineName, stats);
                }
                Map<String, Object> stats = medicineStats.get(medicineName);
                stats.put("quantitySold", (Integer) stats.get("quantitySold") + item.getQuantity());
            }
        }

        List<Map<String, Object>> topMedicines = medicineStats.values().stream()
                .sorted((a, b) -> Integer.compare((Integer) b.get("quantitySold"), (Integer) a.get("quantitySold")))
                .limit(5)
                .collect(Collectors.toList());

        GridPane topSellingGrid = createTableGrid(
                new String[]{"Rank", "Product", "Category", "Quantity Sold"},
                topMedicines.isEmpty() ? null : topMedicines
        );

        // Expiring Products Section
        Label expiringTitle = new Label("Expired & Expiring Products");
        expiringTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 20 0 10 0;");

        GridPane expiringGrid = createExpiringProductsGrid();

        content.getChildren().addAll(
                title, subtitle, dateRange, generatedDate, separator1,
                kpiTitle, kpiGrid,
                topSellingTitle, topSellingGrid,
                expiringTitle, expiringGrid
        );

        return content;
    }

    private Label createKPILabel(String text, boolean isBold) {
        Label label = new Label(text);
        if (isBold) {
            label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        } else {
            label.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
        }
        return label;
    }

    private GridPane createTableGrid(String[] headers, List<Map<String, Object>> data) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setStyle("-fx-padding: 15; -fx-background-color: #F8F9FA; -fx-background-radius: 8px;");

        // Add headers
        for (int i = 0; i < headers.length; i++) {
            Label header = new Label(headers[i]);
            header.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 5;");
            grid.add(header, i, 0);
        }

        // Add data
        if (data == null || data.isEmpty()) {
            Label noData = new Label("No data available");
            noData.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 5;");
            grid.add(noData, 0, 1, headers.length, 1);
        } else {
            for (int row = 0; row < data.size(); row++) {
                Map<String, Object> item = data.get(row);
                Label rank = new Label("#" + (row + 1));
                Label product = new Label(item.get("medicineName").toString());
                Label category = new Label(item.get("category").toString());
                Label quantity = new Label(item.get("quantitySold").toString());

                rank.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                product.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                category.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                quantity.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");

                grid.add(rank, 0, row + 1);
                grid.add(product, 1, row + 1);
                grid.add(category, 2, row + 1);
                grid.add(quantity, 3, row + 1);
            }
        }

        return grid;
    }

    private GridPane createExpiringProductsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setStyle("-fx-padding: 15; -fx-background-color: #F8F9FA; -fx-background-radius: 8px;");

        // Headers
        String[] headers = {"ID", "Product", "Expiration", "Days", "Stock", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Label header = new Label(headers[i]);
            header.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 5;");
            grid.add(header, i, 0);
        }

        // Add expiring products data
        List<Product> expiringProducts = new ArrayList<>(expiringMedicinesTable.getItems());
        if (expiringProducts.isEmpty()) {
            Label noData = new Label("No expiring products");
            noData.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 5;");
            grid.add(noData, 0, 1, headers.length, 1);
        } else {
            int maxRows = Math.min(10, expiringProducts.size()); // Limit to 10 rows for print
            for (int row = 0; row < maxRows; row++) {
                Product product = expiringProducts.get(row);

                List<com.inventory.Calo.s_Drugstore.entity.Batch> batches = productService.getBatchesForProduct(product);
                LocalDate expiryDate = null;
                if (!batches.isEmpty()) {
                    expiryDate = batches.stream()
                            .map(com.inventory.Calo.s_Drugstore.entity.Batch::getExpirationDate)
                            .filter(date -> date != null)
                            .min(LocalDate::compareTo)
                            .orElse(null);
                }
                if (expiryDate == null) {
                    expiryDate = product.getExpirationDate();
                }

                String expiryStr = expiryDate != null ? expiryDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A";
                long daysLeft = expiryDate != null ? java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate) : 0;
                String status = daysLeft < 0 ? "Expired" : "Expiring Soon";

                Label id = new Label(product.getMedicineId());
                Label name = new Label(product.getBrandName());
                Label expiry = new Label(expiryStr);
                Label days = new Label(String.valueOf(daysLeft));
                Label stock = new Label(String.valueOf(product.getStock()));
                Label statusLabel = new Label(status);

                id.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                name.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                expiry.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                days.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                stock.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555; -fx-padding: 5;");
                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (daysLeft < 0 ? "#F44336" : "#FF9800") + "; -fx-padding: 5; -fx-font-weight: bold;");

                grid.add(id, 0, row + 1);
                grid.add(name, 1, row + 1);
                grid.add(expiry, 2, row + 1);
                grid.add(days, 3, row + 1);
                grid.add(stock, 4, row + 1);
                grid.add(statusLabel, 5, row + 1);
            }
        }

        return grid;
    }

    private void performPrint(VBox printContent) {
        try {
            // Remove drop shadow effect for printing (performance)
            printContent.setEffect(null);

            PrinterJob printerJob = PrinterJob.createPrinterJob();
            if (printerJob != null && printerJob.showPrintDialog(dashboardBtn.getScene().getWindow())) {

                // Create a simplified print node without effects
                VBox simplifiedContent = new VBox();
                simplifiedContent.getChildren().addAll(printContent.getChildren());
                simplifiedContent.setStyle("-fx-background-color: white; -fx-padding: 30;");

                boolean success = printerJob.printPage(simplifiedContent);
                if (success) {
                    printerJob.endJob();
                    showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                            "Report sent to printer successfully!");
                } else {
                    showStyledAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to print the report.");
                }

                // Restore drop shadow for preview
                printContent.setEffect(new javafx.scene.effect.DropShadow(10, javafx.scene.paint.Color.rgb(0, 0, 0, 0.2)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to print: " + e.getMessage());
        }
    }

    private LocalDate showMonthSelectionDialog() {
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Select Report Month");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(25);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(450);

        // Title
        Label titleLabel = new Label("Select Month for Report");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Subtitle
        Label subtitleLabel = new Label("Choose which month's data to export");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        // Month selector
        VBox selectorBox = new VBox(10);
        Label selectorLabel = new Label("Report Month:");
        selectorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.setPrefWidth(350);
        monthCombo.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-padding: 12px 15px; " +
                        "-fx-font-size: 15px;"
        );

        // Generate last 12 months
        List<String> months = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        for (int i = 0; i < 12; i++) {
            LocalDate month = LocalDate.now().minusMonths(i);
            months.add(month.format(formatter));
        }

        monthCombo.setItems(FXCollections.observableArrayList(months));
        monthCombo.setValue(months.get(0)); // Current month default

        selectorBox.getChildren().addAll(selectorLabel, monthCombo);

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
            dialogStage.setUserData(null);
            dialogStage.close();
        });

        Button exportButton = new Button("Export Report");
        exportButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );

        exportButton.setOnAction(e -> {
            try {
                String selectedMonth = monthCombo.getValue();
                if (selectedMonth != null) {
                    DateTimeFormatter parseFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
                    YearMonth yearMonth = YearMonth.parse(selectedMonth, parseFormatter);
                    dialogStage.setUserData(yearMonth.atDay(1));
                    dialogStage.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showStyledAlert(Alert.AlertType.ERROR, "Error", "Invalid month selection");
            }
        });

        exportButton.setOnMouseEntered(e -> exportButton.setStyle(
                exportButton.getStyle().replace("#4CAF50", "#45a049")
        ));
        exportButton.setOnMouseExited(e -> exportButton.setStyle(
                exportButton.getStyle().replace("#45a049", "#4CAF50")
        ));

        buttonBox.getChildren().addAll(cancelButton, exportButton);

        mainContainer.getChildren().addAll(titleLabel, subtitleLabel, selectorBox, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (LocalDate) dialogStage.getUserData();
    }

    private void generatePDFReport(java.io.File file) throws Exception {
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(file);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

        // Set margins
        document.setMargins(50, 50, 50, 50);

        // Colors
        com.itextpdf.kernel.colors.Color greenColor = new com.itextpdf.kernel.colors.DeviceRgb(76, 175, 80);
        com.itextpdf.kernel.colors.Color darkColor = new com.itextpdf.kernel.colors.DeviceRgb(44, 62, 80);
        com.itextpdf.kernel.colors.Color grayColor = new com.itextpdf.kernel.colors.DeviceRgb(127, 140, 141);

        // Title
        com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("Calo's Drugstore")
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                .setFontSize(24)
                .setFontColor(greenColor)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        document.add(title);

        com.itextpdf.layout.element.Paragraph subtitle = new com.itextpdf.layout.element.Paragraph("Sales & Inventory Report")
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                .setFontSize(16)
                .setFontColor(darkColor)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(subtitle);

        // Date range
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        com.itextpdf.layout.element.Paragraph dateRange = new com.itextpdf.layout.element.Paragraph(exportMonth.format(formatter))
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                .setFontSize(12)
                .setFontColor(grayColor)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(dateRange);

        // Generated date
        com.itextpdf.layout.element.Paragraph generatedDate = new com.itextpdf.layout.element.Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")))
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                .setFontSize(10)
                .setFontColor(grayColor)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(generatedDate);

        // Separator
        document.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginBottom(20));

        // === KPI SECTION ===
        com.itextpdf.layout.element.Paragraph kpiTitle = new com.itextpdf.layout.element.Paragraph("Key Performance Indicators")
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setFontColor(darkColor)
                .setMarginBottom(15);
        document.add(kpiTitle);

        // Calculate KPIs for export month
        LocalDate exportMonthStart = exportMonth.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate exportMonthEnd = exportMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startOfExportMonth = exportMonthStart.atStartOfDay();
        LocalDateTime endOfExportMonth = exportMonthEnd.atTime(23, 59, 59);

        List<Sale> exportMonthSales = salesService.getSalesBetweenDates(startOfExportMonth, endOfExportMonth);

        BigDecimal exportRevenue = exportMonthSales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int exportTransactions = exportMonthSales.size();

        BigDecimal exportAvgOrder = exportTransactions > 0
                ? exportRevenue.divide(BigDecimal.valueOf(exportTransactions), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        long exportLowStock = productService.getLowStockCount();

        // KPI Table
        com.itextpdf.layout.element.Table kpiTable = new com.itextpdf.layout.element.Table(new float[]{1, 1, 1, 1});
        kpiTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        // Headers
        kpiTable.addHeaderCell(createHeaderCell("Total Revenue"));
        kpiTable.addHeaderCell(createHeaderCell("Transactions"));
        kpiTable.addHeaderCell(createHeaderCell("Avg. Order Value"));
        kpiTable.addHeaderCell(createHeaderCell("Low Stock Items"));

        // Values - use calculated values for export month
        kpiTable.addCell(createValueCell("₱" + String.format("%,.2f", exportRevenue)));
        kpiTable.addCell(createValueCell(String.format("%,d", exportTransactions)));
        kpiTable.addCell(createValueCell("₱" + String.format("%,.2f", exportAvgOrder)));
        kpiTable.addCell(createValueCell(String.valueOf(exportLowStock)));
        document.add(kpiTable.setMarginBottom(30));

        // === TOP SELLING MEDICINES ===
// Calculate for export month
        Map<String, Map<String, Object>> exportMedicineStats = new HashMap<>();

        for (Sale sale : exportMonthSales) {
            List<SaleItem> items = sale.getItems();
            for (SaleItem item : items) {
                String medicineName = item.getMedicineName();

                if (!exportMedicineStats.containsKey(medicineName)) {
                    Optional<Product> productOpt = productService.getProductByMedicineId(item.getMedicineId());
                    String category = productOpt.map(Product::getCategory).orElse("Unknown");

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("medicineName", medicineName);
                    stats.put("category", category);
                    stats.put("quantitySold", 0);
                    exportMedicineStats.put(medicineName, stats);
                }

                Map<String, Object> stats = exportMedicineStats.get(medicineName);
                Integer currentQty = (Integer) stats.get("quantitySold");
                stats.put("quantitySold", currentQty + item.getQuantity());
            }
        }

        List<Map<String, Object>> exportTopMedicines = exportMedicineStats.values().stream()
                .sorted((a, b) -> Integer.compare(
                        (Integer) b.get("quantitySold"),
                        (Integer) a.get("quantitySold")
                ))
                .limit(5)
                .collect(Collectors.toList());

        com.itextpdf.layout.element.Paragraph topSellingTitle = new com.itextpdf.layout.element.Paragraph("Top Selling Products")
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setFontColor(darkColor)
                .setMarginBottom(15);
        document.add(topSellingTitle);

        com.itextpdf.layout.element.Table pdfTopSellingTable = new com.itextpdf.layout.element.Table(new float[]{1, 3, 2, 2});
        pdfTopSellingTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        pdfTopSellingTable.addHeaderCell(createHeaderCell("Rank"));
        pdfTopSellingTable.addHeaderCell(createHeaderCell("Product"));
        pdfTopSellingTable.addHeaderCell(createHeaderCell("Category"));
        pdfTopSellingTable.addHeaderCell(createHeaderCell("Quantity Sold"));

        if (exportTopMedicines.isEmpty()) {
            pdfTopSellingTable.addCell(createTableCell("-"));
            pdfTopSellingTable.addCell(createTableCell("No sales data"));
            pdfTopSellingTable.addCell(createTableCell("-"));
            pdfTopSellingTable.addCell(createTableCell("0"));
        } else {
            for (int i = 0; i < exportTopMedicines.size(); i++) {
                Map<String, Object> item = exportTopMedicines.get(i);
                pdfTopSellingTable.addCell(createTableCell("#" + (i + 1)));
                pdfTopSellingTable.addCell(createTableCell(item.get("medicineName").toString()));
                pdfTopSellingTable.addCell(createTableCell(item.get("category").toString()));
                pdfTopSellingTable.addCell(createTableCell(item.get("quantitySold").toString()));
            }
        }

        document.add(pdfTopSellingTable.setMarginBottom(30));

        // === EXPIRING MEDICINES ===
        com.itextpdf.layout.element.Paragraph expiringTitle = new com.itextpdf.layout.element.Paragraph("Expired & Expiring Products")
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setFontColor(darkColor)
                .setMarginBottom(15);
        document.add(expiringTitle);

        com.itextpdf.layout.element.Table expiringTable = new com.itextpdf.layout.element.Table(new float[]{2, 3, 2, 1, 1, 2});
        expiringTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        expiringTable.addHeaderCell(createHeaderCell("ID"));
        expiringTable.addHeaderCell(createHeaderCell("Product"));
        expiringTable.addHeaderCell(createHeaderCell("Expiration"));
        expiringTable.addHeaderCell(createHeaderCell("Days"));
        expiringTable.addHeaderCell(createHeaderCell("Stock"));
        expiringTable.addHeaderCell(createHeaderCell("Status"));

        for (Product product : expiringMedicinesTable.getItems()) {
            // Get earliest batch expiration date (same logic as the UI table)
            List<com.inventory.Calo.s_Drugstore.entity.Batch> batches = productService.getBatchesForProduct(product);
            
            LocalDate expiryDate = null;
            
            if (!batches.isEmpty()) {
                // Get earliest expiration date from batches
                expiryDate = batches.stream()
                        .map(com.inventory.Calo.s_Drugstore.entity.Batch::getExpirationDate)
                        .filter(date -> date != null)
                        .min(LocalDate::compareTo)
                        .orElse(null);
            }
            
            // Fallback to product expiration date if no batches
            if (expiryDate == null) {
                expiryDate = product.getExpirationDate();
            }
            
            // Skip products without expiration date
            if (expiryDate == null) {
                continue;
            }
            
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
            
            // Include expired products (daysLeft < 0) and products expiring within 30 days
            if (daysLeft > 30) {
                continue;
            }
            
            String status = daysLeft < 0 ? "Expired" :
                    daysLeft <= 7 ? "Critical" :
                            daysLeft <= 30 ? "Warning" : "Good";

            expiringTable.addCell(createTableCell(product.getMedicineId()));
            expiringTable.addCell(createTableCell(product.getBrandName()));
            expiringTable.addCell(createTableCell(expiryDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));
            expiringTable.addCell(createTableCell(daysLeft < 0 ? String.valueOf(Math.abs(daysLeft)) + " (ago)" : String.valueOf(daysLeft)));
            expiringTable.addCell(createTableCell(String.valueOf(product.getStock())));
            expiringTable.addCell(createTableCell(status));
        }

        document.add(expiringTable.setMarginBottom(30));

        // === CATEGORY DISTRIBUTION ===
        com.itextpdf.layout.element.Paragraph categoryTitle = new com.itextpdf.layout.element.Paragraph("Sales by Category")
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setFontColor(darkColor)
                .setMarginBottom(15);
        document.add(categoryTitle);

        // Get category data
        LocalDate monthStart = exportMonth.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = exportMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime startOfMonth = monthStart.atStartOfDay();
        LocalDateTime endOfMonth = monthEnd.atTime(23, 59, 59);
        List<Sale> monthSales = salesService.getSalesBetweenDates(startOfMonth, endOfMonth);

        Map<String, Integer> categoryCount = new HashMap<>();
        int totalItems = 0;

        for (Sale sale : monthSales) {
            for (SaleItem item : sale.getItems()) {
                Optional<Product> productOpt = productService.getProductByMedicineId(item.getMedicineId());
                if (productOpt.isPresent()) {
                    String category = productOpt.get().getCategory();
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0) + item.getQuantity());
                    totalItems += item.getQuantity();
                }
            }
        }

        com.itextpdf.layout.element.Table categoryTable = new com.itextpdf.layout.element.Table(new float[]{3, 2, 2});
        categoryTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        categoryTable.addHeaderCell(createHeaderCell("Category"));
        categoryTable.addHeaderCell(createHeaderCell("Quantity"));
        categoryTable.addHeaderCell(createHeaderCell("Percentage"));

        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            double percentage = totalItems > 0 ? (entry.getValue() * 100.0 / totalItems) : 0;
            categoryTable.addCell(createTableCell(entry.getKey()));
            categoryTable.addCell(createTableCell(String.valueOf(entry.getValue())));
            categoryTable.addCell(createTableCell(String.format("%.1f%%", percentage)));
        }

        document.add(categoryTable);

        // Footer
        document.add(new com.itextpdf.layout.element.Paragraph("\n"));
        document.add(new com.itextpdf.layout.element.LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginTop(20));

        com.itextpdf.layout.element.Paragraph footer = new com.itextpdf.layout.element.Paragraph("© " + LocalDate.now().getYear() + " Calo's Drugstore. All rights reserved.")
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                .setFontSize(10)
                .setFontColor(grayColor)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(footer);

        document.close();
    }

    private com.itextpdf.layout.element.Cell createHeaderCell(String text) throws Exception {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(text))
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                .setFontSize(11)
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(248, 249, 250))
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(44, 62, 80))
                .setPadding(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        return cell;
    }

    private com.itextpdf.layout.element.Cell createTableCell(String text) throws Exception {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(text))
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                .setFontSize(10)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(44, 62, 80))
                .setPadding(8)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        return cell;
    }

    private com.itextpdf.layout.element.Cell createValueCell(String text) throws Exception {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(text))
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                .setFontSize(14)
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(76, 175, 80))
                .setPadding(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        return cell;
    }
}