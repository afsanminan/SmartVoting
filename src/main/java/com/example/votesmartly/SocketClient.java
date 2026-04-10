



package com.example.votesmartly;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class SocketClient {

    private static String getHost() { return AppConfig.get("server.host"); }
    private static int    getPort() { return AppConfig.getInt("server.port", 5000); }

    public static String sendRequest(String jsonRequest) throws IOException {
        String host = getHost();
        int    port = getPort();

        System.out.println("[SocketClient] HOST = '" + host + "', PORT = " + port);
        System.out.println("[SocketClient] REQUEST = " + jsonRequest);

        if (host == null || host.isBlank()) {
            throw new IOException(
                    "server.host is blank in config.properties. " +
                            "Set it to the server IP (e.g. 192.168.0.110) or 'localhost'.");
        }

        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000); // 5s connect timeout
            socket.setSoTimeout(10000);                              // 10s read timeout
            System.out.println("[SocketClient] Connected to " + host + ":" + port);

            OutputStream rawOut = socket.getOutputStream();
            InputStream  rawIn  = socket.getInputStream();

            // Write request — append newline so server's readLine() unblocks
            byte[] requestBytes = (jsonRequest + "\n").getBytes(StandardCharsets.UTF_8);
            rawOut.write(requestBytes);
            rawOut.flush();
            System.out.println("[SocketClient] Request flushed.");

            // Read response line
            System.out.println("[SocketClient] Waiting for response...");
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = rawIn.read()) != -1) {
                if (c == '\n') break;
                sb.append((char) c);
            }

            String response = sb.toString().trim();
            System.out.println("[SocketClient] Response: " + response);

            if (response.isEmpty()) {
                throw new IOException("Server sent an empty response.");
            }

            return response;

        } catch (ConnectException e) {
            throw new IOException(
                    "Connection refused at " + host + ":" + port +
                            ". Make sure the server application is running.", e);
        } catch (SocketTimeoutException e) {
            throw new IOException(
                    "Connection to " + host + ":" + port + " timed out. " +
                            "Check the IP address and that the server is running.", e);
        } catch (UnknownHostException e) {
            throw new IOException(
                    "Cannot resolve server address '" + host + "'. " +
                            "Check server.host in config.properties.", e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static String getElectionStatus() throws IOException {
        return sendRequest("{\"type\":\"GET_ELECTION_STATUS\"}");
    }

    public static String getElectionHistory() throws IOException {
        return sendRequest("{\"type\":\"GET_HISTORY\"}");
    }

    public static String getConstituencies() throws IOException {
        return sendRequest("{\"type\":\"GET_CONSTITUENCIES\"}");
    }

    public static String getNationalCandidates(String constituency) throws IOException {
        return sendRequest("{\"type\":\"GET_NATIONAL_CANDIDATES\","
                + "\"constituency\":\"" + constituency + "\"}");
    }

    public static String castNationalVote(String voterId, String constituency,
                                          String candidateName,
                                          String givenName, String givenDob) throws IOException {
        return sendRequest("{\"type\":\"CAST_NATIONAL_VOTE\","
                + "\"voterId\":\""       + voterId       + "\","
                + "\"constituency\":\""  + constituency  + "\","
                + "\"candidateName\":\"" + candidateName + "\","
                + "\"givenName\":\""     + givenName     + "\","
                + "\"givenDob\":\""      + givenDob      + "\"}");
    }

    public static String getDepartments() throws IOException {
        return sendRequest("{\"type\":\"GET_DEPARTMENTS\"}");
    }

    public static String getStdPosts() throws IOException {
        return sendRequest("{\"type\":\"GET_STD_POSTS\"}");
    }

    public static String getStdCandidates(String post) throws IOException {
        return sendRequest("{\"type\":\"GET_STD_CANDIDATES\",\"post\":\"" + post + "\"}");
    }

    public static String verifyStdVoter(String stdId, String name, String dept) throws IOException {
        return sendRequest("{\"type\":\"VERIFY_STD_VOTER\","
                + "\"stdId\":\"" + stdId + "\","
                + "\"name\":\""  + name  + "\","
                + "\"dept\":\""  + dept  + "\"}");
    }

    public static String castStdVote(String stdId, String post,
                                     String candidateName) throws IOException {
        return sendRequest("{\"type\":\"CAST_STD_VOTE\","
                + "\"stdId\":\""         + stdId         + "\","
                + "\"post\":\""          + post          + "\","
                + "\"candidateName\":\"" + candidateName + "\"}");
    }

    public static String finishStdVoting(String stdId) throws IOException {
        return sendRequest("{\"type\":\"FINISH_STD_VOTING\",\"stdId\":\"" + stdId + "\"}");
    }
}