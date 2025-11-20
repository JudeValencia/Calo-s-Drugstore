package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.ProductService;
import com.inventory.Calo.s_Drugstore.service.UserManagementService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

@Controller
public class InventoryController implements Initializable {

    @Autowired
    private ProductService productService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    @Autowired
    private UserManagementService userManagementService;

    private User currentUser;

    // FXML Components
    @FXML private TextField searchField;
    @FXML private ComboBox<String> supplierFilter;
    @FXML private Label totalCountLabel;

    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, String> medicineIdColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> stockColumn;
    @FXML private TableColumn<Product, String> priceColumn;
    @FXML private TableColumn<Product, String> expirationColumn;
    @FXML private TableColumn<Product, String> supplierColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;

    // Sidebar buttons
    @FXML private Button dashboardBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button staffBtn;
    @FXML private Button logoutBtn;

    // User info labels
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;

    @FXML private TableColumn<Product, String> statusColumn;

    private ObservableList<Product> productList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        loadProducts();
        setActiveButton(inventoryBtn);

    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }

    private void setupTableColumns() {
        // Medicine ID Column
        medicineIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMedicineId()));

        // Name Column with Warning Icon
        nameColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Product product = getTableRow().getItem();
                    HBox container = new HBox(8);
                    container.setAlignment(Pos.CENTER_LEFT);

                    Label nameLabel = new Label(product.getName());
                    nameLabel.setStyle("-fx-font-size: 14px;");

                    if (product.isExpiringSoon()) {
                        Label warningIcon = new Label("⚠");
                        warningIcon.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 16px;");
                        container.getChildren().addAll(nameLabel, warningIcon);
                    } else {
                        container.getChildren().add(nameLabel);
                    }

                    setGraphic(container);
                }
            }
        });
        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        // Stock Column - Number ONLY
        stockColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-alignment: CENTER_LEFT;");
                }
            }
        });
        stockColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getStock())));

        // Status Column - Badge ONLY
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Product product = getTableRow().getItem();

                    Label badge = new Label(product.getStockStatus());
                    if (product.isLowStock()) {
                        badge.setStyle(
                                "-fx-background-color: #dc3545; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-padding: 4px 12px; " +
                                        "-fx-background-radius: 12px; " +
                                        "-fx-font-size: 12px; " +
                                        "-fx-font-weight: bold;"
                        );
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #1a1a1a; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-padding: 4px 12px; " +
                                        "-fx-background-radius: 12px; " +
                                        "-fx-font-size: 12px; " +
                                        "-fx-font-weight: bold;"
                        );
                    }

                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(container);
                    setText(null);
                }
            }
        });
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStockStatus()));

        // Price Column
        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                }
            }
        });
        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + data.getValue().getPrice().toString()));
        // Expiration Date Column
        expirationColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Product product = getTableRow().getItem();
                    if (product.getExpirationDate() != null) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                        setText(product.getExpirationDate().format(formatter));
                        if (product.isExpiringSoon()) {
                            setStyle("-fx-text-fill: #FF6B35; -fx-font-weight: normal;");
                        } else {
                            setStyle("-fx-text-fill: #FF6B35;");
                        }
                    } else {
                        setText("N/A");
                        setStyle("");
                    }
                }
            }
        });
        expirationColumn.setCellValueFactory(data -> {
            if (data.getValue().getExpirationDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                return new SimpleStringProperty(data.getValue().getExpirationDate().format(formatter));
            }
            return new SimpleStringProperty("N/A");
        });

        // Supplier Column
        supplierColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSupplier()));

        // Actions Column
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();

            {
                // Edit button with text icon
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

                // Delete button with text icon
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

                editBtn.setOnAction(event -> {
                    Product product = getTableRow().getItem();
                    if (product != null) {
                        handleEditProduct(product);
                    }
                });

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
                    HBox buttons = new HBox(8, editBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void setupFilters() {
        // Setup supplier filter
        supplierFilter.getItems().add("All Suppliers");
        List<String> suppliers = productService.getAllSuppliers();
        supplierFilter.getItems().addAll(suppliers);
        supplierFilter.setValue("All Suppliers");

        // Add listener for filter changes
        supplierFilter.setOnAction(e -> applyFilters());

        // Add listener for search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        String searchTerm = searchField.getText();
        String supplier = supplierFilter.getValue();

        List<Product> filtered = productService.filterProducts(searchTerm, supplier, null);
        productList.setAll(filtered);
        updateTotalCount();
    }

    private void loadProducts() {
        List<Product> products = productService.getAllProducts();
        productList.setAll(products);
        inventoryTable.setItems(productList);
        updateTotalCount();
    }

    private void updateTotalCount() {
        totalCountLabel.setText("Total: " + productList.size() + " medicines");
    }

    @FXML
    private void handleAddMedicine() {
        showProductDialog(null);
    }

    @FXML
    private void handleBulkAdd() {
        showBulkAddDialog();
    }

    private void showBulkAddDialog() {
        // Create custom dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Bulk Add Medicines");
        dialogStage.setResizable(false);

        // Main container
        VBox mainContainer = new VBox(15);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 25;");
        mainContainer.setPrefWidth(900);
        mainContainer.setMaxHeight(600);

        // Header
        Label titleLabel = new Label("Bulk Add Medicines");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label("Add multiple medicines to inventory at once. Fill in the details for each medicine you want to add.");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        subtitleLabel.setWrapText(true);

        VBox header = new VBox(5, titleLabel, subtitleLabel);

        // ScrollPane for medicine forms
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(350);

        VBox medicineFormsContainer = new VBox(15);
        scrollPane.setContent(medicineFormsContainer);

        // List to store medicine data
        ObservableList<MedicineFormData> medicineDataList = FXCollections.observableArrayList();

        // Add first medicine form
        addMedicineForm(medicineFormsContainer, medicineDataList, 1);

        // Add Another Medicine button
        Button addAnotherButton = new Button("+ Add Another Medicine");
        addAnotherButton.setStyle(
                "-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10px 25px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );

        addAnotherButton.setOnMouseEntered(e -> addAnotherButton.setStyle(
                addAnotherButton.getStyle() + "-fx-background-color: #1976D2;"
        ));
        addAnotherButton.setOnMouseExited(e -> addAnotherButton.setStyle(
                addAnotherButton.getStyle().replace("-fx-background-color: #1976D2;", "-fx-background-color: #2196F3;")
        ));

        // Counter label
        Label counterLabel = new Label("0 of 1 medicines ready to add");
        counterLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        addAnotherButton.setOnAction(e -> {
            int nextNumber = medicineFormsContainer.getChildren().size() + 1;
            // Add new form at index 0 (top) instead of bottom
            addMedicineFormAtTop(medicineFormsContainer, medicineDataList, nextNumber);
            counterLabel.setText("0 of " + nextNumber + " medicines ready to add");

            // Scroll to top to show the new form
            Platform.runLater(() -> scrollPane.setVvalue(0));
        });

        HBox addButtonContainer = new HBox(15, addAnotherButton, counterLabel);
        addButtonContainer.setAlignment(Pos.CENTER_LEFT);

        // Bottom buttons
        HBox buttonContainer = new HBox(15);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

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

        Button saveAllButton = new Button("Add All Medicines");
        saveAllButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );

        saveAllButton.setOnMouseEntered(e -> saveAllButton.setStyle(
                saveAllButton.getStyle() + "-fx-background-color: #45a049;"
        ));
        saveAllButton.setOnMouseExited(e -> saveAllButton.setStyle(
                saveAllButton.getStyle().replace("-fx-background-color: #45a049;", "-fx-background-color: #4CAF50;")
        ));

        saveAllButton.setOnAction(e -> {
            try {
                int successCount = 0;
                int failCount = 0;
                int emptyCount = 0;
                StringBuilder errorMessages = new StringBuilder();

                for (int i = 0; i < medicineDataList.size(); i++) {
                    MedicineFormData formData = medicineDataList.get(i);

                    // Check if form is completely empty (skip it)
                    if (formData.name.getText().trim().isEmpty() &&
                            formData.stock.getText().equals("0") &&
                            formData.price.getText().equals("0.00")) {
                        emptyCount++;
                        continue;
                    }

                    // Validate required fields
                    StringBuilder missingFields = new StringBuilder();

                    if (formData.name.getText().trim().isEmpty()) {
                        missingFields.append("Name, ");
                    }
                    if (formData.stock.getText().trim().isEmpty() || formData.stock.getText().equals("0")) {
                        missingFields.append("Stock, ");
                    }
                    if (formData.price.getText().trim().isEmpty() || formData.price.getText().equals("0.00")) {
                        missingFields.append("Price, ");
                    }
                    if (formData.expiryDate.getValue() == null) {
                        missingFields.append("Expiry Date, ");
                    }
                    if (formData.supplier.getText().trim().isEmpty()) {
                        missingFields.append("Supplier, ");
                    }

                    // If there are missing fields, add to error messages
                    if (missingFields.length() > 0) {
                        failCount++;
                        // Remove last comma and space
                        String missing = missingFields.substring(0, missingFields.length() - 2);
                        errorMessages.append("Medicine #").append(i + 1).append(": Missing ")
                                .append(missing).append("\n");
                        continue;
                    }

                    try {
                        // All validations passed, create the product
                        Product newProduct = new Product();
                        newProduct.setMedicineId(productService.generateNextMedicineId());
                        newProduct.setName(formData.name.getText().trim());
                        newProduct.setStock(Integer.parseInt(formData.stock.getText()));
                        newProduct.setPrice(new BigDecimal(formData.price.getText()));
                        newProduct.setExpirationDate(formData.expiryDate.getValue());
                        newProduct.setSupplier(formData.supplier.getText().trim());
                        newProduct.setCategory(formData.category.getText().trim());

                        productService.saveProduct(newProduct);
                        successCount++;

                    } catch (NumberFormatException ex) {
                        failCount++;
                        errorMessages.append("Medicine #").append(i + 1)
                                .append(": Invalid number format\n");
                    } catch (Exception ex) {
                        failCount++;
                        errorMessages.append("Medicine #").append(i + 1)
                                .append(": ").append(ex.getMessage()).append("\n");
                    }
                }

                // Show appropriate message based on results
                if (successCount == 0 && failCount > 0) {
                    // No medicines added, show error
                    showStyledAlert(Alert.AlertType.ERROR, "Validation Failed",
                            "No medicines were added. Please fix the following errors:\n\n" + errorMessages.toString());
                } else if (successCount > 0 && failCount > 0) {
                    // Some succeeded, some failed
                    loadProducts();
                    dialogStage.close();
                    showStyledAlert(Alert.AlertType.WARNING, "Partial Success",
                            successCount + " medicine(s) added successfully!\n" +
                                    failCount + " medicine(s) failed validation:\n\n" + errorMessages.toString());
                } else if (successCount > 0) {
                    // All succeeded
                    loadProducts();
                    dialogStage.close();
                    showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                            successCount + " medicine(s) added successfully!");
                } else {
                    // All forms were empty
                    showStyledAlert(Alert.AlertType.WARNING, "No Data",
                            "Please fill in at least one medicine form before adding.");
                }

            } catch (Exception ex) {
                showStyledAlert(Alert.AlertType.ERROR, "Error", "Failed to add medicines: " + ex.getMessage());
            }
        });

        buttonContainer.getChildren().addAll(cancelButton, saveAllButton);

        // Add all sections to main container
        mainContainer.getChildren().addAll(header, scrollPane, addButtonContainer, buttonContainer);

        // Create scene
        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void addMedicineFormAtTop(VBox container, ObservableList<MedicineFormData> dataList, int number) {
        // Medicine card
        VBox medicineCard = new VBox(15);
        medicineCard.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-padding: 20; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 10px;"
        );

        // Header
        Label headerLabel = new Label("Medicine #" + number);
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Form fields - Two columns - FRESH FIELDS EACH TIME
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        // Left column - NEW INSTANCES
        TextField nameField = new TextField();
        nameField.setPromptText("Enter medicine name");
        styleTextField(nameField);

        TextField priceField = new TextField("0.00");
        priceField.setPromptText("0.00");
        styleTextField(priceField);

        TextField supplierField = new TextField();
        supplierField.setPromptText("Enter supplier name");
        styleTextField(supplierField);

        // Right column - NEW INSTANCES
        TextField stockField = new TextField("0");
        stockField.setPromptText("0");
        styleTextField(stockField);

        DatePicker expiryPicker = new DatePicker(LocalDate.now().plusYears(1));
        expiryPicker.setPromptText("dd/mm/yyyy");
        expiryPicker.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 12px 15px; " +
                        "-fx-font-size: 14px;"
        );

        TextField categoryField = new TextField();
        categoryField.setPromptText("Enter category");
        styleTextField(categoryField);

        // Column 1
        grid.add(createFieldLabel("Medicine Name"), 0, 0);
        grid.add(nameField, 0, 1);
        grid.add(createFieldLabel("Price (₱)"), 0, 2);
        grid.add(priceField, 0, 3);
        grid.add(createFieldLabel("Supplier"), 0, 4);
        grid.add(supplierField, 0, 5);

        // Column 2
        grid.add(createFieldLabel("Stock Quantity"), 1, 0);
        grid.add(stockField, 1, 1);
        grid.add(createFieldLabel("Expiration Date"), 1, 2);
        grid.add(expiryPicker, 1, 3);
        grid.add(createFieldLabel("Category"), 1, 4);
        grid.add(categoryField, 1, 5);

        // Set column constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        medicineCard.getChildren().addAll(headerLabel, grid);

        // ADD AT INDEX 0 (TOP) instead of bottom
        container.getChildren().add(0, medicineCard);

        // Store form data
        MedicineFormData formData = new MedicineFormData(nameField, stockField, priceField, expiryPicker, supplierField, categoryField);
        dataList.add(0, formData); // Also add to list at top
    }

    private void addMedicineForm(VBox container, ObservableList<MedicineFormData> dataList, int number) {
        // Medicine card
        VBox medicineCard = new VBox(15);
        medicineCard.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-padding: 20; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 10px;"
        );

        // Header
        Label headerLabel = new Label("Medicine #" + number);
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Form fields - Two columns - FRESH FIELDS EACH TIME
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        // Left column - NEW INSTANCES
        TextField nameField = new TextField();
        nameField.setPromptText("Enter medicine name");
        styleTextField(nameField);

        TextField priceField = new TextField("0.00");
        priceField.setPromptText("0.00");
        styleTextField(priceField);

        TextField supplierField = new TextField();
        supplierField.setPromptText("Enter supplier name");
        styleTextField(supplierField);

        // Right column - NEW INSTANCES
        TextField stockField = new TextField("0");
        stockField.setPromptText("0");
        styleTextField(stockField);

        DatePicker expiryPicker = new DatePicker(LocalDate.now().plusYears(1));
        expiryPicker.setPromptText("dd/mm/yyyy");
        expiryPicker.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 12px 15px; " +
                        "-fx-font-size: 14px;"
        );

        TextField categoryField = new TextField();
        categoryField.setPromptText("Enter category");
        styleTextField(categoryField);

        // Column 1
        grid.add(createFieldLabel("Medicine Name"), 0, 0);
        grid.add(nameField, 0, 1);
        grid.add(createFieldLabel("Price (₱)"), 0, 2);
        grid.add(priceField, 0, 3);
        grid.add(createFieldLabel("Supplier"), 0, 4);
        grid.add(supplierField, 0, 5);

        // Column 2
        grid.add(createFieldLabel("Stock Quantity"), 1, 0);
        grid.add(stockField, 1, 1);
        grid.add(createFieldLabel("Expiration Date"), 1, 2);
        grid.add(expiryPicker, 1, 3);
        grid.add(createFieldLabel("Category"), 1, 4);
        grid.add(categoryField, 1, 5);

        // Set column constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        medicineCard.getChildren().addAll(headerLabel, grid);
        container.getChildren().add(medicineCard);

        // Store form data
        MedicineFormData formData = new MedicineFormData(nameField, stockField, priceField, expiryPicker, supplierField, categoryField);
        dataList.add(formData);
    }

    // Helper method to style text fields
    private void styleTextField(TextField field) {
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
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return label;
    }

    // Inner class to store form field references
    private static class MedicineFormData {
        TextField name;
        TextField stock;
        TextField price;
        DatePicker expiryDate;
        TextField supplier;
        TextField category;

        public MedicineFormData(TextField name, TextField stock, TextField price,
                                DatePicker expiryDate, TextField supplier, TextField category) {
            this.name = name;
            this.stock = stock;
            this.price = price;
            this.expiryDate = expiryDate;
            this.supplier = supplier;
            this.category = category;
        }
    }

    private void handleEditProduct(Product product) {
        showProductDialog(product);
    }

    private void handleDeleteProduct(Product product) {
        boolean confirmed = showDeleteConfirmation(product.getName());

        if (confirmed) {
            try {
                productService.deleteProduct(product.getId());
                loadProducts();
                showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                        "Product deleted successfully!");
            } catch (Exception e) {
                showStyledAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to delete product: " + e.getMessage());
            }
        }
    }

    private void showProductDialog(Product product) {
        // Create custom dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(product == null ? "Add New Medicine" : "Edit Medicine");
        dialogStage.setResizable(false);

        // Main container
        VBox mainContainer = new VBox(10);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 20;");
        mainContainer.setPrefWidth(600);

        // Header
        Label titleLabel = new Label(product == null ? "Add New Medicine" : "Edit Medicine");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label(product == null ?
                "Enter the details of the new medicine to add to inventory." :
                "Update the medicine details.");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        subtitleLabel.setWrapText(true);

        VBox header = new VBox(8, titleLabel, subtitleLabel);

        // Form fields
        VBox formContainer = new VBox(15);

        // Medicine ID (auto-generated, read-only for new, disabled for edit)
        TextField medicineIdField = createStyledTextField(
                product == null ? productService.generateNextMedicineId() : product.getMedicineId(),
                "Medicine ID"
        );
        medicineIdField.setDisable(true);
        medicineIdField.setStyle(medicineIdField.getStyle() + "-fx-opacity: 1;");

        // Name
        TextField nameField = createStyledTextField(
                product == null ? "" : product.getName(),
                "Medicine Name"
        );

        // Stock
        TextField stockField = createStyledTextField(
                product == null ? "0" : product.getStock().toString(),
                "Stock Quantity"
        );

        // Price
        TextField priceField = createStyledTextField(
                product == null ? "0.00" : product.getPrice().toString(),
                "Price (₱)"
        );

        // Expiration Date
        DatePicker expirationPicker = new DatePicker(
                product == null ? LocalDate.now().plusYears(1) : product.getExpirationDate()
        );
        expirationPicker.setPromptText("dd/mm/yyyy");
        expirationPicker.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 12px 15px; " +
                        "-fx-font-size: 14px;"
        );

        // Supplier
        TextField supplierField = createStyledTextField(
                product == null ? "" : product.getSupplier(),
                "Supplier"
        );

        // Category
        TextField categoryField = createStyledTextField(
                product == null ? "" : (product.getCategory() != null ? product.getCategory() : ""),
                "Category"
        );

        // Add labels and fields
        formContainer.getChildren().addAll(
                createFieldGroup("Name", nameField),
                createFieldGroup("Stock", stockField),
                createFieldGroup("Price (₱)", priceField),
                createFieldGroup("Expiry Date", expirationPicker),
                createFieldGroup("Supplier", supplierField),
                createFieldGroup("Category", categoryField)
        );

        // Buttons
        HBox buttonContainer = new HBox(15);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

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

        Button saveButton = new Button(product == null ? "Add Medicine" : "Update Medicine");
        saveButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );

        // Hover effects
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                saveButton.getStyle() + "-fx-background-color: #45a049;"
        ));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(
                saveButton.getStyle().replace("-fx-background-color: #45a049;", "-fx-background-color: #4CAF50;")
        ));

        saveButton.setOnAction(e -> {
            try {
                // Validate all required fields
                StringBuilder missingFields = new StringBuilder();

                if (nameField.getText().trim().isEmpty()) {
                    missingFields.append("• Medicine name\n");
                }
                if (stockField.getText().trim().isEmpty() || stockField.getText().equals("0")) {
                    missingFields.append("• Stock quantity (must be greater than 0)\n");
                }
                if (priceField.getText().trim().isEmpty() || priceField.getText().equals("0.00")) {
                    missingFields.append("• Price (must be greater than 0)\n");
                }
                if (expirationPicker.getValue() == null) {
                    missingFields.append("• Expiration date\n");
                }
                if (supplierField.getText().trim().isEmpty()) {
                    missingFields.append("• Supplier name\n");
                }

                if (missingFields.length() > 0) {
                    showStyledAlert(Alert.AlertType.ERROR, "Required Fields Missing",
                            "Please fill in the following required fields:\n\n" + missingFields.toString());
                    return;
                }

                // Validate numeric fields
                try {
                    int stock = Integer.parseInt(stockField.getText());
                    if (stock <= 0) {
                        showStyledAlert(Alert.AlertType.ERROR, "Invalid Stock",
                                "Stock quantity must be greater than 0.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showStyledAlert(Alert.AlertType.ERROR, "Invalid Stock",
                            "Please enter a valid number for stock quantity.");
                    return;
                }

                try {
                    BigDecimal price = new BigDecimal(priceField.getText());
                    if (price.compareTo(BigDecimal.ZERO) <= 0) {
                        showStyledAlert(Alert.AlertType.ERROR, "Invalid Price",
                                "Price must be greater than 0.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showStyledAlert(Alert.AlertType.ERROR, "Invalid Price",
                            "Please enter a valid price.");
                    return;
                }

                // All validations passed, save the product
                Product newProduct = product == null ? new Product() : product;

                if (product == null) {
                    newProduct.setMedicineId(medicineIdField.getText());
                }
                newProduct.setName(nameField.getText().trim());
                newProduct.setStock(Integer.parseInt(stockField.getText()));
                newProduct.setPrice(new BigDecimal(priceField.getText()));
                newProduct.setExpirationDate(expirationPicker.getValue());
                newProduct.setSupplier(supplierField.getText().trim());
                newProduct.setCategory(categoryField.getText().trim());

                productService.saveProduct(newProduct);
                loadProducts();
                dialogStage.close();
                showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                        product == null ? "Medicine added successfully!" : "Medicine updated successfully!");

            } catch (Exception ex) {
                showStyledAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to save product: " + ex.getMessage());
            }
        });

        buttonContainer.getChildren().addAll(cancelButton, saveButton);

        // Add all sections to main container
        mainContainer.getChildren().addAll(header, formContainer, buttonContainer);

        // Create scene
        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);

        // Center on parent window
        dialogStage.centerOnScreen();

        // Show dialog
        dialogStage.showAndWait();
    }

    // Helper method to create styled text fields
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

    // Helper method to create field groups with labels
    private VBox createFieldGroup(String labelText, javafx.scene.Node field) {
        VBox group = new VBox(8);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        group.getChildren().addAll(label, field);
        return group;
    }

//    private void showAlert(Alert.AlertType type, String title, String content) {
//        Alert alert = new Alert(type);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(content);
//        alert.showAndWait();
//    }

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

//    private String getDarkerColor(String color) {
//        switch (color) {
//            case "#4CAF50": return "#45a049";
//            case "#F44336": return "#d32f2f";
//            case "#FF9800": return "#f57c00";
//            case "#2196F3": return "#1976D2";
//            default: return color;
//        }
//    }

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

    private boolean showDeleteConfirmation(String productName) {
        // Create custom dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Delete Product");
        dialogStage.setResizable(false);
        dialogStage.setUserData(false);

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        // Title
        Label titleLabel = new Label("Are you absolutely sure?");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        // Message
        Label messageLabel = new Label("This action cannot be undone. This will permanently delete the medicine from the inventory.");
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

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle(
                "-fx-background-color: #1a1a1a; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );
        deleteButton.setOnAction(e -> {
            dialogStage.setUserData(true);
            dialogStage.close();
        });

        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                deleteButton.getStyle().replace("#1a1a1a", "#000000")
        ));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                deleteButton.getStyle().replace("#000000", "#1a1a1a")
        ));

        buttonBox.getChildren().addAll(cancelButton, deleteButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
    }

    // Navigation methods
    private void setActiveButton(Button activeButton) {
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        staffBtn.getStyleClass().remove("active");

        activeButton.getStyleClass().add("active");
    }

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
    }

    @FXML
    private void handleInventory() {
        // Already on inventory page
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
                controller.refreshDashboard();
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
            newScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());

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

}