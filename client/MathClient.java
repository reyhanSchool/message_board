import javax.print.DocFlavor.INPUT_STREAM;
import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class MathClient {
    private static MessageBoardUI ui;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ui = new MessageBoardUI(); // Create an instance of the UI
                ui.listenForMessages(); // Start listening for incoming messages
            }
        });
    }
}

class MessageBoardUI extends JFrame {
    private JTextArea messageArea;
    private JButton sendButton;
    private JButton refreshButton; // New Refresh Button
    private JTextField inputField;
    private static Socket echo;
    private PrintWriter dos;
    private JButton changeBackgroundColorButton; // Add a new button
    private JButton changeFontColorButton; // Add a new button

    public MessageBoardUI() {
        // Set up the Swing UI components
        setTitle("Public Board");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

                // Create a JPanel for the title
        JPanel titlePanel = new JPanel();

        // Create a JLabel for the title
        JLabel titleLabel = new JLabel("Message Board", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24)); // You can adjust the font and size
        titleLabel.setForeground(Color.GRAY); // You can set the color
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Add some padding

        messageArea = new JTextArea(15, 40);
        messageArea.setEditable(false);
        messageArea.setText("Welcome to the message board! Please start by entering your name and pressing Send.\n\nThen enter your message, and click Send.\n\n");
        JScrollPane scrollPane = new JScrollPane(messageArea);

        sendButton = new JButton("Send");
        inputField = new JTextField(30);

        refreshButton = new JButton("Refresh"); // Create a new Refresh Button

        // Create a new button for changing the background color
        changeBackgroundColorButton = new JButton("Change Background");
        
        // Add an ActionListener to the button
        changeBackgroundColorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open a color picker dialog
                Color selectedColor = JColorChooser.showDialog(MessageBoardUI.this, "Choose Background Color", messageArea.getBackground());
                
                if (selectedColor != null) {
                    // Set the background color of the messageArea to the selected color
                    messageArea.setBackground(selectedColor);
                }
            }
        });


 // Create a new button for changing the background color
        changeFontColorButton = new JButton("Change Font Color");
        
        // Add an ActionListener to the button
        changeFontColorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open a color picker dialog
                Color selectedColor = JColorChooser.showDialog(MessageBoardUI.this, "Choose Font Color", messageArea.getBackground());
                
                if (selectedColor != null) {
                    // Set the background color of the messageArea to the selected color
                    messageArea.setForeground(selectedColor); // Change the text color of the messageArea to green
                }
            }
        });

        titlePanel.add(titleLabel); // Add the title label to the panel
        add(titlePanel, BorderLayout.NORTH); // Add the panel to the top

        JPanel inputPanel = new JPanel();
        inputPanel.add(inputField);
        inputPanel.add(sendButton);
        inputPanel.add(refreshButton); // Add the Refresh Button
        inputPanel.add(changeBackgroundColorButton); // Add the new button to the panel
        inputPanel.add(changeFontColorButton);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Initialize the socket and PrintWriter
        try {
            echo = new Socket("localhost", 3500);
            dos = new PrintWriter(echo.getOutputStream(), true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Send the user's input to the server
                String userMessage = inputField.getText();
                messageArea.append("You entered: " + userMessage +"\n\n");

                if (!userMessage.isEmpty()) {
                    dos.println("entry"); // Send the server that it's an entry
                    dos.flush();
                    dos.println(userMessage); // Send the actual message
                    dos.flush();
                    inputField.setText(""); // Clear the input field after sending
                }
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create a SwingWorker to perform the refresh operation
                SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        // Perform the refresh operation in the background
                        fetchAndDisplayInitialData();
                        return "Refreshed"; // You can return a result if needed
                    }

                    @Override
                    protected void done() {
                        try {
                            String result = get();
                            // Update the UI if needed with the result
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                };

                worker.execute(); // Start the SwingWorker
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

    }

    private void fetchAndDisplayInitialData() {
        try {
            // Send a request to the server to get the initial data
            
            dos.println("fetchData");
            dos.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(MessageBoardUI.echo.getInputStream()));
            StringBuilder initialData = new StringBuilder();
            String receivedMessage;
            
            while ((receivedMessage = reader.readLine()) != null) {
                // Append the received data to the StringBuilder
                initialData.append(receivedMessage).append("\n\n");
            }
            
            messageArea.append("\nRefreshed Board:\n");
            // Display the initial data in the JTextArea
            messageArea.append(initialData.toString());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return;
    }

    public void listenForMessages() {
        Thread messageListener = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(MessageBoardUI.echo.getInputStream()));
                    String receivedMessage;
                    while ((receivedMessage = reader.readLine()) != null) {
                        // Display the received message in the UI
                        displayMessage(receivedMessage);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        messageListener.start();
    }

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Append the message to the text area
                messageArea.append(message + "\n\n");
                // Scroll to the bottom to show the latest message
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
            }
        });
    }
    
}
