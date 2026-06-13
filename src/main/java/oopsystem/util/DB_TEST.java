package oopsystem.util;

import java.sql.Connection;

public class DB_TEST {

    public static void main(String[] args) {

        try (Connection conn = Database.getConnection()) {

            if (conn != null && !conn.isClosed()) {
                System.out.println("SUPABASE CONNECTED SUCCESSFULLY!");
            } else {
                System.out.println("CONNECTION FAILED");
            }

        } catch (Exception e) {
            System.out.println("ERROR CONNECTING TO DATABASE");
            e.printStackTrace();
        }
    }
}