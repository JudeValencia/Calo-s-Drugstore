package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.util.IconUtil;
import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.ProductService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Controller
public class StaffInventoryController implements Initializable {

    @Autowired
    private ProductService productService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;
    private ObservableList<Product> allProducts;
    private ObservableList<Product> filteredProducts;

    // FXML Components
    @FXML private Button dashboardBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button logoutBtn;

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    // KPI Labels
    @FXML private Label totalItemsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label totalValueLabel;

    // Search and Filter
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;

    // Alert Banner
    @FXML private HBox lowStockAlert;
    @FXML private Label lowStockAlertTitle;

    // Table
    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, String> medicineIdCol;
    @FXML private TableColumn<Product, String> nameCol;
    @FXML private TableColumn<Product, String> categoryCol;
    @FXML private TableColumn<Product, String> priceCol;
    @FXML private TableColumn<Product, String> stockCol;
    @FXML private TableColumn<Product, String> statusCol;
    @FXML private TableColumn<Product, String> expiryDateCol;
    @FXML private TableColumn<Product, String> supplierCol;

    @FXML private Label resultsLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setActiveButton(inventoryBtn);
        setupTable();
        setupSearchAndFilter();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userRoleLabel.setText(user.getRole());

            // Load inventory data
            loadInventoryData();
        }
    }

    private void setupTable() {
        // Medicine ID Column
        medicineIdCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMedicineId()));

        // Name Column
        nameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBrandName()));

        // Category Column
        categoryCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategory()));

        // Price Column
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty("₱" + String.format("%.2f", cellData.getValue().getPrice())));

        // Stock Column
        stockCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getStock())));

        // Status Column with badges
        statusCol.setCellValueFactory(cellData -> {
            int stock = cellData.getValue().getStock();
            int minStock = cellData.getValue().getMinStockLevel();
            return new SimpleStringProperty(stock <= minStock ? "Low Stock" : "In Stock");
        });

        statusCol.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("Low Stock".equals(item)) {
                        badge.setStyle(
                                "-fx-background-color: #c62828; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 11px; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-padding: 5px 10px; " +
                                        "-fx-background-radius: 12px;"
                        );
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #2e7d32; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 11px; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-padding: 5px 10px; " +
                                        "-fx-background-radius: 12px;"
                        );
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Expiry Date Column
        expiryDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getExpirationDate()
                                .format(DateTimeFormatter.ofPattern("MMM. dd, yyyy"))
                ));

        // Add warning icon for expiring soon items
        expiryDateCol.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                            java.time.LocalDate.now(),
                            product.getExpirationDate()
                    );

                    if (daysUntilExpiry <= 30) {
                        Label dateLabel = new Label(item + " ⚠");
                        dateLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                        setGraphic(dateLabel);
                        setText(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            }
        });

        // Supplier Column
        supplierCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSupplier()));
    }

    private void setupSearchAndFilter() {
        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterInventory());

        // Category combo listener
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterInventory());
    }

    private void loadInventoryData() {
        try {
            // Load all products
            allProducts = FXCollections.observableArrayList(productService.getAllProducts());
            filteredProducts = FXCollections.observableArrayList(allProducts);

            // Load categories
            List<String> categories = productService.getAllCategories();
            categoryCombo.setItems(FXCollections.observableArrayList(categories));

            // Update table
            inventoryTable.setItems(filteredProducts);

            // Update KPIs
            updateKPIs();

            // Update results label
            updateResultsLabel();

            // Show low stock alert if needed
            updateLowStockAlert();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterInventory() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedCategory = categoryCombo.getValue();

        filteredProducts = FXCollections.observableArrayList(
                allProducts.stream()
                        .filter(product -> {
                            boolean matchesSearch = searchText.isEmpty() ||
                                    product.getBrandName().toLowerCase().contains(searchText) ||
                                    product.getMedicineId().toLowerCase().contains(searchText) ||
                                    product.getSupplier().toLowerCase().contains(searchText);

                            boolean matchesCategory = selectedCategory == null ||
                                    selectedCategory.equals(product.getCategory());

                            return matchesSearch && matchesCategory;
                        })
                        .collect(Collectors.toList())
        );

        inventoryTable.setItems(filteredProducts);
        updateResultsLabel();
    }

    private void updateKPIs() {
        // Total items
        totalItemsLabel.setText(String.valueOf(allProducts.size()));

        // Low stock count
        long lowStockCount = allProducts.stream()
                .filter(p -> p.getStock() <= p.getMinStockLevel())
                .count();
        lowStockLabel.setText(String.valueOf(lowStockCount));

        // Total inventory value
        BigDecimal totalValue = allProducts.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalValueLabel.setText("₱" + String.format("%,.2f", totalValue));
    }

    private void updateResultsLabel() {
        resultsLabel.setText(String.format("Showing %d of %d medicines",
                filteredProducts.size(), allProducts.size()));
    }

    private void updateLowStockAlert() {
        long lowStockCount = allProducts.stream()
                .filter(p -> p.getStock() <= p.getMinStockLevel())
                .count();

        if (lowStockCount > 0) {
            lowStockAlert.setVisible(true);
            lowStockAlert.setManaged(true);
            lowStockAlertTitle.setText(lowStockCount + " items running low on stock");
        } else {
            lowStockAlert.setVisible(false);
            lowStockAlert.setManaged(false);
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
        // Already on inventory
        setActiveButton(inventoryBtn);
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
            if (fxmlPath.contains("staff-dashboard")) {
                StaffDashboardController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("staff-sales") || fxmlPath.contains("sales")) {
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
        }
    }
}