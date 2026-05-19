module oopsystem {
//    javaFX modules
    requires javafx.controls;
    requires javafx.fxml;

//    MySQL JDBC
    requires java.sql;

//    Extra UI libraries
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

//    Open packages to JavaFX for reflection
    opens oopsystem.app to javafx.graphics;


//    Export entry point
    exports oopsystem.app;
}