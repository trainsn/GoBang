import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NonWritableChannelException;
import java.util.ArrayList;

import javax.naming.directory.InitialDirContext;

/**
 *  Client -> Server           	  Server -> Client
 *  ----------------           	  ----------------
 *  MOVE <xy>  (0 <= x,y <= 4)    WELCOME <char>  (char in {X, O})
 *  QUIT <char> (char in {X, O})  VALID_MOVE
 *                             	  OPPOMENT_MOVED <xy>
 *                                VICTORY
 *                                DEFEAT
 *                                TIE
 *                                MESSAGE <text>
 */
public class GameServer {
	/**
	 * Runs the application. Pairs up clients that connect.
	 */
	public static void main(String[] args) throws Exception{
		ServerSocket listener = new ServerSocket(8901);
		System.out.println("Game Servrer is running");
		
		ArrayList<Game> gameList= new ArrayList<Game>();
		ArrayList<Game.Player> playerXLIst = new ArrayList<Game.Player>();
		ArrayList<Game.Player> playerOLIst = new ArrayList<Game.Player>();
		
		try {
			while (true){
				gameList.add(new Game());		
				int index = gameList.size();
				Game game = gameList.get(index-1);
				
				playerXLIst.add(game.new Player(listener.accept(), 'X'));
				playerOLIst.add(game.new Player(listener.accept(), 'O'));

				Game.Player playerX = playerXLIst.get(index-1);
				Game.Player playerO = playerOLIst.get(index-1);	
				playerX.setopponent(playerO);
				playerO.setopponent(playerX);
				
				game.currentPlayer = playerX;
				playerX.start();
				playerO.start();			
			}
		} finally {
			listener.close();
		}
	}
}

/**
 * A two-player game 
 */
class Game implements GameConstants
{

	//private static final int SIZE = 5;
	/**
	 * A board has 25 squares. Each square is either unowned or 
	 * it is owned by a player. So we use a simple array of player
	 * references. If null the corresponding square is unowned,
	 * otherwise the array cell stores a reference to the player that 
	 * owns it.
	 */
	private Player[][] board= new Player[SIZE][SIZE];
	
	/**
	 * The current player.
	 */
	Player currentPlayer;
	
	/**
	 * Returns whether the current state of the board is such that one 
	 * of the player is a winner.
	 */
	public boolean hasWinner(){
		boolean flag=false;
		//row
		for (int i=0;i<SIZE;i++)
		{
			if (board[i][0]!=null)
			{
				flag=true;
				for (int j=1;j<SIZE;j++)
				{
					if (board[i][j]!=board[i][j-1])
					{
						flag=false;
						break;
					}
				}
				if (flag)
					return true;
			}
		}
		
		//col
		for (int j=0;j<SIZE;j++)
		{
			if (board[0][j]!=null)
			{
				flag=true;
				for (int i=1;i<SIZE;i++)
				{
					if (board[i][j]!=board[i-1][j])
					{
						flag=false;
						break;
					}
				}
				if (flag)
					return true;
			}
		}
		
		//leading diagonal
		if (board[0][0]!=null)
		{
			flag=true;
			for (int i=1;i<SIZE;i++)
				if (board[i][i]!=board[i-1][i-1])
					flag=false;
		}
		if (flag)
			return true;
		
		//secondary diagonal
		if (board[0][SIZE-1]!=null)
		{
			flag=true;
			for (int i=1;i<SIZE;i++)
				if (board[i][SIZE-i-1]!=board[i-1][SIZE-i])
					flag=false;
		}
		if (flag)
			return true;
		
		return false;
	}
	
	/**
	 * Returns whether there are no more empty squares
	 */
	public boolean boardFilledUp(){
		for (int i=0;i<SIZE;i++)
			for (int j=0;j<SIZE;j++)
				if (board[i][j]==null)
					return false;
		return true;
	}
	
	/**
	 * Called by the player threads when a player tries to make a 
	 * move. This method checks to see if the move is legal: that 
	 * is, the player requesting the move must be the current player 
	 * and the square in which she is trying to move must not already
	 * be occupied. If the move is legal the game state is updated 
	 * (the square is set and the next player becomes current) and 
	 * the other player is notified so it can update its client.
	 */
	public synchronized boolean legalMove(int locationX,int locationY,Player player){
		if (player == currentPlayer && board[locationX][locationY]==null)
		{
			board[locationX][locationY]=currentPlayer;
			currentPlayer = currentPlayer.opponent;
			currentPlayer.otherPlayerMoved(locationX, locationY);
			return true;
		}
		return false;
	}
	
	/**
	 * The class for the helper threads in this multithreaded sever 
	 * application. A player is identified by a character mark which 
	 * is either 'X' or 'O'. For communication with the client  
	 * the player has a socket with its input and output stream.
	 * Since only only text is being communicated  we use a reader and a 
	 * writer.  
	*/
	class Player extends Thread
	{ 
		char mark;
		Player opponent;
		Socket socket;
		BufferedReader input;
		PrintWriter output;
		
		/**
		 * Constructs a handler thread for a given socket and mark 
		 * initializes the input stream fields, displays the first 
		 * two welcoming messages.
		 */
		public Player(Socket socket,char mark) {
			this.socket=socket;
			this.mark=mark;
			try {
				input = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
				output = new PrintWriter(socket.getOutputStream(),true);
				output.println("WELCOME " + mark);
				output.println("MESSAGE waiting for opponent to connect");
			}catch (IOException e){
				System.out.println("Player died:"+e);
			}
		}
		
		/**
		 * Accepts notification of who the opponent is 
		 */
		public void setopponent (Player opponent){
			this.opponent=opponent;
		}
		
		/**
		 * Handles the otherPlayerMoved message.
		 */
		public void otherPlayerMoved(int locationX,int locationY) {
			output.println("OPPONENT_MOVED "+ locationX+locationY);
			output.println(
					hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
		}
		
		public void otherPlayerLeave(){
			JDBC jdbc = new JDBC();
			jdbc.insertRecord(this.mark);
			output.println("OPPONENT_QUIT");
		}
		
		public void run(){
			try{
				// The thread is only started after everyone connects.
				output.println("MESSAGE All player connected");
				
				//Tell the first player that it is her turn.
				if (mark=='X'){
					output.println("MESSAGE Your move");
				}
				
				//Repeated get commands from the client and process them
				while (true)
				{
					String command = input.readLine();
					while (command == null)
						continue;
					if (command.startsWith("MOVE")){
						int loc =  Integer.parseInt(command.substring(5));
						int locationX = loc / 10;
						int locationY = loc % 10;
						if (legalMove(locationX, locationY,this)){
							output.println("VALID_MOVE");
							boolean hasWin = hasWinner();
							boolean fill = boardFilledUp();
							JDBC jdbc= new JDBC();
							if (hasWin)
							{							
								jdbc.insertRecord(this.mark);
							}else if (fill)
							{
								jdbc.insertRecord('N');
							}
							output.println(hasWinner() ? "VICTORY"
										: boardFilledUp() ? "TIE" 
										: "");
						} else {
							output.println("MEESAGE ?");
						} 
					} else if (command.startsWith("QUIT")){
						if (command.length()==5){
							if (command.charAt(4)==currentPlayer.mark)
								currentPlayer.opponent.otherPlayerLeave();
							else 
								currentPlayer.otherPlayerLeave();
						}						
						return;
					}
				}
			} catch (IOException e){
				System.out.println(currentPlayer.mark);
				System.out.println("Player died:" + e);
			} finally {
				try {socket.close();} catch (IOException e) {}
			}
		}
	}
}