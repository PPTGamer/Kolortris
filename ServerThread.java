import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;
/**
 *	The <code>ServerThread</code> class is a <code>Thread</code> which handles the server side of a single client-server communication line.
**/
public class ServerThread extends Thread{
	private GameServer gameServer;
	private Socket clientSocket;
	private int clientID;
	private Scanner receiveStream; 
	private PrintWriter sendStream;
	/**
	 *	Creates a new <code>ServerThread</code>.
	 *	@param gameServer the <code>GameServer</code> this <code>ServerThread</code> belongs to
	 *	@param clientSocket the <code>Socket</code> this <code>ServerThread</code> communicates through
	 *	@param clientID the client ID to assign to the <code>GameClient</code> 
	**/
	public ServerThread(GameServer gameServer, Socket clientSocket, int clientID) throws IOException{
		super("Server thread for player " + clientID);
		this.gameServer = gameServer;
		this.clientSocket = clientSocket;
		this.clientID = clientID;
		this.sendStream = new PrintWriter(clientSocket.getOutputStream());
		this.receiveStream = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
	}
	/**
	 *	Returns the client ID associated with this client-server communication line.
	 *	@return the client ID associated with this client-server communication line
	**/
	public int getClientID(){ return clientID; }
	/**
	 *	Sends a <code>String</code> to the client this <code>ServerThread</code> is assigned to.
	 *	@param message the <code>String</code> to send.
	**/
	public void sendString(String message){
		synchronized (this){
			sendStream.println(message);
			sendStream.flush();
			sendStream.println("EOM");
			sendStream.flush();
		}
	}
	/**
	 *	The communication loop of this <code>ServerThread</code>.
	**/
	@Override
	public void run(){
		System.err.println("Thread index: "+Thread.getAllStackTraces().keySet().size());
		try{
			sendString("clientID,"+clientID);
			while(true){
				sendString("GameInstanceStart,"+gameServer.getSendableData()+",GameInstanceEnd");
				while(receiveStream.hasNextLine()) {
					String feedback = receiveStream.nextLine();
					if(feedback.equals("EOM")) break;
					gameServer.process(feedback);
				}
				sleep(30);
			}
		}catch(InterruptedException e){
			System.err.println("Thread:"+this.getName()+" properly interrupted");
			return;
		}
	}
}
