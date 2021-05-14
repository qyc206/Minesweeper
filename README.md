# Minesweeper

This is a Java implementation of the game [Minesweeper](https://en.wikipedia.org/wiki/Minesweeper_(video_game)), with a few variants. 

When playing the game, a player is able to load and save previous games, each game has a 1000 second timer that will count down to zero, and when the counter reaches 0, the game is lost. A game is won by either placing all 40 flags correctly (i.e. guessed all the bombs) or revealing all the boxes that do not contain a bomb. 

The loading and saving of a game is done on a server, which connects to a database. The server accepts connections from different Minesweeper applications. Saved games across multiple players can be accessed by any player, and any player can save their game. 

## Additional Notes

1. This project is completed and tested using [Eclipse](https://www.eclipse.org/downloads/packages/release/luna/sr1/eclipse-ide-java-developers). When using Eclipse, make sure to add the sqlite-jdbc-3.30.1.jar to the project (right click the project, then navigate to Properties -> Java Build Path -> Libraries -> Add JARs).

2. The database (savedGames.db) currently has sample saved games. To get a new empty database file, run createEmptyDB.py file. 

3. When running this code, make sure to start the server before attempting to load or save games as the client (i.e. run Server.java before running Minesweeper.java).
