
package com.example.votesmartly;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in  = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter    out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Server] Received: " + line);
                String response = processRequest(line);
                out.println(response);

                if (out.checkError()) {
                    System.out.println("[Server] Write error — client may have disconnected.");
                    break;
                }

                System.out.println("[Server] Sent:     " + response);
            }
        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }


    private String processRequest(String json) {
        try {
            String type = extractField(json, "type");
            switch (type) {
                case "GET_ELECTION_STATUS":     return handleGetElectionStatus();
                case "GET_CONSTITUENCIES":      return handleGetConstituencies();
                case "GET_NATIONAL_CANDIDATES": return handleGetNationalCandidates(json);
                case "CAST_NATIONAL_VOTE":      return handleCastNationalVote(json);
                case "GET_DEPARTMENTS":         return handleGetDepartments();
                case "GET_STD_POSTS":           return handleGetStdPosts();
                case "GET_STD_CANDIDATES":      return handleGetStdCandidates(json);
                case "VERIFY_STD_VOTER":        return handleVerifyStdVoter(json);
                case "CAST_STD_VOTE":           return handleCastStdVote(json);
                case "FINISH_STD_VOTING":       return handleFinishStdVoting(json);
                default:
                    return err("Unknown request type: " + escape(type));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return err(e.getMessage() != null ? escape(e.getMessage()) : "Internal server error");
        }
    }


    private String handleGetElectionStatus() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name FROM election_info WHERE election_running=1 LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return "{\"status\":\"SUCCESS\",\"electionRunning\":true,"
                        + "\"electionId\":"        + rs.getInt("id")          + ","
                        + "\"electionName\":\""    + escape(rs.getString("name")) + "\"}";
            }
            return "{\"status\":\"SUCCESS\",\"electionRunning\":false}";
        }
    }



    private String handleGetConstituencies() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT DISTINCT constituency FROM national ORDER BY constituency ASC");
             ResultSet rs = ps.executeQuery()) {
            StringBuilder sb = new StringBuilder("{\"status\":\"SUCCESS\",\"data\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(rs.getString("constituency"))).append("\"");
                first = false;
            }
            return sb.append("]}").toString();
        }
    }

    private String handleGetNationalCandidates(String json) throws SQLException {
        String constituency = extractField(json, "constituency");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, sign FROM candidate_national WHERE constituency=?")) {
            ps.setString(1, constituency);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("{\"status\":\"SUCCESS\",\"data\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"name\":\"").append(escape(rs.getString("name")))
                        .append("\",\"sign\":\"").append(escape(rs.getString("sign"))).append("\"}");
                first = false;
            }
            return sb.append("]}").toString();
        }
    }

    private String handleCastNationalVote(String json) throws SQLException {
        String voterId       = extractField(json, "voterId");
        String constituency  = extractField(json, "constituency");
        String candidateName = extractField(json, "candidateName");
        String givenName     = extractField(json, "givenName");
        String givenDob      = extractField(json, "givenDob");

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {

                PreparedStatement ps = conn.prepareStatement(
                        "SELECT name, birthday, vote_casted FROM voter_national " +
                                "WHERE voter_id=? AND constituency=?");
                ps.setString(1, voterId);
                ps.setString(2, constituency);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    conn.rollback();
                    return "{\"status\":\"ERROR\",\"field\":\"id\","
                            + "\"message\":\"No voter with this ID exists in this constituency!\"}";
                }

                String dbName     = rs.getString("name");
                String dbDob      = rs.getString("birthday");
                int    voteCasted = rs.getInt("vote_casted");

                if (!dbName.equalsIgnoreCase(givenName)) {
                    conn.rollback();
                    return "{\"status\":\"ERROR\",\"field\":\"name\","
                            + "\"message\":\"Your name does not match registered information!\"}";
                }
                if (!dbDob.equalsIgnoreCase(givenDob)) {
                    conn.rollback();
                    return "{\"status\":\"ERROR\",\"field\":\"dob\","
                            + "\"message\":\"Date of birth does not match registered information!\"}";
                }
                if (voteCasted == 1) {
                    conn.rollback();
                    return "{\"status\":\"ERROR\",\"field\":\"id\","
                            + "\"message\":\"This voter has already cast their vote!\"}";
                }

                // Record vote
                PreparedStatement ps1 = conn.prepareStatement(
                        "UPDATE candidate_national SET vote_earned = vote_earned + 1 " +
                                "WHERE name=? AND constituency=?");
                ps1.setString(1, candidateName);
                ps1.setString(2, constituency);
                ps1.executeUpdate();

                PreparedStatement ps2 = conn.prepareStatement(
                        "UPDATE voter_national SET vote_casted=1 WHERE voter_id=?");
                ps2.setString(1, voterId);
                ps2.executeUpdate();

                PreparedStatement ps3 = conn.prepareStatement(
                        "UPDATE national SET vote_casted = vote_casted + 1 WHERE constituency=?");
                ps3.setString(1, constituency);
                ps3.executeUpdate();

                conn.commit();
                return "{\"status\":\"SUCCESS\",\"message\":\"Vote cast successfully!\"}";

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private String handleGetDepartments() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT dept FROM dept_for_std ORDER BY dept_no");
             ResultSet rs = ps.executeQuery()) {
            StringBuilder sb = new StringBuilder("{\"status\":\"SUCCESS\",\"data\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(rs.getString("dept"))).append("\"");
                first = false;
            }
            return sb.append("]}").toString();
        }
    }

    private String handleGetStdPosts() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT posts FROM student ORDER BY post_no");
             ResultSet rs = ps.executeQuery()) {
            StringBuilder sb = new StringBuilder("{\"status\":\"SUCCESS\",\"data\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(rs.getString("posts"))).append("\"");
                first = false;
            }
            return sb.append("]}").toString();
        }
    }

    private String handleGetStdCandidates(String json) throws SQLException {
        String post = extractField(json, "post");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, sign FROM candidate_std WHERE post_for_vote=?")) {
            ps.setString(1, post);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("{\"status\":\"SUCCESS\",\"data\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"name\":\"").append(escape(rs.getString("name")))
                        .append("\",\"sign\":\"").append(escape(rs.getString("sign"))).append("\"}");
                first = false;
            }
            return sb.append("]}").toString();
        }
    }

    private String handleVerifyStdVoter(String json) throws SQLException {
        String stdId = extractField(json, "stdId");
        String name  = extractField(json, "name");
        String dept  = extractField(json, "dept");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, dept, vote_casted FROM voter_std WHERE std_id=?")) {
            ps.setString(1, stdId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return "{\"status\":\"ERROR\",\"field\":\"stdId\","
                        + "\"message\":\"This student ID does not exist!\"}";
            }

            String dbName     = rs.getString("name");
            String dbDept     = rs.getString("dept");
            int    voteCasted = rs.getInt("vote_casted");

            if (!dbName.equalsIgnoreCase(name)) {
                return "{\"status\":\"ERROR\",\"field\":\"name\","
                        + "\"message\":\"Name does not match registered information!\"}";
            }
            if (!dbDept.equalsIgnoreCase(dept)) {
                return "{\"status\":\"ERROR\",\"field\":\"dept\","
                        + "\"message\":\"No student with this ID exists in this department!\"}";
            }

            return "{\"status\":\"SUCCESS\",\"voteCasted\":" + voteCasted + "}";
        }
    }

    private String handleCastStdVote(String json) throws SQLException {
        String stdId         = extractField(json, "stdId");
        String post          = extractField(json, "post");
        String candidateName = extractField(json, "candidateName");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE candidate_std SET vote_earned = vote_earned + 1 " +
                             "WHERE name=? AND post_for_vote=?")) {
            ps.setString(1, candidateName);
            ps.setString(2, post);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                return "{\"status\":\"ERROR\",\"message\":\"Candidate not found for this post.\"}";
            }
            return "{\"status\":\"SUCCESS\"}";
        }
    }

    private String handleFinishStdVoting(String json) throws SQLException {
        String stdId = extractField(json, "stdId");
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE voter_std SET vote_casted=1 WHERE std_id=?");
            ps1.setString(1, stdId);
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE student SET total_casted = total_casted + 1");
            ps2.executeUpdate();

            return "{\"status\":\"SUCCESS\"}";
        }
    }


   public String extractField(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) throw new IllegalArgumentException("Missing field in JSON: " + key);
        int start = idx + search.length();
        if (start >= json.length()) throw new IllegalArgumentException("Field value missing: " + key);
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            if (end < 0) throw new IllegalArgumentException("Unterminated string for field: " + key);
            return json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String err(String message) {
        return "{\"status\":\"ERROR\",\"message\":\"" + message + "\"}";
    }
}
