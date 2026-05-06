package controller.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import Model.User;
import service.user.Userservice;
import utils.Session;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

public class BackOfficeController {

    @FXML
    private ListView<User> userListView;
    @FXML
    private Label adminNameLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private ComboBox<String> filterRoleComboBox;

    private Userservice userservice = new Userservice();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    @FXML
    public void initialize() {
        User currentAdmin = Session.getInstance().getCurrentUser();
        if (currentAdmin != null) {
            adminNameLabel.setText("Admin: " + currentAdmin.getPrenom() + " " + currentAdmin.getNom());
        }

        // Initialize sorting and filtering options
        sortComboBox.setItems(FXCollections.observableArrayList("Nom (A-Z)", "Nom (Z-A)", "Plus actifs", "Moins actifs"));
        filterRoleComboBox.setItems(FXCollections.observableArrayList("Tous les rôles", "ROLE_ADMIN", "ROLE_USER"));
        filterRoleComboBox.setValue("Tous les rôles");

        userListView.setCellFactory(param -> new UserListCell());
        loadUsers();

        setupFilteringAndSorting();
    }

    private void loadUsers() {
        try {
            userList.setAll(userservice.findAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupFilteringAndSorting() {
        filteredData = new FilteredList<>(userList, p -> true);

        // Search filter logic
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter();
        });

        // Role filter logic
        filterRoleComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter();
        });

        SortedList<User> sortedData = new SortedList<>(filteredData);

        // Sorting logic
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;

            Comparator<User> comparator = null;
            switch (newValue) {
                case "Nom (A-Z)":
                    comparator = Comparator.comparing(User::getNom);
                    break;
                case "Nom (Z-A)":
                    comparator = Comparator.comparing(User::getNom).reversed();
                    break;
                case "Plus actifs":
                    comparator = Comparator.comparing(User::getTotalConnectionTime).reversed();
                    break;
                case "Moins actifs":
                    comparator = Comparator.comparing(User::getTotalConnectionTime);
                    break;
            }
            sortedData.setComparator(comparator);
        });

        userListView.setItems(sortedData);
    }

    private void updateFilter() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String roleFilter = filterRoleComboBox.getValue();

        filteredData.setPredicate(user -> {
            // Search filter
            boolean matchesSearch = user.getNom().toLowerCase().contains(searchText) ||
                    user.getPrenom().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText);

            // Role filter
            boolean matchesRole = roleFilter == null || roleFilter.equals("Tous les rôles") ||
                    user.getRole().equals(roleFilter);

            return matchesSearch && matchesRole;
        });
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        User currentUser = Session.getInstance().getCurrentUser();
        LocalDateTime loginTime = Session.getInstance().getLoginTime();

        if (currentUser != null && loginTime != null) {
            long seconds = java.time.Duration.between(loginTime, LocalDateTime.now()).getSeconds();
            currentUser.setTotalConnectionTime(currentUser.getTotalConnectionTime() + seconds);
            try {
                userservice.updateConnectionTime(currentUser);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Session.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/login.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class UserListCell extends ListCell<User> {
        private HBox hBox = new HBox();
        private VBox infoBox = new VBox();
        private Label nameLabel = new Label();
        private Label emailLabel = new Label();
        private Label statusLabel = new Label();
        private Button banBtn = new Button("Bannir");
        private Button unbanBtn = new Button("Débannir");
        private HBox btnBox = new HBox();

        public UserListCell() {
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setPadding(new Insets(10, 15, 10, 15));
            hBox.setSpacing(20);
            hBox.setStyle("-fx-background-color: #1e2126; -fx-background-radius: 10; -fx-border-color: #3a3d42; -fx-border-radius: 10;");

            nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            emailLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            statusLabel.setStyle("-fx-font-size: 12px;");

            infoBox.getChildren().addAll(nameLabel, emailLabel, statusLabel);
            infoBox.setSpacing(5);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            banBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5 15;");
            unbanBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5 15;");

            btnBox.setSpacing(10);
            btnBox.setAlignment(Pos.CENTER_RIGHT);
            btnBox.getChildren().addAll(banBtn, unbanBtn);

            hBox.getChildren().addAll(infoBox, btnBox);

            banBtn.setOnAction(e -> {
                User u = getItem();
                if (u != null) {
                    try {
                        userservice.ban(u);
                        loadUsers();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            unbanBtn.setOnAction(e -> {
                User u = getItem();
                if (u != null) {
                    try {
                        userservice.unban(u);
                        loadUsers();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(user.getPrenom() + " " + user.getNom() + " (" + user.getRole() + ")");
                emailLabel.setText(user.getEmail() + " | Tél: " + user.getTelephone());

                if (user.getBanUntil() != null && user.getBanUntil().isAfter(LocalDateTime.now())) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                    statusLabel.setText("BANNI jusqu'au " + user.getBanUntil().format(formatter));
                    statusLabel.setStyle("-fx-text-fill: #ef4444;");
                    banBtn.setDisable(true);
                    unbanBtn.setDisable(false);
                } else {
                    statusLabel.setText("ACTIF | Connecté: " + formatDuration(user.getTotalConnectionTime()));
                    statusLabel.setStyle("-fx-text-fill: #10b981;");
                    banBtn.setDisable(false);
                    unbanBtn.setDisable(true);
                }

                setGraphic(hBox);
                setStyle("-fx-background-color: transparent; -fx-padding: 5;");
            }
        }

        private String formatDuration(long seconds) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            if (h > 0) return h + "h " + m + "m";
            return m + " min";
        }
    }
}
