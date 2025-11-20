package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Sale;
import com.inventory.Calo.s_Drugstore.entity.SaleItem;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.SalesService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class StaffReportsController implements Initializable {

    @Autowired
    private SalesService salesService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;
    private String selectedPeriod = "This Week";

    // FXML Components
    @FXML private Button dashboardBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button logoutBtn;

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    // KPI Labels
    @FXML private Label totalTransactionsLabel;
    @FXML private Label transactionsPeriodLabel;
    @FXML private Label itemsSoldLabel;
    @FXML private Label avgItemsLabel;

    // Charts
    @FXML private BarChart<String, Number> dailyTransactionsChart;
    @FXML private LineChart<String, Number> itemsSoldChart;

    // Tables
    @FXML private TableView<Map<String, Object>> topSellingTable;
    @FXML private TableColumn<Map<String, Object>, String> rankCol;
    @FXML private TableColumn<Map<String, Object>, String> medicineCol;
    @FXML private TableColumn<Map<String, Object>, String> categoryCol;
    @FXML private TableColumn<Map<String, Object>, String> quantityCol;

    @FXML private TableView<Map<String, Object>> recentActivityTable;
    @FXML private TableColumn<Map<String, Object>, String> dateCol;
    @FXML private TableColumn<Map<String, Object>, String> transactionsCol;
    @FXML private TableColumn<Map<String, Object>, String> itemsCol;
    @FXML private TableColumn<Map<String, Object>, String> avgCol;

    @FXML private ComboBox<String> timeFilterCombo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveButton(reportsBtn);
        setupTables();
        setupTimeFilter();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userRoleLabel.setText(user.getRole());

            // Load reports data
            loadReportsData();
        }
    }

    private void setupTimeFilter() {
        ObservableList<String> periods = FXCollections.observableArrayList(
                "This Week", "Last Week", "This Month", "Last Month"
        );
        timeFilterCombo.setItems(periods);
        timeFilterCombo.setValue("This Week");

        timeFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            selectedPeriod = newVal;
            loadReportsData();
        });
    }

    private void setupTables() {
        // Top Selling Table
        rankCol.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("rank")));
        // In setupTables() method, update the rank column cell factory:
        rankCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                    setAlignment(Pos.CENTER);
                }
            }
        });

// Update category column cell factory:
        categoryCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle(
                            "-fx-background-color: #F5F5F5; " +
                                    "-fx-text-fill: #2c3e50; " +
                                    "-fx-padding: 6px 14px; " +
                                    "-fx-background-radius: 14px; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-font-weight: 600;"
                    );
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

// Update items sold column cell factory in recent activity table:
        itemsCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle(
                            "-fx-text-fill: #4CAF50; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-font-size: 15px;"
                    );
                    setAlignment(Pos.CENTER);
                }
            }
        });

        medicineCol.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("medicine")));

        categoryCol.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("category")));
        categoryCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle(
                            "-fx-background-color: #F5F5F5; " +
                                    "-fx-text-fill: #2c3e50; " +
                                    "-fx-padding: 6px 12px; " +
                                    "-fx-background-radius: 12px; " +
                                    "-fx-font-size: 12px;"
                    );
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        quantityCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get("quantity").toString()));
        quantityCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        // Recent Activity Table
        dateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("date")));

        transactionsCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get("transactions").toString()));
        transactionsCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        itemsCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get("items").toString()));
        itemsCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label itemsLabel = new Label(item);
                    itemsLabel.setStyle(
                            "-fx-text-fill: #4CAF50; " +
                                    "-fx-font-weight: bold;"
                    );
                    setGraphic(itemsLabel);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        avgCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get("avg").toString()));
        avgCol.setCellFactory(column -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void loadReportsData() {
        if (currentUser == null) return;

        LocalDate[] dateRange = getDateRange(selectedPeriod);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        loadKPIs(startDate, endDate);
        loadDailyTransactionsChart(startDate, endDate);
        loadItemsSoldChart(startDate, endDate);
        loadTopSellingMedicines(startDate, endDate);
        loadRecentActivity(startDate, endDate);
    }

    private LocalDate[] getDateRange(String period) {
        LocalDate now = LocalDate.now();
        LocalDate start, end;

        switch (period) {
            case "Last Week":
                start = now.minusWeeks(1).with(DayOfWeek.MONDAY);
                end = start.plusDays(6);
                break;
            case "This Month":
                start = now.with(TemporalAdjusters.firstDayOfMonth());
                end = now.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "Last Month":
                start = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                end = now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                break;
            default: // This Week
                start = now.with(DayOfWeek.MONDAY);
                end = now.with(DayOfWeek.SUNDAY);
                break;
        }

        return new LocalDate[]{start, end};
    }

    private void loadKPIs(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);

            List<Sale> periodSales = salesService.getSalesBetweenDates(start, end);

            // Filter by current user
            List<Sale> mySales = periodSales.stream()
                    .filter(sale -> sale.getUserId().equals(currentUser.getId()))
                    .collect(Collectors.toList());

            // Calculate KPIs
            int totalTransactions = mySales.size();
            int totalItems = mySales.stream()
                    .mapToInt(Sale::getTotalItems)
                    .sum();
            double avgItems = totalTransactions > 0 ? (double) totalItems / totalTransactions : 0.0;

            // Update labels
            totalTransactionsLabel.setText(String.valueOf(totalTransactions));
            transactionsPeriodLabel.setText(selectedPeriod.toLowerCase());
            itemsSoldLabel.setText(String.valueOf(totalItems));
            avgItemsLabel.setText(String.format("%.1f", avgItems));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDailyTransactionsChart(LocalDate startDate, LocalDate endDate) {
        try {
            dailyTransactionsChart.getData().clear();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Transactions");

            Map<LocalDate, Integer> dailyCounts = new LinkedHashMap<>();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                dailyCounts.put(date, 0);
            }

            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            List<Sale> periodSales = salesService.getSalesBetweenDates(start, end);

            // Filter and count by date
            periodSales.stream()
                    .filter(sale -> sale.getUserId().equals(currentUser.getId()))
                    .forEach(sale -> {
                        LocalDate saleDate = sale.getSaleDate().toLocalDate();
                        dailyCounts.merge(saleDate, 1, Integer::sum);
                    });

            // Add data to chart
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE");
            for (Map.Entry<LocalDate, Integer> entry : dailyCounts.entrySet()) {
                String dayLabel = entry.getKey().format(formatter);
                series.getData().add(new XYChart.Data<>(dayLabel, entry.getValue()));
            }

            dailyTransactionsChart.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadItemsSoldChart(LocalDate startDate, LocalDate endDate) {
        try {
            itemsSoldChart.getData().clear();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Items Sold");

            Map<LocalDate, Integer> dailyItems = new LinkedHashMap<>();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                dailyItems.put(date, 0);
            }

            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            List<Sale> periodSales = salesService.getSalesBetweenDates(start, end);

            // Filter and count items by date
            periodSales.stream()
                    .filter(sale -> sale.getUserId().equals(currentUser.getId()))
                    .forEach(sale -> {
                        LocalDate saleDate = sale.getSaleDate().toLocalDate();
                        int items = sale.getTotalItems();
                        dailyItems.merge(saleDate, items, Integer::sum);
                    });

            // Add data to chart
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE");
            for (Map.Entry<LocalDate, Integer> entry : dailyItems.entrySet()) {
                String dayLabel = entry.getKey().format(formatter);
                series.getData().add(new XYChart.Data<>(dayLabel, entry.getValue()));
            }

            itemsSoldChart.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTopSellingMedicines(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            List<Sale> periodSales = salesService.getSalesBetweenDates(start, end);

            // Filter by current user and aggregate items
            Map<String, MedicineStats> medicineMap = new HashMap<>();

            periodSales.stream()
                    .filter(sale -> sale.getUserId().equals(currentUser.getId()))
                    .flatMap(sale -> sale.getItems().stream())
                    .forEach(item -> {
                        String medicineName = item.getMedicineName();
                        medicineMap.putIfAbsent(medicineName, new MedicineStats(medicineName));
                        medicineMap.get(medicineName).addQuantity(item.getQuantity());
                    });

            // Sort and get top 5
            List<MedicineStats> topMedicines = medicineMap.values().stream()
                    .sorted((a, b) -> Integer.compare(b.totalQuantity, a.totalQuantity))
                    .limit(5)
                    .collect(Collectors.toList());

            // Populate table
            ObservableList<Map<String, Object>> data = FXCollections.observableArrayList();
            int rank = 1;
            for (MedicineStats stats : topMedicines) {
                Map<String, Object> row = new HashMap<>();
                row.put("rank", "#" + rank);
                row.put("medicine", stats.name);
                row.put("category", getCategoryForMedicine(stats.name));
                row.put("quantity", stats.totalQuantity);
                data.add(row);
                rank++;
            }

            topSellingTable.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentActivity(LocalDate startDate, LocalDate endDate) {
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            List<Sale> periodSales = salesService.getSalesBetweenDates(start, end);

            // Group by date
            Map<LocalDate, DailyStats> dailyStatsMap = new TreeMap<>(Collections.reverseOrder());

            periodSales.stream()
                    .filter(sale -> sale.getUserId().equals(currentUser.getId()))
                    .forEach(sale -> {
                        LocalDate saleDate = sale.getSaleDate().toLocalDate();
                        dailyStatsMap.putIfAbsent(saleDate, new DailyStats(saleDate));
                        dailyStatsMap.get(saleDate).addSale(sale);
                    });

            // Populate table
            ObservableList<Map<String, Object>> data = FXCollections.observableArrayList();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd");

            for (DailyStats stats : dailyStatsMap.values()) {
                if (data.size() >= 5) break;

                Map<String, Object> row = new HashMap<>();
                row.put("date", stats.date.format(formatter));
                row.put("transactions", stats.transactions);
                row.put("items", stats.totalItems);
                row.put("avg", String.format("%.1f", stats.getAverage()));
                data.add(row);
            }

            recentActivityTable.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCategoryForMedicine(String medicineName) {
        // Simple category mapping - you can enhance this with actual product data
        if (medicineName.toLowerCase().contains("paracetamol") ||
                medicineName.toLowerCase().contains("aspirin") ||
                medicineName.toLowerCase().contains("ibuprofen")) {
            return "Pain Relief";
        } else if (medicineName.toLowerCase().contains("vitamin")) {
            return "Vitamins";
        } else if (medicineName.toLowerCase().contains("cold") ||
                medicineName.toLowerCase().contains("cough")) {
            return "Cold & Flu";
        } else if (medicineName.toLowerCase().contains("antibiotic")) {
            return "Antibiotics";
        }
        return "Others";
    }

    // Helper classes
    private static class MedicineStats {
        String name;
        int totalQuantity = 0;

        MedicineStats(String name) {
            this.name = name;
        }

        void addQuantity(int quantity) {
            this.totalQuantity += quantity;
        }
    }

    private static class DailyStats {
        LocalDate date;
        int transactions = 0;
        int totalItems = 0;

        DailyStats(LocalDate date) {
            this.date = date;
        }

        void addSale(Sale sale) {
            transactions++;
            totalItems += sale.getTotalItems();
        }

        double getAverage() {
            return transactions > 0 ? (double) totalItems / transactions : 0.0;
        }
    }

    private void setActiveButton(Button activeBtn) {
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active");
        }
    }

    // Navigation handlers
    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        navigateToPage("/fxml/staff-dashboard.fxml", "/css/dashboard.css");
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
        // Already on reports
        setActiveButton(reportsBtn);
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void navigateToPage(String fxmlPath, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Pass current user to the next controller
            if (fxmlPath.contains("staff-dashboard")) {
                StaffDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("staff-inventory")) {
                StaffInventoryController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("staff-sales")) {
                SalesController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }

            Stage stage = (Stage) dashboardBtn.getScene().getWindow();
            Scene newScene = new Scene(root);
            newScene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            stage.setScene(newScene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}