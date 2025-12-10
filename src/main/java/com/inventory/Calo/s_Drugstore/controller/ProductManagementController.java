package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.repository.BatchRepository;
import com.inventory.Calo.s_Drugstore.service.ProductService;
import com.inventory.Calo.s_Drugstore.service.UserManagementService;
import com.inventory.Calo.s_Drugstore.util.IconUtil;
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
import java.util.*;

@Controller
public class ProductManagementController implements Initializable {

    @Autowired
    private ProductService productService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private BatchRepository batchRepository;

    private User currentUser;

    // Navigation Buttons
    @FXML private Button dashboardBtn;
    @FXML private Button productsBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button staffBtn;
    @FXML private Button logoutBtn;

    // User Info
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;

    // Search and Filter
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> supplierFilter;

    // Statistics Labels
    @FXML private Label totalProductsLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label totalSuppliersLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label totalCountLabel;

    // Table
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> productIdColumn;
    @FXML private TableColumn<Product, String> brandNameColumn;
    @FXML private TableColumn<Product, String> genericNameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, String> dosageFormColumn;
    @FXML private TableColumn<Product, String> dosageStrengthColumn;
    @FXML private TableColumn<Product, String> manufacturerColumn;
    @FXML private TableColumn<Product, String> supplierColumn;
    @FXML private TableColumn<Product, String> stockColumn;
    @FXML private TableColumn<Product, String> priceColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;

    private ObservableList<Product> productList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        setupSearchListener();
        loadProducts();
        updateStatistics();
        setActiveButton(productsBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }

    private void setupTableColumns() {
        // Product ID Column
        productIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMedicineId()));

        // Brand Name Column
        brandNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBrandName()));

        // Generic Name Column
        genericNameColumn.setCellValueFactory(data -> {
            String genericName = data.getValue().getGenericName();
            return new SimpleStringProperty(genericName != null ? genericName : "N/A");
        });

        // Category Column
        categoryColumn.setCellValueFactory(data -> {
            String category = data.getValue().getCategory();
            return new SimpleStringProperty(category != null ? category : "N/A");
        });

        // Dosage Form Column
        dosageFormColumn.setCellValueFactory(data -> {
            String dosageForm = data.getValue().getDosageForm();
            return new SimpleStringProperty(dosageForm != null ? dosageForm : "N/A");
        });

        // Dosage Strength Column
        dosageStrengthColumn.setCellValueFactory(data -> {
            String dosageStrength = data.getValue().getDosageStrength();
            return new SimpleStringProperty(dosageStrength != null ? dosageStrength : "N/A");
        });

        // Manufacturer Column
        manufacturerColumn.setCellValueFactory(data -> {
            String manufacturer = data.getValue().getManufacturer();
            return new SimpleStringProperty(manufacturer != null ? manufacturer : "N/A");
        });

        // Supplier Column
        supplierColumn.setCellValueFactory(data -> {
            String supplier = data.getValue().getSupplier();
            return new SimpleStringProperty(supplier != null ? supplier : "N/A");
        });

        // Stock Column with color coding
        stockColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Product product = getTableRow().getItem();
                    setText(String.valueOf(product.getStock()));

                    if (product.isLowStock()) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #2c3e50;");
                    }
                }
            }
        });
        stockColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getStock())));

        // Price Column
        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + data.getValue().getPrice().toString()));

        // Actions Column
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();

            {
                // Edit button
                editBtn.setText("🔧");
                editBtn.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-text-fill: #2196F3; " +
                        "-fx-font-size: 16px; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 6px 12px; " +
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

                editBtn.setOnAction(event -> {
                    Product product = getTableRow().getItem();
                    if (product != null) {
                        showProductDialog(product);
                    }
                });

                // Delete button
                deleteBtn.setText("🗑");
                deleteBtn.setStyle(
                        "-fx-background-color: white; " +
                        "-fx-text-fill: #F44336; " +
                        "-fx-font-size: 16px; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 6px 12px; " +
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

                deleteBtn.setOnAction(event -> {
                    Product product = getTableRow().getItem();
                    if (product != null) {
                        handleDeleteProduct(product);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actionBox = new HBox(8);
                    actionBox.setAlignment(Pos.CENTER);
                    actionBox.getChildren().addAll(editBtn, deleteBtn);
                    setGraphic(actionBox);
                }
            }
        });
    }

    private void setupFilters() {
        // Populate category filter
        List<String> categories = productService.getAllCategories();
        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(categories);
        categoryFilter.setValue("All Categories");

        // Populate supplier filter
        List<String> suppliers = productService.getAllSuppliers();
        supplierFilter.getItems().clear();
        supplierFilter.getItems().add("All Suppliers");
        supplierFilter.getItems().addAll(suppliers);
        supplierFilter.setValue("All Suppliers");

        // Add listeners
        categoryFilter.setOnAction(e -> applyFilters());
        supplierFilter.setOnAction(e -> applyFilters());
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void loadProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            productList.setAll(products);
            productsTable.setItems(productList);
            updateTotalCount();
        } catch (Exception e) {
            showStyledAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        String searchTerm = searchField.getText();
        String category = categoryFilter.getValue();
        String supplier = supplierFilter.getValue();

        try {
            List<Product> filtered = productService.filterProducts(searchTerm, supplier, category);
            productList.setAll(filtered);
            updateTotalCount();
        } catch (Exception e) {
            showStyledAlert(Alert.AlertType.ERROR, "Filter Error", "Failed to apply filters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTotalCount() {
        totalCountLabel.setText("Total: " + productList.size() + " products");
    }

    private void updateStatistics() {
        try {
            List<Product> allProducts = productService.getAllProducts();

            // Total products
            totalProductsLabel.setText(String.valueOf(allProducts.size()));

            // Total categories
            long categoryCount = allProducts.stream()
                    .map(Product::getCategory)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            totalCategoriesLabel.setText(String.valueOf(categoryCount));

            // Total suppliers
            long supplierCount = allProducts.stream()
                    .map(Product::getSupplier)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            totalSuppliersLabel.setText(String.valueOf(supplierCount));

            // Low stock count
            long lowStockCount = allProducts.stream()
                    .filter(Product::isLowStock)
                    .count();
            lowStockLabel.setText(String.valueOf(lowStockCount));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    private void showProductDialog(Product product) {
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(product == null ? "Add New Product Profile" : "Edit Product Profile");
        dialogStage.setResizable(false);

        // Main container
        VBox mainContainer = new VBox(10);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 20;");
        mainContainer.setPrefWidth(650);

        // Header
        Label titleLabel = new Label(product == null ? "Add New Product Profile" : "Edit Product Profile");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label(product == null ?
                "Create a new product profile with master data information." :
                "Update the product profile information.");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        subtitleLabel.setWrapText(true);

        VBox header = new VBox(8, titleLabel, subtitleLabel);

        // ScrollPane for form
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(450);

        // Form container
        VBox formContainer = new VBox(15);

        // ===== BASIC INFORMATION =====
        Label basicInfoLabel = new Label("Basic Information");
        basicInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Product ID (auto-generated)
        TextField productIdField = createStyledTextField(
                product == null ? productService.generateNextMedicineId() : product.getMedicineId(),
                "Auto-generated"
        );
        productIdField.setDisable(true);
        productIdField.setStyle(productIdField.getStyle() + "-fx-opacity: 1;");

        // Brand Name
        TextField brandNameField = createStyledTextField(
                product == null ? "" : product.getBrandName(),
                "Enter brand name"
        );

        // Generic Name
        TextField genericNameField = createStyledTextField(
                product == null ? "" : (product.getGenericName() != null ? product.getGenericName() : ""),
                "Enter generic/active ingredient"
        );

        // Category
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Antibiotic", "Analgesic", "Antipyretic", "Antihistamine",
                "Antacid", "Antihypertensive", "Antidiabetic", "Vitamin", "Supplement", "First Aid", "Other");
        categoryCombo.setEditable(true);
        categoryCombo.setValue(product == null ? null : product.getCategory());
        categoryCombo.setPromptText("Select or enter category");
        styleComboBox(categoryCombo);

        // ===== PRODUCT INFORMATION =====
        Label productInfoLabel = new Label("Product Information");
        productInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 15 0 0 0;");

        // Dosage Form
        ComboBox<String> dosageFormCombo = new ComboBox<>();
        dosageFormCombo.getItems().addAll("Tablet", "Capsule", "Syrup", "Suspension", "Injection",
                "Topical", "Drops", "Inhaler", "Patch", "Suppository");
        dosageFormCombo.setValue(product == null ? "Tablet" : (product.getDosageForm() != null ? product.getDosageForm() : "Tablet"));
        dosageFormCombo.setPromptText("Select dosage form");
        styleComboBox(dosageFormCombo);

        // Dosage Strength
        TextField dosageStrengthField = createStyledTextField(
                product == null ? "" : (product.getDosageStrength() != null ? product.getDosageStrength() : ""),
                "e.g., 500mg, 10ml, 5%"
        );

        // Manufacturer
        TextField manufacturerField = createStyledTextField(
                product == null ? "" : (product.getManufacturer() != null ? product.getManufacturer() : ""),
                "Manufacturer name"
        );

        // Unit of Measure
        ComboBox<String> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll("Piece", "Box", "Bottle", "Vial", "Tube", "Pack", "Strip");
        unitCombo.setValue(product == null ? "Piece" : (product.getUnitOfMeasure() != null ? product.getUnitOfMeasure() : "Piece"));
        unitCombo.setPromptText("Select unit");
        styleComboBox(unitCombo);

        // Reorder Level
        TextField reorderLevelField = createStyledTextField(
                product == null ? "10" : String.valueOf(product.getMinStockLevel() != null ? product.getMinStockLevel() : 10),
                "Minimum stock threshold"
        );

        // Prescription Required
        CheckBox prescriptionCheckBox = new CheckBox("Prescription Required (Rx)");
        prescriptionCheckBox.setSelected(product != null && product.getPrescriptionRequired() != null && product.getPrescriptionRequired());
        prescriptionCheckBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        // ===== PRICING & SUPPLIER =====
        Label pricingInfoLabel = new Label("Pricing & Supplier");
        pricingInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 15 0 0 0;");

        // Price
        TextField priceField = createStyledTextField(
                product == null ? "0.00" : product.getPrice().toString(),
                "Price per unit"
        );

        // Supplier
        ComboBox<String> supplierCombo = new ComboBox<>();
        List<String> suppliers = productService.getAllSuppliers();
        supplierCombo.getItems().addAll(suppliers);
        supplierCombo.setEditable(true);
        supplierCombo.setValue(product == null ? null : product.getSupplier());
        supplierCombo.setPromptText("Select or enter supplier");
        styleComboBox(supplierCombo);

        // Add all fields to form
        formContainer.getChildren().addAll(
                basicInfoLabel,
                createFieldGroup("Product ID", productIdField),
                createFieldGroup("Brand Name *", brandNameField),
                createFieldGroup("Generic Name *", genericNameField),
                createFieldGroup("Category *", categoryCombo),

                productInfoLabel,
                createFieldGroup("Dosage Form *", dosageFormCombo),
                createFieldGroup("Dosage Strength", dosageStrengthField),
                createFieldGroup("Manufacturer", manufacturerField),
                createFieldGroup("Unit of Measure *", unitCombo),
                createFieldGroup("Reorder Level *", reorderLevelField),
                prescriptionCheckBox,

                pricingInfoLabel,
                createFieldGroup("Price (₱) *", priceField),
                createFieldGroup("Supplier *", supplierCombo)
        );

        scrollPane.setContent(formContainer);

        // Buttons
        HBox buttonContainer = new HBox(15);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.setStyle("-fx-padding: 10 0 0 0;");

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: white; " +
                "-fx-text-fill: #2c3e50; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 12px 30px; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> dialogStage.close());

        Button saveButton = new Button(product == null ? "Add Product" : "Save Changes");
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
            try {
                // Validation
                if (brandNameField.getText().trim().isEmpty()) {
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Brand name is required");
                    return;
                }

                if (genericNameField.getText().trim().isEmpty()) {
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Generic name is required");
                    return;
                }

                if (categoryCombo.getValue() == null || categoryCombo.getValue().trim().isEmpty()) {
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Category is required");
                    return;
                }

                if (dosageFormCombo.getValue() == null) {
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Dosage form is required");
                    return;
                }

                if (unitCombo.getValue() == null) {
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Unit of measure is required");
                    return;
                }

                if (priceField.getText().trim().isEmpty()) {
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Price is required");
                    return;
                }

                if (supplierCombo.getValue() == null || supplierCombo.getValue().trim().isEmpty()) {
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Supplier is required");
                    return;
                }

                // Create or update product
                Product targetProduct = product != null ? product : new Product();

                targetProduct.setMedicineId(productIdField.getText().trim());
                targetProduct.setBrandName(brandNameField.getText().trim());
                targetProduct.setGenericName(genericNameField.getText().trim());
                targetProduct.setCategory(categoryCombo.getValue());
                targetProduct.setDosageForm(dosageFormCombo.getValue());
                targetProduct.setDosageStrength(dosageStrengthField.getText().trim());
                targetProduct.setManufacturer(manufacturerField.getText().trim());
                targetProduct.setUnitOfMeasure(unitCombo.getValue());
                targetProduct.setSupplier(supplierCombo.getValue());
                targetProduct.setPrescriptionRequired(prescriptionCheckBox.isSelected());

                // Parse price
                BigDecimal price = new BigDecimal(priceField.getText().trim());
                targetProduct.setPrice(price);

                // Parse reorder level
                int reorderLevel = Integer.parseInt(reorderLevelField.getText().trim());
                targetProduct.setMinStockLevel(reorderLevel);

                // Save product
                productService.saveProduct(targetProduct);

                // Close dialog and refresh
                dialogStage.close();
                loadProducts();
                updateStatistics();
                setupFilters();

                showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                        product == null ? "Product profile created successfully" : "Product profile updated successfully");

            } catch (NumberFormatException ex) {
                showStyledAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter valid numeric values for price and reorder level");
            } catch (Exception ex) {
                showStyledAlert(Alert.AlertType.ERROR, "Save Error", "Failed to save product: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        buttonContainer.getChildren().addAll(cancelButton, saveButton);

        // Main layout
        mainContainer.getChildren().addAll(header, scrollPane, buttonContainer);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void handleDeleteProduct(Product product) {
        if (showConfirmation("Delete Product", "Are you sure you want to delete " + product.getBrandName() + "?",
                "This action cannot be undone.")) {
            try {
                productService.deleteProduct(product.getId());
                loadProducts();
                updateStatistics();
                setupFilters();
                showStyledAlert(Alert.AlertType.INFORMATION, "Success", "Product deleted successfully");
            } catch (Exception e) {
                showStyledAlert(Alert.AlertType.ERROR, "Delete Error", "Failed to delete product: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryFilter.setValue("All Categories");
        supplierFilter.setValue("All Suppliers");
        loadProducts();
    }

    // Navigation Methods
    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
    }

    @FXML
    private void handleProducts() {
        // Already on products page
        setActiveButton(productsBtn);
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
        if (showConfirmation("Logout", "Are you sure you want to logout?",
                "You will be redirected to the login page.")) {
            navigateToPage("/fxml/login.fxml", "/css/login.css");
        }
    }

    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        productsBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        staffBtn.getStyleClass().remove("active");

        // Add active class to the clicked button
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
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

    // Helper Methods
    private TextField createStyledTextField(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setStyle(
                "-fx-background-color: #F8F9FA; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-padding: 12px 15px; " +
                "-fx-font-size: 14px; " +
                "-fx-text-fill: #2c3e50;"
        );

        // Focus effect
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() + "-fx-border-color: #4CAF50;");
            } else {
                field.setStyle(field.getStyle().replace("-fx-border-color: #4CAF50;", "-fx-border-color: #E0E0E0;"));
            }
        });

        return field;
    }

    private VBox createFieldGroup(String labelText, javafx.scene.Node field) {
        VBox group = new VBox(8);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        group.getChildren().addAll(label, field);
        return group;
    }

    private void styleComboBox(ComboBox<?> comboBox) {
        comboBox.setStyle(
                "-fx-background-color: #F8F9FA; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-padding: 12px 15px; " +
                "-fx-font-size: 14px;"
        );
        comboBox.setMaxWidth(Double.MAX_VALUE);
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

    private boolean showConfirmation(String title, String header, String message) {
        // Create custom dialog
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);
        dialogStage.setUserData(false);

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        // Header
        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        headerLabel.setWrapText(true);

        // Message
        Label messageLabel = new Label(message);
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
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> {
            dialogStage.setUserData(false);
            dialogStage.close();
        });

        Button confirmButton = new Button("Confirm");
        confirmButton.setStyle(
                "-fx-background-color: #dc3545; " +
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

        confirmButton.setOnMouseEntered(e -> confirmButton.setStyle(
                confirmButton.getStyle().replace("#dc3545", "#c82333")
        ));
        confirmButton.setOnMouseExited(e -> confirmButton.setStyle(
                confirmButton.getStyle().replace("#c82333", "#dc3545")
        ));

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        mainContainer.getChildren().addAll(headerLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (boolean) dialogStage.getUserData();
    }
}