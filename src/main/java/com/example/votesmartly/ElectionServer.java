


package com.example.votesmartly;

import java.io.*;
import java.net.*;
public class ElectionServer {
    //private static final long serialVersionUID = 1L;
    private static final int PORT = 5000;
    private static volatile boolean      running      = false;
    private static volatile ServerSocket serverSocket = null;

    public static synchronized void start() {
        if (running) {
            System.out.println("[Server] Already running — ignoring duplicate start().");
            return;
        }
        running = true;

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress("0.0.0.0", PORT));

                System.out.println("[Server] ✓ Election Server started on port " + PORT);
                System.out.println("[Server] LAN IP addresses — give one to the client machine:");

                try {
                    NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(ni -> {
                        ni.getInetAddresses().asIterator().forEachRemaining(addr -> {
                            if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                                System.out.println("[Server]   server.host=" + addr.getHostAddress());
                            }
                        });
                    });
                } catch (Exception e) {
                    System.out.println("[Server] Could not enumerate IPs: " + e.getMessage());
                }

                System.out.println("[Server] Waiting for client connections...");

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("[Server] Client connected from "
                                + clientSocket.getInetAddress().getHostAddress());
                        Thread t = new Thread(new ClientHandler(clientSocket));
                        t.setDaemon(true);
                        t.start();
                    } catch (IOException e) {
                        if (running) {
                            System.out.println("[Server] Accept error: " + e.getMessage());
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("[Server] FATAL — could not start server: " + e.getMessage());
                e.printStackTrace();
                running = false;
            }
        }, "ElectionServerThread");

        //serverThread.setDaemon(true);
        serverThread.start();
    }

    public static synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        System.out.println("[Server] Stopped.");
    }

    public static boolean isRunning() {
        return running;
    }
}
