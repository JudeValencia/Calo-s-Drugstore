package com.inventory.Calo.s_Drugstore.controller;

import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.service.UserManagementService;
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

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Controller
public class StaffController implements Initializable {

    @Autowired
    private UserManagementService userManagementService;

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
        updateSummaryCards();
        setActiveButton(staffBtn);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userEmailLabel.setText(user.getEmail());
        }
    }

    private void setupStaffTable() {

        // CRITICAL: Set fixed cell size to prevent cell recycling issues
        staffTable.setFixedCellSize(70);

        // Prevent table from losing items on scroll
        //staffTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Ensure table maintains its items
        staffTable.setEditable(true);

        staffIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));

        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));
        nameColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    setText(null);
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
                Label avatarIcon = new Label("👤");
                avatarIcon.setStyle("-fx-font-size: 20px;");
                avatar.getChildren().add(avatarIcon);

                Label nameLabel = new Label(user.getFullName());
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                container.getChildren().addAll(avatar, nameLabel);
                setGraphic(container);
            }
        });

        contactColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    setText(null);
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

                container.getChildren().addAll(emailBox, usernameBox);
                setGraphic(container);
            }
        });
        contactColumn.setCellValueFactory(data -> new SimpleStringProperty(""));

        roleColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    setText(null);
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
            }
        });
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(""));

        statusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                User user = getTableRow() != null ? getTableRow().getItem() : null;
                if (user == null) {
                    setGraphic(null);
                    setText(null);
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
            }
        });
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(""));

        createdDateColumn.setCellValueFactory(data -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
            return new SimpleStringProperty(data.getValue().getCreatedAt().format(formatter));
        });

        actionsColumn.setCellFactory(column -> new TableCell<User, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
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

                // Edit button
                Button editBtn = new Button("🔧");
                editBtn.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 20px; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-background-radius: 6px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-width: 1.5px; " +
                                "-fx-border-radius: 6px; " +
                                "-fx-cursor: hand;"
                );
                editBtn.setOnAction(e -> handleEditStaff(user));

                // Activate/Deactivate button
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

                // Only show delete button for non-admin users or if current user is different
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
                                    "-fx-border-width: 1.5px; " +
                                    "-fx-border-radius: 6px; " +
                                    "-fx-cursor: hand;"
                    );
                    deleteBtn.setOnAction(e -> handleDeleteStaff(user));
                    buttons.getChildren().add(deleteBtn);
                }

                setGraphic(buttons);
            }
        });
    }

    private void loadStaffData() {
        List<User> users = userManagementService.getAllUsers();
        staffList.setAll(users);
        staffTable.setItems(staffList);

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

    @FXML
    private void handleAddStaff() {
        showAddEditStaffDialog(null);
    }

    private void handleEditStaff(User user) {
        showAddEditStaffDialog(user);
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
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(isEdit ? "Edit Staff Account" : "Add New Staff Account");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-padding: 30;");
        mainContainer.setPrefWidth(550);

        Label titleLabel = new Label(isEdit ? "Edit Staff Account" : "Add New Staff Account");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

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
            usernameField.setDisable(true); // Cannot change username
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

        // Password (only for new users)
        VBox passwordBox = new VBox(8);
        if (!isEdit) {
            Label passwordLabel = new Label("Password *");
            passwordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Enter password (min 6 characters)");
            passwordField.setStyle(nameField.getStyle());
            passwordBox.getChildren().addAll(passwordLabel, passwordField);
            passwordBox.setUserData(passwordField);
        }

        // Role
        VBox roleBox = new VBox(8);
        Label roleLabel = new Label("Role *");
        roleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("STAFF", "ADMIN");
        roleCombo.setValue(isEdit ? editUser.getRole() : "STAFF");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setStyle(nameField.getStyle());
        roleBox.getChildren().addAll(roleLabel, roleCombo);

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
            String role = roleCombo.getValue();

            if (name.isEmpty() || email.isEmpty() || username.isEmpty()) {
                showStyledAlert(Alert.AlertType.WARNING, "Missing Information",
                        "Please fill in all required fields.");
                return;
            }

            try {
                if (!isEdit) {
                    PasswordField passwordField = (PasswordField) passwordBox.getUserData();
                    String password = passwordField != null ? passwordField.getText() : "";
                    if (password.isEmpty() || password.length() < 6) {
                        showStyledAlert(Alert.AlertType.WARNING, "Invalid Password",
                                "Password must be at least 6 characters long.");
                        return;
                    }

                    userManagementService.createStaffAccount(username, email, password, name, role);
                    showStyledAlert(Alert.AlertType.INFORMATION, "Success",
                            "Staff account created successfully!");
                } else {
                    User updatedUser = userManagementService.updateUserInfo(
                            editUser.getId(), name, email, role);

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

        mainContainer.getChildren().addAll(titleLabel, nameBox, usernameBox, emailBox);
        if (!isEdit) mainContainer.getChildren().add(passwordBox);
        mainContainer.getChildren().addAll(roleBox, buttonBox);

        Scene scene = new Scene(mainContainer);
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
        navigateToPage("/fxml/dashboard.fxml", "/css/dashboard.css");
    }

    @FXML
    private void handleInventory() {
        navigateToPage("/fxml/inventory.fxml", "/css/inventory.css");
    }

    @FXML
    private void handleSales() {
        navigateToPage("/fxml/sales.fxml", "/css/sales.css");
    }

    @FXML
    private void handleReports() {
        navigateToPage("/fxml/reports.fxml", "/css/reports.css");
    }

    @FXML
    private void handleStaff() {
        // Already on staff page
    }

    @FXML
    private void handleLogout() {
        boolean confirmed = showLogoutConfirmation();
        if (confirmed) {
            performLogout();
        }
    }

    private boolean showLogoutConfirmation() {
        Stage dialogStage = new Stage();
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
}