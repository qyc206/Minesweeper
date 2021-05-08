package qyc206.minesweeper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


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
	
	private Connection conn;
	private PreparedStatement queryForGame, insertGameStatement;
	
	public Minesweeper() {
		
		// start connection to database with saved games
		
//		try {
//			conn = DriverManager.getConnection("jdbc:sqlite:savedGames.db");
//			
//			queryForGame = conn.prepareStatement("");
//			insertGameStatement = conn.prepareStatement("");
//		} catch (SQLException e) {
//			System.err.println("Connection error: " + e);
//			System.exit(1);
//		}
		
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
		
		setVisible(true);
		
	}
	
	
	/* Methods for starting a new game */
	
	private void loadNewGame() {
		
		// initialize grid with cells
		
		grid = new CellPanel[ROWS][COLUMNS];
		
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
				
				grid[i][j].coverCell();
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
	
	private JMenuItem createFileNewItem() {		// starts a new game when selected
		
		JMenuItem item = new JMenuItem("New");
		
		/* NewActionListener that will load a new game */
		class NewActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				resetFields();
				loadNewGame();
			}
		}
		
		item.addActionListener(new NewActionListener());
		return item;
		
	}
	
	private JMenuItem createFileOpenItem() {
		
		JMenuItem item = new JMenuItem("Open");
		
		/* OpenActionListener that will open the file chooser */
		class OpenActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
			}
		}
		
		item.addActionListener(new OpenActionListener());
		return item;
		
	}
	
	private JMenuItem createFileSaveItem() {
		
		JMenuItem item = new JMenuItem("Save");
		
		/* OpenActionListener that will save the current game */
		class SaveActionListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				
			}
		}
		
		item.addActionListener(new SaveActionListener());
		return item;
		
	}
	
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
	
	private void createBottomPanel() {
		
		JPanel bottomPanel = new JPanel();
		bottomLabel = new JLabel(Integer.toString(bombsGuessesLeft));
		bottomPanel.add(bottomLabel);
		
		add(bottomPanel, BorderLayout.SOUTH);
		
	}
	
	private void createTopPanel() {
		
		JPanel topPanel = new JPanel();
		timerLabel = new JLabel("Time remaining: " + Integer.toString(currTime));
		topPanel.add(timerLabel);
		add(topPanel, BorderLayout.NORTH);
		
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
		Minesweeper game = new Minesweeper();
	}
	
}
