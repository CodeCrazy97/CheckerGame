
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.*;
import java.io.*;
import java.lang.Thread;
import java.util.Enumeration;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/*

CSC 460 - Assignment 2
Client/Server Checker Game

Ethan Vaughan

02/04/2019

*/


public class TcpServer {

	/*
	
	This is the server.

	A checker game will not start until two clients have established connections. Then, the first client to establish 
	a connection will be prompted to enter a move. When the server receives a move from a player, the other player is 
	told the player's move and asked to enter a move. 
	
	Both player's client ends keep track of the moves.
	
	*/



    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner keyboard = new Scanner(System.in);

        ServerSocket serverSocket = null;
        boolean listening = true;
		
		System.out.println("Welcome server! You will oversee a checker game.");
        System.out.println("Enter a port number: ");
        int input = keyboard.nextInt();

        try {
            serverSocket = new ServerSocket(input);
        } catch (IOException ex) {
            System.err.println("Could not listen on port: " + input);
            System.exit(-1);
        }

        while (listening) {
			// Get both threads going for two different clients.
            TcpServerThread tcp1 = new TcpServerThread(serverSocket.accept(), true);
            TcpServerThread tcp2 = new TcpServerThread(serverSocket.accept(), false);
            tcp1.start();
            tcp2.start();
			// Make each thread the "opponent" of the other.
            tcp1.opponent = tcp2; 
            tcp2.opponent = tcp1;
            tcp1.message = "START";  // Send start message to the first. This will prompt the player to enter the first move.
            tcp1.move = true;        // It is true that it's this player's turn.
        }

        serverSocket.close();
    }
}

class TcpServerThread extends Thread {

    public boolean endClientThread = false;  // Monitors if the client thread is still running.
    public TcpServerThread opponent;		// Points to the opponent.
    public String message = "";				// This holds the message to the client. It will also hold the last move.
    public boolean move = false;			// Allows player to move if "true", prevents player from moving if "false".
    private Socket socket = null;			// The socket that the thread is connected to.

    public TcpServerThread(Socket socket, boolean waiting) {
        super("EchoServerThread");
        if (waiting) {  // The first player is waiting on another player to connect. This client is waiting to connect with another player.
            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("Waiting to find an opponent...");
            } catch (IOException ioe) {
                System.out.println(ioe);
            }
        } else {  // else is second opponent - this client is waiting on the first player to make a move.
            try {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("Waiting on your opponent to make a move...");
            } catch (IOException ioe) {
                System.out.println(ioe);
            }
        }
        this.socket = socket;
    }

    public void run() {
        try {
            InputStream in = socket.getInputStream();  	  // Reads input from client side.
            OutputStream out = socket.getOutputStream();   // Prints output to the client side.

            /*
			str will hold the input from the player. 
			NOTE: the only input a client (player) can send is for a move or to resign ("R" or "r" to indicate resignation).
			Moves are in the form "from square->to square", where "from square" is the square a piece is moving from and 
			"to square" is the square the piece is going to. Example: b2->d4. In this case, the piece is going from square b2
			to square d4.
			*/
            String str = "";

            while (true) {  // Continue playing until a break condition is met.
                if (endClientThread) {  // The client's thread has exited for an unexpected reason. Close the thread.
                    break;
                }
                while (move) {  // While it's this client's turn to move...
                    try {
                        /*
						Give the player a message. 
						Possible messages include: 
						- "QUIT " (sent when the opponent resigns), 
						- "START" (sent to client 1 when an opponent has connected), 
						- "MOVE " combined with the opponent's last move, 
						- "Waiting to find an opponent..." (sent when an opponent has not connected yet), 
						- and "Waiting on your opponent to make a move..." (sent when the player is waiting on his/her opponent to move).
						*/						
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF(message); 
                        message = "";  // Clear the string for the next message.
                    } catch (SocketException se) {  // Trouble writing to client - exit the thread.
                        endClientThread = true;
                        break;
                    }

                    try {
                        // Collect the player's move.
                        byte[] userdata = new byte[200];
                        int chars_read = in.read(userdata, 0, 200);
                        if (chars_read < 0) {
                            break;					// at EOF
                        }
                        str = new String(userdata, 0, chars_read);
						System.out.println("Just entered: " + str);
                        if (str.charAt(0) == 'r' || str.charAt(0) == 'R') {
                            move = false;   // Prevent client from moving.
                            opponent.move = true;  // Allow the while loop on opponent's side to execute once (their thread will exit after one execution). 
                            opponent.message = "QUIT ";  // Communicate client's resignation to the opponent.
                        } else {
                            move = false;   // Prevent client from moving.
                            opponent.move = true;  // Allow opponent to move.   
                            opponent.message = "MOVE " + str;  // Communicate client's move to the opponent.
                        }
                    } catch (SocketException se) {
                        System.out.println("Connection issue.");
                        endClientThread = true;  // End the current thread.
                        opponent.endClientThread = true;  // Indicate opponent's thread should end, too.
                        break;
                    }
                }

                // Player resigned.
                if (str.length() > 0 && (str.charAt(0) == 'R' || str.charAt(0) == 'r')) {
                    break;
                }
            }
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thread exiting");
    }
}
