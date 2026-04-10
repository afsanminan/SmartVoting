//package com.example.votesmartly;
//
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//
///**
// * Entry point for the CLIENT (voter booth) machine.
// *
// * Key differences from VotingApplication (server):
// *   ✗  Does NOT check for a database
// *   ✗  Does NOT start ElectionServer
// *   ✓  Opens clientOpening.fxml and communicates via SocketClient
// *
// * Run this class on the client machine, NOT VotingApplication.
// * If you run VotingApplication on the client it starts its own server
// * on port 5000 — then SocketClient connects to THAT local server
// * (which has no data) instead of the real server. That is why nothing works.
// */
//public class ClientApplication extends Application {
//
//    @Override
//    public void start(Stage stage) throws Exception {
//        String host = AppConfig.get("server.host");
//        int    port = AppConfig.getInt("server.port", 5000);
//
//        System.out.println("[Client] Starting. Will connect to " + host + ":" + port);
//
//        if (host == null || host.isBlank()) {
//            System.err.println("[Client] ERROR: server.host is not set in config.properties!");
//            System.err.println("[Client]        Add:  server.host=<IP of server machine>");
//            System.exit(1);
//        }
//
//        FXMLLoader loader = new FXMLLoader(getClass().getResource(
//                "com/example/votesmartly/clientOpening.fxml"));
//        stage.setScene(new Scene(loader.load()));
//        stage.setTitle("VoteSmartly — Client");
//        stage.show();
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}



package com.example.votesmartly;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point for the CLIENT (voter booth) machine.
 *
 * Key differences from VotingApplication (server):
 *   ✗  Does NOT check for a database
 *   ✗  Does NOT start ElectionServer
 *   ✓  Opens clientOpening.fxml and communicates via SocketClient
 *
 * Run this class on the client machine (via Launcher), NOT VotingApplication.
 */
public class   ClientApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        String host = AppConfig.get("server.host");
        int    port = AppConfig.getInt("server.port", 5000);

        System.out.println("[Client] Starting. Will connect to " + host + ":" + port);

        if (host == null || host.isBlank()) {
            System.err.println("[Client] ERROR: server.host is not set in config.properties!");
            System.err.println("[Client]        Add:  server.host=<IP of server machine>");
            System.exit(1);
        }

        // FIXED: use getClass().getResource() without the full package path prefix —
        // because this class is already IN com.example.votesmartly, the resource
        // path is relative to that package in the classpath.
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("clientOpening.fxml"));

        if (loader.getLocation() == null) {
            System.err.println("[Client] ERROR: clientOpening.fxml not found on classpath!");
            System.exit(1);
        }

        stage.setScene(new Scene(loader.load()));
        stage.setTitle("VoteSmartly — Client");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
