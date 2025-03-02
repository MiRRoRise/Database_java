package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static final String SUPER_USERNAME = "postgres";
    public static final String SUPER_PASSWORD = "your_password"; // Замените на ваш пароль

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseManager dbManager = new DatabaseManager();
            if (!databaseExists()) {
                DatabaseInitializer.initializeDatabase(SUPER_USERNAME, SUPER_PASSWORD);
                DatabaseInitializer.createFirstAdmin(dbManager);
            }
            showLoginWindow(dbManager);
        });
    }

    private static boolean databaseExists() {
        try (java.sql.Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", SUPER_USERNAME, SUPER_PASSWORD);
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = 'games_db'")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static void showLoginWindow(DatabaseManager dbManager) {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JPanel loginPanel = new JPanel(new GridLayout(2, 2));
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(null, loginPanel, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                dbManager.connect(usernameField.getText(), new String(passwordField.getPassword()));
                GUIManager guiManager = new GUIManager(dbManager);
                guiManager.createAndShowGUI();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Connection failed: " + e.getMessage());
                System.exit(1);
            }
        } else {
            System.exit(0);
        }
    }
}
