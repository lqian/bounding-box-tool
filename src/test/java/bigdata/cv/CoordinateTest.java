package bigdata.cv;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * 
 */

/**
 * @author link
 *
 */


public class CoordinateTest {
	
	public static void main(String[] args)  throws Exception {
		BufferedImage image = ImageIO.read(new File("/home/link/darknet/data/VOCdevkit/VOC2007/JPEGImages/000001.jpg"));
		
		// voc original 
		BufferedImage dog = image.getSubimage(48, 240, 195 - 48, 371 - 240);
		ImageIO.write(dog, "jpg", new File("dog.jpg"));
		
		//darknet style
		// 11 0.341359773371 0.609 0.416430594901 0.262
		int w = (int)(image.getWidth() * 0.416430594901);
		int h = (int)(image.getHeight() * 0.262);
		int x = (int)(image.getWidth() * 0.341359773371);
		int y = (int)(image.getHeight() * 0.609 );
		
		System.out.format("darknet style coordinate (%d, %d, %d, %d)%n", x, y, w, h);
	}

}
