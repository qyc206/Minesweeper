package qyc206.minesweeper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Server implements Runnable {
	
	int port = 8080;
	private Connection conn;
	private PreparedStatement queryForGame, insertGameStatement, queryForGameName;
	
	public Server() {
		
		try {

			// start connection to database
			conn = DriverManager.getConnection("jdbc:sqlite:savedGames.db");
			
			queryForGame = conn.prepareStatement("SELECT gameStatus FROM savedGames WHERE gameName = ?");
			queryForGameName = conn.prepareStatement("SELECT gameName FROM savedGames WHERE hostname_ip = ?");
			insertGameStatement = conn.prepareStatement("INSERT INTO savedGames (hostname_ip, gameStatus, gameName) VALUES (?,?,?)");
			
		} catch (SQLException e) {
			System.err.println("Connection error: " + e);
			System.exit(1);
		}
		
		Thread t = new Thread(this);
		t.start();

	}
	
	public void run() {

		try {
			
			// create a server socket
			ServerSocket serverSocket = new ServerSocket(port);
			
			// wait for a connection
			while (true) {
				// listen for a new connection request
				Socket socket = serverSocket.accept();
				
				System.out.println("Connection formed!");
				
				// get client host name and IP address
				InetAddress inetAddress = socket.getInetAddress();
				
				// create and start new thread for the connection
				new Thread(new HandleClient(socket, inetAddress.getHostName(), inetAddress.getHostAddress())).start();
			}

		} catch(IOException ex) {
			System.err.println("IOException caught (server): " + ex.getMessage());
		}

	}
	
	// Thread class for handling new client connection
	
	class HandleClient implements Runnable {

		private Socket socket;	
		private String hostname;
		private String ipAddr;
		private GameStatus game;
		
		public HandleClient(Socket socket, String hostname, String ipAddr) {
			this.socket = socket;
			this.hostname = hostname;
			this.ipAddr = ipAddr;
		}
		
		public void run() {
			
			try {
				
				// create data input and output streams
				
				DataInputStream fromClientData = new DataInputStream(socket.getInputStream());
				DataOutputStream toClientData = new DataOutputStream(socket.getOutputStream());
				ObjectOutputStream toClientObject = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream fromClientObject = new ObjectInputStream(socket.getInputStream());
				
				// serve the client
				while (true) {

					String request = fromClientData.readUTF();
					
					if (request.equals("save")) {
						
						System.out.println("save: "+ hostname + ":" + ipAddr);
						
						// read game name
						
						String gameName = fromClientData.readUTF();
						
						// read game status
						
						game = (GameStatus) fromClientObject.readObject();
						
						// convert game status into a blob
						
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream(bos);
						
						oos.writeObject(game);
						oos.flush();
						byte[] data = bos.toByteArray();
						
						ByteArrayInputStream bis = new ByteArrayInputStream(data);
						
						PreparedStatement stmt = insertGameStatement;
						
						stmt.setString(1, hostname+":"+ipAddr);
						stmt.setBinaryStream(2, bis, data.length);
						stmt.setString(3, gameName);
				
						
						stmt.executeUpdate();	// execute statement
						
					} else if (request.equals("load game names")) {
						
						System.out.println("load game names: "+ hostname + ":" + ipAddr);
						
						// get user's saved game from database
						
						PreparedStatement stmt = queryForGameName;
						stmt.setString(1, hostname+":"+ipAddr);
						ResultSet rset = stmt.executeQuery();
						
						String gameNames = "";
						
						while (rset.next()) {
							gameNames += rset.getString("gameName") + ",";
						}
						
						// send saved game names to client
						
						if (!gameNames.isEmpty()) {
							toClientData.writeUTF(gameNames.substring(0, gameNames.length()-1));	// get rid of extra , at end
						} else {
							toClientData.writeUTF(gameNames);
						}

						toClientData.flush();
						
					} else if (request.equals("load game")) {
						
						System.out.println("load game: "+ hostname + ":" + ipAddr);
						
						// get game name from user

						String gameName = fromClientData.readUTF();
						
						// get user's saved game from database
						
						PreparedStatement stmt = queryForGame;
						stmt.setString(1, gameName);
						ResultSet rset = stmt.executeQuery();
						
						ByteArrayInputStream in = new ByteArrayInputStream(rset.getBytes("gameStatus"));
						ObjectInputStream is = new ObjectInputStream(in);
						
						game = (GameStatus) is.readObject();
						
						// send saved game to user
						
						toClientObject.writeObject(game);
						toClientObject.flush();
						
					}
					
				}
				
			} catch (IOException ex) {
				System.err.println("IOException caught (server): " + ex.getMessage());
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
				System.err.println("ClassNotFoundException caught (server): " + ex.getMessage());
				ex.printStackTrace();
			} catch (SQLException ex) {
				System.err.println("SQLException caught (server): " + ex.getMessage());
				ex.printStackTrace();
			} 
		}
	
	}
	
	public void setPort(int port) { this.port = port; }
	
	public static void main(String[] argv) {
		Server gameServer = new Server();
	}
	
}
