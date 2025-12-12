package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.util.IconUtil;
import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.entity.Category;
import com.inventory.Calo.s_Drugstore.service.UserManagementService;
import com.inventory.Calo.s_Drugstore.service.CategoryService;
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
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Controller
public class StaffController implements Initializable {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private User currentUser;

    // FXML Components - Summary Cards
    @FXML private Label totalStaffLabel;
    @FXML private Label activeAccountsLabel;
    @FXML private Label administratorsLabel;
    @FXML private Label staffMembersLabel;

    // FXML Components - Staff Table
    @FXML private TableView<User> staffTable;
    @FXML private TableColumn<User, String> staffIdColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> contactColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> createdDateColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

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

    private ObservableList<User> staffList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== STAFF CONTROLLER INITIALIZE ===");

        setupStaffTable();
        loadStaffData();
        setupColumnWidths();
        updateSummaryCards();
        setActiveButton(staffBtn);

        // Apply custom scrollbar styling to main page
        applyCustomScrollbarToTable();

        // In case scene loads later so add listener
        staffTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyCustomScrollbarToTable();
            }
        });
    }

    private void setupColumnWidths() {
        // Set specific widths for each column
        staffIdColumn.setPrefWidth(120);
        staffIdColumn.setMinWidth(100);

        nameColumn.setPrefWidth(200);
        nameColumn.setMinWidth(180);

        contactColumn.setPrefWidth(250);
        contactColumn.setMinWidth(220);

        roleColumn.setPrefWidth(120);
        roleColumn.setMinWidth(100);

        statusColumn.setPrefWidth(120);
        statusColumn.setMinWidth(100);

        createdDateColumn.setPrefWidth(150);
        createdDateColumn.setMinWidth(130);

        // IMPORTANT: Make actions column wider
        actionsColumn.setPrefWidth(350);
        actionsColumn.setMinWidth(350);

        // Allow table to resize
        staffTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }
    private void setupStaffTable() {
        staffTable.setEditable(false);
        staffTable.setItems(staffList);
        //staffTable.setFixedCellSize(70);

        staffIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));

        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));
        nameColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    return;
                }

                // Create graphics fresh each time
                HBox container = new HBox(10);
                container.setAlignment(Pos.CENTER_LEFT);

                StackPane avatar = new StackPane();
                avatar.setStyle(
                        "-fx-background-color: #E0E0E0; " +
                                "-fx-background-radius: 25px; " +
                                "-fx-pref-width: 40px; " +
                                "-fx-pref-height: 40px;"
                );
                Label avatarIcon = new Label("👤");
                avatarIcon.setStyle("-fx-font-size: 20px;");
                avatar.getChildren().add(avatarIcon);

                Label nameLabel = new Label(user.getFullName());
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                container.getChildren().addAll(avatar, nameLabel);

                // CRITICAL: Set graphic and ensure it's managed
                setGraphic(container);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        contactColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));
        contactColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    return;
                }

                VBox container = new VBox(3);

                HBox emailBox = new HBox(5);
                emailBox.setAlignment(Pos.CENTER_LEFT);
                Label emailIcon = new Label("✉");
                Label emailLabel = new Label(user.getEmail());
                emailLabel.setStyle("-fx-font-size: 13px;");
                emailBox.getChildren().addAll(emailIcon, emailLabel);

                HBox usernameBox = new HBox(5);
                usernameBox.setAlignment(Pos.CENTER_LEFT);
                Label usernameIcon = new Label("👤");
                Label usernameLabel = new Label("@" + user.getUsername());
                usernameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
                usernameBox.getChildren().addAll(usernameIcon, usernameLabel);

                HBox contactNumberBox = new HBox(5);
                contactNumberBox.setAlignment(Pos.CENTER_LEFT);
                Label contactNumberIcon = new Label("\uD83D\uDCDE");
                Label contactNumberLabel = new Label(user.getContactNumber());
                contactNumberLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
                contactNumberBox.getChildren().addAll(contactNumberIcon, contactNumberLabel);

                container.getChildren().addAll(emailBox, usernameBox, contactNumberBox);

                setGraphic(container);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        roleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole()));
        roleColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    return;
                }

                HBox container = new HBox();
                container.setAlignment(Pos.CENTER_LEFT);

                Label roleBadge = new Label();
                boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());

                if (isAdmin) {
                    roleBadge.setText("🛡 Admin");
                    roleBadge.setStyle(
                            "-fx-background-color: #1a1a1a; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-padding: 6px 12px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-font-size: 13px; " +
                                    "-fx-font-weight: 600;"
                    );
                } else {
                    roleBadge.setText("👤 Staff");
                    roleBadge.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-text-fill: #2c3e50; " +
                                    "-fx-padding: 6px 12px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-border-color: #E0E0E0; " +
                                    "-fx-border-width: 1.5px; " +
                                    "-fx-border-radius: 6px; " +
                                    "-fx-font-size: 13px; " +
                                    "-fx-font-weight: 600;"
                    );
                }

                container.getChildren().add(roleBadge);
                setGraphic(container);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        statusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    return;
                }

                Label statusBadge = new Label();

                if (user.isActive()) {
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

        createdDateColumn.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. dd, yyyy");
            return new SimpleStringProperty(data.getValue().getCreatedAt().format(formatter));
        });

        actionsColumn.setCellFactory(column -> new TableCell<User, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    return;
                }

                HBox buttons = new HBox(10);
                buttons.setAlignment(Pos.CENTER_LEFT);

                // View button
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
                viewBtn.setOnAction(e -> handleViewStaff(user));
                buttons.getChildren().addAll(viewBtn);

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
                editBtn.setOnAction(e -> handleEditStaff(user));

                Button toggleBtn = new Button(user.isActive() ? "Deactivate" : "Activate");
                if (user.isActive()) {
                    toggleBtn.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-text-fill: #F44336; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-padding: 8px 16px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-border-color: #FFCDD2; " +
                                    "-fx-border-width: 1.5px; " +
                                    "-fx-border-radius: 6px; " +
                                    "-fx-cursor: hand;"
                    );
                } else {
                    toggleBtn.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-text-fill: #4CAF50; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-padding: 8px 16px; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-border-color: #C8E6C9; " +
                                    "-fx-border-width: 1.5px; " +
                                    "-fx-border-radius: 6px; " +
                                    "-fx-cursor: hand;"
                    );
                }
                toggleBtn.setOnAction(e -> handleToggleStatus(user));

                buttons.getChildren().addAll(editBtn, toggleBtn);

                if (!"ADMIN".equalsIgnoreCase(user.getRole()) ||
                        (currentUser != null && !currentUser.getId().equals(user.getId()))) {
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
                    deleteBtn.setOnAction(e -> handleDeleteStaff(user));
                    buttons.getChildren().add(deleteBtn);
                }

                setGraphic(buttons);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
    }

    private void loadStaffData() {
        List<User> users = userManagementService.getAllUsers();
        staffList.setAll(users);
        staffTable.setItems(staffList);
        staffTable.refresh();
        if (users.isEmpty()) {
            staffTable.setPlaceholder(new Label("No staff accounts found"));
        }
    }

    private void updateSummaryCards() {
        long total = staffList.size();
        long active = staffList.stream().filter(User::isActive).count();
        long admins = staffList.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
        long staff = total - admins;

        totalStaffLabel.setText(String.valueOf(total));
        activeAccountsLabel.setText(String.valueOf(active));
        administratorsLabel.setText(String.valueOf(admins));
        staffMembersLabel.setText(String.valueOf(staff));
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

        Scene scene = staffTable.getScene();
        if (scene != null) {
            scene.getStylesheets().add("data:text/css," + scrollBarStyle);
        }
    }
    
    @FXML
    private void handleAddCategory() {
        showAddCategoryDialog();
    }
    
    @FXML
    private void handleAddStaff() {
        showAddEditStaffDialog(null);
    }

    private void handleEditStaff(User user) {
        showAddEditStaffDialog(user);
    }

    private void handleViewStaff(User user) {
        showStaffDetailsDialog(user);
    }

    private void showStaffDetailsDialog(User user) {
        // Create custom dialog
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Staff Details");
        dialogStage.setResizable(false);

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");
        mainContainer.setPrefWidth(600);
        mainContainer.setMaxHeight(650);

        // Header
        Label titleLabel = new Label(user.getFullName());
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label("@" + user.getUsername());
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        VBox header = new VBox(5, titleLabel, subtitleLabel);

        // ScrollPane for content
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

        String addressStr = (user.getAddress() != null && !user.getAddress().isEmpty())
                ? user.getAddress() : "Not provided";
        String dobStr;
        if (user.getDateOfBirth() != null && !user.getDateOfBirth().isEmpty()) {
            try {
                LocalDate dob = LocalDate.parse(user.getDateOfBirth());
                DateTimeFormatter dobFormatter = DateTimeFormatter.ofPattern("MMM. dd, yyyy");
                dobStr = dob.format(dobFormatter);
            } catch (Exception e) {
                dobStr = user.getDateOfBirth(); // Fallback to raw value if parsing fails
            }
        } else {
            dobStr = "Not provided";
        }


        // Section 1: Basic Information
        VBox basicSection = createDetailSection("Basic Information",
                createDetailRow("Full Name:", user.getFullName()),
                createDetailRow("Username:", user.getUsername()),
                createDetailRow("Role:", user.getRole()),
                createDetailRow("Address:", addressStr),
                createDetailRow("Date of Birth:", dobStr)
        );

        // Section 2: Contact Information
        VBox contactSection = createDetailSection("Contact Information",
                createDetailRow("Email:", user.getEmail()),
                createDetailRow("Contact Number:", user.getContactNumber() != null ? user.getContactNumber() : "N/A")
        );

        // Section 3: Account Status
        String statusText = user.isActive() ? "Active ✓" : "Inactive ✗";
        VBox statusSection = createDetailSection("Account Status",
                createDetailRow("Status:", statusText)
        );

        // Section 4: Account Dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
        String createdAtStr = user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : "N/A";
        String updatedAtStr = user.getLastLogin() != null ? user.getLastLogin().format(formatter) : "N/A";

        VBox datesSection = createDetailSection("Account Information",
                createDetailRow("Created On:", createdAtStr),
                createDetailRow("Last Updated:", updatedAtStr)
        );

        contentContainer.getChildren().addAll(
                basicSection,
                contactSection,
                statusSection,
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
        labelNode.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-min-width: 150px;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");
        valueNode.setWrapText(true);

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private void handleToggleStatus(User user) {
        // Prevent deactivating last admin
        if (user.isActive() && "ADMIN".equalsIgnoreCase(user.getRole())) {
            long activeAdmins = staffList.stream()
                    .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()) && u.isActive())
                    .count();

            if (activeAdmins <= 1) {
                showStyledAlert(Alert.AlertType.WARNING, "Cannot Deactivate",
                        "Cannot deactivate the last active administrator.");
                return;
            }
        }

        String action = user.isActive() ? "deactivate" : "activate";
        boolean confirmed = showStyledConfirmation(
                "Confirm " + (user.isActive() ? "Deactivation" : "Activation"),
                "Are you sure you want to " + action + " " + user.getFullName() + "?"
        );

        if (confirmed) {
            user.setActive(!user.isActive());
            userManagementService.setUserActiveStatus(user.getId(), user.isActive());
            loadStaffData();
            updateSummaryCards();
            showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                    "Staff account " + (user.isActive() ? "activated" : "deactivated") + " successfully!");
        }
    }

    private void handleDeleteStaff(User user) {
        boolean confirmed = showStyledConfirmation(
                "Delete Staff Account",
                "Are you sure you want to delete " + user.getFullName() + "? This action cannot be undone."
        );

        if (confirmed) {
            userManagementService.deleteUser(user.getId());
            loadStaffData();
            updateSummaryCards();
            showStyledAlert(Alert.AlertType.INFORMATION, "Success", "Staff account deleted successfully!");
        }
    }
    private void showAddEditStaffDialog(User editUser) {
        boolean isEdit = editUser != null;

        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(isEdit ? "Edit Staff Account" : "Add New Staff Account");
        dialogStage.setResizable(false);

        // ==================== MAIN CONTAINER  ====================//
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: white;");
        mainContainer.setPrefWidth(550);
        mainContainer.setMaxHeight(650); //maximum height

        // ==================== FIXED HEADER ====================//
        VBox headerBox = new VBox();
        headerBox.setStyle("-fx-background-color: white; -fx-padding: 30 30 20 30;");
        Label titleLabel = new Label(isEdit ? "Edit Staff Account" : "Add New Staff Account");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        headerBox.getChildren().add(titleLabel);

        // ==================== SCROLLABLE ====================//
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        //
        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;" +
                        "-fx-border-width: 0;" +
                        "-fx-background-insets: 0;"
        );

        scrollPane.lookup(".scroll-bar:vertical .track");

        VBox contentBox = new VBox(20);
        contentBox.setStyle("-fx-padding: 0 30 20 30; -fx-background-color: white;");

        // ==================== FORM FIELDS ====================//

        // Full Name
        VBox nameBox = new VBox(8);
        Label nameLabel = new Label("Full Name *");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField nameField = new TextField();
        nameField.setPromptText("Enter full name");
        nameField.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12px 15px;"
        );
        if (isEdit) nameField.setText(editUser.getFullName());
        nameBox.getChildren().addAll(nameLabel, nameField);

        // Username
        VBox usernameBox = new VBox(8);
        Label usernameLabel = new Label("Username *");
        usernameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setStyle(nameField.getStyle());
        if (isEdit) {
            usernameField.setText(editUser.getUsername());
        }
        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Email
        VBox emailBox = new VBox(8);
        Label emailLabel = new Label("Email *");
        emailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField emailField = new TextField();
        emailField.setPromptText("Enter email address");
        emailField.setStyle(nameField.getStyle());
        if (isEdit) emailField.setText(editUser.getEmail());
        emailBox.getChildren().addAll(emailLabel, emailField);

        // Contact Number
        VBox contactBox = new VBox(8);
        Label contactLabel = new Label("Contact Number");
        contactLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField contactField = new TextField();
        contactField.setPromptText("Enter contact number");
        contactField.setStyle(nameField.getStyle());
        if (isEdit && editUser.getContactNumber() != null) contactField.setText(editUser.getContactNumber());
        contactBox.getChildren().addAll(contactLabel, contactField);

        // Address
        VBox addressBox = new VBox(8);
        Label addressLabel = new Label("Address");
        addressLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        TextField addressField = new TextField();
        addressField.setPromptText("Enter address");
        addressField.setStyle(nameField.getStyle());
        if (isEdit && editUser.getAddress() != null) addressField.setText(editUser.getAddress());
        addressBox.getChildren().addAll(addressLabel, addressField);

        // Date of Birth
        VBox dobBox = new VBox(8);
        Label dobLabel = new Label("Date of Birth");
        dobLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("Select date of birth");
        dobPicker.setStyle(
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-padding: 12px 15px; " +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: black;" +
                        "-fx-prompt-text-fill: #7f8c8d;"
        );
        dobPicker.getEditor().setStyle("-fx-text-fill: black;");
        dobPicker.getStyleClass().add("custom-datepicker");

        // Set custom date format (MMM. dd, yyyy)
        dobPicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM. dd, yyyy");

            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(formatter) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty()) ? LocalDate.parse(string, formatter) : null;
            }
        });

        // Set initial value if editing and dateOfBirth exists
        if (isEdit && editUser.getDateOfBirth() != null && !editUser.getDateOfBirth().isEmpty()) {
            try {
                LocalDate dob = LocalDate.parse(editUser.getDateOfBirth());
                dobPicker.setValue(dob);
            } catch (Exception e) {
                System.err.println("Error parsing date of birth: " + e.getMessage());
            }
        }

        dobBox.getChildren().addAll(dobLabel, dobPicker);

        // Password with show/hide toggle
        VBox passwordBox = new VBox(8);
        Label passwordLabel = new Label(isEdit ? "New Password (leave blank to keep current)" : "Password *");
        passwordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        HBox passwordInputBox = new HBox(10);
        passwordInputBox.setAlignment(Pos.CENTER_LEFT);

        TextField passwordTextField = new TextField();
        PasswordField passwordField = new PasswordField();

        String fieldStyle =
                "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 12px 15px;";

        passwordTextField.setStyle(fieldStyle);
        passwordField.setStyle(fieldStyle);
        passwordTextField.setPromptText(isEdit ? "Enter new password (optional)" : "Enter password (min 6 characters)");
        passwordField.setPromptText(isEdit ? "Enter new password (optional)" : "Enter password (min 6 characters)");

        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        Button toggleButton = new Button("👁");
        toggleButton.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-font-size: 16px; " +
                        "-fx-min-width: 45px; " +
                        "-fx-min-height: 45px; " +
                        "-fx-cursor: hand;"
        );

        toggleButton.setOnAction(e -> {
            if (passwordField.isVisible()) {
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                passwordTextField.setVisible(true);
                passwordTextField.setManaged(true);
                toggleButton.setText("👁‍🗨");
            } else {
                passwordTextField.setVisible(false);
                passwordTextField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                toggleButton.setText("👁");
            }
        });

        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox.setHgrow(passwordTextField, Priority.ALWAYS);

        passwordInputBox.getChildren().addAll(passwordField, passwordTextField, toggleButton);
        passwordBox.getChildren().addAll(passwordLabel, passwordInputBox);

        // Confirm Password
        VBox confirmPasswordBox = new VBox(8);
        Label confirmPasswordLabel = new Label(isEdit ? "Confirm New Password" : "Confirm Password *");
        confirmPasswordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        HBox confirmPasswordInputBox = new HBox(10);
        confirmPasswordInputBox.setAlignment(Pos.CENTER_LEFT);

        TextField confirmPasswordTextField = new TextField();
        PasswordField confirmPasswordField = new PasswordField();

        confirmPasswordTextField.setStyle(fieldStyle);
        confirmPasswordField.setStyle(fieldStyle);
        confirmPasswordTextField.setPromptText(isEdit ? "Confirm new password" : "Re-enter password");
        confirmPasswordField.setPromptText(isEdit ? "Confirm new password" : "Re-enter password");

        confirmPasswordTextField.setVisible(false);
        confirmPasswordTextField.setManaged(false);

        confirmPasswordTextField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        Button confirmToggleButton = new Button("👁");
        confirmToggleButton.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-font-size: 16px; " +
                        "-fx-min-width: 45px; " +
                        "-fx-min-height: 45px; " +
                        "-fx-cursor: hand;"
        );

        confirmToggleButton.setOnAction(e -> {
            if (confirmPasswordField.isVisible()) {
                confirmPasswordField.setVisible(false);
                confirmPasswordField.setManaged(false);
                confirmPasswordTextField.setVisible(true);
                confirmPasswordTextField.setManaged(true);
                confirmToggleButton.setText("👁‍🗨");
            } else {
                confirmPasswordTextField.setVisible(false);
                confirmPasswordTextField.setManaged(false);
                confirmPasswordField.setVisible(true);
                confirmPasswordField.setManaged(true);
                confirmToggleButton.setText("👁");
            }
        });

        HBox.setHgrow(confirmPasswordField, Priority.ALWAYS);
        HBox.setHgrow(confirmPasswordTextField, Priority.ALWAYS);

        confirmPasswordInputBox.getChildren().addAll(confirmPasswordField, confirmPasswordTextField, confirmToggleButton);
        confirmPasswordBox.getChildren().addAll(confirmPasswordLabel, confirmPasswordInputBox);

        // Role
        VBox roleBox = new VBox(8);
        Label roleLabel = new Label("Role *");
        roleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        ComboBox<String> roleCombo = new ComboBox<>();
        
        // Load categories dynamically from database
        List<String> categories = categoryService.getAllCategoryNames();
        if (categories.isEmpty()) {
            // Default categories if none exist
            roleCombo.getItems().addAll("STAFF", "ADMIN");
            roleCombo.setValue(isEdit ? editUser.getRole() : "STAFF");
        } else {
            roleCombo.getItems().addAll(categories);
            // Set value to existing role if editing, otherwise first category in list
            if (isEdit) {
                roleCombo.setValue(editUser.getRole());
            } else {
                roleCombo.setValue(categories.isEmpty() ? null : categories.get(0));
            }
        }
        
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setStyle(nameField.getStyle());
        
        // Disable role combo if editing existing user
        if (isEdit) {
            roleCombo.setDisable(true);
            roleCombo.setStyle(nameField.getStyle() + "-fx-opacity: 0.6;");
        }
        
        roleBox.getChildren().addAll(roleLabel, roleCombo);

        // ==================== ALL FIELDS TO SCROLLABLE CONTENT ====================//
        contentBox.getChildren().addAll(
                nameBox, usernameBox, emailBox, contactBox,
                addressBox, dobBox, passwordBox, confirmPasswordBox, roleBox
        );

        scrollPane.setContent(contentBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ==================== FIXED FOOTER WITH BUTTONS ====================//
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

        Button saveButton = new Button(isEdit ? "Update" : "Add Staff");
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
            String name = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String contactNumber = contactField.getText().trim();
            String address = addressField.getText().trim();
            String dateOfBirth = dobPicker.getValue() != null ? dobPicker.getValue().toString() : "";
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();
            String role = roleCombo.getValue();

            if (name.isEmpty() || email.isEmpty() || username.isEmpty()) {
                showStyledAlert(Alert.AlertType.WARNING, "Missing Information",
                        "Please fill in all required fields.");
                return;
            }

            if (!password.isEmpty() && !password.equals(confirmPassword)) {
                showStyledAlert(Alert.AlertType.WARNING, "Password Mismatch",
                        "Passwords do not match. Please try again.");
                return;
            }

            try {
                if (!isEdit) {
                    if (password.isEmpty() || password.length() < 6) {
                        showStyledAlert(Alert.AlertType.WARNING, "Invalid Password",
                                "Password must be at least 6 characters long.");
                        return;
                    }

                    userManagementService.createStaffAccount(username, email, password, name, role, contactNumber, address, dateOfBirth);
                    showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                            "Staff account created successfully!");
                } else {
                    if (!username.equals(editUser.getUsername())) {
                        boolean usernameUpdated = userManagementService.updateUsername(
                                editUser.getId(), username);

                        if (!usernameUpdated) {
                            showStyledAlert(Alert.AlertType.ERROR, "Error",
                                    "Username already taken. Please choose a different username.");
                            return;
                        }
                    }

                    User updatedUser = userManagementService.updateUserInfo(
                            editUser.getId(), name, email, role, contactNumber, address, dateOfBirth);

                    if (!password.isEmpty()) {
                        if (password.length() < 6) {
                            showStyledAlert(Alert.AlertType.WARNING, "Invalid Password",
                                    "Password must be at least 6 characters long.");
                            return;
                        }
                        userManagementService.resetPassword(editUser.getUsername(), password);
                    }

                    if (updatedUser != null) {
                        showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                                "Staff account updated successfully!");
                    } else {
                        showStyledAlert(Alert.AlertType.ERROR, "Error",
                                "Failed to update staff account.");
                        return;
                    }
                }

                loadStaffData();
                updateSummaryCards();
                dialogStage.close();

            } catch (RuntimeException ex) {
                showStyledAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        buttonBox.getChildren().addAll(cancelButton, saveButton);
        footerBox.getChildren().add(buttonBox);

        // ==================== ASSEMBLED EVERYTHING ====================//
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

        // Apply scrollbar style
        scene.getStylesheets().add("data:text/css," + scrollBarStyle);

        // DatePicker calendar popup CSS
        String datePickerCalendarCSS =
                ".date-picker-popup .month-year-pane .label {" +
                        "    -fx-text-fill: black !important;" +
                        "    -fx-font-weight: bold;" +
                        "}" +
                        ".date-picker-popup .calendar-grid {" +
                        "    -fx-background-color: white;" +
                        "}" +
                        ".date-picker-popup .day-name-cell," +
                        ".date-picker-popup .date-cell {" +
                        "    -fx-text-fill: black;" +
                        "}";

        // Apply date picker calendar style
        scene.getStylesheets().add("data:text/css," + datePickerCalendarCSS);

        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
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
        // Already on staff page
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
            }

            Stage stage = (Stage) staffBtn.getScene().getWindow();
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

            Stage stage = (Stage) staffBtn.getScene().getWindow();
            Scene currentScene = stage.getScene();

            Scene newScene = new Scene(root);

            try {
                java.net.URL cssUrl = getClass().getResource("/css/login.css");
                if (cssUrl != null) {
                    newScene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("✅ CSS LOADED: " + cssUrl.toExternalForm());
                } else {
                    System.out.println("❌ CSS NOT FOUND: /css/styles.css");
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
    
    private boolean showDeleteConfirmation(String title, String message) {
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

        Button confirmButton = new Button("Delete");
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

        buttonBox.getChildren().addAll(cancelButton, confirmButton);
        mainContainer.getChildren().addAll(titleLabel, messageLabel, buttonBox);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();

        return (Boolean) dialogStage.getUserData();
    }
    
    private void showAddCategoryDialog() {
        Stage dialogStage = new Stage();
        IconUtil.setApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Manage Categories");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(25);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 10px;");
        mainContainer.setPrefWidth(500);

        // Title
        Label titleLabel = new Label("Manage Role Categories");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label("Add or remove role categories for staff accounts");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        subtitleLabel.setWrapText(true);

        // Add Category Section
        VBox addCategoryBox = new VBox(10);
        addCategoryBox.setStyle(
                "-fx-background-color: #F8F9FA; " +
                "-fx-padding: 20; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px;"
        );

        Label addLabel = new Label("Add New Category:");
        addLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        TextField categoryField = new TextField();
        categoryField.setPromptText("Enter category name");
        categoryField.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 10px 15px; " +
                "-fx-font-size: 14px;"
        );
        HBox.setHgrow(categoryField, Priority.ALWAYS);

        Button addButton = new Button("Add");
        addButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10px 30px; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand;"
        );

        inputBox.getChildren().addAll(categoryField, addButton);
        addCategoryBox.getChildren().addAll(addLabel, inputBox);

        // Existing Categories Section
        VBox categoriesBox = new VBox(10);
        Label existingLabel = new Label("Existing Categories:");
        existingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-background: transparent; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;"
        );

        VBox categoryList = new VBox(5);
        categoryList.setStyle("-fx-padding: 10; -fx-background-color: white;");

        // Load existing categories
        refreshCategoryList(categoryList);

        scrollPane.setContent(categoryList);
        categoriesBox.getChildren().addAll(existingLabel, scrollPane);

        // Add button action
        addButton.setOnAction(e -> {
            String categoryName = categoryField.getText().trim();
            if (categoryName.isEmpty()) {
                showStyledAlert(Alert.AlertType.WARNING, "Empty Field",
                        "Please enter a category name.");
                return;
            }

            try {
                categoryService.addCategory(categoryName);
                showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                        "Category '" + categoryName + "' added successfully!");
                categoryField.clear();
                refreshCategoryList(categoryList);
            } catch (IllegalArgumentException ex) {
                showStyledAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        // Close button
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Close");
        closeButton.setStyle(
                "-fx-background-color: #2196F3; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12px 30px; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> dialogStage.close());
        buttonBox.getChildren().add(closeButton);

        mainContainer.getChildren().addAll(titleLabel, subtitleLabel, addCategoryBox, categoriesBox, buttonBox);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    private void refreshCategoryList(VBox categoryList) {
        categoryList.getChildren().clear();
        List<Category> categories = categoryService.getAllCategories();

        if (categories.isEmpty()) {
            Label emptyLabel = new Label("No categories yet. Add your first category above.");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            categoryList.getChildren().add(emptyLabel);
        } else {
            for (Category category : categories) {
                HBox categoryItem = new HBox(15);
                categoryItem.setAlignment(Pos.CENTER_LEFT);
                categoryItem.setStyle(
                        "-fx-background-color: #F8F9FA; " +
                        "-fx-padding: 10 15; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 6px;"
                );

                Label nameLabel = new Label(category.getName());
                nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                Button deleteButton = new Button("Delete");
                deleteButton.setStyle(
                        "-fx-background-color: #dc3545; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-padding: 6px 15px; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-cursor: hand;"
                );

                deleteButton.setOnAction(e -> {
                    boolean confirmed = showDeleteConfirmation(
                            "Delete Category",
                            "Are you sure you want to delete the category '" + category.getName() + "'?\n\n" +
                            "This will not affect existing staff accounts with this role."
                    );

                    if (confirmed) {
                        try {
                            categoryService.deleteCategory(category.getId());
                            showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                                    "Category deleted successfully!");
                            refreshCategoryList(categoryList);
                        } catch (Exception ex) {
                            showStyledAlert(Alert.AlertType.ERROR, "Error",
                                    "Failed to delete category: " + ex.getMessage());
                        }
                    }
                });

                categoryItem.getChildren().addAll(nameLabel, deleteButton);
                categoryList.getChildren().add(categoryItem);
            }
        }
    }
}