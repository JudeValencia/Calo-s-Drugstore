package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.Supplier;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.SupplierService;
import com.inventory.Calo.s_Drugstore.util.IconUtil;
import javafx.animation.FadeTransition;
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
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Controller
public class SupplierController implements Initializable {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;

    @FXML private Label totalSuppliersLabel;
    @FXML private Label activeSuppliersLabel;
    @FXML private Label newSuppliersLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> supplierFilter;

    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, String> supplierIdColumn;
    @FXML private TableColumn<Supplier, String> supplierNameColumn;
    @FXML private TableColumn<Supplier, String> contactPersonColumn;
    @FXML private TableColumn<Supplier, String> emailColumn;
    @FXML private TableColumn<Supplier, String> statusColumn;
    @FXML private TableColumn<Supplier, Void> actionsColumn;

    @FXML private Button dashboardBtn;
    @FXML private Button productsBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button supplierBtn;
    @FXML private Button salesBtn;
    @FXML private Button reportsBtn;
    @FXML private Button staffBtn;
    @FXML private Button logoutBtn;

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;

    private ObservableList<Supplier> supplierList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== SUPPLIER CONTROLLER INITIALIZE ===");

        supplierService.syncSuppliersFromProducts();

        setupSupplierTable();
        loadSupplierData();
        setupColumnWidths();
        updateSummaryCards();
        setupSearch();
        setupFilter();
        setActiveButton(supplierBtn);

        applyCustomScrollbarToTable();

        supplierTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyCustomScrollbarToTable();
            }
        });
    }

    private void setupColumnWidths() {
        supplierIdColumn.setPrefWidth(120);
        supplierIdColumn.setMinWidth(100);

        supplierNameColumn.setPrefWidth(220);
        supplierNameColumn.setMinWidth(200);

        contactPersonColumn.setPrefWidth(250);
        contactPersonColumn.setMinWidth(220);

        emailColumn.setPrefWidth(200);
        emailColumn.setMinWidth(180);

        statusColumn.setPrefWidth(120);
        statusColumn.setMinWidth(100);

        actionsColumn.setPrefWidth(350);
        actionsColumn.setMinWidth(350);

        supplierTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }

    private void setupSupplierTable() {
        supplierTable.setEditable(false);
        supplierTable.setItems(supplierList);

        supplierIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSupplierId()));

        supplierNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCompanyName()));
        supplierNameColumn.setCellFactory(column -> new TableCell<Supplier, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Supplier supplier = getTableRow() != null ? getTableRow().getItem() : null;
                if (supplier == null) {
                    setGraphic(null);
                    return;
                }

                HBox container = new HBox(10);
                container.setAlignment(Pos.CENTER_LEFT);

                StackPane avatar = new StackPane();
                avatar.setStyle(
                        "-fx-background-color: #E0E0E0; " +
                                "-fx-background-radius: 25px; " +
                                "-fx-pref-width: 40px; " +
                                "-fx-pref-height: 40px;"
                );
                Label avatarIcon = new Label("🏢");
                avatarIcon.setStyle("-fx-font-size: 20px;");
                avatar.getChildren().add(avatarIcon);

                Label nameLabel = new Label(supplier.getCompanyName());
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                container.getChildren().addAll(avatar, nameLabel);

                setGraphic(container);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        contactPersonColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getContactPerson() != null ?
                        data.getValue().getContactPerson() : "N/A"));
        contactPersonColumn.setCellFactory(column -> new TableCell<Supplier, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Supplier supplier = getTableRow() != null ? getTableRow().getItem() : null;
                if (supplier == null) {
                    setGraphic(null);
                    return;
                }

                VBox container = new VBox(3);

                HBox personBox = new HBox(5);
                personBox.setAlignment(Pos.CENTER_LEFT);
                Label personIcon = new Label("👤");
                Label personLabel = new Label(supplier.getContactPerson() != null ? supplier.getContactPerson() : "N/A");
                personLabel.setStyle("-fx-font-size: 13px;");
                personBox.getChildren().addAll(personIcon, personLabel);

                HBox phoneBox = new HBox(5);
                phoneBox.setAlignment(Pos.CENTER_LEFT);
                Label phoneIcon = new Label("📞");
                Label phoneLabel = new Label(supplier.getContactNumber() != null ? supplier.getContactNumber() : "N/A");
                phoneLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
                phoneBox.getChildren().addAll(phoneIcon, phoneLabel);

                container.getChildren().addAll(personBox, phoneBox);

                setGraphic(container);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        emailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail() != null ?
                        data.getValue().getEmail() : "N/A"));

        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));
        statusColumn.setCellFactory(column -> new TableCell<Supplier, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                Supplier supplier = getTableRow() != null ? getTableRow().getItem() : null;
                if (supplier == null) {
                    setGraphic(null);
                    return;
                }

                Label statusBadge = new Label();

                if ("Active".equalsIgnoreCase(supplier.getStatus())) {
                    statusBadge.setText("Active");
                    statusBadge.setStyle(
                            "-fx-background-color: #4CAF50; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-padding: 6px 16px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-font-size: 13px; " +
                                    "-fx-font-weight: 600;"
                    );
                } else {
                    statusBadge.setText("Inactive");
                    statusBadge.setStyle(
                            "-fx-background-color: #E0E0E0; " +
                                    "-fx-text-fill: #7f8c8d; " +
                                    "-fx-padding: 6px 16px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-font-size: 13px; " +
                                    "-fx-font-weight: 600;"
                    );
                }

                setGraphic(statusBadge);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        actionsColumn.setCellFactory(column -> new TableCell<Supplier, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Supplier supplier = getTableRow() != null ? getTableRow().getItem() : null;
                if (supplier == null) {
                    setGraphic(null);
                    return;
                }

                HBox buttons = new HBox(10);
                buttons.setAlignment(Pos.CENTER_LEFT);

                Button viewBtn = new Button("👁");
                viewBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 20px; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                );
                viewBtn.setOnMouseEntered(e -> viewBtn.setStyle(
                        viewBtn.getStyle() + "-fx-background-color: #E8F5E9; -fx-border-color: #4CAF50;"
                ));
                viewBtn.setOnMouseExited(e -> viewBtn.setStyle(
                        viewBtn.getStyle().replace("-fx-background-color: #E8F5E9; -fx-border-color: #4CAF50;",
                                "-fx-background-color: white; -fx-border-color: #E0E0E0;")
                ));
                viewBtn.setOnAction(e -> handleViewSupplier(supplier));
                buttons.getChildren().add(viewBtn);

                Button editBtn = new Button("🔧");
                editBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 20px; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                );
                editBtn.setOnAction(e -> handleEditSupplier(supplier));

                Button toggleBtn = new Button("Active".equalsIgnoreCase(supplier.getStatus()) ? "Deactivate" : "Activate");
                toggleBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 14px; " +
                                "-fx-padding: 8px 16px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                );
                toggleBtn.setOnAction(e -> handleToggleStatus(supplier));

                buttons.getChildren().addAll(editBtn, toggleBtn);

                Button deleteBtn = new Button("🗑");
                deleteBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #F44336; " +
                                "-fx-font-size: 20px; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #FFCDD2; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                );
                deleteBtn.setOnAction(e -> handleDeleteSupplier(supplier));
                buttons.getChildren().add(deleteBtn);

                setGraphic(buttons);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
    }

    private void loadSupplierData() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        supplierList.setAll(suppliers);
        supplierTable.setItems(supplierList);
        supplierTable.refresh();
        if (suppliers.isEmpty()) {
            supplierTable.setPlaceholder(new Label("No suppliers found"));
        }
    }

    private void updateSummaryCards() {
        long total = supplierList.size();
        long active = supplierList.stream()
                .filter(s -> "Active".equalsIgnoreCase(s.getStatus()))
                .count();

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        long newSuppliers = supplierList.stream()
                .filter(s -> s.getDateAdded() != null && s.getDateAdded().isAfter(thirtyDaysAgo))
                .count();

        totalSuppliersLabel.setText(String.valueOf(total));
        activeSuppliersLabel.setText(String.valueOf(active));
        newSuppliersLabel.setText(String.valueOf(newSuppliers));
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    private void setupFilter() {
        if (supplierFilter != null) {
            supplierFilter.getItems().add("All Suppliers");
            supplierFilter.setValue("All Suppliers");
            supplierFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    private void applyFilters() {
        String searchTerm = searchField != null ? searchField.getText().toLowerCase() : "";

        List<Supplier> allSuppliers = supplierService.getAllSuppliers();
        List<Supplier> filtered = allSuppliers.stream()
                .filter(s -> {
                    boolean matchesSearch = searchTerm.isEmpty() ||
                            s.getCompanyName().toLowerCase().contains(searchTerm) ||
                            s.getSupplierId().toLowerCase().contains(searchTerm) ||
                            (s.getContactPerson() != null && s.getContactPerson().toLowerCase().contains(searchTerm)) ||
                            (s.getEmail() != null && s.getEmail().toLowerCase().contains(searchTerm));
                    return matchesSearch;
                })
                .toList();

        supplierList.setAll(filtered);
    }

    @FXML
    private void handleClearFilters() {
        if (searchField != null) searchField.clear();
        if (supplierFilter != null) supplierFilter.setValue("All Suppliers");
        loadSupplierData();
    }

    private void applyCustomScrollbarToTable() {
        String scrollBarStyle =
                ".scroll-bar {" +
                        "    -fx-background-color: transparent !important;" +
                        "}" +
                        ".scroll-bar .thumb {" +
                        "    -fx-background-color:  #cbd5e0 !important;" +
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

        Scene scene = supplierTable.getScene();
        if (scene != null) {
            scene.getStylesheets().add("data:text/css," + scrollBarStyle);
        }
    }

    @FXML
    private void handleAddSupplier() {
        showAddEditSupplierDialog(null);
    }

    private void handleEditSupplier(Supplier supplier) {
        showAddEditSupplierDialog(supplier);
    }

    private void handleViewSupplier(Supplier supplier) {
        showSupplierDetailsDialog(supplier);
    }

    private void showSupplierDetailsDialog(Supplier supplier) {
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Supplier Details");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");
        mainContainer.setPrefWidth(600);
        mainContainer.setMaxHeight(650);

        Label titleLabel = new Label(supplier.getCompanyName());
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label(supplier.getSupplierId());
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        VBox header = new VBox(5, titleLabel, subtitleLabel);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background: transparent; " +
                        "-fx-border-color: transparent;"
        );
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(400);

        VBox contentContainer = new VBox(20);

        String addressStr = (supplier.getPhysicalAddress() != null && !supplier.getPhysicalAddress().isEmpty())
                ? supplier.getPhysicalAddress() : "Not provided";
        String contactPersonStr = (supplier.getContactPerson() != null && !supplier.getContactPerson().isEmpty())
                ? supplier.getContactPerson() : "Not provided";
        String contactNumberStr = (supplier.getContactNumber() != null && !supplier.getContactNumber().isEmpty())
                ? supplier.getContactNumber() : "Not provided";
        String emailStr = (supplier.getEmail() != null && !supplier.getEmail().isEmpty())
                ? supplier.getEmail() : "Not provided";
        String productsStr = (supplier.getProductsSupplied() != null && !supplier.getProductsSupplied().isEmpty())
                ? supplier.getProductsSupplied() : "Not provided";

        VBox basicSection = createDetailSection("Basic Information",
                createDetailRow("Supplier ID:", supplier.getSupplierId()),
                createDetailRow("Company Name:", supplier.getCompanyName()),
                createDetailRow("Physical Address:", addressStr),
                createDetailRow("Products/Services:", productsStr)
        );

        VBox contactSection = createDetailSection("Contact Information",
                createDetailRow("Contact Person:", contactPersonStr),
                createDetailRow("Contact Number:", contactNumberStr),
                createDetailRow("Email:", emailStr)
        );

        String statusText = "Active".equalsIgnoreCase(supplier.getStatus()) ? "Active ✓" : "Inactive ✗";
        VBox statusSection = createDetailSection("Status",
                createDetailRow("Current Status:", statusText)
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String dateAddedStr = supplier.getDateAdded() != null ? supplier.getDateAdded().format(formatter) : "N/A";
        String updatedAtStr = supplier.getUpdatedAt() != null ? supplier.getUpdatedAt().format(formatter) : "N/A";

        VBox datesSection = createDetailSection("Record Information",
                createDetailRow("Date Added:", dateAddedStr),
                createDetailRow("Last Updated:", updatedAtStr)
        );

        contentContainer.getChildren().addAll(
                basicSection,
                contactSection,
                statusSection,
                datesSection
        );

        scrollPane.setContent(contentContainer);

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

        mainContainer.getChildren().addAll(header, scrollPane, buttonContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

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

    private void handleToggleStatus(Supplier supplier) {
        String newStatus = "Active".equalsIgnoreCase(supplier.getStatus()) ? "Inactive" : "Active";
        String action = "Active".equalsIgnoreCase(newStatus) ? "activate" : "deactivate";

        boolean confirmed = showStyledConfirmation(
                "Confirm Status Change",
                "Are you sure you want to " + action + " " + supplier.getCompanyName() + "?"
        );

        if (confirmed) {
            supplier.setStatus(newStatus);
            supplierService.updateSupplier(supplier);
            loadSupplierData();
            updateSummaryCards();
            showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                    "Supplier " + (newStatus.equalsIgnoreCase("Active") ? "activated" : "deactivated") + " successfully!");
        }
    }

    private void handleDeleteSupplier(Supplier supplier) {
        boolean confirmed = showStyledConfirmation(
                "Delete Supplier",
                "Are you sure you want to delete " + supplier.getCompanyName() + "? This action cannot be undone."
        );

        if (confirmed) {
            supplierService.deleteSupplier(supplier.getId());
            loadSupplierData();
            updateSummaryCards();
            showStyledAlert(Alert.AlertType.INFORMATION, "Success", "Supplier deleted successfully!");
        }
    }

    private void showAddEditSupplierDialog(Supplier editSupplier) {
        boolean isEdit = editSupplier != null;

        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(isEdit ? "Edit Supplier" : "Add New Supplier");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white;");
        mainContainer.setPrefWidth(550);
        mainContainer.setMaxHeight(650);

        VBox headerBox = new VBox();
        headerBox.setStyle("-fx-background-color: white; -fx-padding: 30 30 20 30;");
        Label titleLabel = new Label(isEdit ? "Edit Supplier" : "Add New Supplier");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        headerBox.getChildren().add(titleLabel);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;" +
                        "-fx-border-width: 0;" +
                        "-fx-background-insets: 0;"
        );

        VBox contentBox = new VBox(20);
        contentBox.setStyle("-fx-padding: 0 30 20 30; -fx-background-color: white;");

        String fieldStyle = "-fx-background-color: #F8F9FA; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 12px 15px;";

        VBox companyNameBox = new VBox(8);
        Label companyNameLabel = new Label("Company Name *");
        companyNameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField companyNameField = new TextField();
        companyNameField.setPromptText("Enter company name");
        companyNameField.setStyle(fieldStyle);
        if (isEdit) companyNameField.setText(editSupplier.getCompanyName());
        companyNameBox.getChildren().addAll(companyNameLabel, companyNameField);

        VBox contactNumberBox = new VBox(8);
        Label contactNumberLabel = new Label("Contact Number *");
        contactNumberLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField contactNumberField = new TextField();
        contactNumberField.setPromptText("Enter contact number");
        contactNumberField.setStyle(fieldStyle);
        if (isEdit && editSupplier.getContactNumber() != null) contactNumberField.setText(editSupplier.getContactNumber());
        contactNumberBox.getChildren().addAll(contactNumberLabel, contactNumberField);

        VBox addressBox = new VBox(8);
        Label addressLabel = new Label("Physical Address *");
        addressLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextArea addressField = new TextArea();
        addressField.setPromptText("Enter street, city, state, and ZIP code");
        addressField.setStyle(fieldStyle);
        addressField.setPrefRowCount(3);
        addressField.setWrapText(true);

        if (isEdit && editSupplier.getPhysicalAddress() != null) addressField.setText(editSupplier.getPhysicalAddress());

        addressBox.getChildren().addAll(addressLabel, addressField);

        VBox contactPersonBox = new VBox(8);
        Label contactPersonLabel = new Label("Contact Person");
        contactPersonLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField contactPersonField = new TextField();
        contactPersonField.setPromptText("Enter contact person name");
        contactPersonField.setStyle(fieldStyle);
        if (isEdit && editSupplier.getContactPerson() != null) contactPersonField.setText(editSupplier.getContactPerson());
        contactPersonBox.getChildren().addAll(contactPersonLabel, contactPersonField);

        VBox emailBox = new VBox(8);
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField emailField = new TextField();
        emailField.setPromptText("Enter email address");
        emailField.setStyle(fieldStyle);
        if (isEdit && editSupplier.getEmail() != null) emailField.setText(editSupplier.getEmail());
        emailBox.getChildren().addAll(emailLabel, emailField);

        VBox productsBox = new VBox(8);
        Label productsLabel = new Label("Products/Services Supplied");
        productsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextArea productsField = new TextArea();
        productsField.setPromptText("Enter products or services supplied");
        productsField.setStyle(fieldStyle);
        productsField.setPrefRowCount(3);
        productsField.setWrapText(true);
        if (isEdit && editSupplier.getProductsSupplied() != null) productsField.setText(editSupplier.getProductsSupplied());
        productsBox.getChildren().addAll(productsLabel, productsField);

        contentBox.getChildren().addAll(
                companyNameBox, contactNumberBox, addressBox, contactPersonBox,
                emailBox, productsBox
        );

        scrollPane.setContent(contentBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox footerBox = new VBox();
        footerBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 20 30 30 30; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1 0 0 0;"
        );

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
        cancelButton.setOnAction(e -> dialogStage.close());

        Button saveButton = new Button(isEdit ? "Update" : "Add Supplier");
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
            String companyName = companyNameField.getText().trim();
            String contactNumber = contactNumberField.getText().trim();
            String address = addressField.getText().trim();
            String contactPerson = contactPersonField.getText().trim();
            String email = emailField.getText().trim();
            String productsSupplied = productsField.getText().trim();

            if (companyName.isEmpty() || contactNumber.isEmpty() || address.isEmpty()) {
                showStyledAlert(Alert.AlertType.WARNING, "Missing Information",
                        "Please fill in all required fields (Company Name, Contact Number, and Physical Address).");
                return;
            }

            try {
                if (!isEdit) {
                    supplierService.createSupplier(companyName, contactNumber, address,
                            contactPerson, email, productsSupplied);
                    showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                            "Supplier created successfully!");
                } else {
                    editSupplier.setCompanyName(companyName);
                    editSupplier.setContactNumber(contactNumber);
                    editSupplier.setPhysicalAddress(address);
                    editSupplier.setContactPerson(contactPerson.isEmpty() ? null : contactPerson);
                    editSupplier.setEmail(email.isEmpty() ? null : email);
                    editSupplier.setProductsSupplied(productsSupplied.isEmpty() ? null : productsSupplied);

                    supplierService.updateSupplier(editSupplier);
                    showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                            "Supplier updated successfully!");
                }

                loadSupplierData();
                updateSummaryCards();
                dialogStage.close();

                refreshProductManagementSuppliers();

            } catch (RuntimeException ex) {
                showStyledAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        buttonBox.getChildren().addAll(cancelButton, saveButton);
        footerBox.getChildren().add(buttonBox);

        mainContainer.getChildren().addAll(headerBox, scrollPane, footerBox);

        Scene scene = new Scene(mainContainer);

        String scrollBarStyle =
                ".scroll-pane {" +
                        "    -fx-background-color: transparent;" +
                        "    -fx-border-width: 0;" +
                        "}" +
                        ".scroll-pane .viewport {" +
                        "    -fx-background-color: transparent;" +
                        "}" +
                        ".scroll-bar {" +
                        "    -fx-background-color: transparent;" +
                        "}" +
                        ".scroll-bar .thumb {" +
                        "    -fx-background-color:  #cbd5e0;" +
                        "    -fx-background-radius: 4px;" +
                        "}" +
                        ".scroll-bar .thumb:hover {" +
                        "    -fx-background-color: #a0aec0;" +
                        "}" +
                        ".scroll-bar .increment-button," +
                        ".scroll-bar .decrement-button {" +
                        "    -fx-background-color: transparent;" +
                        "    -fx-padding: 0;" +
                        "}";

        scene.getStylesheets().add("data:text/css," + scrollBarStyle);

        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void setActiveButton(Button activeButton) {
        dashboardBtn.getStyleClass().remove("active");
        inventoryBtn.getStyleClass().remove("active");
        salesBtn.getStyleClass().remove("active");
        reportsBtn.getStyleClass().remove("active");
        staffBtn.getStyleClass().remove("active");
        supplierBtn.getStyleClass().remove("active");

        activeButton.getStyleClass().add("active");
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

    private boolean showLogoutConfirmation() {
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Logout");
        dialogStage.setResizable(false);
        dialogStage.setUserData(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        Label titleLabel = new Label("Are you sure you want to logout?");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label("You will be returned to the login screen and will need to log in again to access the system.");
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

        buttonBox.getChildren().addAll(cancelButton, logoutButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
    }

    private void navigateToPage(String fxmlPath, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

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
            } else if (fxmlPath.contains("product-management")) {
                ProductManagementController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            } else if (fxmlPath.contains("staff")) {
                StaffController controller = loader.getController();
                controller.setCurrentUser(currentUser);
            }

            Stage stage = (Stage) supplierBtn.getScene().getWindow();
            Scene currentScene = stage.getScene();

            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            double currentX = stage.getX();
            double currentY = stage.getY();
            boolean isMaximized = stage.isMaximized();

            Scene newScene = new Scene(root);

            String scrollBarStyle =
                    ".scroll-bar {" +
                            "    -fx-background-color: transparent !important;" +
                            "}" +
                            ".scroll-bar .thumb {" +
                            "    -fx-background-color:  #cbd5e0 !important;" +
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

            newScene.getStylesheets().add("data:text/css," + scrollBarStyle);

            try {
                java.net.URL cssUrl = getClass().getResource(cssPath);
                if (cssUrl != null) {
                    newScene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception cssEx) {
                System.err.println("CSS ERROR: " + cssEx.getMessage());
            }

            root.setOpacity(0);

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(30), currentScene.getRoot()
            );
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                stage.setScene(newScene);

                if (isMaximized) {
                    stage.setMaximized(true);
                } else {
                    stage.setWidth(currentWidth);
                    stage.setHeight(currentHeight);
                    stage.setX(currentX);
                    stage.setY(currentY);
                }

                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(30), root
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) supplierBtn.getScene().getWindow();
            Scene currentScene = stage.getScene();

            Scene newScene = new Scene(root);

            try {
                java.net.URL cssUrl = getClass().getResource("/css/login.css");
                if (cssUrl != null) {
                    newScene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception cssEx) {
                System.err.println("Warning: Could not load CSS for login page: " + cssEx.getMessage());
            }

            root.setOpacity(0);

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(30), currentScene.getRoot()
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
                        javafx.util.Duration.millis(30), root
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

        String buttonColor = type == Alert.AlertType.ERROR ? "#dc3545" :
                type == Alert.AlertType.WARNING ? "#FF9800" : "#4CAF50";

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
        buttonBox.getChildren().add(okButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private boolean showStyledConfirmation(String title, String message) {
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
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
    }

    private void refreshProductManagementSuppliers() {
        try {
            // Find ProductManagementController if it's loaded and refresh its supplier dropdown
            Stage stage = (Stage) supplierBtn.getScene().getWindow();
            // This will refresh the next time Product Management is opened
            System.out.println("✅ Supplier list updated - will refresh in Product Management");
        } catch (Exception e) {
            // Ignore if Product Management isn't currently loaded
        }
    }
}