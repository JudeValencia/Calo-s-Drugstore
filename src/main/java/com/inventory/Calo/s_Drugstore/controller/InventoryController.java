    package com.inventory.Calo.s_Drugstore.controller;

    import com.inventory.Calo.s_Drugstore.entity.Batch;
    import com.inventory.Calo.s_Drugstore.entity.Product;
    import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.repository.BatchRepository;
import com.inventory.Calo.s_Drugstore.service.ProductService;
    import com.inventory.Calo.s_Drugstore.service.UserManagementService;
    import com.inventory.Calo.s_Drugstore.util.IconUtil;
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
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Objects;
    import java.util.ResourceBundle;


    @Controller
    public class InventoryController implements Initializable {
    
        @Autowired
        private ProductService productService;
    
        @Autowired
        private ConfigurableApplicationContext springContext;
    
        @Autowired
        private UserManagementService userManagementService;

        @Autowired
private BatchRepository batchRepository;
    
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

        // KPI Labels
        @FXML private Label totalItemsLabel;
        @FXML private Label lowStockLabel;
        @FXML private Label totalValueLabel;
    
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
    
                        Label nameLabel = new Label(product.getBrandName());
                        nameLabel.setStyle("-fx-font-size: 14px;");

                        // Check if any batch is expiring soon
                        List<Batch> batches = productService.getBatchesForProduct(product);
                        boolean hasExpiringBatch = batches.stream()
                                .anyMatch(Batch::isExpiringSoon);

                        if (hasExpiringBatch) {
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
            nameColumn.setCellValueFactory(data -> {
                String brandName = data.getValue().getBrandName();
                String genericName = data.getValue().getGenericName();
                if (genericName != null && !genericName.isEmpty()) {
                    return new SimpleStringProperty(brandName + " (" + genericName + ")");
                }
                return new SimpleStringProperty(brandName);
            });
    
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
            // ===== FIXED: Expiration Date Column - Now reads from batches WITH STOCK CHECK =====
            expirationColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        Product product = getTableRow().getItem();
                        
                        // Check if product has any stock at all
                        if (product.getStock() == 0) {
                            setText("N/A");
                            setStyle("");
                            return;
                        }
                        
                        List<Batch> batches = productService.getBatchesForProduct(product);
                        LocalDate displayDate = null;

                        if (!batches.isEmpty()) {
                            // Get earliest expiration date from batches that HAVE STOCK
                            displayDate = batches.stream()
                                    .filter(batch -> batch.getStock() > 0)  // Only consider batches with stock
                                    .map(Batch::getExpirationDate)
                                    .filter(date -> date != null)
                                    .min(LocalDate::compareTo)
                                    .orElse(null);
                        }

                        // FALLBACK: If no batch data with stock, check if product has expiration
                        if (displayDate == null && product.getExpirationDate() != null) {
                            displayDate = product.getExpirationDate();
                        }

                        if (displayDate != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. dd, yyyy");
                            setText(displayDate.format(formatter));

                            // Check if expiring soon (within 30 days)
                            LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
                            if (displayDate.isBefore(thirtyDaysFromNow)) {
                                setStyle("-fx-text-fill: #FF6B35; -fx-font-weight: bold;");
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
                Product product = data.getValue();
                
                // If no stock, show N/A
                if (product.getStock() == 0) {
                    return new SimpleStringProperty("N/A");
                }
                
                List<Batch> batches = productService.getBatchesForProduct(product);
                LocalDate displayDate = null;

                if (!batches.isEmpty()) {
                    // Only get expiry from batches that have stock
                    displayDate = batches.stream()
                            .filter(batch -> batch.getStock() > 0)
                            .map(Batch::getExpirationDate)
                            .filter(date -> date != null)
                            .min(LocalDate::compareTo)
                            .orElse(null);
                }

                // FALLBACK to product expiration date if it has stock
                if (displayDate == null && product.getExpirationDate() != null) {
                    displayDate = product.getExpirationDate();
                }

                if (displayDate != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                    return new SimpleStringProperty(displayDate.format(formatter));
                }
                return new SimpleStringProperty("N/A");
            });

            // ===== FIXED: Supplier Column - Now reads from batches WITH FALLBACK =====
            supplierColumn.setCellValueFactory(data -> {
                List<Batch> batches = productService.getBatchesForProduct(data.getValue());

                if (!batches.isEmpty()) {
                    // Get unique suppliers from all batches
                    List<String> suppliers = batches.stream()
                            .map(Batch::getSupplier)
                            .filter(s -> s != null && !s.isEmpty())
                            .distinct()
                            .toList();

                    if (!suppliers.isEmpty()) {
                        // If multiple suppliers, show "Multiple" or list them
                        if (suppliers.size() == 1) {
                            return new SimpleStringProperty(suppliers.get(0));
                        } else {
                            // Show first supplier + count
                            return new SimpleStringProperty(suppliers.get(0) + " (+" + (suppliers.size() - 1) + ")");
                        }
                    }
                }

                // FALLBACK: If no batch data, use product's supplier
                String productSupplier = data.getValue().getSupplier();
                if (productSupplier != null && !productSupplier.isEmpty()) {
                    return new SimpleStringProperty(productSupplier);
                }

                return new SimpleStringProperty("N/A");
            });
            // Actions Column
            actionsColumn.setCellFactory(column -> new TableCell<>() {
                private final Button editBtn = new Button();
                private final Button deleteBtn = new Button();
                private final Button viewBtn = new Button();
    
                {
    
                    // View button with text icon
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
    
                    viewBtn.setOnAction(event -> {
                        Product product = getTableRow().getItem();
                        if (product != null) {
                            handleViewProduct(product);
                        }
                    });
    
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
                        HBox buttons = new HBox(8, viewBtn, editBtn, deleteBtn);
                        buttons.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(buttons);
                    }
                }
            });
    
            medicineIdColumn.setMinWidth(120);
            nameColumn.setMinWidth(200);
            stockColumn.setMinWidth(100);
            statusColumn.setMinWidth(120);
            priceColumn.setMinWidth(100);
            expirationColumn.setMinWidth(130);
            supplierColumn.setMinWidth(150);
            actionsColumn.setMinWidth(220);
    
            inventoryTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
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
            productService.clearBatchCache(); // Clear cache before reload
            List<Product> products = productService.getAllProducts();
            productList.setAll(products);
            inventoryTable.setItems(productList);
            updateTotalCount();
            updateKPIs();
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
    
        @FXML
        private void handleExportCSV() {
            showExportOptionsDialog();
        }

        @FXML
        private void handleImportCSV() {
            try {
                // Create file chooser
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Import Inventory from CSV");

                // Set file extension filter
                javafx.stage.FileChooser.ExtensionFilter extFilter =
                        new javafx.stage.FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
                fileChooser.getExtensionFilters().add(extFilter);

                // Show open dialog
                Stage stage = (Stage) inventoryTable.getScene().getWindow();
                java.io.File file = fileChooser.showOpenDialog(stage);

                if (file != null) {
                    // Parse and validate CSV
                    List<Product> productsToImport = parseCSV(file);

                    if (productsToImport.isEmpty()) {
                        Platform.runLater(() -> {
                            showStyledAlert(Alert.AlertType.WARNING, "No Data",
                                    "The CSV file contains no valid products to import.");
                        });
                        return;
                    }

                    // Show preview and confirmation dialog
                    showImportPreviewDialog(productsToImport, file.getName());
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showStyledAlert(Alert.AlertType.ERROR, "Import Failed",
                            "Failed to import CSV file: " + e.getMessage());
                });
            }
        }

        private List<Product> parseCSV(java.io.File file) throws Exception {
            List<Product> products = new ArrayList<>();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Get the starting Medicine ID ONCE before the loop
            String lastMedicineId = productService.generateNextMedicineId();
            int currentIdNumber = Integer.parseInt(lastMedicineId.substring(3)); // Extract number from "MED018"

            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Skip header row
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Skip empty lines
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        // Parse CSV line (handling quoted values)
                        String[] values = parseCSVLine(line);

                        // Validate minimum required fields (15 fields expected)
                        if (values.length < 15) {
                            System.err.println("Skipping invalid row (not enough columns): " + line);
                            continue;
                        }

                        // Create product from CSV data
                        Product product = new Product();

                        // Use the Medicine ID from CSV if provided, otherwise generate new one
                        String csvMedicineId = values[0].trim();
                        if (csvMedicineId != null && !csvMedicineId.isEmpty()) {
                            product.setMedicineId(csvMedicineId);
                        } else {
                            // Generate sequential Medicine ID for this import batch
                            product.setMedicineId(String.format("MED%03d", currentIdNumber));
                            currentIdNumber++; // Increment for next product
                        }

                        // Map CSV columns to product fields
                        product.setBrandName(values[1].trim()); // Brand Name
                        product.setGenericName(values[2].trim()); // Generic Name
                        product.setStock(Integer.parseInt(values[3].trim())); // Stock
                        product.setPrice(new BigDecimal(values[4].trim())); // Price

                        // Expiration Date
                        if (!values[5].trim().isEmpty()) {
                            product.setExpirationDate(LocalDate.parse(values[5].trim(), dateFormatter));
                        }

                        product.setSupplier(values[6].trim()); // Supplier
                        product.setCategory(values[7].trim()); // Category
                        product.setBatchNumber(values[8].trim()); // Batch Number

                        // Min Stock Level
                        if (!values[9].trim().isEmpty()) {
                            product.setMinStockLevel(Integer.parseInt(values[9].trim()));
                        }

                        // Prescription Required
                        product.setPrescriptionRequired(values[10].trim().equalsIgnoreCase("Yes"));

                        product.setDosageForm(values[11].trim()); // Dosage Form
                        product.setDosageStrength(values[12].trim()); // Dosage Strength
                        product.setManufacturer(values[13].trim()); // Manufacturer
                        product.setUnitOfMeasure(values[14].trim()); // Unit of Measure

                        products.add(product);

                    } catch (Exception e) {
                        System.err.println("Error parsing line: " + line);
                        System.err.println("Error: " + e.getMessage());
                        // Continue with next line
                    }
                }
            }

            return products;
        }

        private String[] parseCSVLine(String line) {
            List<String> result = new ArrayList<>();
            boolean inQuotes = false;
            StringBuilder current = new StringBuilder();

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    result.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            result.add(current.toString());

            return result.toArray(new String[0]);
        }

        private void showImportPreviewDialog(List<Product> products, String fileName) {
            // Create custom dialog
            Stage dialogStage = new Stage();
            IconUtil.setApplicationIcon(dialogStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Import Preview");
            dialogStage.setResizable(false);

            // Main container
            VBox mainContainer = new VBox(20);
            mainContainer.setStyle("-fx-background-color: white; -fx-padding: 25;");
            mainContainer.setPrefWidth(700);
            mainContainer.setMaxHeight(600);

            // Header
            Label titleLabel = new Label("Import Preview");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label subtitleLabel = new Label("Review the products to be imported from: " + fileName);
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
            subtitleLabel.setWrapText(true);

            VBox header = new VBox(8, titleLabel, subtitleLabel);

            // Summary info
            Label summaryLabel = new Label("Products found: " + products.size());
            summaryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

            // Preview table
            TableView<Product> previewTable = new TableView<>();
            previewTable.setPrefHeight(300);
            previewTable.setItems(FXCollections.observableArrayList(products));

            // Define columns
            TableColumn<Product, String> nameCol = new TableColumn<>("Brand Name");
            nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBrandName()));
            nameCol.setPrefWidth(150);

            TableColumn<Product, String> genericCol = new TableColumn<>("Generic Name");
            genericCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenericName()));
            genericCol.setPrefWidth(150);

            TableColumn<Product, String> stockCol = new TableColumn<>("Stock");
            stockCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStock())));
            stockCol.setPrefWidth(80);

            TableColumn<Product, String> priceCol = new TableColumn<>("Price");
            priceCol.setCellValueFactory(data -> new SimpleStringProperty("₱" + data.getValue().getPrice()));
            priceCol.setPrefWidth(100);

            TableColumn<Product, String> supplierCol = new TableColumn<>("Supplier");
            supplierCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplier()));
            supplierCol.setPrefWidth(120);

            previewTable.getColumns().addAll(nameCol, genericCol, stockCol, priceCol, supplierCol);

            // Warning message
            Label warningLabel = new Label("⚠ Note: Existing Product IDs will be replaced with new auto-generated IDs.");
            warningLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #FF9800; -fx-font-style: italic;");
            warningLabel.setWrapText(true);

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

            Button importButton = new Button("Import All");
            importButton.setStyle(
                    "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 12px 30px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-cursor: hand;"
            );

            importButton.setOnMouseEntered(e -> importButton.setStyle(
                    importButton.getStyle() + "-fx-background-color: #45a049;"
            ));
            importButton.setOnMouseExited(e -> importButton.setStyle(
                    importButton.getStyle().replace("-fx-background-color: #45a049;", "-fx-background-color: #4CAF50;")
            ));

            importButton.setOnAction(e -> {
                try {
                    int successCount = 0;
                    int failCount = 0;

                    for (Product product : products) {
                        try {
                            // Import as batch instead of direct product save
                            productService.saveProductWithBatch(
                                    product,
                                    product.getStock(),
                                    product.getExpirationDate(),
                                    product.getPrice(),
                                    product.getSupplier()
                            );
                            successCount++;
                        } catch (Exception ex) {
                            failCount++;
                            System.err.println("Failed to import: " + product.getBrandName() + " - " + ex.getMessage());
                        }
                    }

                    dialogStage.close();
                    loadProducts();

                    final int finalSuccess = successCount;
                    final int finalFail = failCount;

                    Platform.runLater(() -> {
                        if (finalFail == 0) {
                            showStyledAlert(Alert.AlertType.INFORMATION, "Import Successful",
                                    "Successfully imported " + finalSuccess + " products!");
                        } else {
                            showStyledAlert(Alert.AlertType.WARNING, "Import Completed with Errors",
                                    "Successfully imported: " + finalSuccess + " products\n" +
                                            "Failed: " + finalFail + " products");
                        }
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showStyledAlert(Alert.AlertType.ERROR, "Import Failed",
                                "Failed to import products: " + ex.getMessage());
                    });
                }
            });

            buttonContainer.getChildren().addAll(cancelButton, importButton);

            // Add all to container
            mainContainer.getChildren().addAll(header, summaryLabel, previewTable, warningLabel, buttonContainer);

            Scene scene = new Scene(mainContainer);
            dialogStage.setScene(scene);
            dialogStage.centerOnScreen();
            dialogStage.showAndWait();
        }
    
        private void showExportOptionsDialog() {
            // Create custom dialog
            Stage dialogStage = new Stage();
            IconUtil.setApplicationIcon(dialogStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Export Inventory");
            dialogStage.setResizable(false);
    
            // Main container
            VBox mainContainer = new VBox(20);
            mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");
            mainContainer.setPrefWidth(500);
    
            // Header
            Label titleLabel = new Label("Export Inventory");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    
            Label subtitleLabel = new Label("Choose the format you want to export your inventory data.");
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
            subtitleLabel.setWrapText(true);
    
            VBox header = new VBox(8, titleLabel, subtitleLabel);
    
            // Export options
            VBox optionsContainer = new VBox(15);
    
            // CSV Option Card
            VBox csvCard = createExportOptionCard(
                    "📊 Export as CSV",
                    "Export inventory data in CSV format for use in Excel, Google Sheets, or other spreadsheet applications.",
                    "• Easy to edit and analyze\n• Compatible with all spreadsheet software\n• Good for data migration",
                    "#4CAF50"
            );
    
            csvCard.setOnMouseClicked(e -> {
                dialogStage.close();
                handleCSVExport();
            });
    
            csvCard.setOnMouseEntered(e -> csvCard.setStyle(
                    csvCard.getStyle().replace("-fx-background-color: #F8F9FA;", "-fx-background-color: #E8F5E9;")
            ));
            csvCard.setOnMouseExited(e -> csvCard.setStyle(
                    csvCard.getStyle().replace("-fx-background-color: #E8F5E9;", "-fx-background-color: #F8F9FA;")
            ));
    
            // PDF Report Option Card
            VBox pdfCard = createExportOptionCard(
                    "📄 Export as PDF Report",
                    "Generate a professional PDF report with complete inventory details, statistics, and formatting.",
                    "• Professional presentation\n• Ready to print or share\n• Includes summary statistics",
                    "#2196F3"
            );
    
            pdfCard.setOnMouseClicked(e -> {
                dialogStage.close();
                handlePDFExport();
            });
    
            pdfCard.setOnMouseEntered(e -> pdfCard.setStyle(
                    pdfCard.getStyle().replace("-fx-background-color: #F8F9FA;", "-fx-background-color: #E3F2FD;")
            ));
            pdfCard.setOnMouseExited(e -> pdfCard.setStyle(
                    pdfCard.getStyle().replace("-fx-background-color: #E3F2FD;", "-fx-background-color: #F8F9FA;")
            ));
    
            optionsContainer.getChildren().addAll(csvCard, pdfCard);
    
            // Cancel button
            HBox buttonContainer = new HBox();
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
    
            buttonContainer.getChildren().add(cancelButton);
    
            // Add all sections
            mainContainer.getChildren().addAll(header, optionsContainer, buttonContainer);
    
            // Create scene
            Scene scene = new Scene(mainContainer);
            dialogStage.setScene(scene);
            dialogStage.centerOnScreen();
            dialogStage.showAndWait();
        }
    
        private VBox createExportOptionCard(String title, String description, String features, String accentColor) {
            VBox card = new VBox(12);
            card.setStyle(
                    "-fx-background-color: #F8F9FA; " +
                            "-fx-background-radius: 10px; " +
                            "-fx-padding: 20; " +
                            "-fx-border-color: #E0E0E0; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 10px; " +
                            "-fx-cursor: hand;"
            );
    
            // Title with icon
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");
    
            // Description
            Label descLabel = new Label(description);
            descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555;");
            descLabel.setWrapText(true);
    
            // Features
            Label featuresLabel = new Label(features);
            featuresLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            featuresLabel.setWrapText(true);
    
            card.getChildren().addAll(titleLabel, descLabel, featuresLabel);
    
            return card;
        }
    
        private void handleCSVExport() {
            try {
                // Get all products from inventory
                List<Product> products = productService.getAllProducts();
    
                if (products.isEmpty()) {
                    showStyledAlert(Alert.AlertType.WARNING, "No Data",
                            "There are no products in the inventory to export.");
                    return;
                }
    
                // Create file chooser
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Export Inventory to CSV");
    
                // Set default filename with current date
                String defaultFileName = "Inventory_Export_" +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";
                fileChooser.setInitialFileName(defaultFileName);
    
                // Set file extension filter
                javafx.stage.FileChooser.ExtensionFilter extFilter =
                        new javafx.stage.FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
                fileChooser.getExtensionFilters().add(extFilter);
    
                // Show save dialog
                Stage stage = (Stage) inventoryTable.getScene().getWindow();
                java.io.File file = fileChooser.showSaveDialog(stage);
    
                if (file != null) {
                    // Export to CSV
                    exportToCSV(products, file);

                    Platform.runLater(() -> {
                        String message = "Inventory exported successfully to CSV!\n\n" +
                                "File: " + file.getName() + "\n" +
                                "Total Products: " + products.size() + "\n\n" +
                                "The file has been saved to:\n" + file.getAbsolutePath();

                        showStyledAlert(Alert.AlertType.INFORMATION, "Export Successful", message);
                    });
                }
    
            } catch (Exception e) {
                e.printStackTrace();
                showStyledAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export inventory: " + e.getMessage());
            }
        }
    
    private void handlePDFExport() {
        try {
            // Get products from the inventory table (filters out deleted products)
            List<Product> products = new ArrayList<>(inventoryTable.getItems());

            if (products.isEmpty()) {
                showStyledAlert(Alert.AlertType.WARNING, "No Data",
                        "There are no products in the inventory to export.");
                return;
            }                // Create file chooser
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Export Inventory Report to PDF");
    
                // Set default filename with current date
                String defaultFileName = "Inventory_Report_" +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
                fileChooser.setInitialFileName(defaultFileName);
    
                // Set file extension filter
                javafx.stage.FileChooser.ExtensionFilter extFilter =
                        new javafx.stage.FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
                fileChooser.getExtensionFilters().add(extFilter);
    
                // Show save dialog
                Stage stage = (Stage) inventoryTable.getScene().getWindow();
                java.io.File file = fileChooser.showSaveDialog(stage);
    
                if (file != null) {
                    // Export to PDF
                    exportToPDF(products, file);

                    Platform.runLater(() -> {
                        String message = "Inventory report exported successfully to PDF!\n\n" +
                                "File: " + file.getName() + "\n" +
                                "Total Products: " + products.size() + "\n\n" +
                                "The file has been saved to:\n" + file.getAbsolutePath();

                        showStyledAlert(Alert.AlertType.INFORMATION, "Export Successful", message);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                showStyledAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export inventory report: " + e.getMessage());
            }
        }
    
        private void exportToCSV(List<Product> products, java.io.File file) throws Exception {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
    
                // Write CSV header
                writer.println("Product ID,Brand Name,Generic Name,Stock,Price,Expiration Date," +
                        "Supplier,Category,Batch Number,Min Stock Level,Prescription Required," +
                        "Dosage Form,Dosage Strength,Manufacturer,Unit of Measure");
    
                // Write each product
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
                for (Product product : products) {
                    StringBuilder line = new StringBuilder();
    
                    // Medicine ID
                    line.append(escapeCSV(product.getMedicineId())).append(",");
    
                    // Brand Name
                    line.append(escapeCSV(product.getBrandName())).append(",");
    
                    // Generic Name
                    line.append(escapeCSV(product.getGenericName() != null ? product.getGenericName() : "")).append(",");
    
                    // Stock
                    line.append(product.getStock()).append(",");
    
                    // Price
                    line.append(product.getPrice()).append(",");
    
                    // Expiration Date
                    line.append(product.getExpirationDate() != null ?
                            product.getExpirationDate().format(dateFormatter) : "").append(",");
    
                    // Supplier
                    line.append(escapeCSV(product.getSupplier())).append(",");
    
                    // Category
                    line.append(escapeCSV(product.getCategory() != null ? product.getCategory() : "")).append(",");
    
                    // Batch Number
                    line.append(escapeCSV(product.getBatchNumber() != null ? product.getBatchNumber() : "")).append(",");
    
                    // Min Stock Level
                    line.append(product.getMinStockLevel() != null ? product.getMinStockLevel() : "10").append(",");
    
                    // Prescription Required
                    line.append(product.getPrescriptionRequired() != null && product.getPrescriptionRequired() ? "Yes" : "No").append(",");
    
                    // Dosage Form
                    line.append(escapeCSV(product.getDosageForm() != null ? product.getDosageForm() : "")).append(",");
    
                    // Dosage Strength
                    line.append(escapeCSV(product.getDosageStrength() != null ? product.getDosageStrength() : "")).append(",");
    
                    // Manufacturer
                    line.append(escapeCSV(product.getManufacturer() != null ? product.getManufacturer() : "")).append(",");
    
                    // Unit of Measure
                    line.append(escapeCSV(product.getUnitOfMeasure() != null ? product.getUnitOfMeasure() : ""));
    
                    writer.println(line.toString());
                }
    
                writer.flush();
            }
        }
    
        private void exportToPDF(List<Product> products, java.io.File file) throws Exception {
            // Using iText7 for PDF generation
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(file);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);
    
            // Set margins
            document.setMargins(40, 40, 40, 40);
    
            // Title
            com.itextpdf.layout.element.Paragraph title = new com.itextpdf.layout.element.Paragraph("INVENTORY REPORT")
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                    .setFontSize(24)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLACK);
            document.add(title);
    
            // Subtitle with date
            com.itextpdf.layout.element.Paragraph subtitle = new com.itextpdf.layout.element.Paragraph(
                    "Calo's Drugstore - Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                    .setFontSize(12)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
                    .setMarginBottom(20);
            document.add(subtitle);
    
            // Summary Statistics
            document.add(new com.itextpdf.layout.element.Paragraph("SUMMARY STATISTICS")
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                    .setFontSize(14)
                    .setMarginTop(10)
                    .setMarginBottom(10));
    
            // Calculate statistics
            int totalProducts = products.size();
            int totalStock = products.stream().mapToInt(Product::getStock).sum();
            BigDecimal totalValue = products.stream()
                    .map(p -> p.getPrice().multiply(new BigDecimal(p.getStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long lowStockCount = products.stream().filter(Product::isLowStock).count();
            
            // Count products with batches expiring in next 30 days (check actual batch dates)
            LocalDate today = LocalDate.now();
            LocalDate thirtyDaysFromNow = today.plusDays(30);
            long expiringCount = products.stream()
                    .filter(product -> {
                        List<com.inventory.Calo.s_Drugstore.entity.Batch> batches = productService.getBatchesForProduct(product);
                        if (batches.isEmpty()) {
                            // Fallback to product expiration date
                            return product.getExpirationDate() != null &&
                                   !product.getExpirationDate().isBefore(today) &&
                                   product.getExpirationDate().isBefore(thirtyDaysFromNow);
                        }
                        // Check if any batch expires in next 30 days
                        LocalDate earliestExpiry = batches.stream()
                                .map(com.inventory.Calo.s_Drugstore.entity.Batch::getExpirationDate)
                                .filter(date -> date != null)
                                .min(LocalDate::compareTo)
                                .orElse(null);
                        return earliestExpiry != null &&
                               !earliestExpiry.isBefore(today) &&
                               earliestExpiry.isBefore(thirtyDaysFromNow);
                    })
                    .count();
    
            // Statistics Table
            com.itextpdf.layout.element.Table statsTable = new com.itextpdf.layout.element.Table(2);
            statsTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
    
            addStatsRow(statsTable, "Total Products:", String.valueOf(totalProducts));
            addStatsRow(statsTable, "Total Stock Units:", String.valueOf(totalStock));
            addStatsRow(statsTable, "Total Inventory Value:", "₱" + String.format("%,.2f", totalValue));
            addStatsRow(statsTable, "Low Stock Items:", String.valueOf(lowStockCount));
            addStatsRow(statsTable, "Expiring Soon (30 days):", String.valueOf(expiringCount));
    
            document.add(statsTable);
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));
    
            // Products Table
            document.add(new com.itextpdf.layout.element.Paragraph("COMPLETE INVENTORY LIST")
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                    .setFontSize(14)
                    .setMarginTop(10)
                    .setMarginBottom(10));
    
            // Create table with 8 columns
            float[] columnWidths = {1.5f, 2.5f, 2f, 1f, 1.5f, 2f, 1.5f, 2f};
            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(columnWidths);
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
    
            // Header row
            String[] headers = {"Product ID", "Brand Name", "Generic Name", "Stock", "Price", "Expiry Date", "Status", "Supplier"};
            for (String header : headers) {
                table.addHeaderCell(createHeaderCell(header));
            }
    
        // Data rows
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        for (Product product : products) {
            // Get earliest batch expiration date (same logic as UI table)
            List<com.inventory.Calo.s_Drugstore.entity.Batch> batches = productService.getBatchesForProduct(product);
            LocalDate expiryDate = null;
            String supplier = product.getSupplier();
            
            if (!batches.isEmpty()) {
                // Get earliest expiration date
                expiryDate = batches.stream()
                        .map(com.inventory.Calo.s_Drugstore.entity.Batch::getExpirationDate)
                        .filter(date -> date != null)
                        .min(LocalDate::compareTo)
                        .orElse(null);
                
                // Get supplier from most recent batch (latest creation date)
                supplier = batches.stream()
                        .max((b1, b2) -> b1.getCreatedAt().compareTo(b2.getCreatedAt()))
                        .map(com.inventory.Calo.s_Drugstore.entity.Batch::getSupplier)
                        .orElse(product.getSupplier());
            }
            
            // Fallback to product expiration date
            if (expiryDate == null) {
                expiryDate = product.getExpirationDate();
            }
            
            table.addCell(createTableCell(product.getMedicineId()));
            table.addCell(createTableCell(product.getBrandName()));
            table.addCell(createTableCell(product.getGenericName() != null ? product.getGenericName() : "N/A"));
            table.addCell(createTableCell(String.valueOf(product.getStock())));
            table.addCell(createTableCell("₱" + product.getPrice().toString()));
            table.addCell(createTableCell(expiryDate != null ?
                    expiryDate.format(dateFormatter) : "N/A"));
            table.addCell(createTableCell(product.getStockStatus() != null ? product.getStockStatus() : "N/A"));
            table.addCell(createTableCell(supplier != null ? supplier : "N/A"));
        }            document.add(table);
    
            // Footer
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));
            document.add(new com.itextpdf.layout.element.Paragraph("End of Report")
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_OBLIQUE))
                    .setFontSize(10)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
    
            document.close();
        }
    
        private void addStatsRow(com.itextpdf.layout.element.Table table, String label, String value) throws Exception {
            com.itextpdf.layout.element.Cell labelCell = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(label))
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                    .setFontSize(11)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(5);
    
            com.itextpdf.layout.element.Cell valueCell = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(value))
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                    .setFontSize(11)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(5);
    
            table.addCell(labelCell);
            table.addCell(valueCell);
        }
    
        private com.itextpdf.layout.element.Cell createHeaderCell(String text) throws Exception {
            return new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(text))
                    .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD))
                    .setFontSize(9)
                    .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setPadding(5);
        }
    
    private com.itextpdf.layout.element.Cell createTableCell(String text) throws Exception {
        // Handle null values
        String cellText = (text != null && !text.isEmpty()) ? text : "N/A";
        return new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(cellText))
                .setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA))
                .setFontSize(8)
                .setPadding(4);
    }        private String escapeCSV(String value) {
            if (value == null) {
                return "";
            }
    
            // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
    
            return value;
        }
    
        private void showBulkAddDialog() {
            // Create custom dialog
            Stage dialogStage = new Stage();
            IconUtil.setApplicationIcon(dialogStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Bulk Add Products");
            dialogStage.setResizable(false);
    
            // Main container
            VBox mainContainer = new VBox(15);
            mainContainer.setStyle("-fx-background-color: white; -fx-padding: 25;");
            mainContainer.setPrefWidth(900);
            mainContainer.setMaxHeight(600);
    
            // Header
            Label titleLabel = new Label("Bulk Add Products");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    
            Label subtitleLabel = new Label("Add multiple products to inventory at once. Fill in the details for each medicine you want to add.");
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
            subtitleLabel.setWrapText(true);
    
            VBox header = new VBox(5, titleLabel, subtitleLabel);
    
            // ScrollPane for medicine forms
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setPrefHeight(350);

            String scrollBarStyle =
                    ".scroll-bar {" +
                            "    -fx-background-color: transparent !important;" +
                            "}" +
                            ".scroll-bar .thumb {" +
                            "    -fx-background-color: #cbd5e0 !important;" +
                            "    -fx-background-radius: 4px !important;" +
                            "}" +
                            ".scroll-bar .thumb:hover {" +
                            "    -fx-background-color: #a0aec0 !important;" +
                            "}" +
                            ".scroll-bar .track {" +
                            "    -fx-background-color: transparent !important;" +
                            "}" +
                            ".scroll-bar .increment-button," +
                            ".scroll-bar .decrement-button {" +
                            "    -fx-background-color: transparent !important;" +
                            "    -fx-padding: 0 !important;" +
                            "}";
            scrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.getStylesheets().add("data:text/css," + scrollBarStyle);
                }
            });

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
                        if (formData.brandName.getText().trim().isEmpty() &&
                                formData.genericName.getText().trim().isEmpty() &&
                                formData.stock.getText().equals("0") &&
                                formData.price.getText().equals("0.00")) {
                            emptyCount++;
                            continue;
                        }
    
                        // Validate required fields
                        StringBuilder missingFields = new StringBuilder();
    
                        if (formData.brandName.getText().trim().isEmpty()) {
                            missingFields.append("Brand Name, ");
                        }
                        if (formData.genericName.getText().trim().isEmpty()) {
                            missingFields.append("Generic Name, ");
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
                            newProduct.setBrandName(formData.brandName.getText().trim());
                            newProduct.setGenericName(formData.genericName.getText().trim());
                            newProduct.setStock(Integer.parseInt(formData.stock.getText()));
                            newProduct.setPrice(new BigDecimal(formData.price.getText()));
                            newProduct.setExpirationDate(formData.expiryDate.getValue());
                            newProduct.setSupplier(formData.supplier.getText().trim());
                            newProduct.setCategory(formData.category.getText().trim());
                            newProduct.setBatchNumber(formData.batchNumber.getText().trim());
                            newProduct.setMinStockLevel(Integer.parseInt(formData.minStockLevel.getText()));
                            newProduct.setPrescriptionRequired(formData.prescriptionRequired.isSelected());
                            newProduct.setDosageForm(formData.dosageForm.getValue());
                            newProduct.setDosageStrength(formData.dosageStrength.getText().trim());
                            newProduct.setManufacturer(formData.manufacturer.getText().trim());
                            newProduct.setUnitOfMeasure(formData.unitOfMeasure.getValue());
    
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
                    showStyledAlert(Alert.AlertType.ERROR, "Error", "Failed to add products: " + ex.getMessage());
                }
            });
    
            buttonContainer.getChildren().addAll(cancelButton, saveAllButton);
    
            // Add all sections to main container
            mainContainer.getChildren().addAll(header, scrollPane, addButtonContainer, buttonContainer);
    
            // Create scene
            Scene scene = new Scene(mainContainer);
            scene.getStylesheets().add("data:text/css," + scrollBarStyle);
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
                            "-fx-padding: 25; " +
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
            TextField brandNameField = new TextField();
            brandNameField.setPromptText("Enter brand name");
            styleTextField(brandNameField);
    
            TextField genericNameField = new TextField();
            genericNameField.setPromptText("Enter generic/active ingredient");
            styleTextField(genericNameField);
    
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
            // Column 1
            grid.add(createFieldLabel("Brand Name *"), 0, 0);
            grid.add(brandNameField, 0, 1);
            grid.add(createFieldLabel("Generic Name *"), 0, 2);
            grid.add(genericNameField, 0, 3);
            grid.add(createFieldLabel("Price (₱) *"), 0, 4);
            grid.add(priceField, 0, 5);
            grid.add(createFieldLabel("Supplier *"), 0, 6);
            grid.add(supplierField, 0, 7);
    
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
            MedicineFormData formData = new MedicineFormData(brandNameField, genericNameField, stockField, priceField, expiryPicker, supplierField, categoryField);
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
            TextField brandNameField = new TextField();
            brandNameField.setPromptText("Enter brand name");
            styleTextField(brandNameField);
    
            TextField genericNameField = new TextField();
            genericNameField.setPromptText("Enter generic/active ingredient");
            styleTextField(genericNameField);
    
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
    
            // New Priority Fields
            TextField batchNumberField = new TextField();
            batchNumberField.setPromptText("Batch/Lot number");
            styleTextField(batchNumberField);
    
            TextField minStockField = new TextField("10");
            minStockField.setPromptText("10");
            styleTextField(minStockField);
    
            CheckBox prescriptionCheckBox = new CheckBox("Rx Required");
            prescriptionCheckBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");
    
            ComboBox<String> dosageFormCombo = new ComboBox<>();
            dosageFormCombo.getItems().addAll("Tablet", "Capsule", "Syrup", "Injection", "Cream", "Ointment", "Drops", "Inhaler", "Other");
            dosageFormCombo.setValue("Tablet");
            dosageFormCombo.setPromptText("Select form");
            styleComboBox(dosageFormCombo);
    
            TextField dosageStrengthField = new TextField();
            dosageStrengthField.setPromptText("e.g., 500mg");
            styleTextField(dosageStrengthField);
    
            TextField manufacturerField = new TextField();
            manufacturerField.setPromptText("Manufacturer name");
            styleTextField(manufacturerField);
    
            ComboBox<String> unitCombo = new ComboBox<>();
            unitCombo.getItems().addAll("Box", "Strip", "Bottle", "Piece", "Vial", "Tube", "Pack");
            unitCombo.setValue("Piece");
            unitCombo.setPromptText("Select unit");
            styleComboBox(unitCombo);
    
            // Column 1
            // Column 1
            grid.add(createFieldLabel("Brand Name *"), 0, 0);
            grid.add(brandNameField, 0, 1);
            grid.add(createFieldLabel("Generic Name *"), 0, 2);
            grid.add(genericNameField, 0, 3);
            grid.add(createFieldLabel("Dosage Form *"), 0, 4);
            grid.add(dosageFormCombo, 0, 5);
            grid.add(createFieldLabel("Dosage Strength"), 0, 6);
            grid.add(dosageStrengthField, 0, 7);
            grid.add(createFieldLabel("Manufacturer"), 0, 8);
            grid.add(manufacturerField, 0, 9);
            grid.add(createFieldLabel("Price (₱) *"), 0, 10);
            grid.add(priceField, 0, 11);
            grid.add(createFieldLabel("Supplier *"), 0, 12);
            grid.add(supplierField, 0, 13);
    
            // Column 2
            grid.add(createFieldLabel("Stock Quantity *"), 1, 0);
            grid.add(stockField, 1, 1);
            grid.add(createFieldLabel("Unit of Measure *"), 1, 2);
            grid.add(unitCombo, 1, 3);
            grid.add(createFieldLabel("Reorder Level *"), 1, 4);
            grid.add(minStockField, 1, 5);
            grid.add(createFieldLabel("Expiration Date *"), 1, 6);
            grid.add(expiryPicker, 1, 7);
            grid.add(createFieldLabel("Category"), 1, 8);
            grid.add(categoryField, 1, 9);
            grid.add(createFieldLabel("Batch Number"), 1, 10);
            grid.add(batchNumberField, 1, 11);
            grid.add(createFieldLabel(""), 1, 12);
            grid.add(prescriptionCheckBox, 1, 13);
    
            // Set column constraints
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(50);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(50);
            grid.getColumnConstraints().addAll(col1, col2);
    
            medicineCard.getChildren().addAll(headerLabel, grid);
            container.getChildren().add(medicineCard);
    
            // Store form data
            MedicineFormData formData = new MedicineFormData(brandNameField, genericNameField, stockField, priceField, expiryPicker,
                    supplierField, categoryField, batchNumberField, minStockField, prescriptionCheckBox,
                    dosageFormCombo, dosageStrengthField, manufacturerField, unitCombo);
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
        }
    
        private Label createFieldLabel(String text) {
            Label label = new Label(text);
            label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            return label;
        }
    
        // Inner class to store form field references
        private static class MedicineFormData {
            TextField brandName;
            TextField genericName;
            TextField stock;
            TextField price;
            DatePicker expiryDate;
            TextField supplier;
            TextField category;
            TextField batchNumber;
            TextField minStockLevel;
            CheckBox prescriptionRequired;
            ComboBox<String> dosageForm;
            TextField dosageStrength;
            TextField manufacturer;
            ComboBox<String> unitOfMeasure;
    
            public MedicineFormData(TextField brandName, TextField genericName, TextField stock, TextField price,
                                    DatePicker expiryDate, TextField supplier, TextField category,
                                    TextField batchNumber, TextField minStockLevel, CheckBox prescriptionRequired,
                                    ComboBox<String> dosageForm, TextField dosageStrength,
                                    TextField manufacturer, ComboBox<String> unitOfMeasure) {
                this.brandName = brandName;
                this.genericName = genericName;
                this.stock = stock;
                this.price = price;
                this.expiryDate = expiryDate;
                this.supplier = supplier;
                this.category = category;
                this.batchNumber = batchNumber;
                this.minStockLevel = minStockLevel;
                this.prescriptionRequired = prescriptionRequired;
                this.dosageForm = dosageForm;
                this.dosageStrength = dosageStrength;
                this.manufacturer = manufacturer;
                this.unitOfMeasure = unitOfMeasure;
            }
    
            public MedicineFormData(TextField brandName, TextField genericName, TextField stock, TextField price,
                                    DatePicker expiryDate, TextField supplier, TextField category) {
                this.brandName = brandName;
                this.genericName = genericName;
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
    
        private void handleViewProduct(Product product) {
            showProductDetailsDialog(product);
        }

        private void updateKPIs() {
            // Total items
            totalItemsLabel.setText(String.valueOf(productList.size()));

            // Low stock count
            long lowStockCount = productList.stream()
                    .filter(p -> p.getStock() <= p.getMinStockLevel())
                    .count();
            lowStockLabel.setText(String.valueOf(lowStockCount));

            // Total inventory value
            BigDecimal totalValue = productList.stream()
                    .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalValueLabel.setText("₱" + String.format("%,.2f", totalValue));
        }

        private void showProductDetailsDialog(Product product) {
            // Create custom dialog
            Stage dialogStage = new Stage();
            IconUtil.setApplicationIcon(dialogStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Product Details");
            dialogStage.setResizable(false);

            // Main container
            VBox mainContainer = new VBox(10);
            mainContainer.setStyle("-fx-background-color: white; -fx-padding: 25;");
            mainContainer.setPrefWidth(700);
            mainContainer.setMaxHeight(700);

            // Header
            Label titleLabel = new Label(product.getBrandName());
            titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label subtitleLabel = new Label("Product ID: " + product.getMedicineId());
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

            VBox header = new VBox(5, titleLabel, subtitleLabel);

            // ScrollPane for content
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setPrefHeight(450);

            String scrollBarStyleDetails =
                    ".scroll-bar {" +
                            "    -fx-background-color: transparent !important;" +
                            "}" +
                            ".scroll-bar .thumb {" +
                            "    -fx-background-color: #cbd5e0 !important;" +
                            "    -fx-background-radius: 4px !important;" +
                            "}" +
                            ".scroll-bar .thumb:hover {" +
                            "    -fx-background-color: #a0aec0 !important;" +
                            "}" +
                            ".scroll-bar .track {" +
                            "    -fx-background-color: transparent !important;" +
                            "}" +
                            ".scroll-bar .increment-button," +
                            ".scroll-bar .decrement-button {" +
                            "    -fx-background-color: transparent !important;" +
                            "    -fx-padding: 0 !important;" +
                            "}";
            scrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.getStylesheets().add("data:text/css," + scrollBarStyleDetails);
                }
            });

            VBox contentContainer = new VBox(20);

            // Section 1: Basic Information
            VBox basicSection = createDetailSection("Basic Information",
                    createDetailRow("Brand Name:", product.getBrandName()),
                    createDetailRow("Generic Name:", product.getGenericName() != null ? product.getGenericName() : "N/A"),
                    createDetailRow("Product ID:", product.getMedicineId()),
                    createDetailRow("Category:", product.getCategory() != null ? product.getCategory() : "N/A")
            );

            // Section 2: Medical Information
            VBox medicalSection = createDetailSection("Product Information",
                    createDetailRow("Dosage Form:", product.getDosageForm() != null ? product.getDosageForm() : "N/A"),
                    createDetailRow("Dosage Strength:", product.getDosageStrength() != null ? product.getDosageStrength() : "N/A"),
                    createDetailRow("Prescription Required:", product.getPrescriptionRequired() != null && product.getPrescriptionRequired() ? "Yes ⚕" : "No")
            );

            // Section 3: Stock & Inventory
            String stockStatus = product.isLowStock() ? product.getStockStatus() + " ⚠" : product.getStockStatus();
            VBox stockSection = createDetailSection("Stock & Inventory",
                    createDetailRow("Current Stock:", String.valueOf(product.getStock())),
                    createDetailRow("Unit of Measure:", product.getUnitOfMeasure() != null ? product.getUnitOfMeasure() : "N/A"),
                    createDetailRow("Minimum Stock Level:", product.getMinStockLevel() != null ? String.valueOf(product.getMinStockLevel()) : "N/A"),
                    createDetailRow("Stock Status:", stockStatus)
            );

            // Section 4: Pricing - GET FROM BATCHES
            List<Batch> batches = productService.getBatchesForProduct(product);
            String priceDisplay = "N/A";
            if (!batches.isEmpty()) {
                // Get the most common price from batches
                BigDecimal batchPrice = batches.get(0).getPrice();
                priceDisplay = "₱" + batchPrice.toString();

                // Check if there are multiple prices
                boolean multiplePrices = batches.stream()
                        .map(Batch::getPrice)
                        .distinct()
                        .count() > 1;

                if (multiplePrices) {
                    priceDisplay += " (varies by batch)";
                }
            } else if (product.getPrice() != null) {
                // Fallback to product price
                priceDisplay = "₱" + product.getPrice().toString();
            }

            VBox pricingSection = createDetailSection("Pricing",
                    createDetailRow("Price per Unit:", priceDisplay)
            );

            // Section 5: Supplier & Manufacturing - GET FROM BATCHES
            String supplierDisplay = "N/A";
            if (!batches.isEmpty()) {
                List<String> suppliers = batches.stream()
                        .map(Batch::getSupplier)
                        .filter(s -> s != null && !s.isEmpty())
                        .distinct()
                        .toList();

                if (!suppliers.isEmpty()) {
                    if (suppliers.size() == 1) {
                        supplierDisplay = suppliers.get(0);
                    } else {
                        supplierDisplay = suppliers.get(0) + " (+" + (suppliers.size() - 1) + " more)";
                    }
                }
            } else if (product.getSupplier() != null && !product.getSupplier().isEmpty()) {
                // Fallback to product supplier
                supplierDisplay = product.getSupplier();
            }

            String batchNumberDisplay = "N/A";
            long batchCount = productService.countBatchesForProduct(product);
            if (batchCount > 0) {
                batchNumberDisplay = batchCount + " batch" + (batchCount > 1 ? "es" : "") + " (click 'View Batches' below)";
            }

            VBox supplierSection = createDetailSection("Supplier & Manufacturing",
                    createDetailRow("Supplier:", supplierDisplay),
                    createDetailRow("Manufacturer:", product.getManufacturer() != null ? product.getManufacturer() : "N/A"),
                    createDetailRow("Batches:", batchNumberDisplay)
            );

            // Section 6: Important Dates - GET FROM BATCHES
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

            LocalDate earliestExpiry = null;
            if (!batches.isEmpty()) {
                earliestExpiry = batches.stream()
                        .map(Batch::getExpirationDate)
                        .filter(date -> date != null)
                        .min(LocalDate::compareTo)
                        .orElse(null);
            }

            // Fallback to product expiry if no batches
            if (earliestExpiry == null) {
                earliestExpiry = product.getExpirationDate();
            }

            String expiryDateStr = earliestExpiry != null ? earliestExpiry.format(formatter) : "N/A";
            String expiryWarning = "";
            String daysUntilExpiryStr = "N/A";

            if (earliestExpiry != null) {
                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), earliestExpiry);

                if (earliestExpiry.isBefore(LocalDate.now())) {
                    expiryWarning = " ⚠ Expired!";
                    daysUntilExpiryStr = "Expired";
                } else if (daysUntilExpiry <= 30) {
                    expiryWarning = " ⚠ Expiring Soon!";
                    daysUntilExpiryStr = daysUntilExpiry + " days";
                } else {
                    daysUntilExpiryStr = daysUntilExpiry + " days";
                }
            }

            VBox datesSection = createDetailSection("Important Dates",
                    createDetailRow("Expiration Date:", expiryDateStr + expiryWarning),
                    createDetailRow("Days Until Expiry:", daysUntilExpiryStr)
            );

            // Add "View Batches" button if product has batches
            if (batchCount > 0) {
                Button viewBatchesBtn = new Button("📦 View All Batches (" + batchCount + ")");
                viewBatchesBtn.setStyle(
                        "-fx-background-color: #2196F3; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 12px 20px; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-cursor: hand;"
                );

                viewBatchesBtn.setOnMouseEntered(e -> viewBatchesBtn.setStyle(
                        viewBatchesBtn.getStyle().replace("#2196F3", "#1976D2")
                ));
                viewBatchesBtn.setOnMouseExited(e -> viewBatchesBtn.setStyle(
                        viewBatchesBtn.getStyle().replace("#1976D2", "#2196F3")
                ));

                viewBatchesBtn.setOnAction(e -> {
                    dialogStage.close();
                    showBatchesDialog(product);
                });

                HBox batchButtonContainer = new HBox(viewBatchesBtn);
                batchButtonContainer.setAlignment(Pos.CENTER);
                batchButtonContainer.setStyle("-fx-padding: 10 0 10 0;");

                contentContainer.getChildren().add(batchButtonContainer);
            }

            contentContainer.getChildren().addAll(
                    basicSection,
                    medicalSection,
                    stockSection,
                    pricingSection,
                    supplierSection,
                    datesSection
            );

            scrollPane.setContent(contentContainer);

            // Close button
            HBox buttonContainer = new HBox();
            buttonContainer.setAlignment(Pos.CENTER_RIGHT);

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

            closeButton.setOnMouseEntered(e -> closeButton.setStyle(
                    closeButton.getStyle() + "-fx-background-color: #45a049;"
            ));
            closeButton.setOnMouseExited(e -> closeButton.setStyle(
                    closeButton.getStyle().replace("-fx-background-color: #45a049;", "-fx-background-color: #4CAF50;")
            ));

            closeButton.setOnAction(e -> dialogStage.close());
            buttonContainer.getChildren().add(closeButton);

            // Add all sections to main container
            mainContainer.getChildren().addAll(header, scrollPane, buttonContainer);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            // Create scene
            Scene scene = new Scene(mainContainer);
            dialogStage.setScene(scene);
            dialogStage.centerOnScreen();
            dialogStage.showAndWait();
        }

        private VBox createDetailSection(String sectionTitle, HBox... rows) {
            VBox section = new VBox(12);
            section.setStyle(
                    "-fx-background-color: #F8F9FA; " +
                            "-fx-background-radius: 10px; " +
                            "-fx-padding: 20; " +
                            "-fx-border-color: #E0E0E0; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 10px;"
            );
    
            Label sectionLabel = new Label(sectionTitle);
            sectionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
    
            section.getChildren().add(sectionLabel);
            section.getChildren().addAll(rows);
    
            return section;
        }
    
        private HBox createDetailRow(String label, String value) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
    
            Label labelNode = new Label(label);
            labelNode.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-min-width: 180px;");
    
            Label valueNode = new Label(value);
            valueNode.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");
            valueNode.setWrapText(true);
    
            row.getChildren().addAll(labelNode, valueNode);
            return row;
        }
    
        private void handleDeleteProduct(Product product) {
            boolean confirmed = showDeleteConfirmation(product.getBrandName());
    
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

        private void handleDeleteBatch(Batch batch, Product product, TableView<Batch> batchTable) {
    // Create confirmation dialog
    Stage confirmStage = new Stage();
    IconUtil.setApplicationIcon(confirmStage);
    confirmStage.initModality(Modality.APPLICATION_MODAL);
    confirmStage.setTitle("Delete Batch");
    confirmStage.setResizable(false);
    confirmStage.setUserData(false);

    VBox mainContainer = new VBox(20);
    mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
    mainContainer.setPrefWidth(500);

    // Title
    Label titleLabel = new Label("Delete Batch?");
    titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

    // Batch details
    VBox detailsBox = new VBox(8);
    detailsBox.setStyle(
            "-fx-background-color: #F8F9FA; " +
                    "-fx-padding: 15; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: #E0E0E0; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 8px;"
    );

    Label batchNumLabel = new Label("Batch Number: " + batch.getBatchNumber());
    batchNumLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");

    Label stockLabel = new Label("Stock: " + batch.getStock() + " units");
    stockLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

    Label expiryLabel = new Label("Expiry: " +
            (batch.getExpirationDate() != null ?
                    batch.getExpirationDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) :
                    "N/A"));
    expiryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

    Label supplierLabel = new Label("Supplier: " + batch.getSupplier());
    supplierLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

    detailsBox.getChildren().addAll(batchNumLabel, stockLabel, expiryLabel, supplierLabel);

    // Warning message
    Label messageLabel = new Label(
            "⚠ This action cannot be undone. The batch will be permanently removed from inventory, " +
                    "and the total stock count will be updated."
    );
    messageLabel.setWrapText(true);
    messageLabel.setStyle(
            "-fx-font-size: 14px; " +
                    "-fx-text-fill: #7f8c8d; " +
                    "-fx-line-spacing: 3px; " +
                    "-fx-padding: 10; " +
                    "-fx-background-color: #FFF3CD; " +
                    "-fx-background-radius: 6px;"
    );

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
        confirmStage.setUserData(false);
        confirmStage.close();
    });

    Button deleteButton = new Button("Delete Batch");
    deleteButton.setStyle(
            "-fx-background-color: #dc3545; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 12px 30px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-cursor: hand;"
    );

    deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
            deleteButton.getStyle().replace("#dc3545", "#c82333")
    ));
    deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
            deleteButton.getStyle().replace("#c82333", "#dc3545")
    ));

    deleteButton.setOnAction(e -> {
        confirmStage.setUserData(true);
        confirmStage.close();
    });

    buttonBox.getChildren().addAll(cancelButton, deleteButton);
    mainContainer.getChildren().addAll(titleLabel, detailsBox, messageLabel, buttonBox);

    Scene scene = new Scene(mainContainer);
    scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
    confirmStage.setScene(scene);
    confirmStage.centerOnScreen();
    confirmStage.showAndWait();

    // If confirmed, proceed with deletion
    if ((Boolean) confirmStage.getUserData()) {
        try {
            // ✅ Store product ID before deletion
            Long productId = product.getId();
            
            // Check if this is the last batch
            long batchCount = productService.countBatchesForProduct(product);

            if (batchCount <= 1) {
                // This is the last batch - warn user that product will be deleted too
                boolean deleteProduct = showLastBatchWarning(product.getName());

                if (deleteProduct) {
                    // Get the stage reference before deletion
                    Stage batchDialogStage = (Stage) batchTable.getScene().getWindow();
                    
                    // Delete the entire product (cascade will delete the batch)
                    productService.deleteProduct(productId);

                    // Close the batch dialog
                    batchDialogStage.close();
                    
                    // ✅ Use Platform.runLater to ensure UI updates after deletion completes
                    Platform.runLater(() -> {
                        loadProducts();
                        showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                                "Last batch deleted. Product removed from inventory.");
                    });
                } else {
                    return; // User cancelled
                }
            } else {
                // Delete the batch
                productService.deleteBatch(batch.getId());

                // ✅ Reload batches using product ID (not stale product object)
                List<Batch> updatedBatches = batchRepository.findByProductOrderByExpirationDate(productId);
                
                // ✅ Clear and reload table
                batchTable.getItems().clear();
                batchTable.setItems(FXCollections.observableArrayList(updatedBatches));
                batchTable.refresh();

                // ✅ Refresh main inventory table to show updated total stock
                Platform.runLater(() -> {
                    loadProducts();
                });

                showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                        "Batch deleted successfully. Total stock updated.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to delete batch: " + ex.getMessage());
        }
    }
}

        private boolean showLastBatchWarning(String productName) {
            Stage warningStage = new Stage();
            IconUtil.setApplicationIcon(warningStage);
            warningStage.initModality(Modality.APPLICATION_MODAL);
            warningStage.setTitle("Last Batch Warning");
            warningStage.setResizable(false);
            warningStage.setUserData(false);

            VBox mainContainer = new VBox(20);
            mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
            mainContainer.setPrefWidth(500);

            Label titleLabel = new Label("⚠ Last Batch - Product Will Be Deleted");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FF9800;");
            titleLabel.setWrapText(true);

            Label messageLabel = new Label(
                    "This is the last batch for \"" + productName + "\". " +
                            "Deleting this batch will also remove the entire product from inventory.\n\n" +
                            "Are you sure you want to proceed?"
            );
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
                warningStage.setUserData(false);
                warningStage.close();
            });

            Button confirmButton = new Button("Delete Product & Batch");
            confirmButton.setStyle(
                    "-fx-background-color: #FF9800; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 12px 30px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-cursor: hand;"
            );

            confirmButton.setOnMouseEntered(e -> confirmButton.setStyle(
                    confirmButton.getStyle().replace("#FF9800", "#f57c00")
            ));
            confirmButton.setOnMouseExited(e -> confirmButton.setStyle(
                    confirmButton.getStyle().replace("#f57c00", "#FF9800")
            ));

            confirmButton.setOnAction(e -> {
                warningStage.setUserData(true);
                warningStage.close();
            });

            buttonBox.getChildren().addAll(cancelButton, confirmButton);
            mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

            Scene scene = new Scene(mainContainer);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            warningStage.setScene(scene);
            warningStage.centerOnScreen();
            warningStage.showAndWait();

            return (Boolean) warningStage.getUserData();
        }

        //UPDATED FOR BATCH SUPPORT WITH ALL FIELDS
        private void showProductDialog(Product product) {
            Stage dialogStage = new Stage();
            IconUtil.setApplicationIcon(dialogStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(product == null ? "Add New Product" : "Edit Product");
            dialogStage.setResizable(false);

            // Main container with ScrollPane for long form
            VBox mainContainer = new VBox(10);
            mainContainer.setStyle("-fx-background-color: white; -fx-padding: 20;");
            mainContainer.setPrefWidth(700);

            // Header
            Label titleLabel = new Label(product == null ? "Add New Product" : "Edit Product");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label subtitleLabel = new Label(product == null ?
                    "Enter the details of the new medicine batch to add to inventory." :
                    "Update the product details or add a new batch.");
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
            subtitleLabel.setWrapText(true);

            VBox header = new VBox(8, titleLabel, subtitleLabel);

            // ScrollPane for form fields
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            scrollPane.setPrefHeight(450);

            // Form container
            VBox formContainer = new VBox(15);

            // ===== BASIC INFORMATION SECTION =====
            Label basicInfoLabel = new Label("Basic Information");
            basicInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            // Medicine ID (auto-generated, read-only)
            TextField medicineIdField = createStyledTextField(
                    product == null ? productService.generateNextMedicineId() : product.getMedicineId(),
                    "Auto-generated"
            );
            medicineIdField.setDisable(true);
            medicineIdField.setStyle(medicineIdField.getStyle() + "-fx-opacity: 1;");

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
            TextField categoryField = createStyledTextField(
                    product == null ? "" : (product.getCategory() != null ? product.getCategory() : ""),
                    "e.g., Pain Relief, Antibiotic, Vitamins"
            );

            // ===== MEDICAL INFORMATION SECTION =====
            Label medicalInfoLabel = new Label("Product Information");
            medicalInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 15 0 0 0;");

            // Dosage Form
            ComboBox<String> dosageFormCombo = new ComboBox<>();
            dosageFormCombo.getItems().addAll("Tablet", "Capsule", "Syrup", "Injection", "Cream", "Ointment", "Drops", "Inhaler", "Other");
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

            // Prescription Required
            CheckBox prescriptionCheckBox = new CheckBox("Prescription Required (Rx)");
            prescriptionCheckBox.setSelected(product != null && product.getPrescriptionRequired() != null && product.getPrescriptionRequired());
            prescriptionCheckBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");

            // ===== BATCH/STOCK INFORMATION SECTION =====
            Label batchInfoLabel = new Label(product == null ? "Initial Batch Information" : "New Batch Information");
            batchInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 15 0 0 0;");

            // Stock Quantity
            TextField stockField = createStyledTextField("0", "Quantity for this batch");

            // Unit of Measure
            ComboBox<String> unitCombo = new ComboBox<>();
            unitCombo.getItems().addAll("Piece", "Box", "Strip", "Bottle", "Vial", "Tube", "Pack");
            unitCombo.setValue(product == null ? "Piece" : (product.getUnitOfMeasure() != null ? product.getUnitOfMeasure() : "Piece"));
            unitCombo.setPromptText("Select unit");
            styleComboBox(unitCombo);

            // Minimum Stock Level (Reorder Point)
            TextField minStockField = createStyledTextField(
                    product == null ? "10" : String.valueOf(product.getMinStockLevel() != null ? product.getMinStockLevel() : 10),
                    "Reorder threshold"
            );

            // ===== PRICING & SUPPLIER SECTION =====
            Label pricingInfoLabel = new Label("Pricing & Supplier");
            pricingInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 15 0 0 0;");

            // Price
            TextField priceField = createStyledTextField(
                    product == null ? "0.00" : product.getPrice().toString(),
                    "Price per unit"
            );

            // Supplier
            TextField supplierField = createStyledTextField(
                    product == null ? "" : (product.getSupplier() != null ? product.getSupplier() : ""),
                    "Supplier name"
            );

            // Expiration Date
            DatePicker expirationPicker = new DatePicker(
                    product == null ? LocalDate.now().plusYears(1) : product.getExpirationDate()
            );
            expirationPicker.setPromptText("Select expiration date");
            expirationPicker.setStyle(
                    "-fx-background-color: #F8F9FA; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-border-color: #E0E0E0; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 8px; " +
                            "-fx-padding: 12px 15px; " +
                            "-fx-font-size: 14px;"
            );

            // Add all fields to form container
            formContainer.getChildren().addAll(
                    basicInfoLabel,
                    createFieldGroup("Product ID", medicineIdField),
                    createFieldGroup("Brand Name *", brandNameField),
                    createFieldGroup("Generic Name *", genericNameField),
                    createFieldGroup("Category", categoryField),

                    medicalInfoLabel,
                    createFieldGroup("Dosage Form *", dosageFormCombo),
                    createFieldGroup("Dosage Strength", dosageStrengthField),
                    createFieldGroup("Manufacturer", manufacturerField),
                    prescriptionCheckBox,

                    batchInfoLabel,
                    createFieldGroup("Stock Quantity *", stockField),
                    createFieldGroup("Unit of Measure *", unitCombo),
                    createFieldGroup("Reorder Level *", minStockField),

                    pricingInfoLabel,
                    createFieldGroup("Price (₱) *", priceField),
                    createFieldGroup("Supplier *", supplierField),
                    createFieldGroup("Expiration Date *", expirationPicker)
            );

            // Add "View Batches" button if editing existing product
            if (product != null) {
                Button viewBatchesBtn = new Button("📦 View All Batches");
                viewBatchesBtn.setStyle(
                        "-fx-background-color: #2196F3; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-padding: 10px 20px; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-cursor: hand;"
                );
                viewBatchesBtn.setOnAction(e -> showBatchesDialog(product));

                HBox batchButtonContainer = new HBox(viewBatchesBtn);
                batchButtonContainer.setAlignment(Pos.CENTER_LEFT);
                batchButtonContainer.setStyle("-fx-padding: 10 0 0 0;");
                formContainer.getChildren().add(batchButtonContainer);
            }

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

            Button saveButton = new Button(product == null ? "Add Product" : "Add New Batch");
            saveButton.setStyle(
                    "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 12px 30px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-cursor: hand;"
            );

            saveButton.setOnMouseEntered(e -> saveButton.setStyle(
                    saveButton.getStyle() + "-fx-background-color: #45a049;"
            ));
            saveButton.setOnMouseExited(e -> saveButton.setStyle(
                    saveButton.getStyle().replace("-fx-background-color: #45a049;", "-fx-background-color: #4CAF50;")
            ));

            saveButton.setOnAction(e -> {
                try {
                    // Will now Prevent adding expired medicine
                    if (expirationPicker.getValue() != null &&
                            expirationPicker.getValue().isBefore(java.time.LocalDate.now())) {
                        showStyledAlert(Alert.AlertType.ERROR, "Cannot Add Expired Medicine",
                                "The expiration date " + expirationPicker.getValue() +
                                        " has already passed. Please select a future date.");
                        return; // Don't proceed
                    }
                    // Validate all required fields
                    StringBuilder missingFields = new StringBuilder();

                    if (brandNameField.getText().trim().isEmpty()) {
                        missingFields.append("• Brand Name\n");
                    }
                    if (genericNameField.getText().trim().isEmpty()) {
                        missingFields.append("• Generic Name\n");
                    }
                    if (stockField.getText().trim().isEmpty() || stockField.getText().equals("0")) {
                        missingFields.append("• Stock Quantity (must be greater than 0)\n");
                    }
                    if (priceField.getText().trim().isEmpty() || priceField.getText().equals("0.00")) {
                        missingFields.append("• Price (must be greater than 0)\n");
                    }
                    if (expirationPicker.getValue() == null) {
                        missingFields.append("• Expiration Date\n");
                    }
                    if (supplierField.getText().trim().isEmpty()) {
                        missingFields.append("• Supplier\n");
                    }

                    if (missingFields.length() > 0) {
                        showStyledAlert(Alert.AlertType.ERROR, "Required Fields Missing",
                                "Please fill in the following required fields:\n\n" + missingFields.toString());
                        return;
                    }

                    // Validate numeric fields
                    int stock;
                    try {
                        stock = Integer.parseInt(stockField.getText());
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

                    BigDecimal price;
                    try {
                        price = new BigDecimal(priceField.getText());
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

                    int minStockLevel;
                    try {
                        minStockLevel = Integer.parseInt(minStockField.getText());
                    } catch (NumberFormatException ex) {
                        minStockLevel = 10; // Default
                    }

                    // Save using the batch-aware method
                    if (product == null) {
                        // New product with first batch
                        Product newProduct = new Product();
                        newProduct.setMedicineId(medicineIdField.getText());
                        newProduct.setBrandName(brandNameField.getText().trim());
                        newProduct.setGenericName(genericNameField.getText().trim());
                        newProduct.setPrice(price);
                        newProduct.setCategory(categoryField.getText().trim());
                        newProduct.setDosageForm(dosageFormCombo.getValue());
                        newProduct.setDosageStrength(dosageStrengthField.getText().trim());
                        newProduct.setManufacturer(manufacturerField.getText().trim());
                        newProduct.setPrescriptionRequired(prescriptionCheckBox.isSelected());
                        newProduct.setUnitOfMeasure(unitCombo.getValue());
                        newProduct.setMinStockLevel(minStockLevel);

                        productService.saveProductWithBatch(
                                newProduct,
                                stock,
                                expirationPicker.getValue(),
                                price,
                                supplierField.getText().trim()
                        );

                        showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                                "Product and first batch added successfully!");
                    } else {
                        // Add new batch to existing product
                        productService.saveProductWithBatch(
                                product,
                                stock,
                                expirationPicker.getValue(),
                                price,
                                supplierField.getText().trim()
                        );

                        showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                                "New batch added successfully to " + product.getBrandName() + "!");
                    }

                    loadProducts();
                    dialogStage.close();

                    // Force refresh of dashboard data if it exists
                    Platform.runLater(() -> {
                        productService.clearBatchCache();
                        updateKPIs(); // Refresh local KPIs
                    });

                } catch (Exception ex) {
                    showStyledAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to save: " + ex.getMessage());
                }
            });

            buttonContainer.getChildren().addAll(cancelButton, saveButton);

            mainContainer.getChildren().addAll(header, scrollPane, buttonContainer);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            Scene scene = new Scene(mainContainer);
            dialogStage.setScene(scene);
            dialogStage.centerOnScreen();
            dialogStage.showAndWait();
        }

        //BATCH VIEW DIALOG (FOR DUPLICATION)
        private void showBatchesDialog(Product product) {
            Stage dialogStage = new Stage();
            IconUtil.setApplicationIcon(dialogStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Batches for " + product.getName());
            dialogStage.setResizable(true);

            VBox mainContainer = new VBox(20);
            mainContainer.getStyleClass().add("table-card");
            mainContainer.setStyle("-fx-padding: 25;");
            mainContainer.setPrefWidth(1000);
            mainContainer.setPrefHeight(650);

            // Header
            Label titleLabel = new Label(product.getName() + " - Batch History");
            titleLabel.getStyleClass().add("table-title");

            // Summary info
            long batchCount = productService.countBatchesForProduct(product);
            Label summaryLabel = new Label(String.format(
                    "Total Stock: %d units | Product ID: %s | Total Batches: %d",
                    product.getStock(),
                    product.getMedicineId(),
                    batchCount
            ));
            summaryLabel.getStyleClass().add("table-subtitle");

            VBox header = new VBox(8, titleLabel, summaryLabel);

            // Batches Table with CSS styling
            TableView<Batch> batchTable = new TableView<>();
            batchTable.getStyleClass().add("inventory-table");

            // Batch Number Column
            TableColumn<Batch, String> batchNumCol = new TableColumn<>("Batch Number");
            batchNumCol.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getBatchNumber()));
            batchNumCol.setPrefWidth(220);

            // Stock Column
            TableColumn<Batch, String> stockCol = new TableColumn<>("Stock");
            stockCol.setCellValueFactory(data ->
                    new SimpleStringProperty(String.valueOf(data.getValue().getStock())));
            stockCol.setPrefWidth(80);
            stockCol.setStyle("-fx-alignment: CENTER;");

            // Price Column
            TableColumn<Batch, String> priceCol = new TableColumn<>("Price");
            priceCol.setCellValueFactory(data ->
                    new SimpleStringProperty("₱" + data.getValue().getPrice().toString()));
            priceCol.setPrefWidth(100);

            // Expiration Date Column
            TableColumn<Batch, String> expiryCol = new TableColumn<>("Expiry Date");
            expiryCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        Batch batch = getTableRow().getItem();
                        if (batch.getExpirationDate() != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. dd, yyyy");
                            setText(batch.getExpirationDate().format(formatter));
                            if (batch.isExpiringSoon()) {
                                setStyle("-fx-text-fill: #FF6B35; -fx-font-weight: bold;");
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
            expiryCol.setCellValueFactory(data -> {
                if (data.getValue().getExpirationDate() != null) {
                    return new SimpleStringProperty(
                            data.getValue().getExpirationDate().format(DateTimeFormatter.ofPattern("M/d/yyyy"))
                    );
                }
                return new SimpleStringProperty("N/A");
            });
            expiryCol.setPrefWidth(130);

            // Supplier Column
            TableColumn<Batch, String> supplierCol = new TableColumn<>("Supplier");
            supplierCol.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getSupplier()));
            supplierCol.setPrefWidth(120);

            // Date Received Column
            TableColumn<Batch, String> receivedCol = new TableColumn<>("Date Received");
            receivedCol.setCellValueFactory(data -> {
                if (data.getValue().getDateReceived() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. dd, yyyy");
                    return new SimpleStringProperty(
                            data.getValue().getDateReceived().format(formatter)
                    );
                }
                return new SimpleStringProperty("N/A");
            });
            receivedCol.setPrefWidth(130);

            // Status Column with badges matching inventory table
            TableColumn<Batch, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Batch batch = getTableRow().getItem();

                        String status = "Active";
                        String bgColor = "#1a1a1a"; // Changed to match inventory table "Good" status

                        if (batch.getStock() <= 0) {
                            status = "Depleted";
                            bgColor = "#9E9E9E";
                        } else if (batch.getExpirationDate() != null &&
                                batch.getExpirationDate().isBefore(LocalDate.now())) {
                            status = "Expired";
                            bgColor = "#dc3545"; // Changed to match inventory table "Low" status
                        } else if (batch.isExpiringSoon()) {
                            status = "Expiring";
                            bgColor = "#FF9800";
                        }

                        Label badge = new Label(status);
                        badge.setStyle(
                                "-fx-background-color: " + bgColor + "; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-padding: 4px 12px; " +
                                        "-fx-background-radius: 12px; " +
                                        "-fx-font-size: 12px; " +
                                        "-fx-font-weight: bold;"
                        );

                        HBox container = new HBox(badge);
                        container.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(container);
                        setText(null);
                    }
                }
            });
            statusCol.setPrefWidth(110);

            // Actions Column with Delete Button
            TableColumn<Batch, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setCellFactory(column -> new TableCell<>() {
                private final Button deleteBtn = new Button("🗑");

                {
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

                    deleteBtn.setOnAction(event -> {
                        Batch batch = getTableRow().getItem();
                        if (batch != null) {
                            handleDeleteBatch(batch, product, batchTable);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox container = new HBox(deleteBtn);
                        container.setAlignment(Pos.CENTER);
                        setGraphic(container);
                    }
                }
            });
            actionsCol.setPrefWidth(100);

            batchTable.getColumns().addAll(
                    batchNumCol, stockCol, priceCol, expiryCol,
                    supplierCol, receivedCol, statusCol, actionsCol
            );

            // Load batches
            List<Batch> batches = productService.getBatchesForProduct(product);
            ObservableList<Batch> batchData = FXCollections.observableArrayList(batches);
            batchTable.setItems(batchData);

            // Make table grow to fill space
            VBox.setVgrow(batchTable, Priority.ALWAYS);

            // Info message with CSS styling
            Label infoLabel = new Label(
                    "ℹBatches are displayed in order of expiration date (earliest first) for FEFO tracking."
            );
            infoLabel.setStyle(
                    "-fx-background-color: #E3F2FD; " +
                            "-fx-padding: 12px 15px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-text-fill: #1976D2; " +
                            "-fx-font-size: 13px;"
            );
            infoLabel.setWrapText(true);

            // Close button
            HBox buttonContainer = new HBox();
            buttonContainer.setAlignment(Pos.CENTER_RIGHT);

            Button closeButton = new Button("Close");
            closeButton.setStyle(
                    "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 12px 30px; " +
                            "-fx-background-radius: 8px; " +
                            "-fx-cursor: hand;"
            );

            closeButton.setOnMouseEntered(e -> closeButton.setStyle(
                    closeButton.getStyle().replace("#4CAF50", "#45a049")
            ));
            closeButton.setOnMouseExited(e -> closeButton.setStyle(
                    closeButton.getStyle().replace("#45a049", "#4CAF50")
            ));

            closeButton.setOnAction(e -> dialogStage.close());

            buttonContainer.getChildren().add(closeButton);

            mainContainer.getChildren().addAll(header, infoLabel, batchTable, buttonContainer);

            // Create scene and load CSS
            Scene scene = new Scene(mainContainer);

            // Load the inventory.css stylesheet
            try {
                scene.getStylesheets().add(
                        Objects.requireNonNull(getClass().getResource("/css/inventory.css")).toExternalForm()
                );
            } catch (Exception ex) {
                System.err.println("Warning: Could not load inventory.css for batch dialog: " + ex.getMessage());
            }

            dialogStage.setScene(scene);
            dialogStage.centerOnScreen();
            dialogStage.show();
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
    
        private boolean showDeleteConfirmation(String productName) {
            // Create custom dialog
            Stage dialogStage = new Stage();
            IconUtil.setApplicationIcon(dialogStage);
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
    
        @FXML
        private void handleDashboard() {
            setActiveButton(dashboardBtn);
            navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
        }
    
        @FXML
        private void handleInventory() {
            setActiveButton(inventoryBtn);
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