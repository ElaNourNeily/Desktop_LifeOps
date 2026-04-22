package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.task.TaskSpace;
import service.TaskSpaceService;

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
        card.setPrefWidth(280);
        card.setSpacing(15);
        card.setCursor(Cursor.HAND);

        // Category Tag
        Label categoryTag = new Label(space.getCategory().toUpperCase());
        categoryTag.getStyleClass().add("board-type-tag");
        
        // Mode Label (Small indicator)
        Label modeLbl = new Label(space.getMode());
        modeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-padding: 2 6; -fx-background-color: #1e293b; -fx-background-radius: 4;");
        
        HBox topRow = new HBox(10, categoryTag, modeLbl);
        card.getChildren().add(topRow);

        // Info
        VBox info = new VBox();
        info.setSpacing(5);
        Label title = new Label(space.getNom());
        title.getStyleClass().add("board-title");
        Label statusLbl = new Label(space.getStatus().getValeur().toUpperCase());
        statusLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #8a8d91;");
        
        info.getChildren().addAll(title, statusLbl);
        card.getChildren().add(info);

        card.setOnMouseClicked(event -> openBoard(space));

        return card;
    }

    private void openBoard(TaskSpace space) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Task/board_view.fxml"));
            Parent root = loader.load();
            BoardViewController controller = loader.getController();
            controller.setBoard(space);
            boardGrid.getScene().setRoot(root);
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
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
