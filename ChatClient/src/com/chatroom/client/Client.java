/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chatroom.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client {
    
    // My port
    private static final int PORT = 13908;

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("ChatRoom Version 1: Client");
    JTextField inputField = new JTextField(70);
    JTextArea consoleArea = new JTextArea(20, 70);

    public Client() {

        // Setting up GUI components
        inputField.setEditable(true);
        consoleArea.setEditable(false);
        frame.getContentPane().add(inputField, "South");
        frame.getContentPane().add(new JScrollPane(consoleArea), "Center");
        frame.pack();
        
        // Field listener
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(inputField.getText());
                // Print user input with '>'
                consoleArea.append(">" + inputField.getText() + "\n");
                inputField.setText("");
            }
        });
    }

    // Get server address from user
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server, or localhost:",
            "Chatroom version 1",
            JOptionPane.QUESTION_MESSAGE);
    }

    // Remain in loop for as long as connection is stable
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Receives messages from server
        while (true) {
            String line = in.readLine();
            // Error message from server, just print
            if (line.startsWith("ERROR")) {
                consoleArea.append("Server: " + line.substring(6) + "\n");
            }
            // Success message from server, just print
            else if (line.startsWith("SUCCESS")) {
                consoleArea.append("Server: " + line.substring(8) + "\n");
            }
            // Info message from server, just print
            else if (line.startsWith("INFO")) {
                consoleArea.append(line.substring(5) + "\n");
            }
            // User message from server, just print
            else if (line.startsWith("MESSAGE")) {
                consoleArea.append(line.substring(8) + "\n");
            }
            // Exit server, close program
            else if (line.startsWith("EXIT")) {
                System.exit(0);
            }
            else {
                consoleArea.append(line + "\n");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
