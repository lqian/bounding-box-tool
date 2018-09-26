package bigdata.cv;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class SimpleImagePanel extends JPanel {
	
	BufferedImage image;

	public SimpleImagePanel(BufferedImage image) {
		this.image = image;
	}

	@Override
	public void print(Graphics g) {
		if (image != null) {
			g.drawImage(image, 0, 0, null);
		}
	}
}