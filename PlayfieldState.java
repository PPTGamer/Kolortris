/** 
 * 	The <code>PlayfieldState</code> enclosing class contains all playfield state object classes. Each state object class represents a possible state of a {@link Playfield Playfield}, and also defines its behavior and possible state transitions.
 *	<p> Every state object inherits from the {@link PlayfieldState.State PlayfieldState.State} abstract base class.
**/
public final class PlayfieldState{
	/** 
	 * 	Abstract base class for State objects assigned to a Playfield.
	 *	The exact behavior of a State depends on the implementation of the abstract {@link #update() update()} method.
	**/
	public abstract static class State{
		/** 
		 * 	A reference to the Playfield associated with the state.
		**/
		protected Playfield actor = null;
		
		/**
		 *	Helper function which obtains the class name of a object derived from <code>State</code>.
		 *	@return A <code>String</code> which contains the name of the state class.
		**/
		public String getName(){
			return this.getClass().getName();
		}
		/**
		 *	Updates <code>actor</code> based on the current state.
		 *	This method is called once per frame.
		**/
		public abstract void update();
	}
	/** 
	 * 	The <code>PlayfieldState.Normal</code> state object represents a normal state of gameplay.
	 *	<p> The <code>Normal</code> state has the following gameplay characteristics:
	 *	<ul>
	 *		<li> There is a piece currently in play, and the player can interact with it. </li>
	 *		<li> Gravity acts on the current piece in play. </li>
	 *	</ul>
	 *	<p> The <code>Normal</code> state has the following state transitions:
	 *	<ul>
	 *		<li> When the current piece is grounded, the playfield changes to the {@link PlayfieldState.Grounded Grounded} state. </li>
	 *		<li> When the current piece is hard dropped, the playfield changes to the {@link PlayfieldState.Reload Reload} state. </li>
	 *		<li> When the playfield overflows such that no new piece can spawn without immediately colliding, the playfield changes to the {@link PlayfieldState.GameOver GameOver} state. </li>
	 *	</ul>
	 *	<p> The above behavior is implemented in this state's implementation of the {@link #update() update()}
	**/
	public static class Normal extends State{
		int framesPerCell = 10;
		private int count;
		/**
		 *	Creates a new <code>Normal</code> state object which is associated with a <code>Playfield</code>.
		 *	@param actor the <code>Playfield</code> to associate with this state object
		**/
		public Normal(Playfield actor){
			this.actor = actor;
			this.count = 0;
			actor.setHoldEnabled(true);
		}
		@Override
		public void update(){
			try{
				count++;
				if(count>=framesPerCell){
					actor.getCurrentPiece().move(0,1);
					count = 0;
				}
				if(actor.getCurrentPiece().isGrounded()){
					actor.setState(new Grounded(actor));
				}
			}catch(NullPointerException e){
				actor.setState(new Reload(actor));
			}
		}
	}
	/** 
	 * 	The <code>PlayfieldState.Grounded</code> state object represents the state when a piece is unable to move further downward.
	 *	<p> The <code>Grounded</code> state has the following gameplay characteristics:
	 *	<ul>
	 *		<li> There is a piece currently in play, and the player can interact with it. </li>
	 *		<li> The piece undergoes a <i>lock delay</i> as long as it is on the ground. At the end of this delay, the piece will lock into place (see transitions below). The timer for this delay begins as soon as the playfield enters the <code>Grounded</code> state.</li>
	 *	</ul>
	 *	<p> The <code>Grounded</code> state has the following state transitions:
	 *	<ul>
	 *		<li> When the lock delay ends (after 30 frames), the playfield changes to the {@link PlayfieldState.Reload Reload} state. </li>
	 *		<li> If the piece is no longer grounded (e.g. if the player moves the current piece to a location where it can fall further), the playfield reverts to the {@link PlayfieldState.Normal Normal} state. </li>
	 *	</ul>
	 *	<p> The above behavior is implemented in this state's implementation of the {@link #update() update()} method.
	**/
	public static class Grounded extends State{
		private int ticks;
		/**
		 *	Creates a new <code>Grounded</code> state object which is associated with a <code>Playfield</code>.
		 *	An internal frame counter is also started by this constructor.
		 *	@param actor the <code>Playfield</code> to associate with this state object
		**/
		public Grounded(Playfield actor){
			this.actor = actor;
			this.ticks = 0;
		}
		@Override
		public void update(){
			if(actor.getCurrentPiece()!=null && !actor.getCurrentPiece().isGrounded()){
				actor.setState(new Normal(actor));
			}
			ticks++;
			if(ticks >= 30){
				actor.setState(new Reload(actor));
			}
		}
	}
	/** 
	 * 	The <code>PlayfieldState.Reload</code> state object represents the state when the current piece is locked into place, and a new piece is retrieved from the piece queue.
	 *	<p> The <code>Reload</code> state has the following gameplay characteristics:
	 *	<ul>
	 *		<li> The player does not have control over the pieces in this state. </li>
	 *		<li> The game internally handles locking in the current piece and clearing blocks while in this state.</li>
	 *	</ul>
	 *	<p> The <code>Reload</code> state has the following state transitions:
	 *	<ul>
	 *		<li> If the game is unable to spawn a new piece due to the presence of blocks at the spawn point, the playfield changes to the {@link PlayfieldState.GameOver GameOver} state. </li>
	 *		<li> Otherwise, when the game is finished performing all tasks for this state, the playfield reverts to the {@link PlayfieldState.Normal Normal} state. </li>
	 *	</ul>
	 *	<p> The above behavior is implemented in this state's implementation of the {@link #update() update()} method, as well as its constructor.
	**/
	public static class Reload extends State{
		/**
		 *	Creates a new <code>Reload</code> state object which is associated with a <code>Playfield</code>. Reloading operations are also started when the constructor is invoked.
		 *	@param actor the <code>Playfield</code> to associate with this state object
		**/
		public Reload(Playfield actor){
			this.actor = actor;
			if(actor.getCurrentPiece()!=null){
				boolean valid = actor.getCurrentPiece().addToPlayfield();
				if(!valid) {
					actor.setState(new GameOver(actor));
					return;
				}
			}
			actor.clear();
		}
		@Override
		public void update(){
			if(actor.isDoneClearing()){
				actor.spawnGarbage();
				actor.getNextPiece();
				actor.setState(new Normal(actor));
			}
		}
	}
	/** 
	 * 	The <code>PlayfieldState.GameOver</code> state object represents the state when the playfield has overflowed with blocks, and needs to be reset.
	 *	<p> The <code>GameOver</code> state has the following gameplay characteristics:
	 *	<ul>
	 *		<li> The player does not have control over the pieces in this state. </li>
	 *	</ul>
	 *	<p> The <code>GameOver</code> state has the following state transitions:
	 *	<ul>
	 *		<li> When the game is finished performing all tasks for this state, the playfield reverts to the {@link PlayfieldState.Normal Normal} state. </li>
	 *	</ul>
	 *	<p> The above behavior is implemented in this state's implementation of the {@link #update() update()} method, as well as its constructor.
	**/
	public static class GameOver extends State{
		/**
		 *	Creates a new <code>GameOver</code> state object which is associated with a <code>Playfield</code>. Resetting operations are also started when the constructor is invoked.
		 *	@param actor the <code>Playfield</code> to associate with this state object
		**/
		public GameOver(Playfield actor){
			this.actor = actor;
			actor.clearField();
			actor.setState(new Normal(actor));
		}
		@Override
		public void update(){actor.setState(new Normal(actor));}
	}
}