package Controller.task;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.task.TaskSpace;
import service.task.TaskSpaceService;
import Controller.user.MainLayoutController;

import java.io.IOException;
import java.util.Comparator;

public class BoardHubController {

    @FXML private FlowPane boardGrid;
    @FXML private TextField searchField;
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterPersonal;
    @FXML private Button btnFilterTeam;

    private final TaskSpaceService spaceService = new TaskSpaceService();
    private ObservableList<TaskSpace> masterData = FXCollections.observableArrayList();
    private FilteredList<TaskSpace> filteredData;
    private String currentModeFilter = null; // null = ALL, "Solo", "Equipe"

    @FXML
    public void initialize() {
        // 1. Setup Data
        filteredData = new FilteredList<>(masterData, p -> true);
        
        // 2. Setup Sorted List
        SortedList<TaskSpace> sortedData = new SortedList<>(filteredData);
        sortedData.setComparator(Comparator.comparing(TaskSpace::getNom));

        // 3. Bind Search Listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilters();
        });

        loadBoards();
    }

    public void loadBoards() {
        masterData.setAll(spaceService.readAll());
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        
        filteredData.setPredicate(space -> {
            // Mode Filter (Solo / Equipe)
            boolean matchesMode = (currentModeFilter == null) || space.getMode().equalsIgnoreCase(currentModeFilter);
            
            // Text Search (Name / Description / Category)
            boolean matchesSearch = searchText.isEmpty() || 
                                    space.getNom().toLowerCase().contains(searchText) || 
                                    (space.getDescription() != null && space.getDescription().toLowerCase().contains(searchText)) ||
                                    space.getCategory().toLowerCase().contains(searchText);
            
            return matchesMode && matchesSearch;
        });

        renderBoards();
    }

    private void renderBoards() {
        boardGrid.getChildren().clear();
        for (TaskSpace space : filteredData) {
            boardGrid.getChildren().add(createBoardCard(space));
        }
    }

    @FXML
    private void handleFilterAll(ActionEvent event) {
        updateActiveFilter(btnFilterAll);
        currentModeFilter = null;
        applyFilters();
    }

    @FXML
    private void handleFilterPersonal(ActionEvent event) {
        updateActiveFilter(btnFilterPersonal);
        currentModeFilter = "Solo";
        applyFilters();
    }

    @FXML
    private void handleFilterTeam(ActionEvent event) {
        updateActiveFilter(btnFilterTeam);
        currentModeFilter = "Equipe";
        applyFilters();
    }

    private void updateActiveFilter(Button activeBtn) {
        btnFilterAll.getStyleClass().remove("filter-btn-active");
        btnFilterPersonal.getStyleClass().remove("filter-btn-active");
        btnFilterTeam.getStyleClass().remove("filter-btn-active");
        
        if (!btnFilterAll.getStyleClass().contains("filter-btn")) btnFilterAll.getStyleClass().add("filter-btn");
        if (!btnFilterPersonal.getStyleClass().contains("filter-btn")) btnFilterPersonal.getStyleClass().add("filter-btn");
        if (!btnFilterTeam.getStyleClass().contains("filter-btn")) btnFilterTeam.getStyleClass().add("filter-btn");

        activeBtn.getStyleClass().add("filter-btn-active");
    }

    private VBox createBoardCard(TaskSpace space) {
        VBox card = new VBox();
        card.getStyleClass().add("board-card");
        card.setPrefWidth(260);
        card.setMinWidth(220);
        card.setSpacing(14);
        card.setCursor(Cursor.HAND);

        // ── Top row: category pill + mode pill ──────────────
        Label categoryTag = new Label(space.getCategory().toUpperCase());
        categoryTag.getStyleClass().add("board-type-tag");

        boolean isTeam = "Equipe".equalsIgnoreCase(space.getMode());
        Label modeLbl = new Label(isTeam ? "Équipe" : "Solo");
        modeLbl.setStyle(
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-text-fill: " + (isTeam ? "#60a5fa" : "#a78bfa") + ";" +
            "-fx-padding: 2 8;" +
            "-fx-background-color: " + (isTeam ? "rgba(59,130,246,0.12)" : "rgba(139,92,246,0.12)") + ";" +
            "-fx-background-radius: 999;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, categoryTag, spacer, modeLbl);
        topRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(topRow);

        // ── Middle: title + description ──────────────────────
        VBox info = new VBox(6);
        Label title = new Label(space.getNom());
        title.getStyleClass().add("board-title");
        title.setWrapText(true);

        String desc = space.getDescription();
        if (desc != null && !desc.isBlank()) {
            Label descLbl = new Label(desc.length() > 60 ? desc.substring(0, 60) + "…" : desc);
            descLbl.setStyle("-fx-text-fill: #6a6d71; -fx-font-size: 11px;");
            descLbl.setWrapText(true);
            info.getChildren().addAll(title, descLbl);
        } else {
            info.getChildren().add(title);
        }
        card.getChildren().add(info);

        // ── Divider ──────────────────────────────────────────
        javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
        divider.setPrefHeight(1);
        divider.setMinHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: #2a2d32;");
        card.getChildren().add(divider);

        // ── Bottom row: status dot + label ───────────────────
        String statusVal = space.getStatus().getValeur().toUpperCase();
        boolean isActive = "ACTIVE".equalsIgnoreCase(space.getStatus().getValeur());
        Label statusDot = new Label("●");
        statusDot.setStyle("-fx-text-fill: " + (isActive ? "#34d399" : "#6a6d71") + "; -fx-font-size: 9px;");
        Label statusLbl = new Label(statusVal);
        statusLbl.setStyle("-fx-text-fill: " + (isActive ? "#34d399" : "#6a6d71") + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox bottomRow = new HBox(6, statusDot, statusLbl);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(bottomRow);

        card.setOnMouseClicked(event -> openBoard(space));
        return card;
    }

    private void openBoard(TaskSpace space) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/board_view.fxml"));
            Parent root = loader.load();
            BoardViewController boardController = loader.getController();
            boardController.setBoard(space);
            MainLayoutController.getInstance().loadContent(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateBoard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/create_board_popup.fxml"));
            Parent root = loader.load();
            CreateBoardController controller = loader.getController();
            controller.setHubController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            
            final double[] xOffset = {0};
            final double[] yOffset = {0};
            root.setOnMousePressed(e -> {
                xOffset[0] = e.getSceneX();
                yOffset[0] = e.getSceneY();
            });
            root.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - xOffset[0]);
                stage.setY(e.getScreenY() - yOffset[0]);
            });

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
