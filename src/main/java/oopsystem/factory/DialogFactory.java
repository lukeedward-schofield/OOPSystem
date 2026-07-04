package oopsystem.factory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import oopsystem.util.SceneNavigator;

public class DialogFactory {

    public static void showPermissionDialog(Scene scene){

        Parent originalRoot = scene.getRoot();

        StackPane newParentStackRoot = new StackPane();

        scene.setRoot(new StackPane());

        StackPane darkOverlaySheet = new StackPane();
        darkOverlaySheet.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        darkOverlaySheet.setAlignment(Pos.CENTER);

//      MAIN DIALOG FRAME
        VBox dialog = new VBox();
        dialog.setStyle("-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 0);");
        dialog.setMaxSize(320, 500);
        dialog.setAlignment(Pos.CENTER);

        // 2. Header Section
        Label headerTitle = new Label("PUP Data Privacy Verification");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerTitle.setStyle("-fx-text-fill: #800000;");
        
        // 3. FACULTY SECTION: The Read-Aloud Script Box
        VBox scriptContainer = new VBox(8);
        scriptContainer.setStyle("-fx-background-color: #FFF9C4; " + // Soft warning yellow
                "-fx-padding: 15px; " +
                "-fx-background-radius: 5px; " +
                "-fx-border-color: #FBC02D; " +
                "-fx-border-width: 1px;");

        Label scriptLabel = new Label("📣 FACULTY MANDATORY READ-ALOUD REMINDER:");
        scriptLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        scriptLabel.setStyle("-fx-text-fill: #5D4037;");

        Text scriptText = new Text(
                "\"Please listen carefully before we proceed. To activate your employee profile, " +
                        "I need to record your personal information into the University network. " +
                        "This information will be managed securely under the Polytechnic University of the Philippines " +
                        "Privacy Statement. Do you grant permission to process your data?\""
        );
        scriptText.setFont(Font.font("System", 14));
        scriptText.setWrappingWidth(530);
        scriptContainer.getChildren().addAll(scriptLabel, scriptText);

        // 4. EMPLOYEE SECTION: Scrollable legal definitions directly mapped from pup.edu.ph/privacy/
        VBox technicalContainer = new VBox(8);
        Label implicationsLabel = new Label("📋 DATA PROCESSING IMPLICATIONS FOR THE EMPLOYEE:");
        implicationsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        implicationsLabel.setStyle("-fx-text-fill: #424242;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefHeight(180);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #FAFAFA;");

        TextFlow technicalTextFlow = new TextFlow();
        technicalTextFlow.setPadding(new Insets(10));
        technicalTextFlow.setLineSpacing(6);

        Text legalText = new Text(
                "By logging this record, the PUP HRIS and associated services will process the following items:\n\n" +
                        "🔑 Credentials & Identity:\n" +
                        "Collection of first, middle, and last names (validated via Philippine Statistics Office birth certificates) " +
                        "alongside employee numbers and system hint answers used exclusively for network authentication.\n\n" +
                        "📊 Demographics & Records:\n" +
                        "Processing of birthdates, sex, occupation, and tracking details to customize institutional services and metrics.\n\n" +
                        "💳 Financial & Payment Data:\n" +
                        "Transactional storage managed via LANDBANK's Electronic Payment System to facilitate payroll allocations safely.\n\n" +
                        "🌐 Connectivity & Footprint Logs:\n" +
                        "Automated telemetry recording of system usage, device identifiers, IP addresses, browser variants, " +
                        "and technical diagnostic crash information to guard the network perimeter from unauthorized changes."
        );
        legalText.setFont(Font.font("System", 13));
        legalText.setWrappingWidth(550);
        technicalTextFlow.getChildren().add(legalText);
        scrollPane.setContent(technicalTextFlow);
        technicalContainer.getChildren().addAll(implicationsLabel, scrollPane);

        Button cancelBtn = new Button("Reject");
        Button approveBtn = new Button("Approve");

        cancelBtn.setOnAction(event -> {
            newParentStackRoot.getChildren().clear();
            scene.setRoot(originalRoot);
            SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
        });

        approveBtn.setOnAction(event -> {
            newParentStackRoot.getChildren().clear();

            scene.setRoot(originalRoot);
        });




        HBox actionRow = new HBox(15, approveBtn, cancelBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(headerTitle, scriptContainer, technicalContainer, actionRow);
        darkOverlaySheet.getChildren().add(dialog);

        newParentStackRoot.getChildren().addAll(originalRoot, darkOverlaySheet);
        scene.setRoot(newParentStackRoot);
    }

}
