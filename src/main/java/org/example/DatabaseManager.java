package org.example;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private Connection conn;
    private String currentRole = "guest";

    public void connect(String username, String password) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/games_db", props);

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT rolname FROM pg_roles WHERE pg_has_role(current_user, oid, 'member')")) {
            while (rs.next()) {
                String role = rs.getString("rolname");
                if ("admin_role".equals(role)) {
                    currentRole = "admin";
                    break;
                } else if ("guest_role".equals(role)) {
                    currentRole = "guest";
                }
            }
        }
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void addGame(int id, String title, String releaseDate, double rating) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("CALL add_game(?,?,?,?)")) {
            stmt.setInt(1, id);
            stmt.setString(2, title);
            stmt.setDate(3, Date.valueOf(releaseDate));
            stmt.setDouble(4, rating);
            stmt.execute();
        }
    }

    public String searchGames(String title) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM search_by_title(?)")) {
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();
            StringBuilder result = new StringBuilder();
            while (rs.next()) {
                result.append(String.format("ID: %d, Title: %s, Date: %s, Rating: %.1f\n",
                        rs.getInt("game_id"), rs.getString("title"), rs.getDate("release_date"), rs.getDouble("rating")));
            }
            rs.close();
            return !result.isEmpty() ? result.toString() : "No results";
        }
    }

    public Game getGameById(int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM games WHERE game_id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Game(
                        rs.getInt("game_id"),
                        rs.getString("title"),
                        rs.getDate("release_date").toString(),
                        rs.getDouble("rating")
                );
            }
            rs.close();
            return null;
        }
    }

    public void updateGame(int id, String title, String releaseDate, double rating) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("CALL update_game(?,?,?,?)")) {
            stmt.setInt(1, id);
            stmt.setString(2, title);
            stmt.setDate(3, Date.valueOf(releaseDate));
            stmt.setDouble(4, rating);
            stmt.execute();
        }
    }

    public int deleteGame(String title) throws SQLException {
        try (CallableStatement stmt = conn.prepareCall("CALL delete_by_title(?, ?)")) {
            stmt.setString(1, title);
            stmt.registerOutParameter(2, Types.INTEGER);
            stmt.execute();
            return stmt.getInt(2);
        }
    }

    public void clearTable() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("CALL clear_games_table()")) {
            stmt.execute();
        }
    }

    public void createUser(String username, String password, String role) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("CALL create_db_user(?,?,?)")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.execute();
        }
    }

    public void deleteDatabase() throws SQLException {
        close();
        Properties props = new Properties();
        props.setProperty("user", Main.SUPER_USERNAME);
        props.setProperty("password", Main.SUPER_PASSWORD);
        try (Connection adminConn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", props);
             Statement stmt = adminConn.createStatement()) {
            stmt.execute("DROP DATABASE games_db");
            stmt.execute("DROP ROLE IF EXISTS admin_role");
            stmt.execute("DROP ROLE IF EXISTS guest_role");
        }
    }

    public record Game(int id, String title, String releaseDate, double rating) {}

}
