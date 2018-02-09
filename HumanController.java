import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
/**
 *	Assigns key bindings to a {@link Playfield Playfield} through a {@link GameDisplay GameDisplay}
**/
public class HumanController{
	ArrayList<KeyBinding> keyBindings;
	private Integer playerID;
	private GameDisplay currentDisplay;
	/**
	 *	Creates a new <code>HumanController</code>.
	**/
	public HumanController(){
		this.currentDisplay = null;
		this.playerID = null;
		this.keyBindings = new ArrayList<KeyBinding>();
		keyBindings.add(new KeyBinding(KeyStroke.getKeyStroke("SPACE"),new Commands.HardDrop()));
		keyBindings.add(new KeyBinding(KeyStroke.getKeyStroke("DOWN"),new Commands.MoveDown()));
		keyBindings.add(new KeyBinding(KeyStroke.getKeyStroke("LEFT"),new Commands.MoveLeft()));
		keyBindings.add(new KeyBinding(KeyStroke.getKeyStroke("RIGHT"),new Commands.MoveRight()));
		keyBindings.add(new KeyBinding(KeyStroke.getKeyStroke("UP"),new Commands.Rotate()));
		keyBindings.add(new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,InputEvent.SHIFT_DOWN_MASK, false),new Commands.Hold()));
	}
	private class KeyBinding{
		public KeyStroke keyStroke;
		public Commands.Command command;
		protected KeyBinding(KeyStroke keyStroke, Commands.Command command){
			this.keyStroke = keyStroke;
			this.command = command;
		}
	}
	private void setCommand(KeyBinding kb){
		if(currentDisplay!=null){
			currentDisplay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(kb.keyStroke,kb.command.getCommandName());
			currentDisplay.getActionMap().put(kb.command.getCommandName(),kb.command);
		}
	}
	/**
	 *	Binds this  <code>HumanController</code>'s key bindings to a {@link Playfield Playfield} through a {@link GameDisplay GameDisplay}.
	 * @param display the GameDisplay which receives the key bindings
	 * @param playerID the player ID of the Playfield which is affected by the key bindings
	**/
	public void bindToDisplay(GameDisplay display, int playerID){
		this.currentDisplay = display;
		this.playerID = playerID;
		this.currentDisplay.setFocusedPlayfield(playerID);
		for(KeyBinding kb : keyBindings){
			kb.command.setPlayerID(playerID);
			setCommand(kb);
		}
	}
}
