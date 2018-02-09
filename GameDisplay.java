import javax.swing.*;
import javax.swing.border.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.*;
/**
 *	A <code>JComponent</code> which displays a {@link GameInstance GameInstance}.
**/
public class GameDisplay extends JComponent{
	private static int numObjects = 0;
	private GameInstance gameInstance;
	private int focusedPlayfield = 0;
	private int[] playfieldOrder;
	private Font uiFont;
	
	/**
	 *	Creates a <code>GameDisplay</code> with an associated {@link GameInstance GameInstance}.
	 *	@param gameInstance the <code>GameInstance</code> to associate with this <code>GameDisplay</code> 
	**/
	public GameDisplay(GameInstance gameInstance){
		this.gameInstance = gameInstance;
		this.setPreferredSize(new Dimension(800,600));
		this.setBorder(new EmptyBorder(0,0,0,0));
		javax.swing.Timer timer = new javax.swing.Timer(30,new ActionListener(){
			public void actionPerformed(ActionEvent e){
				repaint();
			}
		});
		timer.start();
		try{
			uiFont = Font.createFont(Font.TRUETYPE_FONT, new File("joystix monospace.ttf"));
			uiFont = uiFont.deriveFont(24f);
		}catch (Exception e){
			uiFont = null;
			System.out.println("Failed to load resources.");
		}
	}	
	private void addCommand(KeyStroke ks, Commands.Command c){
		this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks,c.getCommandName());
		this.getActionMap().put(c.getCommandName(),c);
	}
	/**
	 *	Obtains the <code>GameInstance</code> associated with this <code>GameDisplay</code>.
	 *	@return the <code>GameInstance</code> associated with this <code>GameDisplay</code>
	**/
	public GameInstance getGameInstance(){
		return gameInstance;
	}
	/**
	 *	Sets the focused playfield.
	 *	@param playerID the player ID of the playfield to focus on
	**/
	public void setFocusedPlayfield(int playerID){
		focusedPlayfield = playerID;
	}
	/**
	 *	Draws this <code>GameDisplay</code> using the provided <code>Graphics</code> context.
	 *	@param g the <code>Graphics</code> context to draw this GameDisplay with.
	**/
	protected void paintComponent(Graphics g){
		Graphics2D g2d = (Graphics2D)g;
		
		int time = gameInstance.getTime();
		String timeString;
		if(time%60<10) timeString = "0"+(time/60)+":0"+(time%60);
		else timeString = "0"+(time/60)+":"+(time%60);
		g2d.setPaint(Color.WHITE);
		g2d.setFont(uiFont);
		g2d.drawString(timeString,370,590);
		
		int numPlayers = gameInstance.getNumberOfPlayers();
		playfieldOrder = new int[numPlayers];
		for(int i = 0; i<numPlayers; i++){
			playfieldOrder[i] = i;
		}
		if(numPlayers>1){
			playfieldOrder[0] = focusedPlayfield;
			playfieldOrder[focusedPlayfield] = 0;
			Arrays.sort(playfieldOrder,1,numPlayers-1);
		}
		switch(numPlayers){
			case 1:
			g2d.translate(320,50);
			gameInstance.getPlayfield(playfieldOrder[0]).draw(g2d,20,true);
			break;
			case 2:
			g2d.translate(120,50);
			gameInstance.getPlayfield(playfieldOrder[0]).draw(g2d,20,true);
			g2d.translate(360,0);
			gameInstance.getPlayfield(playfieldOrder[1]).draw(g2d,20,false);
			break;
			case 3:
			g2d.translate(90,50);
			gameInstance.getPlayfield(playfieldOrder[0]).draw(g2d,20,true);
			g2d.translate(320,30);
			gameInstance.getPlayfield(playfieldOrder[1]).draw(g2d,15,false);
			g2d.translate(170,0);
			gameInstance.getPlayfield(playfieldOrder[2]).draw(g2d,15,false);
			break;
			case 4:
			g2d.translate(90,50);
			gameInstance.getPlayfield(playfieldOrder[0]).draw(g2d,20,true);
			g2d.translate(300,50);
			gameInstance.getPlayfield(playfieldOrder[1]).draw(g2d,12,false);
			g2d.translate(135,0);
			gameInstance.getPlayfield(playfieldOrder[2]).draw(g2d,12,false);
			g2d.translate(135,0);
			gameInstance.getPlayfield(playfieldOrder[3]).draw(g2d,12,false);
		}
	}
	
}