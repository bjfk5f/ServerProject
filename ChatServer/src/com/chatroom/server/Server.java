/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chatroom.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Server {

    // My port
    private static final int PORT = 13908;

    // Map of registered users, read from txt
    private static Map<String, String> registeredUsers = new HashMap<String, String>();
    
    // Connected clients and their usernames
    private static Map<String, PrintWriter> connectedUsers = new HashMap<String, PrintWriter>();

    // GUI components
    private static JFrame frame = new JFrame("ChatRoom Version 1: Server");
    private static JTextField inputField = new JTextField(70);
    private static JTextArea consoleArea = new JTextArea(20, 70);

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        initRegisteredUsers();
        initGui();
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }
    
    // Initialize registeredUsers from txt file
    public static void initRegisteredUsers() {
        BufferedReader reader;
        String[] splitstr;
        try {
            reader = new BufferedReader(new FileReader("RegisteredUsers.txt"));
            String line = reader.readLine();
            while (line != null) {
                splitstr = line.split(" ", 2);
                registeredUsers.put(splitstr[0], splitstr[1]);
                line = reader.readLine();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Initialize GUI components
    public static void initGui() {
        inputField.setEditable(true);
        consoleArea.setEditable(false);
        frame.getContentPane().add(inputField, "South");
        frame.getContentPane().add(new JScrollPane(consoleArea), "Center");
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    
    // Send message to connected users
    public static void sendMessageAll(String msg) {
        for (Map.Entry<String, PrintWriter> pair : connectedUsers.entrySet()) {
            pair.getValue().println("MESSAGE " + msg);
        }
        consoleArea.append(msg + "\n");
    }
    
    // Send message directly to another user
    public static void sendMessageDirect(String from, String to, String msg) {
        for (Map.Entry<String, PrintWriter> pair : connectedUsers.entrySet()) {
            if (pair.getKey().equalsIgnoreCase(from)) {
                pair.getValue().println("MESSAGE " + "(" + "me" + "->" + to + "): " + msg);
            }
            if (pair.getKey().equalsIgnoreCase(to)) {
                pair.getValue().println("MESSAGE " + "(" + from + "->" + "me" + "): " + msg);
            }
        }
        consoleArea.append("(" + from + "->" + to + "): " + msg + "\n");
    }

    // Print message to console
    public static void sendMessageServer(String msg) {
        consoleArea.append(msg + "\n");
    }
    
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private boolean loggedIn;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                String cmd;
                String user, pass;
                String[] splitstr;
                
                while (true) {
                    splitstr = in.readLine().split(" ", 2);
                    cmd = splitstr[0];
                    if (cmd.equalsIgnoreCase("login")) {
                        if (loggedIn) {
                            out.println("ERROR Already logged in.");
                            continue;
                        }
                        if (splitstr.length != 2) {
                            out.println("ERROR Invalid login.");
                            continue;
                        }
                        splitstr = splitstr[1].split(" ", 2);
                        if (splitstr.length != 2) {
                            out.println("ERROR Invalid login.");
                            continue;
                        }
                        user = splitstr[0];
                        pass = splitstr[1];
                        boolean foundUser = false;
                        for (Map.Entry<String, String> pair : registeredUsers.entrySet()) {
                            if (pair.getKey().equalsIgnoreCase(user)) {
                                foundUser = true;
                                if (pair.getValue().equals(pass)) {
                                    out.println("SUCCESS Logged in.");
                                    name = pair.getKey();
                                    connectedUsers.put(name, out);
                                    loggedIn = true;
                                    sendMessageAll(name + " logged in.");
                                }
                                else {
                                    out.println("ERROR Password is incorrect.");
                                    break;
                                }
                            }
                        }
                        if (!foundUser) {
                            out.println("ERROR Username does not exist.");
                        }
                    }
                    else if (cmd.equalsIgnoreCase("newuser")) {
                        if (loggedIn) {
                            out.println("ERROR Already logged in.");
                            continue;
                        }
                        
                        if (splitstr.length != 2) {
                            out.println("ERROR Invalid registry.");
                            continue;
                        }
                        splitstr = splitstr[1].split(" ", 2);
                        if (splitstr.length != 2) {
                            out.println("ERROR Invalid registry.");
                            continue;
                        }
                        user = splitstr[0];
                        pass = splitstr[1];
                        if (!registeredUsers.containsKey(user)) {
                            registeredUsers.put(user, pass);
                            try(FileWriter fw = new FileWriter("RegisteredUsers.txt", true);
                                BufferedWriter bw = new BufferedWriter(fw);
                                PrintWriter print = new PrintWriter(bw))
                            {
                                print.println(user + " " + pass);
                            } catch (IOException e) {
                                //exception handling left as an exercise for the reader
                            }

                            out.println("SUCCESS Registered user.");
                            sendMessageServer("New user registered (" + user + ", " + pass + ")");
                        }
                        else {
                            out.println("ERROR User already exists.");
                        }
                    }
                    else if (cmd.equalsIgnoreCase("send")) {
                        if (!loggedIn) {
                            out.println("ERROR Not logged in.");
                            continue;
                        }
                        
                        if (splitstr.length != 2) {
                            out.println("ERROR Use the syntax 'send all' or 'send user'");
                            continue;
                        }
                        String allmsg = splitstr[1];
                        splitstr = splitstr[1].split(" ", 2);
                        if (splitstr.length != 2) {
                            out.println("ERROR Send a message after the user's name");
                            continue;
                        }
                        
                        if (splitstr[0].equalsIgnoreCase("all")) {
                            sendMessageAll(name + ": " + splitstr[1]);
                        }
                        else{
                            boolean userExists = false;
                            for (Map.Entry<String, PrintWriter> pair : connectedUsers.entrySet()) {
                                if (pair.getKey().equalsIgnoreCase(splitstr[0])) {
                                    sendMessageDirect(name, pair.getKey(), splitstr[1]);
                                    userExists = true;
                                }
                            }
                            if (!userExists) {
                                out.println("ERROR User is not logged in.");
                            }
                        }
                    }
                    else if (cmd.equalsIgnoreCase("who")) {
                        if (!loggedIn) {
                            out.println("ERROR Not logged in.");
                            continue;
                        }
                        String namesListed = "";
                        Object[] userList = connectedUsers.keySet().toArray();
                        for (int i = 0; i < userList.length; i++) {
                            namesListed += userList[i];
                            if (i < userList.length - 1) {
                                namesListed += ", ";
                            }
                        }
                        out.println("MESSAGE Connected users(" + userList.length + "): " + namesListed);
                    }
                    else if (cmd.equalsIgnoreCase("logout")) {
                        if (!loggedIn) {
                            out.println("ERROR Not logged in.");
                            continue;
                        }
                        loggedIn = false;
                        if (name != null) {
                            connectedUsers.remove(name);
                        }
                        out.println("SUCCESS Logged out.");
                        sendMessageAll(name + " logged out.");
                    }
                    else if (cmd.equalsIgnoreCase("exit")) {
                        out.println("EXIT");
                        break;
                    }
                    else if (cmd.equalsIgnoreCase("help")) {
                        out.println("INFO login <USER> <PASS>: Log in to the room");
                        out.println("INFO newuser <USER> <PASS>: Register a user to the room");
                        out.println("INFO send all <MESSAGE>: Send a message to connected clients");
                        out.println("INFO send <UserID> <MESSAGE>: Send a message to a user directly");
                        out.println("INFO who: List all the clients in the chat room");
                        out.println("INFO logout: Logs out of current user");
                        out.println("INFO help: Shows available commands");
                        out.println("INFO Exit: Exit out of program");
                    }
                    else {
                        out.println("ERROR Unknown command\nType 'help' for a list of commands");
                    }
                }

            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // Remove client from sets, close socket
                if (loggedIn) {
                    loggedIn = false;
                    if (name != null) {
                        connectedUsers.remove(name);
                    }
                    out.println("SUCCESS Logged out.");
                    sendMessageAll(name + " logged out.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
