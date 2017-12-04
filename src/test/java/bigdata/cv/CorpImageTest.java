/**
 * 
 */
package bigdata.cv;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * @author link
 *
 */
public class CorpImageTest {
	
	public static void main(String[] args) throws Exception {
		BufferedImage image = ImageIO.read(new File("/home/link/Intelligence-Transport-Dateset/JPEGImages/000562181000_20171014080546788_1.jpg"));
		BufferedImage vehicle = image.getSubimage(728, 420, 647, 673);
		ImageIO.write(vehicle, "jpg", new File("vehicle.jpg"));
		
		BufferedImage driver = image.getSubimage(1119, 632, 123, 111);
		ImageIO.write(driver, "jpg", new File("driver.jpg"));
		
		BufferedImage pdvs = image.getSubimage(853, 592, 50, 26);
		ImageIO.write(pdvs, "jpg", new File("pdvs.jpg"));
		
		BufferedImage plate = image.getSubimage(988, 980, 114, 39);
		ImageIO.write(plate, "jpg", new File("plate.jpg"));
		
	}

}
