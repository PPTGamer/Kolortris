import javax.swing.*;
import java.awt.event.*;
/** 
 * 	The <code>Commands</code> enclosing class contains all command classes. Each command class is an <code>AbstractAction</code> which is invoked from a {@link GameDisplay GameDisplay}, and modifies a {@link Playfield Playfield} in the {@link GameInstance GameInstance} associated with the <code>GameDisplay</code>.
 *	<p> Every state object inherits from the {@link Commands.Command Commands.Command} abstract base class.
**/
public final class Commands{
	/**
	 *	 Abstract base class for game commands.
	**/
	public abstract static class Command extends AbstractAction{
		/**
		 *	The <code>Playfield</code> this command acts on.
		**/
		protected Playfield source = null;
		/**
		 *	The player ID associated with the <code>Playfield</code> this command acts on.
		**/
		protected Integer playerID = null;
		/**
		 *	This function takes an <code>ActionEvent</code> from a <code>GameDisplay</code> component, changes the source to the appropriate <code>Playfield</code>, then calls the {@link #execute() execute()} method.
		 * <p> If this command object is not bound to a player ID, nothing happens.
		 *	@param e an <code>ActionEvent</code> from a <code>GameDisplay</code> component
		**/
		public void actionPerformed(ActionEvent e){
			if(playerID==null){
				return;
			}
			source = ((GameDisplay)e.getSource()).getGameInstance().getPlayfield(playerID);
			execute();
		}
		/**
		 *	Sets the player ID that this command acts on.
		 *	@param playerID the player ID to bind to this command
		**/
		public void setPlayerID(int playerID){
			this.playerID = playerID;
		}
		/**
		 *	Returns the class name of this command.
		 * @return the class name of this command
		**/
		public String getCommandName(){
			return this.getClass().getName();
		}
		/**
		 *	Performs a command on the <code>Playfield</code> this command acts on.
		**/
		abstract protected void execute();
	}
	/**
	 *	A {@link Commands.Command Commands.Command} which moves the current piece to the left.
	**/
	public static class MoveLeft extends Command{
		protected void execute(){
			if(source.getCurrentPiece()!=null){
				source.getCurrentPiece().move(-1,0);
			}
		}
	}
	/**
	 *	A {@link Commands.Command Commands.Command} which moves the current piece to the right.
	**/
	public static class MoveRight extends Command{
		protected void execute(){
			if(source.getCurrentPiece()!=null){
				source.getCurrentPiece().move(1,0);
			}
		}
	}
	/**
	 *	<b>Debug command:</b> A {@link Commands.Command Commands.Command} which moves the current piece upward.
	**/
	public static class MoveUp extends Command{
		protected void execute(){
			if(source.getCurrentPiece()!=null){
				source.getCurrentPiece().move(0,-1);
			}
		}
	}
	/**
	 *	A {@link Commands.Command Commands.Command} which moves the current piece downward.
	**/
	public static class MoveDown extends Command{
		protected void execute(){
			if(source.getCurrentPiece()!=null){
				source.getCurrentPiece().move(0,1);
			}
		}
	}
	/**
	 *	A {@link Commands.Command Commands.Command} which rotates the current piece clockwise.
	**/
	public static class Rotate extends Command{
		protected void execute(){
			if(source.getCurrentPiece()!=null){
				source.getCurrentPiece().rotate();
			}
		}
	}
	/**
	 *	A {@link Commands.Command Commands.Command} which holds the current piece.
	**/
	public static class Hold extends Command{
		protected void execute(){
			source.hold();
		}
	}
	/**
	 *	A {@link Commands.Command Commands.Command} which hard drops the current piece.
	**/
	public static class HardDrop extends Command{
		protected void execute(){
			if(source.getCurrentPiece()!=null){
				source.getCurrentPiece().hardDrop();
			}
		}
	}
	
	
	private static String savedState = null;
	/**
	 *	<b>Debug command:</b> A {@link Commands.Command Commands.Command} which saves the state of this <code>Playfield</code> in memory.
	**/
	public static class SaveState extends Command{
		protected void execute(){
			savedState = source.toString();
			System.out.println("saved state\n" + savedState); 
		}
	}
	/**
	 *	<b>Debug command:</b> A {@link Commands.Command Commands.Command} which loads the state of this <code>Playfield</code> from memory.
	**/
	public static class LoadState extends Command{
		protected void execute(){
			if(savedState==null){
				return;
			}
			source.loadData(savedState);
		}
	}
	/**
	 *	<b>Debug command:</b> A {@link Commands.Command Commands.Command} which adds one garbage line.
	**/
	public static class AddGarbage extends Command{
		protected void execute(){
			source.addGarbageToQueue(1);
		}
	}
}