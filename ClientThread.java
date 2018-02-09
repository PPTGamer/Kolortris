import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;
/**
 *	The <code>ClientThread</code> class is a <code>Thread</code> which handles the server side of a single client-server communication line.
**/
public class ClientThread extends Thread{
	private GameClient gameClient;
	private Socket clientSocket;
	private Scanner receiveStream; 
	private PrintWriter sendStream;
	private int currentScoreMilestone;
	/**
	 *	Creates a new <code>ClientThread</code>.
	 *	@param gameClient the <code>GameClient</code> this <code>ClientThread</code> belongs to
	 *	@param clientSocket the <code>Socket</code> this <code>ClientThread</code> communicates through
	**/
	public ClientThread(GameClient gameClient, Socket clientSocket) throws IOException{
		super("Client thread");
		this.gameClient = gameClient;
		this.clientSocket = clientSocket;
		this.sendStream = new PrintWriter(clientSocket.getOutputStream());
		this.receiveStream = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
		this.currentScoreMilestone = 150;
	}
	/**
	 *	Sends a <code>String</code> to the server.
	 *	@param message the <code>String</code> to send.
	**/
	public synchronized void sendString(String message){
		synchronized(this){
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
			String[] input = receiveStream.nextLine().split(",");
			gameClient.setClientID( Integer.parseInt(input[1]) );
			System.out.println("Client has connected, player ID received:" + input[1] +".");
			while(true){
				int newGarbage = (gameClient.getMyField().getScore()-currentScoreMilestone)/150;
				if(newGarbage>0){
					sendString(gameClient.getClientID() + ",sendGarbage,"+newGarbage);
					currentScoreMilestone+=newGarbage*150;
					sleep(20);
				}
				sendString(gameClient.getClientID() + ",PlayfieldDataBegin," +  gameClient.getMyField().toString() + ",PlayfieldDataEnd");
				while(receiveStream.hasNextLine()) {
					String feedback = receiveStream.nextLine();
					if(feedback.equals("EOM")) break;
					gameClient.process(feedback);
				}
				sleep(30);
			}
		}catch(InterruptedException e){
			System.err.println("Thread:"+this.getName()+" properly interrupted");
			return;
		}
	}
}
