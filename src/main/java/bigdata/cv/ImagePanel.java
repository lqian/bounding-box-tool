/**
 * 
 */
package bigdata.cv;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * @author lqian
 *
 */
public class ImagePanel extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = -7794767941999850410L;
	String imageFile;

	int imageWidth;

	int imageHeight;

	int corpX1, corpY1, corpX2, corpY2;

	int moveX, moveY;

	CorpStatus corpStatus = CorpStatus.unkonw;

	BufferedImage image;

	public ImagePanel() {
		super();

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void setImageFile(String imageFile) {
		corpStatus = CorpStatus.unkonw;
		this.imageFile = imageFile;
		try {
			image = ImageIO.read(new File(imageFile));
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();
		} catch (IOException e) {
			image = null;
			imageWidth = -1;
			imageHeight = -1;
		}
		repaint();
	}

	@Override
	public void update(Graphics g) {
		super.update(g);
	}

	public void paint(Graphics g) {
		if (imageFile != null) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.drawImage(image, 0, 0, this);
			g2d.setColor(Color.WHITE);
//			g2d.re
			if (corpStatus == CorpStatus.moving) {
				int x = corpX1 < moveX ? corpX1 : moveX;
				int y = corpY1 < moveY ? corpY1 : moveY;
				int w = Math.abs(corpX1 - moveX);
				int h = Math.abs(corpY1 - moveY);
				g2d.drawRect(x, y, w, h);
			}
			else if (corpStatus == CorpStatus.endCorp) {
				int x = corpX1 < corpX2 ? corpX1 : corpX2;
				int y = corpY1 < corpY2 ? corpY1 : corpY2;
				int w = Math.abs(corpX1 - corpX2);
				int h = Math.abs(corpY1 - corpY2);
				
				g2d.drawRect(x, y, w, h);
				corpStatus = CorpStatus.unkonw;
			}
		} else {
			super.paintComponents(g);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {

		if (e.getButton() == MouseEvent.BUTTON1) {
			// left click
			if (corpStatus.equals(CorpStatus.unkonw)) {
				corpStatus = CorpStatus.startCorp;
				corpX1 = e.getX();
				corpY1 = e.getY();
			}

			else if (corpStatus.equals(CorpStatus.moving)) {
				corpStatus = CorpStatus.endCorp;
				corpX2 = e.getX();
				corpY2 = e.getY();
				repaint();
			}
		} else if (e.getButton() == MouseEvent.BUTTON2) {
			// right click
			corpStatus = CorpStatus.unkonw;
			repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (corpStatus.equals(CorpStatus.startCorp)) {
			corpStatus = CorpStatus.moving;
		}
		else if (corpStatus.equals(CorpStatus.moving))  {
			moveX = e.getX();
			moveY = e.getY();
			repaint();
		}
	}

	static enum CorpStatus {
		unkonw, startCorp, moving, endCorp
	}
}
