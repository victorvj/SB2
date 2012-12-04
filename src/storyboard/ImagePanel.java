package storyboard;

import java.awt.Graphics;
import java.awt.image.WritableRaster;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.tree.TreePath;

/**
 * A JPanel with a Picture.
 * @author hannaschneider
 *
 */
public class ImagePanel extends JPanel{

	/**
	 * Image in the ImagePanel
	 */
    private BufferedImage image;
    
    /**
     * Width of image and ImagePanel
     */
	static final int IMGX = 300;
	
	/**
	 * Height of image and ImagePanel
	 */
	static final int IMGY = 200;
	
	/**
	 * Creates a new ImagePanel with a picture
	 * @param filename
	 * 			The source of the picture in the ImagePanel
	 */
    public ImagePanel(String filename) {
       try {            
    	  System.out.println("Try to load picture: "+filename);
          Image readimage = ImageIO.read(new File(filename));
          BufferedImage scaledImage = getScaledImage(readimage, IMGX, IMGY);
          image = scaledImage;
       } catch (IOException ex) {
            // handle exception...
       }
    }

    public ImagePanel(BufferedImage duplicate) {
		image = duplicate;
	}

	/**
     * Resizes the image using a Graphics2D object backed by a BufferedImage.
     * @param srcImg - source image to scale
     * @param w - desired width
     * @param h - desired height
     * @return - the new resized image
     */
    private BufferedImage getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }
    
    public BufferedImage getImage(){
    	return image;
    }
    /**
     * Loads a new picture in the ImagePanel
     * @param filename
     * 			The new source of the ImagePanel
     */
    public void setImagePath(String filename){
    	try {
    		Image readimage = image = ImageIO.read(new File(filename));
			BufferedImage scaledImage = getScaledImage(readimage, IMGX, IMGY);
	          image = scaledImage;
		} catch (IOException e) {
			e.printStackTrace();
		}
    	repaint();
    }
    
    public ImagePanel duplicate(){
    	BufferedImage duplicate = deepCopy(this.image);
		return new ImagePanel(duplicate);
    }
    
    static BufferedImage deepCopy(BufferedImage bi) {
    	 ColorModel cm = bi.getColorModel();
    	 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    	 WritableRaster raster = bi.copyData(null);
    	 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    	}
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);          
    }

}