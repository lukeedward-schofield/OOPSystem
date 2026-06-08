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

        return DriverManager.getConnection(
                ENV.get("DB_URL"),
                ENV.get("DB_USER"),
                ENV.get("DB_PASSWORD")
        );

    }
}
