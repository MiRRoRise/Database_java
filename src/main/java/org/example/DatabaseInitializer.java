package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DatabaseInitializer {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    private static final String DEFAULT_DB = "postgres";
    private static final String GAMES_DB = "games_db";

    public static void initializeDatabase(String superUsername, String superPassword) {
        try (Connection adminConn = DriverManager.getConnection(DB_URL + DEFAULT_DB, superUsername, superPassword);
             Statement stmt = adminConn.createStatement()) {
            stmt.execute("CREATE DATABASE " + GAMES_DB);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database creation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void createFirstAdmin(DatabaseManager dbManager) {
        try (Connection conn = DriverManager.getConnection(DB_URL + GAMES_DB, Main.SUPER_USERNAME, Main.SUPER_PASSWORD)) {
            loadAndExecuteSQLScript(conn);

            JTextField usernameField = new JTextField(15);
            JPasswordField passwordField = new JPasswordField(15);
            JPanel panel = new JPanel(new GridLayout(2, 2));
            panel.add(new JLabel("Admin Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Admin Password:"));
            panel.add(passwordField);

            int result = JOptionPane.showConfirmDialog(null, panel, "Create First Administrator", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                JOptionPane.showMessageDialog(null, "Initial admin required. Restart the app.");
                System.exit(1);
            }

            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            try (PreparedStatement stmt = conn.prepareStatement("CALL create_db_user(?,?,?)")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, "admin");
                stmt.execute();

                try (Statement grantStmt = conn.createStatement()) {
                    grantStmt.execute("ALTER ROLE " + username + " CREATEROLE INHERIT");
                    grantStmt.execute("GRANT admin_role TO " + username + " WITH ADMIN OPTION");
                    grantStmt.execute("GRANT guest_role TO " + username + " WITH ADMIN OPTION");
                }
                JOptionPane.showMessageDialog(null, "Admin '" + username + "' created with full privileges.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Failed to create first admin: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void loadAndExecuteSQLScript(Connection conn) throws SQLException {
        try (InputStream is = DatabaseInitializer.class.getClassLoader().getResourceAsStream("init.sql")) {
            if (is == null) throw new SQLException("SQL script not found: " + "init.sql");
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to execute SQL script: " + e.getMessage(), e);
        }
    }
}