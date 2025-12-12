package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Supplier;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.SupplierService;
import com.inventory.Calo.s_Drugstore.util.IconUtil;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
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
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.net.HttpCookie;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

@Controller
public class SupplierController implements Initializable {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;

    // FXML Components
    @FXML private TextField searchField;
    @FXML private Label totalCountLabel;

    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, String> supplierIdColumn;
    @FXML private TableColumn<Supplier, String> companyNameColumn;
    @FXML private TableColumn<Supplier, String> contactPersonColumn;
    @FXML private TableColumn<Supplier, String> mobileNumberColumn;
    @FXML private TableColumn<Supplier, String> emailColumn;
    @FXML private TableColumn<Supplier, String> addressColumn;
    @FXML private TableColumn<Supplier, Void> actionsColumn;

    // Sidebar buttons
    @FXML private Button dashboardBtn;
    @FXML private Button productsBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button supplierBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button staffBtn;
    @FXML private Button logoutBtn;

    // User info labels
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;

    // KPI labels
    @FXML private Label totalSuppliersLabel;
    @FXML private Label activeSuppliersLabel;
    @FXML private Label newSuppliersLabel;

    private ObservableList<Supplier> supplierList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupSearch();
        loadSuppliers();
        setActiveButton(supplierBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }

    private void setupTableColumns() {
        // Supplier ID Column
        supplierIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSupplierId()));

        // Company Name Column
        companyNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCompanyName()));

        // Contact Person Column
        contactPersonColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getContactPerson() != null ?
                        data.getValue().getContactPerson() : "N/A"));

        // Mobile Number Column
        mobileNumberColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMobileNumber() != null ?
                        data.getValue().getMobileNumber() : "N/A"));

        // Email Column
        emailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail() != null ?
                        data.getValue().getEmail() : "N/A"));

        // Address Column
        addressColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPhysicalAddress() != null ?
                        data.getValue().getPhysicalAddress() : "N/A"));

        // Actions Column
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewBtn = new Button();
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();

            {
                // View button
                viewBtn.setText("👁");
                viewBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 16px; " +
                                "-fx-cursor: hand; " +
                                "-fx-padding: 8px 12px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-width: 1px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-background-radius: 6px;"
                );

                viewBtn.setOnMouseEntered(e -> viewBtn.setStyle(
                        viewBtn.getStyle() + "-fx-background-color: #E8F5E9; -fx-border-color: #4CAF50;"
                ));
                viewBtn.setOnMouseExited(e -> viewBtn.setStyle(
                        viewBtn.getStyle().replace("-fx-background-color: #E8F5E9; -fx-border-color: #4CAF50;",
                                "-fx-background-color: white; -fx-border-color: #E0E0E0;")
                ));

                // Edit button
                editBtn.setText("🔧");
                editBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 16px; " +
                                "-fx-cursor: hand; " +
                                "-fx-padding: 8px 12px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-width: 1px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-background-radius: 6px;"
                );

                editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                        editBtn.getStyle() + "-fx-background-color: #E3F2FD; -fx-border-color: #2196F3;"
                ));
                editBtn.setOnMouseExited(e -> editBtn.setStyle(
                        editBtn.getStyle().replace("-fx-background-color: #E3F2FD; -fx-border-color: #2196F3;",
                                "-fx-background-color: white; -fx-border-color: #E0E0E0;")
                ));

                // Delete button
                deleteBtn.setText("🗑");
                deleteBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #F44336; " +
                                "-fx-font-size: 16px; " +
                                "-fx-cursor: hand; " +
                                "-fx-padding: 8px 12px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-width: 1px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-background-radius: 6px;"
                );

                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                        deleteBtn.getStyle() + "-fx-background-color: #FFEBEE; -fx-border-color: #F44336;"
                ));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                        deleteBtn.getStyle().replace("-fx-background-color: #FFEBEE; -fx-border-color: #F44336;",
                                "-fx-background-color: white; -fx-border-color: #E0E0E0;")
                ));

                viewBtn.setOnAction(event -> {
                    Supplier supplier = getTableRow().getItem();
                    if (supplier != null) {
                        handleViewSupplier(supplier);
                    }
                });

                editBtn.setOnAction(event -> {
                    Supplier supplier = getTableRow().getItem();
                    if (supplier != null) {
                        handleEditSupplier(supplier);
                    }
                });

                deleteBtn.setOnAction(event -> {
                    Supplier supplier = getTableRow().getItem();
                    if (supplier != null) {
                        handleDeleteSupplier(supplier);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(8, viewBtn, editBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(buttons);
                }
            }
        });

        supplierIdColumn.setMinWidth(120);
        companyNameColumn.setMinWidth(200);
        contactPersonColumn.setMinWidth(150);
        mobileNumberColumn.setMinWidth(130);
        emailColumn.setMinWidth(180);
        addressColumn.setMinWidth(200);
        actionsColumn.setMinWidth(220);

        supplierTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearch());
    }

    private void applySearch() {
        String searchTerm = searchField.getText();
        List<Supplier> filtered = supplierService.searchSuppliers(searchTerm);
        supplierList.setAll(filtered);
        updateTotalCount();
    }

    private void loadSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        supplierList.setAll(suppliers);
        supplierTable.setItems(supplierList);
        updateTotalCount();
    }

    private void updateTotalCount() {
        totalCountLabel.setText("Total: " + supplierList.size() + " suppliers");
    }

    private void handleViewSupplier(Supplier supplier) {
        showSupplierDetailsDialog(supplier);
    }

    private void handleEditSupplier(Supplier supplier) {
        // Edit an existing supplier
        showStyledAlert(Alert.AlertType.INFORMATION, "Edit Supplier",
                "hi Baflorr, ikaw sad ari bi pls, thank u");
    }

    @FXML
    private void handleAddSupplier() {
        // Add a new supplier
        showStyledAlert(Alert.AlertType.INFORMATION, "Add Supplier",
                "hi Baflor paki add diri sa kuwang pls, thank u");
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        HttpCookie supplierFilter = null;
        supplierFilter.setValue(null);
        loadSuppliers(); // Reload all suppliers
    }

    private void handleDeleteSupplier(Supplier supplier) {
        boolean confirmed = showDeleteConfirmation(supplier.getCompanyName());

        if (confirmed) {
            try {
                supplierService.deleteSupplier(supplier.getId());
                loadSuppliers();
                showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                        "Supplier deleted successfully!");
            } catch (Exception e) {
                showStyledAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to delete supplier: " + e.getMessage());
            }
        }
    }

    private void showSupplierDetailsDialog(Supplier supplier) {
        // Similar to your product details dialog - styled to match system
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Supplier Details");
        alert.setHeaderText(supplier.getCompanyName());
        alert.setContentText(
                "Supplier ID: " + supplier.getSupplierId() + "\n" +
                        "Company: " + supplier.getCompanyName() + "\n" +
                        "Contact Person: " + (supplier.getContactPerson() != null ? supplier.getContactPerson() : "N/A") + "\n" +
                        "Mobile: " + (supplier.getMobileNumber() != null ? supplier.getMobileNumber() : "N/A") + "\n" +
                        "Email: " + (supplier.getEmail() != null ? supplier.getEmail() : "N/A") + "\n" +
                        "Address: " + (supplier.getPhysicalAddress() != null ? supplier.getPhysicalAddress() : "N/A")
        );
        alert.showAndWait();
    }

    private void showStyledAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showDeleteConfirmation(String companyName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Supplier");
        alert.setHeaderText("Are you sure you want to delete this supplier?");
        alert.setContentText("Company: " + companyName);
        return alert.showAndWait().get() == ButtonType.OK;
    }

    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        supplierBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        staffBtn.getStyleClass().remove("active");

        // Add active class to the clicked button
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
    }

    @FXML
    private void handleProducts() {
        setActiveButton(productsBtn);
        navigateToPage("/fxml/product-management.fxml", "/css/inventory.css");
    }

    @FXML
    private void handleInventory() {
        setActiveButton(inventoryBtn);
        navigateToPage("/fxml/inventory.fxml", "/css/inventory.css");
    }

    @FXML
    private void handleSupplier() {
        setActiveButton(supplierBtn);
        // Already on supplier page
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
            } else if (fxmlPath.contains("staff")) {
                StaffController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("product-management")) {
                ProductManagementController controller = loader.getController();
                controller.setCurrentUser(currentUser);
                cssPath = "/css/inventory.css"; // Use inventory CSS for product management
            } else if (fxmlPath.contains("reports")) {
                ReportsController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("supplier")) {
                SupplierController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }

            Stage stage = (Stage) dashboardBtn.getScene().getWindow();

            // Save current window size
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            // Create new scene
            Scene newScene = new Scene(root);
            newScene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            // Set scene and restore window size
            stage.setScene(newScene);
            stage.setWidth(currentWidth);
            stage.setHeight(currentHeight);

        } catch (Exception e) {
            showStyledAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate: " + e.getMessage());
            e.printStackTrace();
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
                URL cssUrl = getClass().getResource("/css/styles.css");
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
            FadeTransition fadeOut = new FadeTransition(
                    Duration.millis(30),
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
                FadeTransition fadeIn = new FadeTransition(
                        Duration.millis(30),
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
        scene.setFill(Color.TRANSPARENT);
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