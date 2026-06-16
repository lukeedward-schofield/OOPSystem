package oopsystem.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final Dotenv ENV = Dotenv.load();

    private Database(){

    }

    public static Connection getConnection() throws SQLException {

        try{
            Class.forName("org.postgresql.Driver");
        }catch (ClassNotFoundException e){
            throw new RuntimeException("PostgreSQL Driver not found");
        }

        String url = ENV.get("DB_URL");

// Append prepareThreshold=0 to disable server-side prepared statement caching.
// Without this, the PostgreSQL JDBC driver reuses cached statement names (S_1, S_2)
// across connections, causing "prepared statement already exists" errors when
// a connection is reused after a failed transaction.
        if (!url.contains("prepareThreshold")) {
            url += (url.contains("?") ? "&" : "?") + "prepareThreshold=0";
        }

        return DriverManager.getConnection(
                url,
                ENV.get("DB_USER"),
                ENV.get("DB_PASSWORD")
        );

    }
}
