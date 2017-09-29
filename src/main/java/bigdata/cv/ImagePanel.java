/**
 * 
 */
package bigdata.cv;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * @author lqian
 *
 */
public class ImagePanel extends JPanel implements MouseListener, MouseMotionListener {
	private static final long serialVersionUID = -7794767941999850410L;

	final static Object[] lebels = { "驾驶员", "车牌", "年检标", "纸巾盒", "挂饰", "摆件", "安全带", "手机", "标志牌", "危险品", "黄标" };
	String imageFile;
	String labelFile;

	int imageWidth;

	int imageHeight;

	/**
	 * copred bounding boxes
	 */
	ArrayList<LabeledBoundingBox> boundingBoxes = new ArrayList<LabeledBoundingBox>();

	// x1, y1, x2, y2 of corp bounding box
	int corpX1, corpY1, corpX2, corpY2;

	int moveX, moveY;

	boolean hasChanged = false;

	CorpStatus corpStatus = CorpStatus.unkonw;

	BufferedImage image;

	public ImagePanel() {
		super();
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void load(String imageFile) {
		if (hasChanged) {
			saveLabelsToFile();
			hasChanged = false;
		}

		corpStatus = CorpStatus.unkonw;
		this.imageFile = imageFile;
		int i = imageFile.lastIndexOf(".");
		if (i == -1) {
			String msg = "not found extension name for file:\n" + imageFile;
			showWarningMsg(msg);
		} else {
			labelFile = imageFile.substring(0, i) + ".label";
			try {
				image = ImageIO.read(new File(imageFile));
				imageWidth = image.getWidth();
				imageHeight = image.getHeight();
				loadExistedBoundingBox();
			} catch (IOException e) {
				image = null;
				imageWidth = -1;
				imageHeight = -1;
				showWarningMsg("cannot read image content for file:\n" + imageFile);
			}

			boundingBoxes.clear();
			loadExistedBoundingBox();

			repaint();
		}
	}

	public void showWarningMsg(String text) {
		Toolkit.getDefaultToolkit().beep();
		JOptionPane optionPane = new JOptionPane(text, JOptionPane.WARNING_MESSAGE);
		JDialog dialog = optionPane.createDialog("Warning!");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
	}

	public void loadExistedBoundingBox() {
		Path path = Paths.get(labelFile);
		if (Files.exists(path)) {
			try {
				BufferedReader reader = Files.newBufferedReader(path);
				String line = null;
				while ((line = reader.readLine()) != null) {
					boundingBoxes.add(LabeledBoundingBox.from(line));
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				showWarningMsg("cannot read label file:\n" + labelFile);
			}
		}
	}

	public String showLabelDialog() {
		String label = (String) JOptionPane.showInputDialog(this, "select one label:", "", JOptionPane.PLAIN_MESSAGE,
				null, lebels, null);

		return label;
	}

	public void saveLabelsToFile() {
		int i = imageFile.lastIndexOf(".");
		if (i != -1) {
			try {
				BufferedWriter writer = Files.newBufferedWriter(Paths.get(labelFile), Charset.defaultCharset());
				for (LabeledBoundingBox bb : boundingBoxes) {
					writer.write(bb.toString());
					writer.newLine();
				}
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				showWarningMsg("cannot write label file:\n" + labelFile);
			}
		}
	}

	public void removeBoundingBox(int i) {
		boundingBoxes.remove(i);
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

			for (LabeledBoundingBox bb : boundingBoxes) {
				g2d.drawRect(bb.x, bb.y, bb.w, bb.h);
			}

			if (corpStatus == CorpStatus.moving) {
				int x = corpX1 < moveX ? corpX1 : moveX;
				int y = corpY1 < moveY ? corpY1 : moveY;
				int w = Math.abs(corpX1 - moveX);
				int h = Math.abs(corpY1 - moveY);
				g2d.drawRect(x, y, w, h);
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
			} else if (corpStatus.equals(CorpStatus.moving)) {
				corpStatus = CorpStatus.endCorp;
				String label = showLabelDialog();
				if (label != null) {
					corpX2 = e.getX();
					corpY2 = e.getY();
					hasChanged = true;
					LabeledBoundingBox bb = LabeledBoundingBox.wrap(label, corpX1, corpY1, corpX2, corpY2);
					boundingBoxes.add(bb);
					repaint();
					corpStatus = CorpStatus.unkonw;
				}
			}
		} else if (e.getButton() != MouseEvent.BUTTON1) {
			// right or middle button click
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
		} else if (corpStatus.equals(CorpStatus.moving)) {
			moveX = e.getX();
			moveY = e.getY();
			repaint();
		}
	}

	static enum CorpStatus {
		unkonw, startCorp, moving, endCorp
	}

}
