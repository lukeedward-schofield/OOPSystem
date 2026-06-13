package oopsystem.util;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label statOut, statPending, statTotal;
    @FXML private Label statOutNote, statPendingNote, statTotalNote;
    @FXML private Label slipSeries, slipDate, slipEmp, slipTime, slipDest;
    @FXML private Button btnNewEntry;
    @FXML private TableView movementTable;
    @FXML private TableColumn colEmpName, colDept, colReason, colTimeOut, colStatus, colAction;
    @FXML private Label movFooter;

    @Override
    public void initialize(URL u, ResourceBundle r) {
        movementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // TODO: load from DB
    }

    @FXML
    private void handleNewEntry() {
        oopsystem.util.SceneNavigator.switchTo("PassSlipIssuanceView");
    }

    @FXML
    private void handlePrintCopy() {
        // TODO: print logic
    }
}