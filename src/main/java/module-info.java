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

//    Open packages to JavaFX for reflection
    opens oopsystem.app to javafx.graphics;
    opens oopsystem.controller to javafx.fxml;
    opens oopsystem.model to javafx.base;

//    Export entry point
    exports oopsystem.app;
}