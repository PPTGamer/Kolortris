import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
/**
 * 	The <code>MenuState</code> enclosing class contains all menu state object classes. Each state object class is a <code>JPanel</code> which gets swapped out depending on the state of the parent frame.
 *	<p> Every state object inherits from the {@link MenuState.MenuPanel MenuState.MenuPanel} abstract base class.
**/
public final class MenuState{
	/**
	 * The <code>MenuPanel</code> abstract base class provides an abstraction for a <code>JPanel</code> used as a menu screen.
	 *	<p> This abstract base class handles the following common operations:
	 *	<ul>
	 *		<li> Setting the preferred size of the <code>JPanel</code> </li>
	 *		<li> Creating and starting a <code>javax.swing.timer</code> which regularly repaints the <code>JPanel</code> </li>
	 *		<li> Loading the font used for the GUI </li>
	 *		<li> Adding an <code>ActionListener</code> to all <code>JButton</code> components: instead of having to create a new <code>ActionListener</code> for each subclass, one should needs to implement the {@link #handleEvent(ActionEvent e) handleEvent(ActionEvent e)} method. </li>
	 *		<li> Casting the <code>Graphics</code> object passed to <code>paintComponent(Graphics)</code> to a <code>Graphics2D</code> object and applying antialiasing: instead of overriding <code>paintComponent(Graphics)</code> directly, one should implement the {@link #paint(Graphics2D g2d) paint(Graphics2D g2d)} method.</li>
	 *	</ul>
	 */
	public abstract static class MenuPanel extends JPanel{
		/**
		 *	The {@link GameFrame GameFrame} which is the parent of this <code>MenuPanel</code>.
		**/
		protected GameFrame parentFrame;
		/**
		 *	The <code>Font</code> for the user interface of this <code>MenuPanel</code>.
		**/
		protected Font uiFont;
		private final ActionListener generalListener = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(parentFrame!=null) handleEvent(e);
			}
		};
		private MenuPanel(){
			this.setPreferredSize(new Dimension(800,600));
			this.parentFrame = null;
			javax.swing.Timer timer = new javax.swing.Timer(30,new ActionListener(){
			public void actionPerformed(ActionEvent e){
				repaint();
			}
			});
			timer.start();
			try{
				uiFont = Font.createFont(Font.TRUETYPE_FONT, new File("joystix monospace.ttf"));
				uiFont = uiFont.deriveFont(12f);
			}catch (Exception e){
				uiFont = null;
				System.out.println("Failed to load resources.");
			}
		}
		/**
		 *	<b> Copied from online documentation:</b> Appends the specified component to the end of this container.
		 *	<p> If the component is a <code>JButton</code>, an <code>ActionListener</code> which refers to {@link #handleEvent(ActionEvent e) handleEvent(ActionEvent e)} is added to the component.
		 *	@param comp the component to be added
		 *	@return the component argument
		 *	@throws NullPointerException if comp is <code>null</code>
		**/
		@Override
		public Component add(Component comp){
			super.add(comp);
			if(comp instanceof JButton)
				((JButton)comp).addActionListener(generalListener);
			return comp;
		}
		/**
		 *	<b> Copied from online documentation:</b> Appends the specified component to the end of this container.
		 *	<p> If the component is a <code>JButton</code>, an <code>ActionListener</code> which refers to {@link #handleEvent(ActionEvent e) handleEvent(ActionEvent e)} is added to the component.
		 *	@param comp the component to be added
		 *  @param constraints an object expressing layout constraints for this component
		 *	@throws NullPointerException if comp is <code>null</code>
		**/
		@Override
		public void add(Component comp, Object constraints){
			super.add(comp,constraints);
			if(comp instanceof JButton)
				((JButton)comp).addActionListener(generalListener);
		}
		/**
		 *	<b> Copied from online documentation:</b> Calls the UI delegate's paint method, if the UI delegate is non-<code>null</code>. We pass the delegate a copy of the <code>Graphics</code> object to protect the rest of the paint code from irrevocable changes (for example, <code>Graphics.translate</code>).
		 *	<p> This override casts <code>Graphics</code> object to a <code>Graphics2D</code> object, applies antialiasing, then calls {@link #paint(Graphics2D g2d) paint(Graphics2D g2d)}.
		 *	@param g the Graphics object to protect
		**/
		@Override
		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
			paint(g2d);
		}
		/**
		 *	Processes an <code>ActionEvent</code> generated from this <code>MenuPanel</code>.
		 *	@param e the <code>ActionEvent</code> to process
		**/
		protected abstract void handleEvent(ActionEvent e);
		/**
		 *	Paints this <code>MenuPanel</code> using a <code>Graphics2D</code> context.
		 *	@param g2d the <code>Graphics2D</code> context
		**/
		protected abstract void paint(Graphics2D g2d);
	}
	/**
	 *	The {@link MenuState.MenuPanel MenuPanel} corresponding to the main menu.
	**/
	public static class MainMenu extends MenuPanel{
		private final ScrollingBackgroundThread background = new ScrollingBackgroundThread("MainMenuBackground.png");
		private final Logo logo = new Logo();
		private final JButton buttonPlay = new JButton("Play");
		private final JButton buttonExit = new JButton("Exit");

		/**
		 *	Creates a new <code>MainMenu</code> with the given parent {@link GameFrame GameFrame}.
		 *	@param parentFrame the {@link GameFrame GameFrame} which is the parent of this {@link MenuState.MenuPanel MenuPanel}
		**/
		public MainMenu(GameFrame parentFrame){
			this.parentFrame = parentFrame;
			if(uiFont!=null){
				buttonPlay.setFont(uiFont);
				buttonExit.setFont(uiFont);
			}

			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 3;
			this.add(logo,c);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.weightx = 1;
			c.insets = new Insets(50,10,0,10);
			this.add(buttonPlay,c);

			c.gridwidth = 1;
			c.gridx = 2;
			c.gridy = 1;
			this.add(buttonExit,c);

			revalidate();
			repaint();
		}
		@Override
		protected void handleEvent(ActionEvent e){
			if(e.getSource() instanceof JButton){
				JButton source = (JButton)e.getSource();
				if(source.equals(buttonExit)){
					System.exit(0);
				}else if(source.equals(buttonPlay)){
					parentFrame.setState(new Lobby(parentFrame));
				}
			}
		}
		@Override
		protected void paint(Graphics2D g2d){
			background.draw(g2d);
		}
	}
	/**
	 *	The {@link MenuState.MenuPanel MenuPanel} corresponding to the lobby, where one enters their player name.
	**/
	public static class Lobby extends MenuPanel{
		private final ScrollingBackgroundThread background = new ScrollingBackgroundThread("LobbyBackground.png");
		private final JTextField textFieldPlayerName = new JTextField(10);
		private final JLabel labelInstructions = new JLabel("Input player name (max 10 characters):");
		private final JButton buttonBack = new JButton("Back");
		private final JButton buttonServer = new JButton("Host Game");
		private final JButton buttonClient = new JButton("Join Game");
		/**
		 *	Creates a new <code>Lobby</code> with the given parent {@link GameFrame GameFrame}.
		 *	@param parentFrame the {@link GameFrame GameFrame} which is the parent of this {@link MenuState.MenuPanel MenuPanel}
		**/
		public Lobby(GameFrame parentFrame){
			this.parentFrame = parentFrame;
			textFieldPlayerName.setDocument(new PlainDocument(){
				@Override
				public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
					if (str == null)
					  return;

					if ((getLength() + str.length()) <= 10) {
					  super.insertString(offset, str, attr);
					}
				}
			});

			if(uiFont!=null){
				buttonBack.setFont(uiFont);
				buttonServer.setFont(uiFont);
				buttonClient.setFont(uiFont);
				labelInstructions.setFont(uiFont);
				textFieldPlayerName.setFont(uiFont);
			}

			textFieldPlayerName.setText("player");

			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			c.weightx = 1;
			c.anchor = GridBagConstraints.EAST;
			this.add(labelInstructions,c);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 0;
			c.gridwidth = 1;
			c.insets = new Insets(0,10,0,10);
			this.add(textFieldPlayerName,c);


			c.gridx = 0;
			c.gridy = 1;
			c.weightx = 1;
			c.insets = new Insets(50,10,0,10);
			this.add(buttonBack,c);

			c.gridx = 1;
			c.gridy = 1;
			this.add(buttonServer,c);

			c.gridx = 2;
			c.gridy = 1;
			this.add(buttonClient,c);

			revalidate();
			repaint();
		}
		@Override
		protected void handleEvent(ActionEvent e){
			if(e.getSource() instanceof JButton){
				JButton source = (JButton)e.getSource();
				if(source.equals(buttonBack)){
					parentFrame.setState(new MainMenu(parentFrame));
				}else if(source.equals(buttonServer)){
					parentFrame.playerName = textFieldPlayerName.getText();
					parentFrame.setState(new ServerLobby(parentFrame));
				}else if(source.equals(buttonClient)){
					parentFrame.playerName = textFieldPlayerName.getText();
					parentFrame.setState(new ClientLobby(parentFrame));
				}
			}
		}
		@Override
		protected void paint(Graphics2D g2d){
			background.draw(g2d);
			g2d.setPaint(new Color(255,255,255,150));
			g2d.fill(new Rectangle2D.Double(0,200,900,200));
		}
	}
	/**
	 *	The {@link MenuState.MenuPanel MenuPanel} corresponding to the server lobby, where one waits for clients to connect.
	**/
	public static class ServerLobby extends MenuPanel{
		private final ScrollingBackgroundThread background = new ScrollingBackgroundThread("ServerBackground.png");
		private final JTextArea textAreaClientList = new JTextArea(4,20);
		private final JLabel labelInstructions = new JLabel("Waiting for players . . . ");
		private final JButton buttonBack = new JButton("Back");
		private final JButton buttonStart = new JButton("Start Game");
		/**
		 *	Creates a new <code>ServerLobby</code> with the given parent {@link GameFrame GameFrame}.
		 *	@param parentFrame the {@link GameFrame GameFrame} which is the parent of this {@link MenuState.MenuPanel MenuPanel}
		**/
		public ServerLobby(GameFrame parentFrame){
			this.parentFrame = parentFrame;
			this.textAreaClientList.setEditable(false);

			parentFrame.createServer();
			parentFrame.getGameServer().startAcceptingConnections();

			parentFrame.createClient("localhost");

			parentFrame.getGameClient().getMyField().setName(parentFrame.playerName);

			if(uiFont!=null){
				textAreaClientList.setFont(uiFont);
				labelInstructions.setFont(uiFont);
				buttonStart.setFont(uiFont);
				buttonBack.setFont(uiFont);
			}

			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			c.weightx = 1;
			c.anchor = GridBagConstraints.EAST;
			this.add(labelInstructions,c);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 0;
			c.gridwidth = 1;
			c.insets = new Insets(0,10,0,10);
			this.add(textAreaClientList,c);

			c.gridx = 0;
			c.gridy = 1;
			c.weightx = 1;
			c.insets = new Insets(50,10,0,10);
			this.add(buttonBack,c);

			c.gridx = 1;
			c.gridy = 1;
			c.gridwidth = 2;
			c.weightx = 2;
			this.add(buttonStart,c);

		}
		@Override
		protected void handleEvent(ActionEvent e){
			if(e.getSource() instanceof JButton){
				JButton source = (JButton)e.getSource();
				if(source.equals(buttonBack)){
					parentFrame.getGameServer().stopAcceptingConnections();
					parentFrame.destroyGameManagers();
					parentFrame.setState(new Lobby(parentFrame));
				}else if(source.equals(buttonStart)){
					parentFrame.getGameServer().stopAcceptingConnections();
					parentFrame.setState(new GamePanel(parentFrame));
					parentFrame.getGameServer().startGame();
				}
			}
		}
		@Override
		protected void paint(Graphics2D g2d){
			textAreaClientList.setText(parentFrame.getGameServer().getNames());
			background.draw(g2d);
			g2d.setPaint(new Color(255,255,255,150));
			g2d.fill(new Rectangle2D.Double(0,200,900,200));
		}
	}
	/**
	 *	The {@link MenuState.MenuPanel MenuPanel} corresponding to the client lobby, where one enters a server IP to connect to.
	**/
	public static class ClientLobby extends MenuPanel{
		private final ScrollingBackgroundThread background = new ScrollingBackgroundThread("ClientBackground.png");
		private final JLabel labelInstructions = new JLabel("Input IP address to connect to:");
		private final JTextField textFieldIPAddress = new JTextField(15);
		private final JButton buttonConnect = new JButton("Connect");
		private final JButton buttonBack = new JButton("Back");
		/**
		 *	Creates a new <code>ClientLobby</code> with the given parent {@link GameFrame GameFrame}.
		 *	@param parentFrame the {@link GameFrame GameFrame} which is the parent of this {@link MenuState.MenuPanel MenuPanel}
		**/
		public ClientLobby(GameFrame parentFrame){
			this.parentFrame = parentFrame;

			if(uiFont!=null){
				textFieldIPAddress.setFont(uiFont);
				labelInstructions.setFont(uiFont);
				buttonConnect.setFont(uiFont);
				buttonBack.setFont(uiFont);
			}

			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			c.weightx = 1;
			c.anchor = GridBagConstraints.EAST;
			this.add(labelInstructions,c);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 0;
			c.gridwidth = 1;
			c.insets = new Insets(0,10,0,10);
			this.add(textFieldIPAddress,c);


			c.gridx = 0;
			c.gridy = 1;
			c.weightx = 1;
			c.insets = new Insets(50,10,0,10);
			this.add(buttonBack,c);

			c.gridx = 1;
			c.gridy = 1;
			c.gridwidth = 2;
			c.weightx = 3;
			this.add(buttonConnect,c);

		}
		@Override
		protected void handleEvent(ActionEvent e){
			if(e.getSource() instanceof JButton){
				JButton source = (JButton)e.getSource();
				if(source.equals(buttonBack)){
					parentFrame.setState(new Lobby(parentFrame));
				}else if(source.equals(buttonConnect)){
					if(parentFrame.createClient(textFieldIPAddress.getText())){
						parentFrame.getGameClient().getMyField().setName(parentFrame.playerName);
						parentFrame.setState(new GamePanel(parentFrame));
					}else{
						JOptionPane.showMessageDialog(this,"Unable to connect to " + textFieldIPAddress.getText());
					}
				}
			}
		}
		@Override
		protected void paint(Graphics2D g2d){
			background.draw(g2d);
			g2d.setPaint(new Color(255,255,255,150));
			g2d.fill(new Rectangle2D.Double(0,200,900,200));
		}
	}
	/**
	 *	The {@link MenuState.MenuPanel MenuPanel} corresponding to the game screen.
	**/
	public static class GamePanel extends MenuPanel{
		private final ScrollingBackgroundThread background = new ScrollingBackgroundThread("GameBackground.png");
		private final GameDisplay gameDisplay;
		/**
		 *	Creates a new <code>GamePanel</code> with the given parent {@link GameFrame GameFrame}.
		 *	@param parentFrame the {@link GameFrame GameFrame} which is the parent of this {@link MenuState.MenuPanel MenuPanel}
		**/
		public GamePanel(GameFrame parentFrame){
			this.parentFrame = parentFrame;
			if(parentFrame.getGameClient().getGameInstance()!=null){
				gameDisplay = new GameDisplay(parentFrame.getGameClient().getGameInstance());
				this.add(gameDisplay);
			}else gameDisplay = null;
			int clientID = parentFrame.getGameClient().getClientID();
			(new HumanController()).bindToDisplay(gameDisplay, clientID);
		}
		@Override
		protected void handleEvent(ActionEvent e){}
		@Override
		protected void paint(Graphics2D g2d){
			if(parentFrame.getGameClient()!=null && parentFrame.getGameClient().isGameOver()){
				parentFrame.setState(new Results(parentFrame));
			}
			background.draw(g2d);
		}
	}
	/**
	 *	The {@link MenuState.MenuPanel MenuPanel} corresponding to the results screen.
	**/
	public static class Results extends MenuPanel{
		private final ScrollingBackgroundThread background = new ScrollingBackgroundThread("GameBackground.png");
		private final JTextArea textAreaPlayerName = new JTextArea(4,20);
		private final JTextArea textAreaScores = new JTextArea(4,20);
		private final JButton buttonBack = new JButton("Back to Main Menu");
		/**
		 *	Creates a new <code>Results</code> with the given parent {@link GameFrame GameFrame}.
		 *	@param parentFrame the {@link GameFrame GameFrame} which is the parent of this {@link MenuState.MenuPanel MenuPanel}
		**/
		public Results(GameFrame parentFrame){
			this.parentFrame = parentFrame;

			this.textAreaPlayerName.setEditable(false);
			this.textAreaScores.setEditable(false);

			ArrayList<GameInstance.Record> results = parentFrame.getGameClient().getGameInstance().getRecords();
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for(int i = 0; i<results.size(); i++){
				sb1.append(results.get(i).playerName);
				sb1.append("\n");
				sb2.append(results.get(i).score);
				sb2.append("\n");
			}
			textAreaPlayerName.setText(sb1.toString());
			textAreaScores.setText(sb2.toString());


			if(uiFont!=null){
				textAreaPlayerName.setFont(uiFont);
				textAreaScores.setFont(uiFont);
				buttonBack.setFont(uiFont);
			}

			parentFrame.destroyGameManagers();
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(0,30,0,0);
			this.add(textAreaPlayerName,c);

			c.gridx = 1;
			c.gridy = 0;
			c.weightx = 1;
			c.insets = new Insets(0,0,0,30);
			this.add(textAreaScores,c);

			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.weightx = 1;
			c.insets = new Insets(50,10,0,10);
			this.add(buttonBack,c);

		}
		@Override
		protected void handleEvent(ActionEvent e){
			if(e.getSource() instanceof JButton){
				JButton source = (JButton)e.getSource();
				if(source.equals(buttonBack)){
					parentFrame.setState(new MainMenu(parentFrame));
				}
			}
		}
		@Override
		protected void paint(Graphics2D g2d){
			background.draw(g2d);
			g2d.setPaint(new Color(255,255,255,150));
			g2d.fill(new Rectangle2D.Double(0,200,900,200));
		}
	}
}
