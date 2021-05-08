package qyc206.minesweeper;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class CellPanel extends JPanel {
	private boolean isRevealed;
	private boolean isBomb = false;
	private boolean isFlagged = false;
	private int rowPos;
	private int colPos;
	
	private Image img;
	private int underlyingState = 0;
	
	public CellPanel(int row, int col) {
		this.rowPos = row;
		this.colPos = col;
	}
	
	// getters
	public boolean isBomb() { return isBomb; }
	public boolean isRevealed() { return isRevealed; }
	public boolean isFlagged() { return isFlagged; }
	public int getRowPos() { return rowPos; }
	public int getColPos() { return colPos; }
	
	public void placeBomb() { 
		this.isBomb = true; 
		this.underlyingState = 9;
	}
	
	public void placeFlag() {
		isFlagged = true;
		setImg("res/minesweepertiles/11.png");
		repaint();
	}
	
	public void removeFlag() { 
		isFlagged = false; 
		coverCell();
	}
	
	public void setGuessedWrong() {
		setImg("res/minesweepertiles/12.png");
		repaint();
	}
	
	public void reveal() { 
		this.isRevealed = true; 
		setImg("res/minesweepertiles/"+underlyingState+".png");
		repaint();
	}
	
	public void coverCell() {
		this.isRevealed = false;
		setImg("res/minesweepertiles/10.png");
		repaint();
	}
	
	public void setImg(String img) { 
		this.img = new ImageIcon(img).getImage();
		
		Dimension size = new Dimension(this.img.getWidth(null), this.img.getHeight(null));
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
		setSize(size);
		setLayout(null);
	}
	
	public void setUnderlyingState(int state) { underlyingState = state; }
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);	// clears area
		g.drawImage(img, 0, 0, null);
	}
}
