package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class GUIManager {
    private final DatabaseManager dbManager;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public GUIManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void createAndShowGUI() {
        JFrame frame = new JFrame("Game Database Manager - " + dbManager.getCurrentRole());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    dbManager.close();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error closing connection: " + ex.getMessage());
                }
            }
        });

        String[] columnNames = {"ID", "Title", "Release Date", "Rating"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        refreshTable("");
        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel(new FlowLayout());
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search by Title");
        searchPanel.add(new JLabel("Search Title:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        frame.add(searchPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
        JButton addButton = new JButton("Add Game");
        JButton updateButton = new JButton("Update Game");
        JButton deleteButton = new JButton("Delete by Title");
        JButton clearButton = new JButton("Clear Table");
        JButton createUserButton = new JButton("Create User");
        JButton deleteDbButton = new JButton("Delete Database");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(createUserButton);
        buttonPanel.add(deleteDbButton);

        frame.add(buttonPanel, BorderLayout.EAST);

        searchButton.addActionListener(_ -> searchGames());
        addButton.addActionListener(_ -> addGame());
        updateButton.addActionListener(_ -> updateGame());
        deleteButton.addActionListener(_ -> deleteGame());
        clearButton.addActionListener(_ -> clearTable());
        createUserButton.addActionListener(_ -> createUser());
        deleteDbButton.addActionListener(_ -> deleteDatabase());

        frame.setVisible(true);
    }

    private void refreshTable(String searchTitle) {
        try {
            tableModel.setRowCount(0);
            String result = dbManager.searchGames(searchTitle);
            if (!result.equals("No results")) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    String[] parts = line.split(", ");
                    String id = parts[0].substring(4);
                    String title = parts[1].substring(7);
                    String date = parts[2].substring(6);
                    String rating = parts[3].substring(8);
                    tableModel.addRow(new Object[]{id, title, date, rating});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error refreshing table: " + e.getMessage());
        }
    }

    private void addGame() {
        if (dbManager.getCurrentRole().equals("guest")) {
            JOptionPane.showMessageDialog(null, "Error: Guests do not have permission to add games.");
            return;
        }
        try {
            String idStr = JOptionPane.showInputDialog("Game ID:");
            if (idStr == null) return;
            int id = Integer.parseInt(idStr);

            String title = JOptionPane.showInputDialog("Title:");
            if (title == null) return;

            String releaseDate = JOptionPane.showInputDialog("Release Date (YYYY-MM-DD):");
            if (releaseDate == null) return;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            try {
                dateFormat.parse(releaseDate);
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(null, "Error: Invalid date format. Use YYYY-MM-DD.");
                return;
            }

            String ratingStr = JOptionPane.showInputDialog("Rating:");
            if (ratingStr == null) return;
            double rating = Double.parseDouble(ratingStr);

            dbManager.addGame(id, title, releaseDate, rating);
            JOptionPane.showMessageDialog(null, "Game added successfully");
            refreshTable(searchField.getText());
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists")) {
                JOptionPane.showMessageDialog(null, "Error: A game with this ID already exists.");
            } else {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Error: Invalid number format");
        }
    }

    private void searchGames() {
        String searchTitle = searchField.getText();
        refreshTable(searchTitle);
    }

    private void updateGame() {
        if (dbManager.getCurrentRole().equals("guest")) {
            JOptionPane.showMessageDialog(null, "Error: Guests do not have permission to update games.");
            return;
        }
        try {
            String idStr = JOptionPane.showInputDialog("Enter Game ID to update:");
            if (idStr == null) return;
            int id = Integer.parseInt(idStr);

            DatabaseManager.Game currentGame = dbManager.getGameById(id);
            if (currentGame == null) {
                JOptionPane.showMessageDialog(null, "Game with ID " + id + " not found");
                return;
            }

            JTextField titleField = new JTextField(currentGame.title(), 15);
            JTextField dateField = new JTextField(currentGame.releaseDate(), 15);
            JTextField ratingField = new JTextField(String.valueOf(currentGame.rating()), 15);
            JPanel panel = new JPanel(new GridLayout(3, 2));
            panel.add(new JLabel("Title (leave empty to keep unchanged):"));
            panel.add(titleField);
            panel.add(new JLabel("Release Date (YYYY-MM-DD, leave empty to keep unchanged):"));
            panel.add(dateField);
            panel.add(new JLabel("Rating (leave empty to keep unchanged):"));
            panel.add(ratingField);

            int result = JOptionPane.showConfirmDialog(null, panel, "Update Game", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) return;

            String newTitle = titleField.getText().isEmpty() ? currentGame.title() : titleField.getText();
            String newDate = dateField.getText().isEmpty() ? currentGame.releaseDate() : dateField.getText();
            if (!newDate.equals(currentGame.releaseDate())) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateFormat.setLenient(false);
                try {
                    dateFormat.parse(newDate);
                } catch (ParseException e) {
                    JOptionPane.showMessageDialog(null, "Error: Invalid date format. Use YYYY-MM-DD.");
                    return;
                }
            }
            double newRating = ratingField.getText().isEmpty() ? currentGame.rating() : Double.parseDouble(ratingField.getText());

            dbManager.updateGame(id, newTitle, newDate, newRating);
            JOptionPane.showMessageDialog(null, "Game updated successfully");
            refreshTable(searchField.getText());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Error: Invalid number format");
        }
    }

    private void deleteGame() {
        if (dbManager.getCurrentRole().equals("guest")) {
            JOptionPane.showMessageDialog(null, "Error: Guests do not have permission to delete games.");
            return;
        }
        try {
            String title = JOptionPane.showInputDialog("Title to delete:");
            if (title == null) return;
            int rowsDeleted = dbManager.deleteGame(title);
            if (rowsDeleted > 0) {
                JOptionPane.showMessageDialog(null, "Game deleted successfully");
                refreshTable(searchField.getText());
            } else {
                JOptionPane.showMessageDialog(null, "No game found with title: " + title);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private void clearTable() {
        if (dbManager.getCurrentRole().equals("guest")) {
            JOptionPane.showMessageDialog(null, "Error: Guests do not have permission to clear the table.");
            return;
        }
        try {
            dbManager.clearTable();
            JOptionPane.showMessageDialog(null, "Table cleared successfully");
            refreshTable(searchField.getText());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private void createUser() {
        if (dbManager.getCurrentRole().equals("guest")) {
            JOptionPane.showMessageDialog(null, "Error: Guests do not have permission to create users.");
            return;
        }
        try {
            String username = JOptionPane.showInputDialog("New username:");
            if (username == null) return;

            String password = JOptionPane.showInputDialog("Password:");
            if (password == null) return;

            String role = JOptionPane.showInputDialog("Role (admin/guest):");
            if (role == null) return;

            dbManager.createUser(username, password, role);
            JOptionPane.showMessageDialog(null, "User created successfully");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private void deleteDatabase() {
        if (dbManager.getCurrentRole().equals("guest")) {
            JOptionPane.showMessageDialog(null, "Error: Guests do not have permission to delete the database.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(null,
                "Are you sure? The app will close.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dbManager.deleteDatabase(); // Используем фиксированные данные из DatabaseManager
                JOptionPane.showMessageDialog(null, "Database deleted. Restart the app.");
                System.exit(0);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        }
    }
}