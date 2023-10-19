package server;


import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class MathServer {
    private static final int MAX_THREADS = 3; // Define the maximum number of threads
    public static ConcurrentHashMap<String, String> messageBoard = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(3500)) {
            System.out.println("Server Listening on port 3500....");

            ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println("Accepted connection from client: " + clientSocket.getInetAddress() + "\tPort: " + clientSocket.getPort());

                // Make sure the thread count doesn't exceed
                if (((ThreadPoolExecutor) executor).getActiveCount() >= MAX_THREADS) {
                    System.out.println("The server is currently busy. Rejecting client: " + clientSocket.getInetAddress() + " " + clientSocket.getPort());
                    clientSocket.close();
                    continue;
                }

                // Create a new thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executor.execute(clientHandler);
            }
        }
    }

    static class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                // Use BufferedReader to read the string sent by the client
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String selection;
                while((selection = reader.readLine()) != null){
                    // Check if it's a "fetchData" request
                    if (selection.equals("fetchData")) {
                        sendBoardData();
                    } else if (selection.equals("entry")){
                        // Handle other requests, such as "entry" or "refresh"
                        handleRequest(reader);
                }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    System.out.print("Closing the client socket.");
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRequest(BufferedReader reader) throws IOException {
            // Handle other requests (e.g., "entry" or "refresh") here
            String clientName = reader.readLine(); // enter a name
            reader.readLine();
            String clientBoardEntry = reader.readLine();
            String entryTime = getCurrentTimeAsString();
            String completeEntry = entryTime + "\t" + clientName + " :\t" + clientBoardEntry;
            MathServer.messageBoard.put(clientName, completeEntry);

            // Send the updated board data back to the client
            sendBoardData();
        }

        private void sendInitialData() throws IOException{
            // Send the initial board data to the client
            PrintWriter dos = new PrintWriter(clientSocket.getOutputStream(), true);
            String initialData = getBoardAsString(MathServer.messageBoard);
            dos.println(initialData);
            dos.flush();
        }

        private void sendBoardData() throws IOException{
            // Send the updated board data to the client
            PrintWriter dos = new PrintWriter(clientSocket.getOutputStream());
            String boardData = getBoardAsString(MathServer.messageBoard);
            System.out.println("Sending the board data \t " + boardData);
            dos.println(boardData);
            dos.flush();
        }

        private static String getBoardAsString(ConcurrentHashMap<String, String> messageBoard)  throws IOException{
            // Convert the ConcurrentHashMap to a string
            StringBuilder result = new StringBuilder();
            for (String entry : messageBoard.values()) {
                result.append(entry).append(System.lineSeparator());
            }
            return result.toString();
        }

        private String getCurrentTimeAsString() {
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return currentDateTime.format(formatter);
        }
    }
}
