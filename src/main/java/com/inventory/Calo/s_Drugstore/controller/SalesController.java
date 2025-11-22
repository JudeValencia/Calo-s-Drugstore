package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Product;
import com.inventory.Calo.s_Drugstore.entity.Sale;
import com.inventory.Calo.s_Drugstore.entity.SaleItem;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.ProductService;
import com.inventory.Calo.s_Drugstore.service.SalesService;
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
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class SalesController implements Initializable {

    @Autowired
    private SalesService salesService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;

    // FXML Components - Add to Cart Section
    @FXML private TextField medicineSearchField;
    @FXML private ComboBox<Product> medicineCombo;
    @FXML private TextField quantityField;

    // FXML Components - Cart Section
    @FXML private VBox cartSection;
    @FXML private Label cartCountLabel;
    @FXML private VBox emptyCartState;
    @FXML private VBox cartItemsContainer;
    @FXML private HBox cartTotalSection;
    @FXML private Label cartTotalLabel;
    @FXML private Button completeTransactionBtn;
    @FXML private Button clearCartBtn;

    // FXML Components - Summary Section
    @FXML private Label totalTransactionsLabel;
    @FXML private Label totalItemsSoldLabel;
    @FXML private Label totalSalesLabel;

    // FXML Components - Transactions Table
    @FXML private TableView<Sale> transactionsTable;
    @FXML private TableColumn<Sale, String> transactionIdColumn;
    @FXML private TableColumn<Sale, String> itemsColumn;
    @FXML private TableColumn<Sale, String> totalAmountColumn;
    @FXML private TableColumn<Sale, String> dateTimeColumn;
    @FXML private TableColumn<Sale, Void> actionsColumn;

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
    @FXML private Label userRoleLabel;

    // Cart data
    private ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private ObservableList<Product> allProducts = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== SALES CONTROLLER INITIALIZE ===");

        // Force CSS reload after scene is ready
        setupMedicineCombo();
        setupSearchField();
        setupTransactionsTable();
        loadProducts();
        loadTodaysSummary();
        loadTodaysTransactions();
        setActiveButton(salesBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            // Set username (both admin and staff views have this)
            if (userNameLabel != null) {
                userNameLabel.setText(user.getFullName());
            }

            // Set user email (only admin view has this)
            if (userEmailLabel != null) {
                userEmailLabel.setText(user.getEmail());
            }

            // Set user role (only staff view has this)
            if (userRoleLabel != null) {
                userRoleLabel.setText(user.getRole());
            }
        }
    }
    private void setupMedicineCombo() {
        medicineCombo.setCellFactory(lv -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " - ₱" + item.getPrice());
            }
        });

        medicineCombo.setButtonCell(new ListCell<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void setupSearchField() {
        // Real-time filtering
        medicineSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                medicineCombo.setItems(allProducts);
                medicineCombo.hide();
            } else {
                String searchText = newVal.toLowerCase();
                ObservableList<Product> filtered = allProducts.filtered(product ->
                        product.getName().toLowerCase().contains(searchText) ||
                                product.getMedicineId().toLowerCase().contains(searchText) ||
                                product.getCategory().toLowerCase().contains(searchText) ||
                                product.getSupplier().toLowerCase().contains(searchText)
                );

                medicineCombo.setItems(filtered);

                // Show dropdown when there are results
                if (!filtered.isEmpty()) {
                    if (!medicineCombo.isShowing()) {
                        medicineCombo.show();
                    }
                } else {
                    medicineCombo.hide();
                }
            }
        });

        // Show all products when search field is focused
        medicineSearchField.setOnMouseClicked(event -> {
            if (medicineSearchField.getText().trim().isEmpty()) {
                medicineCombo.setItems(allProducts);
                if (!allProducts.isEmpty()) {
                    medicineCombo.show();
                }
            }
        });

        // Focus management
        medicineCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                quantityField.requestFocus();
                quantityField.selectAll();
            }
        });

        // Enter key support
        medicineSearchField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                // If combo has selection, move to quantity
                if (medicineCombo.getValue() != null) {
                    quantityField.requestFocus();
                    quantityField.selectAll();
                }
            }
        });

        quantityField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                handleAddToCart();
            }
        });
    }

    private void setupTransactionsTable() {
        transactionIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTransactionId()));

        // Keep your nice badge display
        itemsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Sale sale = getTableRow().getItem();
                    HBox container = new HBox(5);
                    container.setAlignment(Pos.CENTER_LEFT);

                    for (SaleItem saleItem : sale.getItems()) {
                        Label badge = new Label(saleItem.getMedicineName() + " " + saleItem.getQuantity() + "x");
                        badge.setStyle(
                                "-fx-background-color: #E3F2FD; " +
                                        "-fx-text-fill: #1976D2; " +
                                        "-fx-padding: 4px 8px; " +
                                        "-fx-background-radius: 12px; " +
                                        "-fx-font-size: 12px;"
                        );
                        container.getChildren().add(badge);
                    }

                    setGraphic(container);
                }
            }
        });
        itemsColumn.setCellValueFactory(data -> new SimpleStringProperty(""));

        totalAmountColumn.setCellValueFactory(data ->
                new SimpleStringProperty("₱" + String.format("%.2f", data.getValue().getTotalAmount())));

        dateTimeColumn.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy, h:mm a");
            return new SimpleStringProperty(data.getValue().getSaleDate().format(formatter));
        });

        // Updated actions with Details and Print buttons
        actionsColumn.setCellFactory(column -> new TableCell<Sale, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(10);
                    buttons.setAlignment(Pos.CENTER);

                    // Details button
                    Button detailsBtn = new Button("👁");
                    detailsBtn.setStyle(
                            "-fx-background-color: #2196F3; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-border-radius: 6px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-min-width: 36px; " +
                                    "-fx-min-height: 36px; " +
                                    "-fx-font-size: 14px;"
                    );
                    detailsBtn.setOnAction(e -> handleViewDetails(getTableRow().getItem()));

                    detailsBtn.setOnMouseEntered(e -> detailsBtn.setStyle(
                            detailsBtn.getStyle().replace("#2196F3", "#1976D2")
                    ));
                    detailsBtn.setOnMouseExited(e -> detailsBtn.setStyle(
                            detailsBtn.getStyle().replace("#1976D2", "#2196F3")
                    ));

                    // Print button
                    Button printBtn = new Button("🖨");
                    printBtn.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-border-color: #E0E0E0; " +
                                    "-fx-border-width: 1px; " +
                                    "-fx-border-radius: 6px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-min-width: 36px; " +
                                    "-fx-min-height: 36px; " +
                                    "-fx-font-size: 14px;"
                    );
                    printBtn.setOnAction(e -> handlePrint(getTableRow().getItem()));

                    printBtn.setOnMouseEntered(e -> printBtn.setStyle(
                            printBtn.getStyle().replace("white", "#F5F5F5")
                    ));
                    printBtn.setOnMouseExited(e -> printBtn.setStyle(
                            printBtn.getStyle().replace("#F5F5F5", "white")
                    ));

                    buttons.getChildren().addAll(detailsBtn, printBtn);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void loadProducts() {
        List<Product> products = productService.getAllProducts();
        allProducts.setAll(products);
        medicineCombo.setItems(allProducts);
    }

    @FXML
    private void handleAddToCart() {
        Product selectedProduct = medicineCombo.getValue();
        if (selectedProduct == null) {
            showStyledAlert(Alert.AlertType.WARNING, "No Medicine Selected", "Please select a medicine to add to cart.");
            return;
        }

        String qtyText = quantityField.getText().trim();
        if (qtyText.isEmpty()) {
            showStyledAlert(Alert.AlertType.WARNING, "Invalid Quantity", "Please enter a quantity.");
            return;
        }

        try {
            int quantity = Integer.parseInt(qtyText);
            if (quantity <= 0) {
                showStyledAlert(Alert.AlertType.WARNING, "Invalid Quantity", "Quantity must be greater than 0.");
                return;
            }

            if (quantity > selectedProduct.getStock()) {
                showStyledAlert(Alert.AlertType.WARNING, "Insufficient Stock",
                        "Only " + selectedProduct.getStock() + " units available in stock.");
                return;
            }

            // Check if item already in cart
            Optional<SaleItem> existingItem = cartItems.stream()
                    .filter(item -> item.getProduct().getId().equals(selectedProduct.getId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                SaleItem item = existingItem.get();
                int newQty = item.getQuantity() + quantity;
                if (newQty > selectedProduct.getStock()) {
                    showStyledAlert(Alert.AlertType.WARNING, "Insufficient Stock",
                            "Only " + selectedProduct.getStock() + " units available in stock.");
                    return;
                }
                item.setQuantity(newQty);
            } else {
                SaleItem newItem = new SaleItem(selectedProduct, quantity);
                cartItems.add(newItem);
            }

            updateCartUI();
            medicineCombo.setValue(null);
            quantityField.setText("1");

        } catch (NumberFormatException e) {
            showStyledAlert(Alert.AlertType.WARNING, "Invalid Quantity", "Please enter a valid number.");
        }
    }

    @FXML
    private void handleBulkAdd() {
        // Create bulk add dialog
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Bulk Add Medicines");
        dialogStage.setResizable(true);
        dialogStage.setWidth(900);
        dialogStage.setHeight(700);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");

        Label titleLabel = new Label("Add Multiple Medicines");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitle = new Label("Search and select medicines to add to cart");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        // ==================== SEARCH SECTION ====================
        VBox searchSection = new VBox(12);
        searchSection.setStyle("-fx-background-color: #F8F9FA; -fx-padding: 20; -fx-background-radius: 10px;");

        Label searchLabel = new Label("🔍 Search and Add Medicine");
        searchLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox searchRow = new HBox(15);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        // Search TextField
        TextField searchField = new TextField();
        searchField.setPromptText("Type medicine name, ID, category, or supplier...");
        searchField.setPrefWidth(400);
        searchField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #2196F3; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-font-size: 15px; " +
                        "-fx-padding: 12px 15px;"
        );

        // ComboBox for filtered results
        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setPromptText("Select medicine");
        productCombo.setPrefWidth(350);
        productCombo.setItems(allProducts);
        productCombo.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10px 12px;"
        );

        // Custom cell factory for combo
        productCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - ₱" + String.format("%.2f", item.getPrice()) +
                            " (Stock: " + item.getStock() + ")");
                }
            }
        });

        productCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        searchRow.getChildren().addAll(searchField, productCombo);

        // Add controls row
        HBox addControlsRow = new HBox(15);
        addControlsRow.setAlignment(Pos.CENTER_LEFT);

        TextField qtyField = new TextField("1");
        qtyField.setPrefWidth(100);
        qtyField.setPromptText("Qty");
        qtyField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 10px;"
        );

        Button addToListBtn = new Button("➕ Add to List");
        addToListBtn.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10px 20px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );

        addControlsRow.getChildren().addAll(qtyField, addToListBtn);

        searchSection.getChildren().addAll(searchLabel, searchRow, addControlsRow);

        // ==================== SELECTED ITEMS TABLE ====================
        Label selectedLabel = new Label("Selected Medicines (0)");
        selectedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<BulkAddItem> bulkTable = new TableView<>();
        bulkTable.getStyleClass().add("bulk-add-table");
        bulkTable.setPlaceholder(new Label("No medicines added yet. Search and add items above."));

        bulkTable.setFixedCellSize(50);
        bulkTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<BulkAddItem, String> medicineCol = new TableColumn<>("Medicine");
        medicineCol.setMinWidth(220);
        medicineCol.setMaxWidth(220);
        medicineCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct() != null ?
                        data.getValue().getProduct().getName() : ""));

        TableColumn<BulkAddItem, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setMinWidth(150);
        quantityCol.setMaxWidth(150);
        quantityCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        quantityCol.setCellFactory(column -> new TableCell<BulkAddItem, Integer>() {
            private HBox controls;
            private Button minusBtn;
            private Label qtyLabel;
            private Button plusBtn;

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                BulkAddItem bulkItem = getTableRow() != null ? getTableRow().getItem() : null;

                if (empty || bulkItem == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (controls == null) {
                        controls = new HBox(6);  // Reduced spacing
                        controls.setAlignment(Pos.CENTER);

                        minusBtn = new Button("−");
                        qtyLabel = new Label();
                        plusBtn = new Button("+");

                        // Apply CSS classes
                        minusBtn.getStyleClass().add("qty-btn");
                        plusBtn.getStyleClass().add("qty-btn");
                        qtyLabel.getStyleClass().add("qty-label");

                        // Override sizes inline (smaller)
                        String compactBtnStyle =
                                "-fx-min-width: 28px; " +
                                        "-fx-min-height: 28px; " +
                                        "-fx-max-width: 28px; " +
                                        "-fx-max-height: 28px; " +
                                        "-fx-font-size: 14px; " +
                                        "-fx-padding: 0;";

                        minusBtn.setStyle(compactBtnStyle);
                        plusBtn.setStyle(compactBtnStyle);

                        qtyLabel.setStyle(
                                "-fx-font-size: 14px; " +
                                        "-fx-min-width: 35px; " +
                                        "-fx-alignment: CENTER;"
                        );

                        controls.getChildren().addAll(minusBtn, qtyLabel, plusBtn);
                    }

                    qtyLabel.setText(String.valueOf(bulkItem.getQuantity()));

                    minusBtn.setOnAction(e -> {
                        if (bulkItem.getQuantity() > 1) {
                            bulkItem.setQuantity(bulkItem.getQuantity() - 1);
                            qtyLabel.setText(String.valueOf(bulkItem.getQuantity()));
                            getTableView().refresh();
                        }
                    });

                    plusBtn.setOnAction(e -> {
                        Product product = bulkItem.getProduct();
                        if (product != null && bulkItem.getQuantity() < product.getStock()) {
                            bulkItem.setQuantity(bulkItem.getQuantity() + 1);
                            qtyLabel.setText(String.valueOf(bulkItem.getQuantity()));
                            getTableView().refresh();
                        }
                    });

                    setGraphic(controls);
                }
            }
        });
        TableColumn<BulkAddItem, String> stockCol = new TableColumn<>("Available");
        stockCol.setMinWidth(100);
        stockCol.setMaxWidth(100);
        stockCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct() != null ?
                        String.valueOf(data.getValue().getProduct().getStock()) : ""));

        TableColumn<BulkAddItem, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.setMinWidth(120);
        priceCol.setMaxWidth(120);
        priceCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct() != null ?
                        "₱" + String.format("%.2f", data.getValue().getProduct().getPrice()) : ""));

        TableColumn<BulkAddItem, String> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setMinWidth(120);
        subtotalCol.setMaxWidth(120);
        subtotalCol.setCellValueFactory(data -> {
            BulkAddItem item = data.getValue();
            if (item.getProduct() != null) {
                BigDecimal subtotal = item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                return new SimpleStringProperty("₱" + String.format("%.2f", subtotal));
            }
            return new SimpleStringProperty("");
        });

        TableColumn<BulkAddItem, Void> actionCol = new TableColumn<>("Action");
        actionCol.setMinWidth(140);
        actionCol.setMaxWidth(140);

        actionCol.setCellFactory(column -> new TableCell<>() {
            private final Button removeBtn = new Button("✕ Remove");
            {
                removeBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #F44336; " +
                                "-fx-font-size: 13px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 8px 16px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #FFCDD2; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                );

// Add hover effect right after the removeBtn.setStyle()
                removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(
                        "-fx-background-color: #FFEBEE; " +
                                "-fx-text-fill: #F44336; " +
                                "-fx-font-size: 13px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 8px 16px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #F44336; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                ));
                removeBtn.setOnMouseExited(e -> removeBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #F44336; " +
                                "-fx-font-size: 13px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 8px 16px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #FFCDD2; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                ));
                removeBtn.setOnAction(e -> {
                    BulkAddItem item = getTableRow().getItem();
                    if (item != null) {
                        bulkTable.getItems().remove(item);
                        selectedLabel.setText("Selected Medicines (" + bulkTable.getItems().size() + ")");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        bulkTable.getColumns().addAll(medicineCol, quantityCol, stockCol, priceCol, subtotalCol, actionCol);
        ObservableList<BulkAddItem> bulkItems = FXCollections.observableArrayList();
        bulkTable.setItems(bulkItems);

        // ==================== SEARCH FUNCTIONALITY ====================
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                productCombo.setItems(allProducts);
                productCombo.hide();
            } else {
                String searchText = newVal.toLowerCase();
                ObservableList<Product> filtered = allProducts.filtered(product ->
                        product.getName().toLowerCase().contains(searchText) ||
                                product.getMedicineId().toLowerCase().contains(searchText) ||
                                product.getCategory().toLowerCase().contains(searchText) ||
                                product.getSupplier().toLowerCase().contains(searchText)
                );

                productCombo.setItems(filtered);

                // Show dropdown when there are results
                if (!filtered.isEmpty()) {
                    if (!productCombo.isShowing()) {
                        productCombo.show();
                    }
                } else {
                    productCombo.hide();
                }
            }
        });

        // Show all when search field clicked
        searchField.setOnMouseClicked(event -> {
            if (searchField.getText().trim().isEmpty()) {
                productCombo.setItems(allProducts);
                if (!allProducts.isEmpty()) {
                    productCombo.show();
                }
            }
        });

        // Focus quantity field when medicine selected
        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                qtyField.requestFocus();
                qtyField.selectAll();
            }
        });

        // Enter key support
        searchField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                if (productCombo.getValue() != null) {
                    qtyField.requestFocus();
                    qtyField.selectAll();
                }
            }
        });

        qtyField.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                addToListBtn.fire();
            }
        });

        // ==================== ADD TO LIST FUNCTIONALITY ====================
        addToListBtn.setOnAction(e -> {
            Product selected = productCombo.getValue();
            if (selected == null) {
                showStyledAlert(Alert.AlertType.WARNING, "No Medicine Selected",
                        "Please select a medicine from the dropdown.");
                return;
            }

            try {
                int qty = Integer.parseInt(qtyField.getText());
                if (qty <= 0) {
                    showStyledAlert(Alert.AlertType.WARNING, "Invalid Quantity",
                            "Quantity must be greater than 0.");
                    return;
                }

                if (qty > selected.getStock()) {
                    showStyledAlert(Alert.AlertType.WARNING, "Insufficient Stock",
                            "Only " + selected.getStock() + " units available.");
                    return;
                }

                // Check if already in list
                boolean alreadyAdded = bulkItems.stream()
                        .anyMatch(item -> item.getProduct().getId().equals(selected.getId()));

                if (alreadyAdded) {
                    showStyledAlert(Alert.AlertType.WARNING, "Already Added",
                            selected.getName() + " is already in your list.");
                    return;
                }

                // Add to list
                bulkItems.add(new BulkAddItem(selected, qty));
                selectedLabel.setText("Selected Medicines (" + bulkItems.size() + ")");

                // Reset fields
                productCombo.setValue(null);
                searchField.clear();
                qtyField.setText("1");
                searchField.requestFocus();

            } catch (NumberFormatException ex) {
                showStyledAlert(Alert.AlertType.WARNING, "Invalid Quantity",
                        "Please enter a valid number.");
            }
        });

        // Bottom buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
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
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button addAllBtn = new Button("Add All to Cart");
        addAllBtn.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12px 30px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;"
        );

        addAllBtn.setOnAction(e -> {
            if (bulkItems.isEmpty()) {
                showStyledAlert(Alert.AlertType.WARNING, "No Items", "Please add at least one medicine.");
                return;
            }

            int addedCount = 0;
            for (BulkAddItem item : bulkItems) {
                Optional<SaleItem> existingItem = cartItems.stream()
                        .filter(ci -> ci.getProduct().getId().equals(item.getProduct().getId()))
                        .findFirst();

                if (existingItem.isPresent()) {
                    SaleItem saleItem = existingItem.get();
                    int newQty = saleItem.getQuantity() + item.getQuantity();
                    if (newQty <= item.getProduct().getStock()) {
                        saleItem.setQuantity(newQty);
                        addedCount++;
                    }
                } else {
                    cartItems.add(new SaleItem(item.getProduct(), item.getQuantity()));
                    addedCount++;
                }
            }

            updateCartUI();
            dialogStage.close();
            showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                    addedCount + " medicine(s) added to cart!");
        });

        buttonBox.getChildren().addAll(cancelBtn, addAllBtn);

        mainContainer.getChildren().addAll(titleLabel, subtitle, searchSection, selectedLabel, bulkTable, buttonBox);
        VBox.setVgrow(bulkTable, Priority.ALWAYS);

        Scene scene = new Scene(mainContainer);

        // Load the CSS file for the dialog
        try {
            String css = getClass().getResource("/css/sales.css").toExternalForm();
            scene.getStylesheets().add(css);
            System.out.println("✅ Loaded CSS for Bulk Add dialog: " + css);
        } catch (Exception ex) {
            System.err.println("❌ Could not load CSS for Bulk Add dialog: " + ex.getMessage());
        }

        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();

        // Focus search field on open
        Platform.runLater(() -> searchField.requestFocus());

        dialogStage.show();
    }

    @FXML
    private void handleClearCart() {
        if (showStyledConfirmation("Clear Cart", "Are you sure you want to clear the cart?")) {
            cartItems.clear();
            updateCartUI();
        }
    }

    @FXML
    private void handleCompleteTransaction() {
        if (cartItems.isEmpty()) {
            showStyledAlert(Alert.AlertType.WARNING, "Empty Cart", "Please add items to cart before completing transaction.");
            return;
        }

        try {
            Sale completedSale = salesService.completeSale(new ArrayList<>(cartItems), currentUser);

            showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                    "Transaction " + completedSale.getTransactionId() + " completed successfully!\n" +
                            "Total: ₱" + String.format("%.2f", completedSale.getTotalAmount()));

            cartItems.clear();
            updateCartUI();
            loadTodaysSummary();
            loadTodaysTransactions();

        } catch (Exception e) {
            showStyledAlert(Alert.AlertType.ERROR, "Transaction Failed", e.getMessage());
        }
    }

    private void loadTodaysSummary() {
        Map<String, Object> summary = salesService.getTodaysSummary();
        totalTransactionsLabel.setText(String.valueOf(summary.get("totalTransactions")));
        totalItemsSoldLabel.setText(String.valueOf(summary.get("totalItemsSold")));
        totalSalesLabel.setText("₱" + String.format("%.2f", (BigDecimal) summary.get("totalSales")));
    }

    private void loadTodaysTransactions() {
        List<Sale> sales = salesService.getTodaysTransactions();
        transactionsTable.setItems(FXCollections.observableArrayList(sales));

        if (sales.isEmpty()) {
            transactionsTable.setPlaceholder(new Label("No transactions recorded today"));
        }
    }

    private void handlePrint(Sale sale) {
        showStyledAlert(Alert.AlertType.INFORMATION, "Print", "Print receipt for " + sale.getTransactionId());
    }

    private void handleDownload(Sale sale) {
        showStyledAlert(Alert.AlertType.INFORMATION, "Download", "Download receipt for " + sale.getTransactionId());
    }

//    // Navigation methods
//    private void setActiveButton(Button activeBtn) {
//        // Remove active class from all buttons
//        dashboardBtn.getStyleClass().remove("active");
//        inventoryBtn.getStyleClass().remove("active");
//        salesBtn.getStyleClass().remove("active");
//        reportsBtn.getStyleClass().remove("active");
//
//        // Only remove active from staffBtn if it exists (admin view has it, staff view doesn't)
//        if (staffBtn != null) {
//            staffBtn.getStyleClass().remove("active");
//        }
//
//        // Add active class to the clicked button
//        if (activeBtn != null) {
//            activeBtn.getStyleClass().add("active");
//        }
//    }

    // Replace the setActiveButton method in all controllers with this version:

    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");

        // Only remove active from staffBtn if it exists (admin view has it, staff view doesn't)
        if (staffBtn != null) {
            staffBtn.getStyleClass().remove("active");
        }

        // Add active class to the clicked button
        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }

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

        // Check if staff user
        if (currentUser != null && !currentUser.getRole().equalsIgnoreCase("ADMIN")) {
            System.out.println("Staff user detected - navigating to staff-dashboard");
            navigateToPage("/fxml/staff-dashboard.fxml", "/css/dashboard.css");
        } else {
            System.out.println("Admin user detected - navigating to admin dashboard");
            navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
        }
    }

    @FXML
    private void handleInventory() {
        setActiveButton(inventoryBtn);

        // Check if staff user
        if (currentUser != null && !currentUser.getRole().equalsIgnoreCase("ADMIN")) {
            System.out.println("Staff user detected - navigating to staff-inventory");
            navigateToPage("/fxml/staff-inventory.fxml", "/css/dashboard.css");
        } else {
            System.out.println("Admin user detected - navigating to admin inventory");
            navigateToPage("/fxml/inventory.fxml", "/css/inventory.css");
        }
    }

    @FXML
    private void handleSales() {
        setActiveButton(salesBtn);
        // Already on sales page
    }


    @FXML
    private void handleReports() {
        setActiveButton(reportsBtn);

        // Check if staff user
        if (currentUser != null && !currentUser.getRole().equalsIgnoreCase("ADMIN")) {
            System.out.println("Staff user detected - navigating to staff-reports");
            navigateToPage("/fxml/staff-reports.fxml", "/css/staff-reports.css");
        } else {
            System.out.println("Admin user detected - navigating to admin reports");
            navigateToPage("/fxml/reports.fxml", "/css/reports.css");
        }
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

    private void navigateToPage(String fxmlPath, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            // Pass current user to the next controller
            // Pass current user to the next controller
            if (fxmlPath.contains("inventory")) {
                if (fxmlPath.contains("staff-inventory")) {
                    // Staff inventory controller
                    StaffInventoryController controller = loader.getController();
                    controller.setCurrentUser(currentUser);
                    System.out.println("✅ Set user for StaffInventoryController");
                } else {
                    // Admin inventory controller
                    InventoryController controller = loader.getController();
                    controller.setCurrentUser(currentUser);
                    System.out.println("✅ Set user for InventoryController");
                }
            } else if (fxmlPath.contains("dashboard")) {
                if (fxmlPath.contains("staff-dashboard")) {
                    // Staff dashboard controller
                    StaffDashboardController controller = loader.getController();
                    controller.setCurrentUser(currentUser);
                    System.out.println("✅ Set user for StaffDashboardController");
                } else {
                    // Admin dashboard controller
                    DashboardController controller = loader.getController();
                    controller.setCurrentUser(currentUser);
                    controller.refreshDashboard();
                    System.out.println("✅ Set user for DashboardController");
                }
            } else if (fxmlPath.contains("sales")) {
                SalesController controller = loader.getController();
                controller.setCurrentUser(currentUser);
                System.out.println("✅ Set user for SalesController");
            } else if (fxmlPath.contains("reports")) {
                if (fxmlPath.contains("staff-reports")) {
                    StaffReportsController controller = loader.getController();
                    controller.setCurrentUser(currentUser);
                    System.out.println("✅ Set user for StaffReportsController");
                } else {
                    ReportsController controller = loader.getController();
                    controller.setCurrentUser(currentUser);
                    System.out.println("✅ Set user for ReportsController");
                }
            }


            Stage stage = (Stage) salesBtn.getScene().getWindow();  // Change salesBtn to appropriate button
            Scene currentScene = stage.getScene();

            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            double currentX = stage.getX();
            double currentY = stage.getY();
            boolean isMaximized = stage.isMaximized();

            // CREATE NEW SCENE AND FORCE CSS LOAD
            Scene newScene = new Scene(root);

            try {
                java.net.URL cssUrl = getClass().getResource(cssPath);
                if (cssUrl != null) {
                    String cssString = cssUrl.toExternalForm();
                    newScene.getStylesheets().add(cssString);
                    System.out.println("✅ CSS LOADED: " + cssString);
                } else {
                    System.out.println("❌ CSS NOT FOUND: " + cssPath);
                }
            } catch (Exception cssEx) {
                System.err.println("❌ CSS ERROR: " + cssEx.getMessage());
            }

            root.setOpacity(0);

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(30),
                    currentScene.getRoot()
            );
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                stage.setScene(newScene);  // Scene is attached here!

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

//    private void showStyledAlert(Alert.AlertType type, String title, String message) {
//        Alert alert = new Alert(type);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//        alert.showAndWait();
//    }

//    private boolean showConfirmation(String title, String message) {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//        Optional<ButtonType> result = alert.showAndWait();
//        return result.isPresent() && result.get() == ButtonType.OK;
//    }

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

    private boolean showStyledConfirmation(String title, String message) {
        // Create custom dialog
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality( Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);
        dialogStage.setUserData(false);

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

        confirmButton.setOnMouseEntered(e -> confirmButton.setStyle(
                confirmButton.getStyle().replace("#4CAF50", "#45a049")
        ));
        confirmButton.setOnMouseExited(e -> confirmButton.setStyle(
                confirmButton.getStyle().replace("#45a049", "#4CAF50")
        ));

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
    }

    private void handleViewDetails(Sale sale) {
        if (sale == null) return;

        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Transaction Details");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");
        mainContainer.setPrefWidth(600);

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

        titleBox.getChildren().addAll(titleLabel, dateLabel);
        headerBox.getChildren().addAll(iconLabel, titleBox);

        Separator separator1 = new Separator();

        Label itemsTitle = new Label("Items Purchased:");
        itemsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox itemsList = new VBox(12);
        itemsList.setStyle("-fx-background-color: #F8F9FA; -fx-padding: 15; -fx-background-radius: 8px;");

        for (SaleItem item : sale.getItems()) {
            VBox itemBox = new VBox(5);

            HBox itemNameRow = new HBox();
            itemNameRow.setAlignment(Pos.CENTER_LEFT);

            Label itemName = new Label(item.getMedicineName());
            itemName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
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

        Separator separator2 = new Separator();

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle("-fx-background-color: #E8F5E9; -fx-padding: 15; -fx-background-radius: 8px;");

        Label totalLabel = new Label("Total Amount:");
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(totalLabel, Priority.ALWAYS);

        Label totalAmount = new Label("₱" + String.format("%.2f", sale.getTotalAmount()));
        totalAmount.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        totalRow.getChildren().addAll(totalLabel, totalAmount);

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

        mainContainer.getChildren().addAll(headerBox, separator1, itemsTitle, itemsList, separator2, totalRow, buttonBox);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void updateCartUI() {
        if (cartItems.isEmpty()) {
            emptyCartState.setVisible(true);
            cartItemsContainer.setVisible(false);
            cartTotalSection.setVisible(false);
            completeTransactionBtn.setVisible(false);
            clearCartBtn.setVisible(false);
            cartCountLabel.setText("0 items in cart");
        } else {
            emptyCartState.setVisible(false);
            cartItemsContainer.setVisible(true);
            cartTotalSection.setVisible(true);
            completeTransactionBtn.setVisible(true);
            clearCartBtn.setVisible(true);

            int totalItems = cartItems.stream().mapToInt(SaleItem::getQuantity).sum();
            cartCountLabel.setText(totalItems + " item" + (totalItems != 1 ? "s" : "") + " in cart");

            cartItemsContainer.getChildren().clear();
            BigDecimal total = BigDecimal.ZERO;

            for (SaleItem item : cartItems) {
                VBox cartItem = createCartItemUI(item);
                cartItemsContainer.getChildren().add(cartItem);
                total = total.add(item.getSubtotal());
            }

            cartTotalLabel.setText("₱" + String.format("%.2f", total));
        }
    }

    private VBox createCartItemUI(SaleItem item) {
        VBox container = new VBox();
        container.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-padding: 15px;"
        );

        HBox mainRow = new HBox(15);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        // Medicine info
        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(item.getMedicineName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label priceLabel = new Label("₱" + String.format("%.2f", item.getUnitPrice()) + " each");
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        infoBox.getChildren().addAll(nameLabel, priceLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Quantity controls
        HBox qtyControls = new HBox(10);
        qtyControls.setAlignment(Pos.CENTER);

        Button minusBtn = new Button("−");
        minusBtn.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-min-width: 36px; " +
                        "-fx-min-height: 36px; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; " +
                        "-fx-cursor: hand;"
        );
        minusBtn.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                updateCartUI();
            }
        });

        Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
        qtyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #2c3e50; -fx-min-width: 40px; -fx-alignment: CENTER;");

        Button plusBtn = new Button("+");
        plusBtn.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-min-width: 36px; " +
                        "-fx-min-height: 36px; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; " +
                        "-fx-cursor: hand;"
        );
        plusBtn.setOnAction(e -> {
            if (item.getQuantity() < item.getProduct().getStock()) {
                item.setQuantity(item.getQuantity() + 1);
                updateCartUI();
            } else {
                showStyledAlert(Alert.AlertType.WARNING, "Stock Limit",
                        "Only " + item.getProduct().getStock() + " units available.");
            }
        });

        qtyControls.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        // Remove button
        Button removeBtn = new Button("✕");
        removeBtn.setStyle(
                "-fx-background-color: #F44336; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-padding: 6px 12px; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-cursor: hand;"
        );
        removeBtn.setOnAction(e -> {
            cartItems.remove(item);
            updateCartUI();
        });

        // Item total
        Label totalLabel = new Label("₱" + String.format("%.2f", item.getSubtotal()));
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        mainRow.getChildren().addAll(infoBox, qtyControls, removeBtn, totalLabel);
        container.getChildren().add(mainRow);

        return container;
    }

    // Helper class for bulk add
    private static class BulkAddItem {
        private Product product;
        private int quantity;

        public BulkAddItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}