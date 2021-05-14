package qyc206.minesweeper;

import java.io.Serializable;

public class GameStatus implements Serializable {

	// game status info

	private CellPanel[][] grid;
	private int bombsGuessesLeft, currTime;
	private int correctlyFlagged, selectedSafeCells;
	
	public GameStatus(CellPanel[][] grid, int bombsGuessesLeft, int currTime,
						int correctlyFlagged, int selectedSafeCells){
		this.grid = grid;
		this.bombsGuessesLeft = bombsGuessesLeft;
		this.currTime = currTime;
		this.correctlyFlagged = correctlyFlagged;
		this.selectedSafeCells = selectedSafeCells;
	}
	
	// getters

	public CellPanel[][] getGrid() { return grid; }
	public int getBombsGuessesLeft() { return bombsGuessesLeft; }
	public int getCurrTime() { return currTime; }
	public int getCorrectlyFlagged() { return correctlyFlagged; }
	public int getSelectedSafeCells() { return selectedSafeCells; }
}
