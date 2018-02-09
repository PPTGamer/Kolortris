import java.util.*;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;

enum RotationState{
	SPAWN,
	CW,
	FLIP,
	CCW
}
enum PieceType{
	O	( new int[][]{
		{1, 1},
		{1, 1},
		} ), 
	I	( new int[][]{
		{0, 0, 0, 0},
		{1, 1, 1, 1},
		{0, 0, 0, 0},
		{0, 0, 0, 0}
		} ), 
	Z	( new int[][]{
		{1, 1, 0},
		{0, 1, 1},
		{0, 0, 0},
		} ), 
	S	( new int[][]{
		{0, 1, 1},
		{1, 1, 0},
		{0, 0, 0},
		} ), 
	J	( new int[][]{
		{1, 0, 0},
		{1, 1, 1},
		{0, 0, 0},
		} ), 
	L	( new int[][]{
		{0, 0, 1},
		{1, 1, 1},
		{0, 0, 0},
		} ), 
	T	( new int[][]{
		{0, 1, 0},
		{1, 1, 1},
		{0, 0, 0},
		} );
	private int[][] blueprint;
	PieceType( int[][] blueprint ){
		this.blueprint = blueprint;
	}
	public int[][] getBlueprint(){
		int size = this.blueprint.length;
		int[][] copy = new int[size][size];
		for(int i = 0; i<size; i++)
			for(int j = 0; j<size; j++)
				copy[i][j] = blueprint[i][j];
		return copy;
	}
}
enum Tile {
	NONE 	(new Color(0,0,0,0),	new Color(0,0,0,0)),
	RED 	(new Color(120, 0, 0),	new Color(210, 24, 10)),
	BLUE 	(new Color(49, 97, 255),new Color(0, 195, 255)),
	YELLOW 	(new Color(191, 191, 0),new Color(255, 255, 50)),
	GREEN 	(new Color(0, 145, 0),	new Color(0, 247, 37)),
	PURPLE 	(new Color(115, 0, 186),new Color(163, 55, 230));
	private final String STR_BLOCK_IMAGE_FILENAME = "FinalBlock.png";
	private BufferedImage IMG_BLOCK_IMAGE;
	private Color color1,color2;
	private boolean clearing;
	private int currentFrame;
	Tile(Color color1, Color color2){
		try{
			this.IMG_BLOCK_IMAGE = ImageIO.read(new File(STR_BLOCK_IMAGE_FILENAME));
		}catch (IOException e){
			this.IMG_BLOCK_IMAGE = null;
			System.out.println("Failed to load images.");
		}
		this.color1 = color1;
		this.color2 = color2;
		this.clearing = false;
		this.currentFrame = 0;
	}
	public Paint getPaint(Rectangle2D.Double tile, int alpha){
		if(this==Tile.NONE) return color1;
		Color color1 = new Color(
			this.color1.getRed(),
			this.color1.getGreen(),
			this.color1.getBlue(),
			alpha
		);
		Color color2 = new Color(
			this.color2.getRed(),
			this.color2.getGreen(),
			this.color2.getBlue(),
			alpha
		);
		return new GradientPaint(
			(float)(tile.x+tile.width/2),
			(float)(tile.y+tile.height),
			color1,
			(float)(tile.x+tile.width/2),
			(float)(tile.y),
			color2
		);
	}
	public static int getTileCode(Tile tile){
		switch(tile){
			case RED: 		return 1;
			case BLUE: 		return 2;
			case YELLOW: 	return 3;
			case GREEN: 	return 4;
			case PURPLE: 	return 5;
		}
		return 0;
	}
	public void draw(Graphics2D context, int x, int y, int tileSize, int alpha){
		if(this!=Tile.NONE){
			AffineTransform scaleTransform = new AffineTransform();
			scaleTransform.scale(tileSize/64.0,tileSize/64.0);
			AffineTransformOp drawOp = new AffineTransformOp(
				scaleTransform, 
				null
			);
			Rectangle2D.Double cell = new Rectangle2D.Double(x,y,tileSize,tileSize);
			if(this!=Tile.NONE && IMG_BLOCK_IMAGE != null){
				context.drawImage(IMG_BLOCK_IMAGE, drawOp, x,y);
			}
			context.setPaint(getPaint(cell,alpha));
			context.fill(cell);
			context.setPaint(new Color(0,0,0,(int)Math.max(0,(255-alpha*1.4))));
			context.fill(cell);
		}
	}
	public void draw(Graphics2D context, Rectangle2D.Double tileArea, int alpha){
		draw(context, (int)tileArea.x,(int)tileArea.y,(int)tileArea.width,alpha);
	}
}
/**
 *	Represents a single playfield, and contains most game logic.
**/
public class Playfield{
	private final String STR_PLAYFIELD_FILENAME = "Playfield2.png";
	private final String STR_SMALL_PLAYFIELD_FILENAME = "Playfield2_2.png";
	private final String STR_UIFONT_FILENAME = "joystix monospace.ttf";
	private BufferedImage IMG_PLAYFIELD_IMAGE;
	private BufferedImage IMG_SMALL_PLAYFIELD_IMAGE;
	private Font FNT_UIFONT;
	private final int normalAlpha = 180;
	private final int ghostAlpha = 100;
	private final int NUM_ROWS = 21;
	private final int NUM_COLUMNS = 10;
	private Random rand;
	private Tile[][] playfield;
	private int[][] drawAlpha;
	
	private LinkedList<Piece> pieceQueue;
	private Piece currentPiece, heldPiece;
	
	private PlayfieldState.State currentState;
	java.util.Timer logicTimer;
	private TimerTask logic = new TimerTask(){
		public void run(){
			currentState.update();
		}
	};
	
	private int score;
	private String playerName;
	private int garbageWaiting = 0;
	
	/**
	 *	Adds an integer amount of incoming garbage lines to this <code>Playfield</code>.
	 *	<p> If <code>numLines</code> is non-positive, this <code>Playfield</code> is unmodified.
	 *	@param numLines the number of garbage to add. 
	**/
	public synchronized void addGarbageToQueue(int numLines){
		if(numLines>=0)
			garbageWaiting+=numLines;
	}
	/**
	 *	Spawns all incoming garbage onto this <code>Playfield</code>.
	 *	<p> If the garbage overflows this <code>Playfield</code>, the <code>Playfield</code> is reset and all remaining incoming garbage is ignored.
	**/
	public synchronized void spawnGarbage(){
		int freeColumn = rand.nextInt(NUM_COLUMNS);
		for(int k = 0; k<garbageWaiting; k++){
			for(int i = 0; i<NUM_COLUMNS; i++){
				if(playfield[0][i]!=Tile.NONE){
					setState(new PlayfieldState.GameOver(this));
					garbageWaiting = 0;
					return;
				}
			}
			Tile[] newRow = new Tile[NUM_COLUMNS];
			for(int i = 0; i<NUM_COLUMNS; i++){
				if(i==freeColumn){
					newRow[i] = Tile.NONE;
					continue;
				}
				ArrayList<Tile> choices = new ArrayList<Tile>();
				for(Tile t : Tile.values()){
					choices.add(t);
				}
				choices.remove(Tile.NONE);
				choices.remove(playfield[NUM_ROWS-1][i]);
				if(i>0) choices.remove(newRow[i-1]);
				if(i<NUM_COLUMNS-1) choices.remove(newRow[i+1]);
				
				newRow[i] = choices.get(rand.nextInt(choices.size()));
			}
			for(int i = 0; i<NUM_ROWS-1; i++){
				playfield[i] = playfield[i+1];
			}
			playfield[NUM_ROWS-1] = newRow;
		}
		garbageWaiting = 0;
	}
	/**
	 *	Sets the current behavior of this <code>Playfield</code> using a {@link PlayfieldState.State PlayfieldState.State} object.
	 *	@param newState the new state object to assign to this <code>Playfield</code>
	**/
	public void setState(PlayfieldState.State newState){
		this.currentState = newState;
	}
	private boolean logicEnabled = false;
	/**
	 *	Starts executing the internal logic of this <code>Playfield</code>.
	**/
	public void startLogic(){
		this.logicTimer.scheduleAtFixedRate(logic,0, 33);
		this.logicEnabled = true;
	}
	/**
	 *	Stops executing the internal logic of this <code>Playfield</code>.
	**/
	public void stopLogic(){
		this.logic.cancel();
		this.logicEnabled = false;
	}
	/**
	 *	Checks if the internal logic of this <code>Playfield</code> is enabled.
	 *	@return <code>true</code> if the logic of this <code>Playfield</code> is enabled, and <code>false</code> otherwise
	**/
	public boolean isLogicEnabled(){
		return logicEnabled;
	}
	/**
	 *	Creates a new blank <code>Playfield</code>.
	**/
	public Playfield(){
		try{
			this.IMG_PLAYFIELD_IMAGE = ImageIO.read(new File(STR_PLAYFIELD_FILENAME));
			this.IMG_SMALL_PLAYFIELD_IMAGE = ImageIO.read(new File(STR_SMALL_PLAYFIELD_FILENAME));
			this.FNT_UIFONT = Font.createFont(Font.TRUETYPE_FONT, new File(STR_UIFONT_FILENAME));
		}catch (Exception e){
			this.IMG_PLAYFIELD_IMAGE = null;
			this.IMG_SMALL_PLAYFIELD_IMAGE = null;
			this.FNT_UIFONT = null;
			System.out.println("Failed to load resources.");
		}
		this.rand = new Random();
		this.playfield = new Tile[NUM_ROWS][NUM_COLUMNS];
		this.drawAlpha = new int[NUM_ROWS][NUM_COLUMNS];
		this.currentPiece = null;
		this.heldPiece = null;
		this.pieceQueue = new LinkedList<Piece>();
		this.logicTimer = new java.util.Timer();
		this.score = 0;
		this.playerName = "Loading name...";
		setState(new PlayfieldState.Normal(this));
		this.clearField();
		reloadQueue();
		getNextPiece();
	}
	/**
	 *	Creates a new <code>Playfield</code> from a String generated by {@link #toString() toString()}.
	 *	<p>Using this constructor is equivalent to the following:
	 *	<p><code>(new Playfield()).loadData(objectData)</code>
	 *	@param objectData the string to be parsed and loaded as <code>Playfield</code> data.
	**/
	public Playfield(String objectData){
		this();
		loadData(objectData);
	}
	/**
	 *	Sets the player name of this <code>Playfield</code>.
	 *	@param newName the new player name of this <code>Playfield</code>.
	**/
	public void setName(String newName){
		playerName = newName;
	}
	/**
	 *	Gets the player name of this <code>Playfield</code>.
	 *	@return the player name of this <code>Playfield</code>.
	**/
	public String getName(){
		return playerName;
	}
	/**
	 *	Sets the score of this <code>Playfield</code>.
	 *	@param newScore the new score of this <code>Playfield</code>.
	**/
	public void setScore(int newScore){
		score = newScore;
	}
	/**
	 *	Gets the score of this <code>Playfield</code>.
	 *	@return the score of this <code>Playfield</code>.
	**/
	public int getScore(){
		return score;
	}
	
	/**
	 *	Draws this <code>Playfield</code> at the origin of the given <code>Graphics2D</code> context.
	 *	@param context the <code>Graphics2D</code> context with which to draw the <code>Playfield</code>
	 *	@param tileSize the length in pixels of each side of a single tile
	**/
	public synchronized void draw(Graphics2D context, int tileSize, boolean fullUI){
		AffineTransform scaleTransform = new AffineTransform();
		scaleTransform.scale(tileSize/20.0,tileSize/20.0);
		AffineTransformOp drawOp = new AffineTransformOp(
			scaleTransform, 
			new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
		);
		
		context.setPaint(new Color(0,0,0,175));
		context.fill(new Rectangle2D.Double(0,0,NUM_COLUMNS*tileSize,NUM_ROWS*tileSize));
		
		
		for(int x = 0; x<NUM_COLUMNS; x++){
			for(int y = 0; y<NUM_ROWS; y++){
				context.setPaint(new Color(100,100,100,175));
				context.draw(new Rectangle2D.Double(x*tileSize,y*tileSize,tileSize,tileSize));
				playfield[y][x].draw(context,x*tileSize,y*tileSize,tileSize,drawAlpha[y][x]);
			}
		}
		if(fullUI){
			if(IMG_PLAYFIELD_IMAGE!=null){
				context.drawImage(IMG_PLAYFIELD_IMAGE,drawOp,-4*tileSize,(int)(-1.5*tileSize));
			}
		}else{
			if(IMG_SMALL_PLAYFIELD_IMAGE!=null){
				context.drawImage(IMG_SMALL_PLAYFIELD_IMAGE,drawOp,(int)(-0.2*tileSize),(int)(-1.5*tileSize));
			}
		}
		
		context.setPaint(Color.WHITE);
		context.setFont(FNT_UIFONT.deriveFont((float)tileSize));
		int width = context.getFontMetrics().stringWidth(playerName);
		context.drawString(playerName, (NUM_COLUMNS*tileSize-width)/2, -tileSize/2);

		width = context.getFontMetrics().stringWidth(Integer.toString(score));
		context.drawString(Integer.toString(score), (NUM_COLUMNS*tileSize-width)/2, (int)((NUM_ROWS*tileSize)+tileSize*1.1));
		
		if(fullUI){
			if(garbageWaiting>0){
				width = context.getFontMetrics().stringWidth("Warning! "+Integer.toString(garbageWaiting)+" incoming!");
				context.drawString("Warning! "+Integer.toString(garbageWaiting)+" incoming!", (NUM_COLUMNS*tileSize-width)/2, (int)((NUM_ROWS*tileSize)+tileSize*3));
			}
		}
		if(currentPiece!=null){
			Piece ghostPiece = new Piece(currentPiece);
			while(!ghostPiece.isGrounded()){
				ghostPiece.move(0,1);
			}
			ghostPiece.draw(context,tileSize,ghostPiece.x*tileSize,ghostPiece.y*tileSize,ghostAlpha);
			currentPiece.draw(context,tileSize,currentPiece.x*tileSize,currentPiece.y*tileSize,normalAlpha);
		}
		if(!fullUI) return;
		if(heldPiece!=null){
			heldPiece.draw(context,tileSize*2/3,(int)(-3.4*tileSize),(int)(1.2*tileSize),normalAlpha);
		}
		if(pieceQueue!=null){
			pieceQueue.get(0).draw(context,tileSize*2/3,(int)((NUM_COLUMNS+0.6)*tileSize),(int)(1.2*tileSize),normalAlpha);
			pieceQueue.get(1).draw(context,tileSize*2/3,(int)((NUM_COLUMNS+0.6)*tileSize),(int)(5*tileSize),normalAlpha);
			pieceQueue.get(2).draw(context,tileSize*2/3,(int)((NUM_COLUMNS+0.6)*tileSize),(int)(8.8*tileSize),normalAlpha);
		}
	}
	/**
	 *	Clears all the tiles in this <code>Playfield</code>.
	**/
	public synchronized void clearField(){
		for(int i = 0; i<NUM_ROWS; i++){
			for(int j = 0; j<NUM_COLUMNS; j++){
				this.playfield[i][j] = Tile.NONE;
				this.drawAlpha[i][j] = normalAlpha;
			}
		}
	}
	
	private synchronized void reloadQueue(){
		while(pieceQueue.size()<7){
			PieceType[] types = PieceType.values();
			ArrayList<Integer> insertOrder = new ArrayList<Integer>();
			for(int i = 0; i<types.length; i++) insertOrder.add(i);
			Collections.shuffle(insertOrder);
			for(int i : insertOrder){
				pieceQueue.offerLast(new Piece(PieceType.values()[i]));
			}
		}
	}
	
	/**
	 *	Discards the current {@link Piece Piece} in play, and obtains a new <code>Piece</code> from the piece queue.
	**/
	public synchronized void getNextPiece(){
		currentPiece = pieceQueue.poll();
		reloadQueue();
	}
	/**
	 *	Returns the current <code>Piece</code> in play.
	 *	@return The <code>Piece</code> which is currently in play.
	**/
	public synchronized Piece getCurrentPiece(){
		if(!logicEnabled) return null;
		return currentPiece;
	}
	
	private synchronized void flag(int i,int j){
		drawAlpha[i][j]-=20;
	}
	private synchronized boolean isFlagged(int i, int j){
		return drawAlpha[i][j]<normalAlpha;
	}
	private synchronized void nextFrame(int i, int j){
		drawAlpha[i][j] = Math.max(0,drawAlpha[i][j]-20);
		if(drawAlpha[i][j]==0){
			drawAlpha[i][j] = normalAlpha;
			playfield[i][j] = Tile.NONE;
			score+=10;
			for(int k = i; k>0; k--){
				if(playfield[k][j]==Tile.NONE && playfield[k-1][j]!=Tile.NONE){
					playfield[k][j] = playfield[k-1][j];
					playfield[k-1][j] = Tile.NONE;
				}
			}
		}
	}
	private boolean[][] visited;
	private int countAdjacent(int i, int j, Tile match){
		if(i<0 || j<0 || i>=NUM_ROWS || j>=NUM_COLUMNS) return 0;
		if(playfield[i][j]!=match) return 0;
		if(visited[i][j]) return 0;
		visited[i][j] = true;
		int count = 1;
		count += countAdjacent(i+1,j,match);
		count += countAdjacent(i-1,j,match);
		count += countAdjacent(i,j+1,match);
		count += countAdjacent(i,j-1,match);
		return count;
	}
	private void flagAdjacent(int i, int j, Tile match){
		if(i<0 || j<0 || i>=NUM_ROWS || j>=NUM_COLUMNS) return;
		if(playfield[i][j]!=match) return;
		if(isFlagged(i,j)) return;
		flag(i,j);
		flagAdjacent(i+1,j,match);
		flagAdjacent(i-1,j,match);
		flagAdjacent(i,j+1,match);
		flagAdjacent(i,j-1,match);
		return;
	}
	/**
	 *	Flags for deletion any lines or color matches currently in the field.
	**/
	public void clear(){
		if(!logicEnabled) return;
		currentPiece = null;
		visited = new boolean[NUM_ROWS][NUM_COLUMNS];
		for(int i = 0; i<NUM_ROWS; i++){
			for(int j = 0; j<NUM_COLUMNS; j++){
				if(playfield[i][j]!=Tile.NONE && countAdjacent(i,j,playfield[i][j])>=4){
					flagAdjacent(i,j,playfield[i][j]);
				}
			}
		}
		for(int i = NUM_ROWS-1; i>=0; i--){
			boolean fullLine = true;
			for(int j = 0; j<NUM_COLUMNS; j++){
				if(playfield[i][j]==Tile.NONE){
					fullLine = false;
					break;
				}
			}
			if(fullLine){
				for(int j = 0; j<NUM_COLUMNS; j++){
					flag(i,j);
				}
			}
		}
	}
	/**
	 *	Returns <code>true</code> if all blocks flagged for deletion are cleared.
	 *	<p> Otherwise, all blocks flagged for deletion proceed to the next frame of animation.
	 *	@return	<code>true</code> if all blocks flagged for deletion are cleared, and <code>false</code> otherwise.
	**/
	public synchronized boolean isDoneClearing(){
		boolean isDone = true;
		for(int i = 0; i<NUM_ROWS; i++){
			for(int j = 0; j<NUM_COLUMNS; j++){
				if(isFlagged(i,j)){
					isDone = false;
					nextFrame(i,j);
				}
			}
		}
		//Check again for any chains
		if(isDone){
			clear();
			for(int i = 0; i<NUM_ROWS; i++){
				for(int j = 0; j<NUM_COLUMNS; j++){
					if(isFlagged(i,j)){
						isDone = false;
					}
				}
			}
		}
		return isDone;
	}
	private boolean holdEnabled = false;
	/**
	 *	Sets whether or not holding is enabled.
	 *	@see #hold()
	**/
	public synchronized void setHoldEnabled(boolean enabled){
		holdEnabled = enabled;
	}
	/**
	 *	If holding is currently enabled, swaps the current piece with the held piece, and then spawns the held piece at the default spawn position.
	 *	<p> If a hold is successful, hold is disabled. In order to hold again, <code>setHoldEnabled(true)</code> must be called.
	 *	@see #setHoldEnabled(boolean enabled)
	 *	@see Piece#setToSpawnPosition()
	**/
	public synchronized void hold(){
		if(logicEnabled==false || currentPiece==null) return;
		if(holdEnabled==false) return;
		if(heldPiece==null){
			heldPiece = new Piece(currentPiece);
			getNextPiece();
		}else{
			Piece temp = heldPiece;
			heldPiece = currentPiece;
			currentPiece = temp;
			currentPiece.setToSpawnPosition();
		}
		heldPiece.setToSpawnPosition();
		setHoldEnabled(false);
	}
	private String getDataBlock(String data, String startToken, String endToken){
		int startIndex = data.indexOf(startToken)+startToken.length()+1;
		int endIndex = data.indexOf(endToken)-1;
		return data.substring(startIndex,endIndex);
	}
	/**
	 *	Sets the data of this <code>Playfield</code> based on a <code>String</code> generated by {@link #toString() toString()}.
	 *	<p> The data currently in this <code>Playfield</code> will be overwritten.
	 *	@param objectData the string to be parsed and loaded as <code>Playfield</code> data.
	**/
	public synchronized void loadData(String objectData){
		playerName = getDataBlock(objectData,"playerNameStart","playerNameEnd");
		score = Integer.parseInt(getDataBlock(objectData,"scoreStart","scoreEnd"));
		
		String[] playfieldData = getDataBlock(objectData,"playfieldStart","playfieldEnd").split(",");
		for(int i = 0; i<NUM_ROWS; i++){
			for(int j = 0; j<NUM_COLUMNS; j++){
				playfield[i][j] = Tile.values()[Integer.parseInt(playfieldData[i*NUM_COLUMNS+j])];
			}
		}
		
		String currentPieceData = getDataBlock(objectData,"currentPieceStart","currentPieceEnd");
		if(currentPieceData.equals("null")){
			currentPiece = null;
		}else{
			currentPiece = new Piece(currentPieceData);
		}
		
		String heldPieceData = getDataBlock(objectData,"heldPieceStart","heldPieceEnd");
		if(heldPieceData.equals("null")){
			heldPiece =null;
		}else{
			heldPiece = new Piece(heldPieceData);
		}
		String[] pieceQueueData = getDataBlock(objectData,"pieceQueueStart","pieceQueueEnd").split("PieceData,");
		LinkedList<Piece> newPieceQueue = new LinkedList<Piece>();
		for(int i = 1; i<pieceQueueData.length; i++){
			newPieceQueue.add(new Piece(pieceQueueData[i]));
		}
		pieceQueue = newPieceQueue;
	}
	/**
	 *	Generates a <code>String</code> containing all the data of this <code>Playfield</code>.
	 *	<p> This string returned by this function can be parsed by  {@link #loadData(String) loadData(String)}.
	 *	@return A <code>String</code> which represents the data of this <code>Playfield</code>
	**/
	@Override
	public synchronized String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("playerNameStart,");
		sb.append(playerName);
		sb.append(",playerNameEnd,");
		
		sb.append("scoreStart,");
		sb.append(score);
		sb.append(",scoreEnd,");
		
		sb.append("playfieldStart,");
		for(int i = 0; i<NUM_ROWS; i++){
			for(int j = 0; j<NUM_COLUMNS; j++){
				sb.append(Tile.getTileCode(playfield[i][j]));
				sb.append(",");
			}
		}
		sb.append("playfieldEnd,");
		
		sb.append("currentPieceStart,");
		if(currentPiece==null){
			sb.append("null");
		}else{
			sb.append(currentPiece.toString());
		}
		sb.append(",currentPieceEnd,");
		
		sb.append("heldPieceStart,");
		if(heldPiece==null){
			sb.append("null");
		}else{
			sb.append(heldPiece.toString());
		}
		sb.append(",heldPieceEnd,");
		sb.append("pieceQueueStart,");
		for(Piece p : pieceQueue){
			sb.append("PieceData,");
			sb.append(p.toString());
			sb.append(",");
		}
		sb.append("pieceQueueEnd");
		return sb.toString();
	}
	
	public class Piece{
		private int x, y;
		private int SIZE;
		private int[][] body;
		private RotationState currRotationState;
		private Piece(PieceType p){
			this.body = p.getBlueprint();
			this.SIZE = body.length;
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					if(body[i][j]!=0){
						body[i][j] = rand.nextInt(Tile.values().length-1)+1;
					}
				}	
			}
			currRotationState = RotationState.SPAWN;
			setToSpawnPosition();
		}
		private Piece(){
			this( PieceType.values()[rand.nextInt(PieceType.values().length)] );
		}
		private Piece(String objectData){
			loadData(objectData);
		}
		private Piece(Piece other){
			this.x = other.x;
			this.y = other.y;
			this.SIZE = other.SIZE;
			this.currRotationState = other.currRotationState;
			this.body = new int[SIZE][SIZE];
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					this.body[i][j] = other.body[i][j];
				}
			}
		}
		
		private int[][] rotateClockwise(int[][] array){
			/*
				0 1 2 3   2 8 4 0
				4 5 6 7   7 9 5 1 
				8 9 0 1   4 0 6 2
				2 3 4 5   5 1 7 3
			*/
			int[][] result = new int[SIZE][SIZE];
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					result[i][j] = array[SIZE-j-1][i];
				}
			}
			return result;
		}
		
		private boolean isValidPosition(int dx, int dy, int rotation){
			Tile grid[][] = playfield;
			int x = this.x+dx;
			int y = this.y+dy;
			int[][] body = this.body;
			for(int i = 0; i<rotation; i++){
				body = rotateClockwise(body);
			}
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					if(body[i][j]!=0){
						//	Check if piece lies out of bounds.
						if(y+i<0 || y+i>=NUM_ROWS || x+j<0 || x+j>=NUM_COLUMNS){
							return false;
						}
						//	Check if piece overlaps with current field
						if(grid[y+i][x+j]!=Tile.NONE){
							return false;
						}
					}	
				}
			}
			return true;
		}
		
		/**
		 *	Returns <code>true</code> if this <code>Piece</code> cannot move further downward.
		 *	@return <code>true</code> if this <code>Piece</code> cannot move further downward, and <code>false</code> otherwise.
		**/
		public synchronized boolean isGrounded(){
			return !isValidPosition(0,1,0);
		}
		
		/**
		 *	Moves the current piece by an x,y offset, measured in number of tiles.
		 *	<p>By convention, a positive x offset indicates a rightward offset, and a positive y offset indicates a downward offset.
		 *	<p> No change to the state of the piece is made if a movement is currently impossible.
		 *	@param dx the x offset, measured in number of tiles
		 *	@param dy the y offset, measured in number of tiles
		**/
		public synchronized void move(int dx, int dy){
			if(isValidPosition(dx,dy,0)){
				x+=dx;
				y+=dy;
			}
		}
		
		private final int numChecks = 5;
		private final int[] WALLKICKTABLE_SIZE3_SPAWN_X = 	{ 0,-1,-1, 0,-1};
		private final int[] WALLKICKTABLE_SIZE3_SPAWN_Y = 	{ 0, 0,-1, 2, 2};
		private final int[] WALLKICKTABLE_SIZE3_CW_X = 		{ 0, 1, 1, 0, 1};
		private final int[] WALLKICKTABLE_SIZE3_CW_Y = 		{ 0, 0, 1,-2,-2};
		private final int[] WALLKICKTABLE_SIZE3_FLIP_X = 	{ 0, 1, 1, 0, 1};
		private final int[] WALLKICKTABLE_SIZE3_FLIP_Y = 	{ 0, 0,-1, 2, 2};
		private final int[] WALLKICKTABLE_SIZE3_CCW_X = 	{ 0,-1,-1, 0,-1};
		private final int[] WALLKICKTABLE_SIZE3_CCW_Y = 	{ 0, 0, 1,-2,-2};
		private final int[] WALLKICKTABLE_SIZE4_SPAWN_X = 	{ 0,-2, 1,-2, 1};
		private final int[] WALLKICKTABLE_SIZE4_SPAWN_Y = 	{ 0, 0, 0, 1,-2};
		private final int[] WALLKICKTABLE_SIZE4_CW_X = 		{ 0,-1, 2,-1, 2};
		private final int[] WALLKICKTABLE_SIZE4_CW_Y = 		{ 0, 0, 0,-2, 1};
		private final int[] WALLKICKTABLE_SIZE4_FLIP_X = 	{ 0, 2,-1, 2,-1};
		private final int[] WALLKICKTABLE_SIZE4_FLIP_Y = 	{ 0, 0, 0,-1, 2};
		private final int[] WALLKICKTABLE_SIZE4_CCW_X = 	{ 0, 1,-2, 1,-2};
		private final int[] WALLKICKTABLE_SIZE4_CCW_Y = 	{ 0, 0, 0, 2,-1};
		/**
		 *	Rotates the current piece clockwise by 90 degrees.
		 *	<p> If simple rotation is impossible, wall kicks are tested using the <a href=http://harddrop.com/wiki/SRS>offset table for the Super Rotation System (SRS)</a>  used in official Tetris games.
		 *	<p> No change to the state of the piece is made if a rotation is currently impossible.
		**/
		public synchronized void rotate(){
			boolean successful = false;
			switch(SIZE){
				case 2:
					if(isValidPosition(0,0,1)){
						body = rotateClockwise(body);
					}
					break;
				case 3:
					switch(currRotationState){
						case SPAWN:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE3_SPAWN_X[i],WALLKICKTABLE_SIZE3_SPAWN_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE3_SPAWN_X[i],WALLKICKTABLE_SIZE3_SPAWN_Y[i]);
									successful = true;
									break;
								}
							}
							break;
						case CW:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE3_CW_X[i],WALLKICKTABLE_SIZE3_CW_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE3_CW_X[i],WALLKICKTABLE_SIZE3_CW_Y[i]);
									successful = true;
									break;
								}
							}
							break;
						case FLIP:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE3_FLIP_X[i],WALLKICKTABLE_SIZE3_FLIP_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE3_FLIP_X[i],WALLKICKTABLE_SIZE3_FLIP_Y[i]);
									successful = true;
									break;
								}
							}
							break;
						case CCW:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE3_CCW_X[i],WALLKICKTABLE_SIZE3_CCW_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE3_CCW_X[i],WALLKICKTABLE_SIZE3_CCW_Y[i]);
									successful = true;
									break;
								}
							}
							break;
					}
					break;
				case 4:
					switch(currRotationState){
						case SPAWN:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE4_SPAWN_X[i],WALLKICKTABLE_SIZE4_SPAWN_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE4_SPAWN_X[i],WALLKICKTABLE_SIZE4_SPAWN_Y[i]);
									successful = true;
									break;
								}
							}
							break;
						case CW:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE4_CW_X[i],WALLKICKTABLE_SIZE4_CW_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE4_CW_X[i],WALLKICKTABLE_SIZE4_CW_Y[i]);
									successful = true;
									break;
								}
							}
							break;
						case FLIP:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE4_FLIP_X[i],WALLKICKTABLE_SIZE4_FLIP_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE4_FLIP_X[i],WALLKICKTABLE_SIZE4_FLIP_Y[i]);
									successful = true;
									break;
								}
							}
							break;
						case CCW:
							for(int i = 0; i<numChecks; i++){
								if(isValidPosition(WALLKICKTABLE_SIZE4_CCW_X[i],WALLKICKTABLE_SIZE4_CCW_Y[i],1)){
									body = rotateClockwise(body);
									move(WALLKICKTABLE_SIZE4_CCW_X[i],WALLKICKTABLE_SIZE4_CCW_Y[i]);
									successful = true;
									break;
								}
							}
							break;
					}
					break;
			}
			if(successful){
				switch(currRotationState){
					case SPAWN: currRotationState = RotationState.CW; break;
					case CW: currRotationState = RotationState.FLIP; break;
					case FLIP: currRotationState = RotationState.CCW; break;
					case CCW: currRotationState = RotationState.SPAWN; break;
				}
			}
		}
		
		/**
		 *	Drops this piece to the bottom of the <code>Playfield</code> it is in.
		 *	<p> The enclosing <code>Playfield</code> is then set to the {@link PlayfieldState.Reload Reload} state.
		**/
		public synchronized void hardDrop(){
			while(!isGrounded()){
				move(0,1);
			}
			(Playfield.this).setState(new PlayfieldState.Reload(Playfield.this));
		}
		
		/**
		 *	Set the current position of this <code>Piece</code> to the top-center of the enclosing <code>Playfield</code>.
		**/
		public synchronized void setToSpawnPosition(){
			x = NUM_COLUMNS/2-SIZE/2;
			y = 0;
			while(currRotationState!=RotationState.SPAWN){
				body = rotateClockwise(body);
				switch(currRotationState){
					case SPAWN: currRotationState = RotationState.CW; break;
					case CW: currRotationState = RotationState.FLIP; break;
					case FLIP: currRotationState = RotationState.CCW; break;
					case CCW: currRotationState = RotationState.SPAWN; break;
				}
			}
		}
		
		/**
		 *	Copies the tiles of this <code>Piece</code> to its enclosing <code>Playfield</code>, at its current position.
		 *	@return <code>true</code> if the current position of this <code>Piece</code> is valid, and <code>false</code> otherwise.
		**/
		public boolean addToPlayfield(){
			if(!isValidPosition(0,0,0)) return false;
			Tile[][] grid = playfield;
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					if(body[i][j]!=0){
						grid[y+i][x+j] = Tile.values()[body[i][j]];
					}
				}
			}
			return true;
		}
		/**
		 *	Draws this <code>Piece</code> at its internal <code>x</code> and <code>y</code> coordinates.
		 *	@param context the <code>Graphics2D</code> context with which to draw the <code>Piece</code>
		 *	@param tileSize the length in pixels of each side of a single tile
		 *	@see #draw(Graphics2D context, int tileSize, int x, int y)
		 *	@see #draw(Graphics2D context, int tileSize, int x, int y, int alpha)
		**/
		public void draw(Graphics2D context, int tileSize){
			draw(context,tileSize,this.x*tileSize,this.y*tileSize,255);
		}
		/**
		 *	Draws this <code>Piece</code> at specified <code>x</code> and <code>y</code> coordinates.
		 *	@param context the <code>Graphics2D</code> context with which to draw the <code>Piece</code>
		 *	@param tileSize the length in pixels of each side of a single tile
		 *	@param x the x coordinate of the upper-left corner of the <code>Piece</code>
		 *	@param y the y coordinate of the upper-left corner of the <code>Piece</code>
		 *	@see #draw(Graphics2D context, int tileSize)
		 *	@see #draw(Graphics2D context, int tileSize, int x, int y, int alpha)
		**/
		public void draw(Graphics2D context, int tileSize, int x, int y){
			draw(context,tileSize,x,y,255);
		}
		/**
		 *	Draws this <code>Piece</code> at specified <code>x</code> and <code>y</code> coordinates, with an specified alpha value.
		 *	@param context the <code>Graphics2D</code> context with which to draw the <code>Piece</code>
		 *	@param tileSize the length in pixels of each side of a single tile
		 *	@param x the x coordinate of the upper-left corner of the <code>Piece</code>
		 *	@param y the y coordinate of the upper-left corner of the <code>Piece</code>
		 *	@param alpha the alpha value (0-255) with which to draw this <code>Piece</code>.
		 *	@see #draw(Graphics2D context, int tileSize)
		 *	@see #draw(Graphics2D context, int tileSize, int x, int y)
		**/
		public void draw(Graphics2D context, int tileSize, int x, int y, int alpha){
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					if(body[i][j]!=0){
						Tile.values()[body[i][j]].draw(context,x+j*tileSize,y+i*tileSize,tileSize,alpha);
					}
				}
			}
		}
		/**
		 *	Sets the data of this <code>Piece</code> based on a <code>String</code> generated by {@link #toString() toString()}.
		 *	<p> The data currently in this <code>Piece</code> will be overwritten.
		 *	@param objectData the string to be parsed and loaded as <code>Piece</code> data.
		**/
		public synchronized void loadData(String objectData){
			String[] data = objectData.split(",");
			int k = 0;
			this.SIZE = Integer.parseInt(data[k++]);
			this.body = new int[SIZE][SIZE];
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					this.body[i][j] = Integer.parseInt(data[k++]);
				}
			}
			this.x = Integer.parseInt(data[k++]);
			this.y = Integer.parseInt(data[k++]);
			this.currRotationState = RotationState.values()[Integer.parseInt(data[k++])];
		}
		/**
		 *	Generates a <code>String</code> containing all the data of this <code>Piece</code>.
		 *	<p> This string returned by this function can be parsed by  {@link #loadData(String) loadData(String)}.
		 *	@return A <code>String</code> which represents the data of this <code>Piece</code>
		**/
		@Override
		public synchronized String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append(SIZE);
			sb.append(",");
			for(int i = 0; i<SIZE; i++){
				for(int j = 0; j<SIZE; j++){
					sb.append(body[i][j]);
					sb.append(",");
				}
			}
			sb.append(x);
			sb.append(",");
			sb.append(y);
			sb.append(",");
			switch(currRotationState){
				case SPAWN: sb.append(0); break;
				case CW: sb.append(1); break;
				case FLIP: sb.append(2); break;
				case CCW: sb.append(3); break;
			}
			return sb.toString();
		}
	}
}