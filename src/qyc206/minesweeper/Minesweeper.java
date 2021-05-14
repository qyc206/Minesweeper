package qyc206.minesweeper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class Minesweeper extends JFrame {
	
	private final int ROWS = 16;
	private final int COLUMNS = 16;
	private final int BOMB_TOTAL = 40;
	private final int TIME_DELAY = 1000;
	
	private int bombsGuessesLeft = 40, currTime = 1000;
	private int correctlyFlagged = 0, selectedSafeCells = 0;
	
	CellPanel[][] grid;
	JLabel bottomLabel, timerLabel;
	Timer time;
	boolean startedGame = false, endGame = false;
	
	int port;
	String hostname;
	
	Socket socket = null;
	DataOutputStream toServerData = null;
	DataInputStream fromServerData = null;
	ObjectOutputStream toServerObject = null;
	ObjectInputStream fromServerObject = null;

	JFrame popupSave, popupLoad;
	JLabel saveMessage, loadMessage;
	JList<String> savedGames;
	
	
	public Minesweeper(String hostname, int port) {
		
		// initialize connection info
		
		this.hostname = hostname;
		this.port = port;
		
		// initialize timer with action listener to display count down
		
		ActionListener actionPerformed = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				timerLabel.setText("Time remaining: " + Integer.toString(--currTime));
				if (currTime == 0) {
					loseGame();
				}
			}
		};
		
		time = new Timer(TIME_DELAY, actionPerformed);
		
		// set up GUI 
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(Color.LIGHT_GRAY);
		setSize(270, 300);
		
		createMenu();	
		createTopPanel();
		loadNewGame();
		createBottomPanel();
		createSavePopup();
		createLoadPopup();
		
		setVisible(true);
		
	}
	
	
	// define mouse click listener behavior for cells
	
	class MouseClickListener extends MouseAdapter { 
		private int row;
		private int col;
		
		MouseClickListener(int row, int col){ 
			this.row = row;
			this.col = col;
		}
		
		public void mouseClicked(MouseEvent e) {
			if (endGame) {		// if lost the game and something clicked
				resetFields();
				loadNewGame();
				return;
			}
			
			if (!startedGame) {		// start timer if it is the first cell clicked
				time.start();
				startedGame = true;
			}
			
			// don't do anything if cell is already revealed or a flagged cell is clicked
			if (grid[row][col].isRevealed() || 
					(grid[row][col].isFlagged() && SwingUtilities.isLeftMouseButton(e))) {
				return;
			}
			
			if (SwingUtilities.isRightMouseButton(e)) {		// if trying to flag a cell
				if (grid[row][col].isFlagged()) {		// if cell already flagged, remove the flag
					if (grid[row][col].isBomb()) correctlyFlagged -= 1;	 // removing a correctly flagged cell
					
					grid[row][col].removeFlag();
					bottomLabel.setText(Integer.toString(++bombsGuessesLeft));
				}
				else {	// cell is not flagged, so flag it
					if (bombsGuessesLeft > 0) {		// if there are still flags left
						if (grid[row][col].isBomb()) correctlyFlagged += 1;	 // flagged a cell with bomb
						
						grid[row][col].placeFlag();
						bottomLabel.setText(Integer.toString(--bombsGuessesLeft));
					}
				}
			} else if (!grid[row][col].isBomb()){		// if selected cell is not a bomb
				revealCell(row, col);
			} else {	// selected a bomb
				grid[row][col].reveal();
				loseGame();
			}
			
			// if correctly selected all non-bomb cells or correctly flagged all bomb cells, game is won
			if ((selectedSafeCells == ROWS*COLUMNS - BOMB_TOTAL) || 
					correctlyFlagged == BOMB_TOTAL) {
				winGame();
			}
	  }
	}
	
	
	/* Methods for loading a game */
	
	private void loadNewGame() {
		
		// initialize grid with cells
		
		grid = new CellPanel[ROWS][COLUMNS];
		
		// add mouse click listeners to each cell 
		
		for (int i = 0; i < ROWS; i++) {
			for (int j = 0; j < COLUMNS; j++) {
				grid[i][j] = new CellPanel(i, j);
				grid[i][j].addMouseListener(new MouseClickListener(i, j));
			}
		}
		
		// set random bombs 
		
		Random random = new Random();
		int bombCnt = 0;
		
		while (bombCnt < BOMB_TOTAL) {
			int row = random.nextInt(ROWS);
			int col = random.nextInt(COLUMNS);
			
			// if not already a bomb, make it a bomb
			if (!grid[row][col].isBomb()) { 
				grid[row][col].placeBomb();
				bombCnt += 1;
			}
		}
		
		displayBoard();
		
	}
	
	private void loadSavedGame(GameStatus game) {
		
		// if there is a game running, stop the game
		
		resetFields();
		
		// update fields with saved information
		
		grid = game.getGrid();
		bombsGuessesLeft = game.getBombsGuessesLeft();
		currTime = game.getCurrTime();
		correctlyFlagged = game.getCorrectlyFlagged();
		selectedSafeCells = game.getSelectedSafeCells();
		
		timerLabel.setText("Time remaining: " + Integer.toString(currTime));
		bottomLabel.setText(Integer.toString(bombsGuessesLeft));
		
		// add mouse click listeners to each cell 
		
		for (int i = 0; i < ROWS; i++) {
			for (int j = 0; j < COLUMNS; j++) {
				grid[i][j].addMouseListener(new MouseClickListener(i, j));
			}
		}
		
		displayBoard();
		
	}
	
	private void displayBoard() {
		
		JPanel gridPanel = new JPanel();
		gridPanel.setLayout(new GridLayout(ROWS, COLUMNS));
		
		// cover all cells (while printing out an answer key in console)
		
		System.out.print("\nAnswer key:\n");
		
		for (int i = 0; i < ROWS; i++) {
			for (int j = 0; j < COLUMNS; j++) {
				// print out cells in console
				if (grid[i][j].isBomb()) System.out.print("* ");
				else System.out.print("_ ");
				
				if (grid[i][j].isFlagged()) { // if cell is flagged, place flag
					grid[i][j].placeFlag();
				} else if (!grid[i][j].isRevealed()) { // else if cell is not already revealed, cover it
					grid[i][j].coverCell();
				}
			}
			System.out.print("\n");
		}
		
		// add cell panels to grid panel
		
		for (int i = 0; i < ROWS; i++) {
			for (int j = 0; j < COLUMNS; j++) {
				gridPanel.add(grid[i][j]);
			}
		}
		
		// add grid panel to main frame
		add(gridPanel, BorderLayout.CENTER);
		
	}
	
	private void resetFields() {		// reset fields and labels for new game
		
		time.stop();
		bombsGuessesLeft = BOMB_TOTAL;
		currTime = TIME_DELAY;
		
		timerLabel.setText("Time remaining: " + Integer.toString(currTime));
		bottomLabel.setText(Integer.toString(bombsGuessesLeft));
		
		startedGame = false;
		endGame = false;
		correctlyFlagged = 0;
		selectedSafeCells = 0;
		
	}
	
	
	/* Methods for creating & handling menu items */
	
	private void createMenu() {
		
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		
		setJMenuBar(menuBar);
		
		menu.add(createFileNewItem());		// "New" item
		menu.add(createFileOpenItem());		// "Open" item
		menu.add(createFileSaveItem());		// "Save" item
		menu.add(createFileExitItem());		// "Exit" item

		menuBar.add(menu);
		
	}
	
	// handle the "New" menu option (start a new game)
	
	private JMenuItem createFileNewItem() {		// starts a new game when selected
		
		JMenuItem item = new JMenuItem("New");
		
		/* NewActionListener will load a new game */
		class NewActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				resetFields();
				loadNewGame();
			}
		}
		
		item.addActionListener(new NewActionListener());
		return item;
		
	}
	
	// handle the "Open" menu option (view and open a saved game)
	
	private JMenuItem createFileOpenItem() {
		
		JMenuItem item = new JMenuItem("Open");
		
		/* OpenActionListener that will open the file chooser */
		class OpenActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {

				if (!popupLoad.isVisible()) {
					
					savedGames.setListData(loadGameNameFromServer());
					
					// display a message if there are no saved games in system					
					if (savedGames.getModel().getSize() == 0) {
						loadMessage.setText("There are no saved games at this moment...");
					} else {
						loadMessage.setText("");
					}
					
					popupLoad.repaint();
					popupLoad.setVisible(true);
					
				}
				
			}
		}
		
		item.addActionListener(new OpenActionListener());
		return item;
		
	}
	
	// create load pop-up
	
	private void createLoadPopup() {
		
		popupLoad = new JFrame();
		popupLoad.setSize(500,350);
		
		JPanel panel = new JPanel(new BorderLayout());
		
		JPanel btn = new JPanel();
		JPanel select = new JPanel();
		JPanel bottomMessage = new JPanel();
		
		JLabel loadGameLabel = new JLabel("Select a saved game: ");
		loadMessage = new JLabel("");
		JButton selectBtn = new JButton("Load Game");
		JButton cancelBtn = new JButton("Close");
		
		String[] gameStr = {};
		savedGames = new JList<String>(gameStr);
		
		// detect list selection (hide the "load" button if nothing is selected)
		
		class ListOptionListener implements ListSelectionListener {
		   public void valueChanged(ListSelectionEvent event) {
			   
			   if (!event.getValueIsAdjusting()) {				// if user does not change selected values
				   if (savedGames.getSelectedIndex() == -1) {	// if nothing is selected
					   selectBtn.setEnabled(false);				// don't enable select button
				   } else {										// if something valid is selected
					   selectBtn.setEnabled(true);				// enable select button
				   }
			   }
			   
		   }            
		}
		
		// handle list option selection (load the selected game)
		
		class SelectOptionListener implements ActionListener {
		   public void actionPerformed(ActionEvent event) {
			
		  	 if (savedGames.getSelectedIndex() != -1) {
		  		 loadMessage.setText("");
		  		 loadGameFromServer(savedGames.getSelectedValue());
		  		 popupLoad.setVisible(false);	// exit pop-up
		  	 } else {
		  		 loadMessage.setText("You didn't select a game!");
		  	 }
			   
		   }            
		}
		
		// handle "cancel" selection (hide pop-up)
		
		class CancelTextFieldListener implements ActionListener {
		   public void actionPerformed(ActionEvent event) {
			   
			   popupLoad.setVisible(false);	// exit pop-up
			   
		   }            
		}
		
		savedGames.addListSelectionListener(new ListOptionListener());
		selectBtn.addActionListener(new SelectOptionListener());
		cancelBtn.addActionListener(new CancelTextFieldListener());
		
		select.add(loadGameLabel);
		select.add(new JScrollPane(savedGames));
		btn.add(selectBtn);
		btn.add(cancelBtn);
		bottomMessage.add(loadMessage);

		panel.add(select, BorderLayout.NORTH);
		panel.add(btn, BorderLayout.SOUTH);
		panel.add(bottomMessage);
		popupLoad.add(panel);
		
	}
	
	// handle retrieval of saved game names from database
	
	private String[] loadGameNameFromServer() {
		
		try {
			
			// connect to server
			
			socket = new Socket(hostname, port);
			fromServerData = new DataInputStream(socket.getInputStream());
			toServerData = new DataOutputStream(socket.getOutputStream());
			fromServerObject = new ObjectInputStream(socket.getInputStream());
			toServerObject = new ObjectOutputStream(socket.getOutputStream());
			
			// indicate to server that client wants to load previous game names
			
			toServerData.writeUTF("load game names");
			toServerData.flush();
		
			String gameNamesStr = fromServerData.readUTF();
			String[] gameNames = {};
			
			if (!gameNamesStr.isEmpty()) {
				gameNames = gameNamesStr.split(",");
			}
			
			return gameNames;
			
		} catch(IOException ex) {
			System.err.println("IOException caught (client): " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			try {
				toServerData.close();
				fromServerObject.close();
				
				fromServerData.close();
				toServerObject.close();
			} catch (IOException ex) {
				System.err.println("IOException caught (client): " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		
		return null;
		
	}
	
	// handle retrieval of selected game from database
	
	private void loadGameFromServer(String gameName) {
		
		try {
			
			// connect to server
			
			socket = new Socket(hostname, port);
			fromServerData = new DataInputStream(socket.getInputStream());
			toServerData = new DataOutputStream(socket.getOutputStream());
			fromServerObject = new ObjectInputStream(socket.getInputStream());
			toServerObject = new ObjectOutputStream(socket.getOutputStream());
			
			// indicate to server that client wants to load a previous game
			
			toServerData.writeUTF("load game");
			toServerData.flush();
			
			// send the chosen game name
			
			toServerData.writeUTF(gameName);
			toServerData.flush();
			
			GameStatus game = (GameStatus) fromServerObject.readObject();	// get the game
			
			loadSavedGame(game);	// load the saved game
			
		} catch(IOException ex) {
			System.err.println("IOException caught (client): " + ex.getMessage());
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			System.err.println("ClassNotFoundException caught (client): " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			try {
				toServerData.close();
				fromServerObject.close();
				
				fromServerData.close();
				toServerObject.close();
			} catch (IOException ex) {
				System.err.println("IOException caught (client): " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		
	}
	
	// handle the "Save" menu option (save the current game onto the database)
	
	private JMenuItem createFileSaveItem() {
		
		JMenuItem item = new JMenuItem("Save");
		
		/* SaveActionListener will save the current game */
		class SaveActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
				// pause existing game if client is playing one
				
				if (startedGame) {
				  time.stop();
				  startedGame = false;
				}
		  	
				saveMessage.setText("");	// clear any previous messages
				if (!popupSave.isVisible()) popupSave.setVisible(true);	// display pop-up
				
			}
		}
		
		item.addActionListener(new SaveActionListener());
		return item;
		
	}
	
	// create save pop-up
	
	private void createSavePopup() {
		
		popupSave = new JFrame();
		popupSave.setSize(400,200);
		
		JPanel panel = new JPanel();
		JPanel input = new JPanel();
		JPanel feedback = new JPanel();
		
		input.setLayout(new GridLayout(2, 2));
		
		JLabel addGameLabel = new JLabel("Enter a game name: ");
		JTextField addGameTextField = new JTextField(10);
		JButton saveBtn = new JButton("Save Game");
		JButton cancelBtn = new JButton("Close");
		
		saveMessage = new JLabel("");
		
		// handle "save" selection (save the game onto database)

		class SaveTextFieldListener implements ActionListener {
		   public void actionPerformed(ActionEvent event) {
		  	 
			   String gameName = addGameTextField.getText();
			   Date date = new Date();
			   SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
			   
			   if (gameName.equals("")) {
			  	 saveMessage.setText("Please enter a game name!");
			   } else {
				   saveInServer(gameName+" "+formatter.format(date));	// send game data to server
				   saveMessage.setText("Your file has been saved!");
				   addGameTextField.setText("");	// clear text field
			   }
			   
		   }            
		}
		
		// handle "cancel" selection (hide pop-up)

		class CancelTextFieldListener implements ActionListener {
		   public void actionPerformed(ActionEvent event) {
		  	 
			   popupSave.setVisible(false);	// exit pop-up
			   
		   }            
		}
		
		saveBtn.addActionListener(new SaveTextFieldListener());
		cancelBtn.addActionListener(new CancelTextFieldListener());
		
		input.add(addGameLabel);
		input.add(addGameTextField);	
		input.add(saveBtn);
		input.add(cancelBtn);
		feedback.add(saveMessage);

		panel.add(input, BorderLayout.NORTH);
		panel.add(feedback, BorderLayout.SOUTH);
		popupSave.add(panel);
		
	}
	
	// handle the sending of saved information to server (to save onto database)
	
	private void saveInServer(String gameName) {
		
		try {
			
			// connect to server
			
			socket = new Socket(hostname, port);
			fromServerData = new DataInputStream(socket.getInputStream());
			toServerData = new DataOutputStream(socket.getOutputStream());
			fromServerObject = new ObjectInputStream(socket.getInputStream());
			toServerObject = new ObjectOutputStream(socket.getOutputStream());
			
			// indicate to server that client wants to save game
			
			toServerData.writeUTF("save");
			toServerData.flush();
			
			// send game name
			
			toServerData.writeUTF(gameName);
			toServerData.flush();
			
			GameStatus game = new GameStatus(grid, bombsGuessesLeft, currTime, 
												correctlyFlagged, selectedSafeCells);
			
			toServerObject.writeObject(game);
			toServerObject.flush();
			
		} catch(IOException ex) {
			System.err.println("IOException caught (client): " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			try {
				toServerData.close();
				toServerObject.close();
				
				fromServerData.close();
				fromServerObject.close();
			} catch (IOException ex) {
				System.err.println("IOException caught (client): " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		
	}
	
	// handle the "Exit" menu option (exit the GUI)
	
	private JMenuItem createFileExitItem() {		// exits the GUI when selected
		
		JMenuItem item = new JMenuItem("Exit");    
		
		class MenuItemListener implements ActionListener {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		}      

		item.addActionListener(new MenuItemListener());
		return item;
		
	}


	/* Methods to create top and bottom panels */
	
	private void createTopPanel() {
		
		JPanel topPanel = new JPanel();
		timerLabel = new JLabel("Time remaining: " + Integer.toString(currTime));
		topPanel.add(timerLabel);
		add(topPanel, BorderLayout.NORTH);
		
	}
	
	private void createBottomPanel() {
		
		JPanel bottomPanel = new JPanel();
		bottomLabel = new JLabel(Integer.toString(bombsGuessesLeft));
		bottomPanel.add(bottomLabel);
		
		add(bottomPanel, BorderLayout.SOUTH);
		
	}
	
	
	/* Methods for handling game behaviors */
	
	private void revealCell(int row, int col) {
		
		selectedSafeCells += 1;		// a safe cell is selected
		int countBombs = 0;
		
		for (int i = row-1; i <= row + 1; i++) {
			if (i >= 0 && i < ROWS) {
				for (int j = col-1; j <= col + 1; j++) {
					if (j >= 0 && j < COLUMNS && grid[i][j].isBomb()) {
						countBombs += 1;
					}
				}
			}
		}
		
		grid[row][col].setUnderlyingState(countBombs);
		grid[row][col].reveal();
		
		if (countBombs == 0) {		// no bombs in neighboring cells, so reveal neighbors too
			revealNeighboringCells(row, col);
		}
		
	}
	
	private void revealNeighboringCells(int row, int col) {
		
		// loop through neighbors and reveal those cells
		
		for (int i = row-1; i <= row + 1; i++) {
			if (i >= 0 && i < ROWS) {
				for (int j = col-1; j <= col + 1; j++) {
					if (j >= 0 && j < COLUMNS && !grid[i][j].isRevealed()) {
						// if flagged cell is found to be safe
						if (grid[i][j].isFlagged()) 	// restore a flag opportunity to player
							bottomLabel.setText(Integer.toString(++bombsGuessesLeft));
							
						revealCell(i, j);
					}
				}
			}
		}
		
	}
	
	private void loseGame() {
		
		time.stop();
		startedGame = false;
		endGame = true;
		
		// loop through to reveal the rest of the bombs & indicate correctness of flagged cells
		
		for (int i = 0; i < ROWS; i++) {
			for (int j = 0; j < COLUMNS; j++) {
				if (!grid[i][j].isRevealed()) {		// if not yet revealed
					if (grid[i][j].isFlagged() && !grid[i][j].isBomb()) {	// if flagged but not a bomb
						grid[i][j].setGuessedWrong();
					} else if (!grid[i][j].isFlagged() && grid[i][j].isBomb()){	// if not flagged and is a bomb
						grid[i][j].reveal();
					}
				}
			}
		}
		
		bottomLabel.setText("Sorry, you lost... Better luck next time :)");
		
	}
	
	private void winGame() {
		
		time.stop();
		startedGame = false;
		endGame = true;
		bottomLabel.setText("Congratulations! You beat the game!");
		
	}
	
	public static void main(String[] argv) {
		Minesweeper game = new Minesweeper("localhost", 8080);
	}
	
}
