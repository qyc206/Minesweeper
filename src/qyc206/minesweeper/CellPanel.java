package qyc206.minesweeper;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class CellPanel extends JPanel implements Serializable {

	private boolean isRevealed = false;
	private boolean isBomb = false;
	private boolean isFlagged = false;
	private int rowPos;
	private int colPos;
	
	transient BufferedImage img;
	private int underlyingState = 0;
	
	public CellPanel(int row, int col) {
		this.rowPos = row;
		this.colPos = col;
	}
	
	// implement custom writeObject and readObject (for serializing/de-serializing this object)

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		ImageIO.write(img, "png", out);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		img = ImageIO.read(in);
	}
	
	// getters

	public boolean isBomb() { return isBomb; }
	public boolean isRevealed() { return isRevealed; }
	public boolean isFlagged() { return isFlagged; }
	public int getRowPos() { return rowPos; }
	public int getColPos() { return colPos; }
	
	// setters (behaviors)

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
		setImg("res/minesweepertiles/10.png");
		repaint();
	}
	
	public void setImg(String img) { 
		try {
			this.img = ImageIO.read(new File(img));
			
			Dimension size = new Dimension(this.img.getWidth(null), this.img.getHeight(null));
			setPreferredSize(size);
			setMinimumSize(size);
			setMaximumSize(size);
			setSize(size);
			setLayout(null);
		} catch (IOException e) {
			System.err.println("IOException caught: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void setUnderlyingState(int state) { underlyingState = state; }
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);	// clears area
		g.drawImage(img, 0, 0, null);
	}

}
