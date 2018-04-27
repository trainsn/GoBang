import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.plaf.metal.MetalBorders.OptionDialogBorder;

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


public class GameClient 
	implements GameConstants{
	

	
	JFrame frame =  new JFrame("Chess Game");
	 JLabel messageLabel = new JLabel("");
	 ImageIcon icon;
	 ImageIcon opponentIcon;
	
	 Square[][] board = new Square[SIZE][SIZE];
	 Square currentSquare;
	
	 static int PORT = 8901;
	 Socket socket;
	 BufferedReader in;
	 PrintWriter out;
	
	/**
	 * Constructs the client by connecting to a server, laying out 
	 * the GUI and registering GUI listeners.
	 */
	public GameClient(String serverAddress) throws Exception{
		
		//Setup networking 
		socket = new Socket(serverAddress, PORT);
		in = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(),true);
		
		//Layout GUI
		messageLabel.setBackground(Color.lightGray);
		frame.getContentPane().add(messageLabel,"South");
		
		JPanel boardPanel = new JPanel();
		boardPanel.setBackground(Color.black);
		boardPanel.setLayout(new GridLayout(SIZE, SIZE,2,2));
		for (int i=0;i<SIZE;i++)
			for(int j=0;j<SIZE;j++)
			{
				final int fi = i;
				final int fj = j;
				board[i][j] = new Square();
				board[i][j].addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent e){
						currentSquare = board[fi][fj];
						out.println("MOVE "+fi+fj);
					}
				});
				boardPanel.add(board[i][j]);
			}
		frame.getContentPane().add(boardPanel, "Center");
	}
	
	/**
	 * The main thread of the client will listen for messages 
	 * from the server. The first message will be a "WELCOME"
	 * message in which we receive our mark. Then we got into 
	 * a loop listening for "VALID_", "OPPOMENT_MOVED", 
	 * "VICTORY", "DEFEAT", "TIE", "OPPONENT_QUIT" OR "MESSAGE" 
	 * messages, and handling each message approximately. The 
	 * "VICTORY", "DEFEAT" and "TIE" ask the user whether or not 
	 * to play another another game. If the answer is no, the loop 
	 * is exited and the server is sent a "QUIT" message. If an
	 * OPPONENT_QUIT message is received then the loop will exit 
	 * and the server will be sent a "QUIT" message also.   
	 */
	public void play() throws Exception{
		String response;
		try {
			response = in.readLine();
			if (response.startsWith("WELCOME")){
				char mark = response.charAt(8);
				frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						out.println("QUIT"+mark);
						System.exit(0);
					}
				});
				icon = new ImageIcon(mark == 'X' ? "X.png" : "O.png");
				opponentIcon = new ImageIcon(mark == 'X' ? "O.png" : "X.png");
				frame.setTitle("Chess Game PLayer" + mark);
			}
			while (true){
				response = in.readLine();
				while (response==null)
					continue;
				if (response.startsWith("VALID_MOVE")){
					messageLabel.setText("Valid move, please wait");
					currentSquare.setIcon(icon);
					currentSquare.repaint();
				}else if (response.startsWith("OPPONENT_MOVED")){
					int loc =  Integer.parseInt(response.substring(15));
					int locX = loc/10;;
					int locY = loc%10;
					board[locX][locY].setIcon(opponentIcon);
					board[locX][locY].repaint();
					messageLabel.setText("Opponent moved, your turn");
				}else if (response.startsWith("VICTORY")){
					messageLabel.setText("You win");
					break;
				}else if (response.startsWith("DEFEAT")){
					messageLabel.setText("You lose");
					break;
				}else if (response.startsWith("TIE")){
					messageLabel.setText("You tied");
					break;
				}else if (response.startsWith("MESSAGE")){
					messageLabel.setText(response.substring(8));
				}else if (response.startsWith("OPPONENT_QUIT")){
					messageLabel.setText("Your opponent has leaved the game");
					break;
				}
			}
			out.println("QUIT");
		} 
		finally{
			socket.close();
		}
	}
	
	boolean wantsToPlayAgain(){
		int response = JOptionPane.showConfirmDialog(frame, 
							"Want to Play again?",
							"Chess Game is really fun!",
							JOptionPane.YES_NO_OPTION);
		frame.dispose();
		return response == JOptionPane.YES_OPTION;
	}
	
	/**
	 * Graphical square in the client window. Each square is 
	 * a white panel containing. A client calls setIcon() to fill
	 * it with an Icon, presumably an X or O.
	 */
	static class Square extends JPanel{
		JLabel label = new JLabel((Icon)null);
		
		public Square(){
			setBackground(Color.white);
			add(label);
		}
		
		public void setIcon(Icon icon){
			label.setIcon(icon);
		}
	}

	
	/**
	 * Runs the client as an application
	 */
	public static void main(String[] args) throws Exception{ 
		while (true)
		{
			String serverAddress = "localhost";
			GameClient client = new GameClient(serverAddress);
					 
			client.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			client.frame.setSize(120*SIZE, 120*SIZE);
			client.frame.setVisible(true);
			client.frame.setResizable(false);
			client.play();
			if (!client.wantsToPlayAgain()){
				break;
			}
		}
	} 
} 