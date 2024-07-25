package client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ClientInstance {
    // define attributes for the ClientInstance object
    String hostname;
    int port;
    String clientId;
    User user;
    boolean isStudent;
    boolean isAuthenticated;

    public ClientInstance(String hostname, int port, User user) {
        // constructor class for the client instance
        this.hostname = hostname;
        this.port = port;
        this.user = user;
    }

    public static boolean isValid(String input) {
        String regex = "^\\{.*\\}$";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        return pattern.matcher(input).matches();
    }

    public static JSONObject displayQuestionSet(JSONObject challengeObj) {
        System.out.println("CHALLENGE " + challengeObj.getInt("challenge_id") + " (" + challengeObj.get("challenge_name") + ")");
        Scanner scanner = new Scanner(System.in);
        JSONArray questions = challengeObj.getJSONArray("questions");
        for (int i = 0; i < questions.length(); i++) {
            JSONObject question = questions.getJSONObject(i);
            System.out.println(question.get("id") + ". " + question.getString("question"));
            String answer = scanner.nextLine();
            System.out.print("\n");
        }
        return new JSONObject();
    }

    public void start() throws IOException {
        // Todo: create a parent menu
        // execute code for interacting with the server
        try (
                Socket socket = new Socket(hostname, port);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        ) {
            this.clientId = (String) socket.getInetAddress().getHostAddress();
            Serializer serializer = new Serializer(this.user);
            System.out.print("[" + this.clientId + "] (" + this.user.username + ") -> ");
            // read command line input
            // Continuously read from the console and send to the server
            ClientController clientController = new ClientController(user);
            String regex = "^\\{.*\\}$";
            Pattern pattern = Pattern.compile(regex);
            String userInput;
            while ((userInput = consoleInput.readLine()) != null) {
                // send command to the server
                if (userInput.equals("logout")) {
                    System.out.println("Session successfully logged out");
                    this.user.logout();
                    System.out.print("[" + this.clientId + "] (" + (!this.user.username.isBlank() ? this.user.username : null) + ") -> ");
                    continue;
                }

                String serializedCommand = serializer.serialize(userInput);
                if (isValid(serializedCommand)) {
                    output.println(serializedCommand);
                    // read response here from the server
                    String response = input.readLine();
                    this.user = clientController.exec(response);
                    if (!pattern.matcher(this.user.output).matches()) {
                        System.out.println("\n" + user.output + "\n");
                    } else {
                        JSONObject questions = new JSONObject(this.user.output);
                        JSONObject answerSet = displayQuestionSet(questions);
                    }
                } else {
                    System.out.println(serializedCommand);
                }
                // prompt for the next instruction
                System.out.print("[" + this.clientId + "] (" + this.user.username + ") -> ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Connection with the server timeout");
        }
    }
}