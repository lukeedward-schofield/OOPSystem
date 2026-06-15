package oopsystem.controller;

import oopsystem.model.User;
import oopsystem.repository.UserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    // --- YOUR TABLES AND COLUMNS INJECTIONS FROM FXML ---
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> departmentColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Void> actionColumn; // Void because it holds custom buttons, not data text

    // The data container linked directly to your UI table view
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1. LINK TEXT DATA: Match table text columns to your User class property names
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // 2. CONNECT DATA CONTAINER: Point your table view to your live list wrapper
        usersTable.setItems(userList);

        // 3. ADD BUTTONS DYNAMICALLY: Build the edit/delete layout rows
        setupActionButtonsColumn();

        // 4. POPULATE DATA: Run database retrieval to fill rows
        loadUsersFromDatabase();
    }

    /**
     * =========================================================================
     * FOCUS AREA: DYNAMICALLY CREATING AND ATTACHING BUTTONS TO EACH ROW
     * =========================================================================
     */
    private void setupActionButtonsColumn() {
        // We set a Cell Factory on the action column to completely override text rendering
        actionColumn.setCellFactory(param -> new TableCell<User, Void>() {

            // Create layout containers and controls once per row pool instance for optimization
            private final HBox layoutContainer = new HBox(10); // 10px spacing between buttons
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                // Optional: Basic styling to make the layout look clean
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");

                // Pack both buttons neatly inside the horizontal HBox row box
                layoutContainer.getChildren().addAll(editBtn, deleteBtn);

                // -------------------------------------------------------------
                // HANDLE CLICK ACTION FOR THE EDIT BUTTON PER ROW
                // -------------------------------------------------------------
                editBtn.setOnAction(event -> {
                    // getIndex() tells JavaFX exactly which row number was interacted with
                    // We pull the complete User object directly from memory via that index
                    User selectedUser = getTableView().getItems().get(getIndex());

                    // You have complete access to the ID and fields here without extra database calls
                    System.out.println("User clicked EDIT on row index: " + getIndex());
                    System.out.println("Extracted Hidden Database ID: " + selectedUser.getUserId());
                    System.out.println("Extracted Target Username: " + selectedUser.getUsername());

                    // TODO: Put your view transition or overlay activation logic here!
                });

                // -------------------------------------------------------------
                // HANDLE CLICK ACTION FOR THE DELETE BUTTON PER ROW
                // -------------------------------------------------------------
                deleteBtn.setOnAction(event -> {
                    User selectedUser = getTableView().getItems().get(getIndex());

                    System.out.println("User clicked DELETE on row index: " + getIndex());
                    System.out.println("Extracted Target ID to drop: " + selectedUser.getUserId());

                    // Example action: Removing the item from your ObservableList
                    // instantly forces the row to vanish from the screen layout smoothly
                    userList.remove(selectedUser);
                });
            }

            // This method controls rendering. It hides components on blank lines so they don't glitch
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null); // Clear graphics completely on empty spacer rows
                } else {
                    setGraphic(layoutContainer); // Inject the buttons container layout on active rows
                }
            }
        });
    }

    /**
     * =========================================================================
     * BACKGROUND PROCESS: POPULATING THE DATA FROM THE REPOSITORY LAYER
     * =========================================================================
     */
    private void loadUsersFromDatabase() {
        // Run database calls in a background Task thread so your user interface stays responsive
        Task<ObservableList<User>> fetchTask = new Task<>() {
            @Override
            protected ObservableList<User> call() throws Exception {
                // Returns an ObservableList populated via the INNER JOIN query from your Repository class
                return userRepository.findAllUsersWithEmployeeDetails();
            }
        };

        // When the database fetching completes, push the final results into your live UI list tracking wrapper
        fetchTask.setOnSucceeded(event -> userList.setAll(fetchTask.getValue()));
        fetchTask.setOnFailed(event -> fetchTask.getException().printStackTrace());

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true); // Closes the background processing thread if the application window is terminated
        thread.start();
    }

    @FXML
    private void goToAddEmployee(){
        SceneNavigator.switchTo("addUserView");
    }
}
