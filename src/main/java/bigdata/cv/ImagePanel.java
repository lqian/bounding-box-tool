/**
 * 
 */
package bigdata.cv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

/**
 * @author lqian
 *
 */
public class ImagePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	private static final long serialVersionUID = -7794767941999850410L;

	final float ZOOM_LEVEL = 1.2f;

	static final float MAX_SCALE_FACTOR = 4;

	static final float MIN_SCALE_FACTOR = 0.25f;

	final static Object[] lebels = { "车辆", "驾驶员", "年检标", "车牌", "纸巾盒", "挂饰", "摆件", "安全带", "手机", "标志牌", "危险品", "黄标", "实习标" };

	DefaultTableModel tableModel;

	Path datasetPath;
	String imageFile;
	String labelFile;

	/**
	 * copred bounding boxes
	 */
	ArrayList<LabeledBoundingBox> boundingBoxes = new ArrayList<LabeledBoundingBox>();

	// x1, y1, x2, y2 of corp bounding box
	int corpX1, corpY1, corpX2, corpY2;

	// x, y of corping bounding box
	int corpX, corpY;

	boolean hasChanged = false;

	CorpStatus corpStatus = CorpStatus.unkonw;

	BufferedImage image;
	int imageWidth, imageHeight;

	BufferedImage scaleImage;
	double scaleFactor;
	int showX, showY;

	int lastDragX, lastDragY;
	int moveX, moveY;
	boolean hasDragged = false;
	int selectBoundingBoxIndex = -1;
	
	LabelFileListener  labelFileListener;

	public ImagePanel() {
		super();
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
	}

	public void renderTableModel() {
		for (LabeledBoundingBox bb : boundingBoxes) {
			tableModel.addRow(new Object[] { bb.labelName, bb.boundingBoxString() });
		}
	}

	public void selectBoundingBox(int index) {
		selectBoundingBoxIndex = index;
		repaint();
	}

	public void load(String imageFile) {
		if (hasChanged) {
			saveLabelsToFile();
			hasChanged = false;
		}

		lastDragX = lastDragY = 0;
		showX = showY = moveX = moveY = 0;
		boundingBoxes.clear();
		selectBoundingBoxIndex = -1;
		hasDragged = false;
		scaleImage = null;

		while (tableModel.getRowCount() > 0) {
			tableModel.removeRow(0);
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
				image = ImageIO.read(new File(datasetPath.toFile(), imageFile));
				imageWidth = image.getWidth();
				imageHeight = image.getHeight();
				loadExistedBoundingBox();
				repaint();
			} catch (IOException e) {
				image = null;
				imageWidth = -1;
				imageHeight = -1;
				showWarningMsg("cannot read image content for file:\n" + imageFile);
			}
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
		Path path = datasetPath.resolve(labelFile);
		if (Files.exists(path)) {
			try {
				BufferedReader reader = Files.newBufferedReader(path);
				String line = null;
				while ((line = reader.readLine()) != null) {
					LabeledBoundingBox bb = LabeledBoundingBox.from(line);
					if (bb != null)
						boundingBoxes.add(bb);
				}
				reader.close();

				renderTableModel();
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
				BufferedWriter writer = Files.newBufferedWriter(datasetPath.resolve(labelFile), Charset.defaultCharset());
				writer.write(this.imageWidth + "," + this.imageHeight);
				writer.newLine();
				for (LabeledBoundingBox bb : boundingBoxes) {
					writer.write(bb.toString());
					writer.newLine();
				}
				writer.flush();
				writer.close();
				
				if (labelFileListener != null) {
					labelFileListener.postLabelFileSave();
				}
			} catch (IOException e) {
				e.printStackTrace();
				showWarningMsg("cannot write label file:\n" + labelFile);
			}
		}
	}

	public void removeBoundingBox(int i) {
		selectBoundingBoxIndex = -1;
		boundingBoxes.remove(i);
		tableModel.removeRow(i);
		hasChanged = true;
		repaint();
	}

	public void removeAllBoundingBox() {
		selectBoundingBoxIndex = -1;
		while (tableModel.getRowCount() > 0) {
			tableModel.removeRow(0);
		}
		boundingBoxes.clear();
		hasChanged = true;
		repaint();
	}

	public void paint(Graphics g) {
		int pw = this.getWidth();
		int ph = this.getHeight();
		g.setColor(this.getBackground());
		g.fillRect(0, 0, pw, ph);

		if (scaleImage != null) {
			int sw = scaleImage.getWidth();
			int sh = scaleImage.getHeight();
			if (sw <= pw) {
				showX = (pw - sw) / 2;
			}
			if (sh <= ph) {
				showY = (ph - sh) / 2;
			}

			if (hasDragged) {
				showX += moveX;
				showY += moveY;
			} else {
				showX = (pw - sw) / 2;
				showY = (ph - sh) / 2;
			}

			g.drawImage(scaleImage, showX, showY, this);
		} else if (imageFile != null) {
			int iw = image.getWidth();
			int ih = image.getHeight();
			int x = 0, y = 0;
			double sw = iw * 1.0 / pw;
			double sh = ih * 1.0 / ph;

			{
				scaleFactor = sh > sw ? sh : sw;
				int w = (int) (iw / scaleFactor);
				int h = (int) (ih / scaleFactor);
				x = (pw - w) / 2;
				y = (ph - h) / 2;
				g.drawImage(image, x, y, w, h, this);
				showX = x;
				showY = y;
			}

		} else {
			super.paintComponents(g);
		}

		// if (image != null) {
		// existed bounding box

		Graphics2D g2d = (Graphics2D) g;
		for (int i = 0; i < boundingBoxes.size(); i++) {
			LabeledBoundingBox bb = boundingBoxes.get(i);
			int bx = (int) (bb.x / scaleFactor);
			int by = (int) (bb.y / scaleFactor);
			int bw = (int) (bb.w / scaleFactor);
			int bh = (int) (bb.h / scaleFactor);
			if (i == selectBoundingBoxIndex) {
				g2d.setColor(Color.MAGENTA);
				g2d.setStroke(new BasicStroke(5f));
				g2d.drawRect(showX + bx, showY + by, bw, bh);
			} else {
				g2d.setStroke(new BasicStroke(1f));
				g2d.setColor(Color.WHITE);
				g2d.drawRect(showX + bx, showY + by, bw, bh);
			}
		}

		// current un-corp bounding box if exists
		if (corpStatus == CorpStatus.moving) {
			int x = corpX1 < corpX ? corpX1 : corpX;
			int y = corpY1 < corpY ? corpY1 : corpY;
			int w = Math.abs(corpX1 - corpX);
			int h = Math.abs(corpY1 - corpY);
			g2d.setStroke(new BasicStroke(1f));
			g2d.setColor(Color.WHITE);
			g2d.drawRect(x, y, w, h);
		}
		// }
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
					LabeledBoundingBox bb = LabeledBoundingBox.wrap(label, corpX1, corpY1, corpX2, corpY2, scaleFactor,
							showX, showY);
					boundingBoxes.add(bb);
					tableModel.addRow(new Object[] { label, bb.boundingBoxString() });
					corpStatus = CorpStatus.unkonw;
					repaint();
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
		lastDragX = e.getX();
		lastDragY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		moveX = moveY = 0;
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (scaleImage == null)
			return;
		if (lastDragX != 0 && lastDragY != 0) {
			moveX = e.getX() - lastDragX;
			moveY = e.getY() - lastDragY;
			hasDragged = true;
		}

		lastDragX = e.getX();
		lastDragY = e.getY();

		repaint();

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (corpStatus.equals(CorpStatus.startCorp)) {
			corpStatus = CorpStatus.moving;
		} else if (corpStatus.equals(CorpStatus.moving)) {
			corpX = e.getX();
			corpY = e.getY();
			repaint();
		}
	}

	static enum CorpStatus {
		unkonw, startCorp, moving, endCorp
	}

	/**
	 * clip central party of scaled image
	 * 
	 * @param rotation
	 */
	void zoom(int rotation) {

		double factor = Math.pow(ZOOM_LEVEL, -1 * rotation);
		scaleFactor /= factor;
		if (scaleFactor > MAX_SCALE_FACTOR) {
			scaleFactor = MAX_SCALE_FACTOR;
		}

		if (scaleFactor < MIN_SCALE_FACTOR) {
			scaleFactor = MIN_SCALE_FACTOR;
		}

		int w = (int) (imageWidth / scaleFactor);
		int h = (int) (imageHeight / scaleFactor);

		scaleImage = new BufferedImage(w, h, image.getType());
		AffineTransform transform = new AffineTransform();
		transform.setToScale(1 / scaleFactor, 1 / scaleFactor);
		AffineTransformOp imageOp = new AffineTransformOp(transform, null);
		imageOp.filter(image, scaleImage);

		repaint();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom(e.getWheelRotation());
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		switch (key) {
		case 'H':
			moveLeft();
			break;
		case 'J':
			moveDown();
			break;
		case 'L':
			moveRight();
			break;
		case 'K':
			moveUp();
			break;
		case 'Y':
			expandLeft();
			break;
		case 'U':
			expandRight();
			break;
		case 'I':
			expandTop();
			break;
		case 'O':
			expandBottom();
			break;
		case 'E':
			expand();
			break;
		case 'y':
			shrinkLeft();
			break;
		case 'u':
			shrinkRight();
			break;
		case 'i':
			shrinkTop();
			break;
		case 'o':
			shrinkBottom();
			break;
		case 'e':
			shrink();
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
	
	void moveLeft() {}
	void moveDown() {}
	void moveRight(){}
	void moveUp(){}
	void shrinkLeft() {}
	void shrinkRight(){}
	void shrinkTop(){}
	void shrinkBottom(){}
	void shrink(){}
	void expand() {}
	void expandLeft() {}
	void expandRight(){}
	void expandTop(){}
	void expandBottom(){}

}
