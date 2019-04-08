
import java.io.*;
import java.net.*;		 // get access to Socket class
import java.time.LocalTime;
import java.util.LinkedList;

import java.util.Scanner;

public class TcpClient {

    public static String moves = "";

    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);
        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        System.out.println("Enter a host to which you would like to connect:");
        String host = input.nextLine();
        System.out.println("The host is: " + host + ". \nEnter the port number to which you are connecting:");
        int port = input.nextInt();

        try {
            echoSocket = new Socket(host, port);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + host);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + host);
            System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        DataInputStream dis = new DataInputStream((echoSocket.getInputStream()));
        String userInput;

        // Is this the first move for this player?
        boolean firstMove = true;

        while (true) {  
            String messageFromServer = "";
			/*
			Give the player a message. 
			Possible messages include: 
				- "QUIT " (sent when the opponent resigns), 
				- "START" (sent to client 1 when an opponent has connected), 
				- "MOVE " combined with the opponent's last move (example: "MOVE f4->h6")
				- "Waiting to find an opponent..." (sent when an opponent has not connected yet), 
				- "Waiting on your opponent to make a move..." (sent when the player is waiting on his/her opponent to move).
			*/	
			
            try {
                messageFromServer = dis.readUTF();  // Read input from the server.
                if (messageFromServer.substring(0, 7).equals("Waiting")) {  // In this case, the client is either waiting to find an opponent or waiting on the opponent to make a first move.
                    System.out.println("\n" + messageFromServer);  // Show the server's wait message.
                    messageFromServer = dis.readUTF();  // Continue waiting on another message to arrive from the server.
                }
            } catch (SocketException se) {
                System.out.println("\nThere was a problem with the connection!");
                System.out.println("We will have to terminate.");
                System.out.println("Below information can be collected and escalated to our IT department:");
                System.out.println(se);
                System.out.println();
                break;
            } catch (EOFException eofe) {
                System.out.println("\nThere was a problem with the program.");
                System.out.println("Your opponent's connection may have been terminated.");
                System.out.println("Below information can be collected and escalated to our IT department:");
                System.out.println(eofe);
                System.out.println();
                System.out.println("Please restart.");
                break;
            }

			System.out.println("msg from server: " + messageFromServer);
            if (messageFromServer.substring(0, 5).equals("START")) {  // This player needs to make the initial move.
                System.out.println("******************************************************************");
                System.out.println("Your move!");
                System.out.println("Type the coordinates of the position you are moving a piece to.");
                System.out.println("Type coordinates EXACTLY like so: from position->to position");
                System.out.println("Example: f4->h6 (moves the piece on square f4 to square h6)");
                System.out.println("Type R to resign.");
                firstMove = false;
            } else if (messageFromServer.substring(0, 5).equals("MOVE ")) {  // The other player has moved.
                // Add the opponent's move to the list of moves.
                moves += messageFromServer.substring(5, 11) + " ";

                if (firstMove) {  // If this is the player's first move, then show them how to play.
                    System.out.println("******************************************************************");
                    System.out.println("Your move!");
                    System.out.println("Type the coordinates of the position you are moving a piece to.");
                    System.out.println("Type coordinates EXACTLY like so: from position->to position");
                    System.out.println("Example: f4->h6 (moves the piece on square f4 to square h6)");
                    System.out.println("Type R to resign.");
                    System.out.println("Your opponent's first move: " + messageFromServer.substring(5));  
                    firstMove = false;
                } else {
                    System.out.println("..............");
                    System.out.println("\nYour turn!");

                    // Display a list of all moves.
                    String allMoves = moves.replace(" ", "");
                    int moveNum = 1;
                    System.out.println("Moves list: ");
                    for (int i = 0; i < allMoves.length() - 1; i += 12) {
                        if (!(i % 12 == 0)) {  // If it is the second move in the volley, don't show the move number.
                            System.out.println(moveNum + ". " + allMoves.substring(i, i + 6));
                        } else {
                            System.out.print(moveNum + ". " + allMoves.substring(i, i + 6));
                            if (allMoves.length() - i >= 12) {  // There is one more move for this volley.
                                System.out.println(", " + allMoves.substring(i + 6, i + 12));
                            } else {  // Print blank line.
                                System.out.println();
                            }
                            moveNum++;
                        }
                    }
                    System.out.println();
                    System.out.println("Enter your move: ");
                }
            } else if (messageFromServer.substring(0, 5).equals("QUIT ")) {  // Opponent resigned.
                System.out.println("\nYou win! Your opponent resigned.\n");
                break;
            }

            userInput = stdIn.readLine();  // Fetch the move from the player.
            
            if (userInput.equals("r") || userInput.equals("R")) {  // User wants to resign.
				// Ask user to confirm resign request.
				System.out.println("Sure you want to resign? y/n");
                String confirm = input.nextLine();
                if (confirm.charAt(0) == 'n' || confirm.charAt(0) == 'N') {
                    System.out.println("You have declined to resign. Enter your move.");
                    userInput = stdIn.readLine();
                }
            }
            // Catch any input errors.
            while (!userInput.contains("->") || !(userInput.length() == 6)) {  // Input must be exactly 6 characters long and contain "->".
                if (userInput.equals("r") || userInput.equals("R")) {  // In this case, user wanted to resign.
                    break;
                } // otherwise, show an error message...				
                System.out.println("\nOops! Something's wrong with your input.");
                System.out.println("Make sure it is of the form a2->c4 (where a2, in this case, is the starting position and c4 is the ending position).");
                System.out.println("Must be exactly 6 characters long and contain \"->\"");
                System.out.println("If you want to resign, type \"R\"");
                System.out.println("Type your move again.");
                userInput = stdIn.readLine();
            }

            moves += userInput + " ";  // Add the move to the list of moves.

            out.println(userInput);
            if (userInput == null || userInput.equals("R") || userInput.equals("r")) {
                break;
            }
            System.out.println("Waiting for opponent...");
        }

        out.close();

        in.close();

        stdIn.close();

        echoSocket.close();
    }

}
