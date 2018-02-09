import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.io.*;
import javax.imageio.*;
/**
 *	Handles scrolling backgrounds.
**/
public class ScrollingBackgroundThread extends Thread{
	private BufferedImage IMG_BACKGROUND_IMAGE; 
	private static double t = -1;
	/**
	 *	Creates and starts a new thread that handles a scrolling background, using the given image.
	 *	@param imageFilename the filename of the image to use as a background
	**/
	public ScrollingBackgroundThread(String imageFilename){
		try{
			this.IMG_BACKGROUND_IMAGE = ImageIO.read(new File(imageFilename));
		}catch (IOException e){
			this.IMG_BACKGROUND_IMAGE = null;
			System.out.println("Failed to load images.");
		}
		this.start();
	}
	/**
	 *	The thread's manipulation loop. 
	**/
	public void run(){
		if(t>0) return;
		if(t<0) t=1;
		try{
			while(true){
				t = (t+0.0003)%(30*Math.PI);
				sleep(30);
			}
		}catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	private double getXOffset(double t){
		return 200*Math.sin(5*Math.PI*t);
	}
	private double getYOffset(double t){
		return 150*Math.sin(6*Math.PI*t);
	}
	/**
	 * Draws this background using the given Graphics2D context.
	 * @param g2d the Graphics2D context with which to draw this background.
	**/
	public void draw(Graphics2D g2d){
		AffineTransform initial = g2d.getTransform();
		g2d.translate(getXOffset(t),getYOffset(t));
		g2d.drawImage(IMG_BACKGROUND_IMAGE, null, -200,-150);
		g2d.setTransform(initial);
	}
}