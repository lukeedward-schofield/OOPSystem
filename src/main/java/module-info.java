module oopsystem {
//    javaFX modules
    requires javafx.controls;
    requires javafx.fxml;

//    MySQL JDBC
    requires java.sql;
    requires io.github.cdimascio.dotenv.java;

//    Extra UI libraries
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.postgresql.jdbc;
    requires jbcrypt;

    // =========================================================================
    // ADD THESE TWO LINES FOR THE FXML SNAPSHOT PDF FEATURE
    // =========================================================================
    requires javafx.swing;  // Grants permission to utilize SwingFXUtils
    requires java.desktop;  // Grants permission to utilize java.awt.Desktop (Auto-Open feature)
    // =========================================================================


//    Open packages to JavaFX for reflection
    opens oopsystem.app to javafx.graphics;
    opens oopsystem.model to javafx.base;

//    Export entry point
    exports oopsystem.app;
    opens oopsystem.controller.employeeDirectory to javafx.fxml;
    opens oopsystem.controller.profile to javafx.fxml;
    opens oopsystem.controller.dashboard to javafx.fxml;
    opens oopsystem.controller.login to javafx.fxml;
    opens oopsystem.controller.movementLog to javafx.fxml;
    opens oopsystem.components to javafx.fxml;
    opens oopsystem.controller.passSlipIssuance to javafx.fxml;
    opens oopsystem.controller.reportsAnalytics to javafx.fxml;
}