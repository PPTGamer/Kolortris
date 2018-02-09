import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.imageio.*;
import java.io.*;
import java.util.*;

/**
 *	A JComponent that displays the Kolortris logo.
**/
public class Logo extends JComponent{
	BufferedImage image;
	/**
	 *	Creates a new <code>Logo</code>.
	**/
	public Logo(){
		this.setPreferredSize(new Dimension(583,77));
		try{
			this.image = ImageIO.read(new File("Kolortris Logo.png"));
		}catch (IOException e){
			this.image = null;
			System.out.println("Failed to load images.");
		}
	}
	/**
	 *	Draws this component using a given <code>Graphics</code> context.
	 *	@param g a <code>Graphics</code> context
	**/
	protected void paintComponent(Graphics g){
		Graphics2D g2d = (Graphics2D)g;
		g2d.drawImage(image,null,0,0);
	}
}