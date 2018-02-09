import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
/**
 *	Main frame of the game. Stores high-level objects, namely one {@link GameServer GameServer} and one {@link GameClient GameClient}.
**/
public class GameFrame extends JFrame{
	private GameServer gameServer = null;
	private GameClient gameClient = null;
	public String playerName = null;
	/**
	 *	Creates a new <code>GameFrame</code>, and sets it to the {@link MenuState.MainMenu MenuState.MainMenu} state.
	**/
	public GameFrame(){
		super("Kolortris");
		this.setState(new MenuState.MainMenu(this));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.getContentPane().setPreferredSize( new Dimension( 800, 600 ) );
		this.setResizable(false);
		this.pack();
		this.setVisible(true);
	}
	/**
	 *	Sets the state of this <code>GameFrame</code>.
	 *	@param newState the new State of this <code>GameFrame</code>
	**/
	public void setState(MenuState.MenuPanel newState){
		this.getContentPane().removeAll();
		this.getContentPane().add(newState);
		this.revalidate();
		this.repaint();
	}
	/**
	 *	Instantiates a {@link GameServer GameServer}, properly destroying any existing <code>GameServer</code>s if they exist.
	**/
	public void createServer(){
		if(gameServer!=null) gameServer.destroy();
		gameServer = new GameServer(new GameInstance());
	}
	/**
	 *	Instantiates a {@link GameClient GameClient}, properly destroying any existing <code>GameClient</code>s if they exist.
	 *	@param IPAddress the IP address (or host name) to connect to
	 *	@return <code>true</code> if connection was successful, and <code>false</code> otherwise
	**/
	public boolean createClient(String IPAddress){
		if(gameClient!=null) gameClient.destroy();
		gameClient = new GameClient(new GameInstance());
		if(!gameClient.connectTo(IPAddress)){
			gameClient.destroy();
			gameClient = null;
			return false;
		}
		else return true;
	}
	/**
	 *	Clears the {@link GameServer GameServer} and/or {@link GameClient GameClient} instances, if they exist.
	**/
	public void destroyGameManagers(){
		if(gameServer!=null) gameServer.destroy();
		if(gameClient!=null) gameClient.destroy();
		gameServer = null;
		gameClient = null;
	}
	/**
	 *	Returns the {@link GameServer GameServer} stored in this <code>GameFrame</code>.
	 *	@return the {@link GameServer GameServer} stored in this <code>GameFrame</code>.
	**/
	public GameServer getGameServer(){
		return gameServer;
	}
	/**
	 *	Returns the {@link GameClient GameClient} stored in this <code>GameFrame</code>.
	 *	@return the {@link GameClient GameClient} stored in this <code>GameFrame</code>.
	**/
	public GameClient getGameClient(){
		return gameClient;
	}
}
