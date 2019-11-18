/**
 * 
 */
package bigdata.cv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.table.DefaultTableModel;

/**
 * @author lqian
 *
 */
public class MTCNNImagePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	private static final long serialVersionUID = -7794767941999850410L;

	final float ZOOM_LEVEL = 1.2f;

	static final float MAX_SCALE_FACTOR = 4;

	static final float MIN_SCALE_FACTOR = 0.25f;
	
	DefaultTableModel tableModel;

	DataSet dataSet;
	String imageFile;
	String rawLabelFile;

	/**
	 * copred bounding boxes
	 */
	ArrayList<LabeledBoundingBox> boundingBoxes = new ArrayList<LabeledBoundingBox>();
	
	LabeledBoundingBox workingBoudingBox = null ;

	// x1, y1, x2, y2 of corp bounding box
	int corpX1, corpY1, corpX2, corpY2;

	// x, y of corping bounding box
	int corpX, corpY;
	
	int x0, y0, x1, y1, x2, y2, x3, y3;

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
	
	ImagePanelListener  listener;
	
	boolean enabled = false;

	LabelConfig labelConfig;
	
	LabelNamesPanel labelNamesPanel;
	
	boolean pressingCtrl = false;

	public MTCNNImagePanel() {
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
		if (index < 0 && boundingBoxes.size() < 1) return ;
		selectBoundingBoxIndex = index;
		workingBoudingBox = boundingBoxes.get(index);
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
			DialogUtil.showWarningMsg(msg);
		} else {
			rawLabelFile = imageFile.substring(0, i) + ".label";
			try {
				image = ImageIO.read(dataSet.getImage(imageFile).toFile());
				imageWidth = image.getWidth();
				imageHeight = image.getHeight();
				listener.postOpen();
				loadExistedBoundingBox();
				repaint();
			} catch (IOException e) {
				image = null;
				imageWidth = -1;
				imageHeight = -1;
				DialogUtil.showWarningMsg("cannot read image content for file:\n" + imageFile);
			}
		}
	}

	public void loadExistedBoundingBox() {
		Path path = dataSet.getRawLabel(rawLabelFile);
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
				DialogUtil.showWarningMsg("cannot read label file:\n" + rawLabelFile);
			}
		}
	}

	public String showLabelDialog() {
		
		if (labelNamesPanel == null) {
			labelNamesPanel = new LabelNamesPanel();
		}
		
		int r = JOptionPane.showConfirmDialog(this,  labelNamesPanel, "select one label name:", 
				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE );
		if (r == JOptionPane.OK_OPTION) {
			return labelNamesPanel.selected();
		}
		else {
			return null;
		}
	}

	public void saveLabelsToFile() {
		int i = imageFile.lastIndexOf(".");
		if (i != -1) {
			try {
				Path path = dataSet.getRawLabel(rawLabelFile);
				Path subDir = path.getParent();
				if (Files.notExists(subDir)) 
					Files.createDirectories(subDir);
				boolean update = Files.exists(path);
				BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset());
				writer.write(this.imageWidth + "," + this.imageHeight);
				writer.newLine();
				for (LabeledBoundingBox bb : boundingBoxes) {
					writer.write(bb.toString());
					writer.newLine();
				}
				writer.flush();
				writer.close();
				
				listener.postLabelFileSave(update);
			} catch (IOException e) {
				e.printStackTrace();
				DialogUtil.showWarningMsg("cannot write label file:\n" + rawLabelFile);
			}
		}
	}
	
	/**
	 * support parse plate color and plate nbr from file name. only support one plate
	 */
	public void confirmPlateNbr() {
		if (boundingBoxes.size() > 1) {
			JOptionPane.showMessageDialog(this, "not support!", "MTCNN", JOptionPane.WARNING_MESSAGE);
			return ;
		}
		
		int s = imageFile.indexOf("__");
		if (s != -1) {
			int e = imageFile.lastIndexOf("__");
			if (e != -1) {
				String sub = imageFile.substring(s+2, e);
				if (sub.equalsIgnoreCase("NoPlate")) {
					return ;
				}
				else {
					String tokens[] = sub.split("_");
					workingBoudingBox.plateColor = tokens[0];
					workingBoudingBox.plateNbr = tokens[1];
					saveLabelsToFile();
					repaint();
				}
			}
		}		
	}

	public void removeBoundingBox(int i) {
		selectBoundingBoxIndex = -1;
		workingBoudingBox = null;
		boundingBoxes.remove(i);
		tableModel.removeRow(i);
		hasChanged = true;
		repaint();
	}

	public void removeAllBoundingBox() {
		selectBoundingBoxIndex = -1;
		workingBoudingBox = null;
		while (tableModel.getRowCount() > 0) {
			tableModel.removeRow(0);
		}
		boundingBoxes.clear();
		hasChanged = true;
		repaint();
	}

	public void paint(Graphics g) {
		if (!enabled) {
			super.paint(g);
			return;
		}
		
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
		} else if (image != null) {
			int iw = image.getWidth();
			int ih = image.getHeight();
			int x = 0, y = 0;
			double sw = iw * 1.0 / pw;
			double sh = ih * 1.0 / ph;
			scaleFactor = sh > sw ? sh : sw;
			if (scaleFactor <1) {
				scaleFactor = 1;
			}
			
			{
				int w = (int) (iw / scaleFactor) ;
				int h = (int) (ih / scaleFactor) ;
				x = (pw - w) / 2 - 1;
				y = (ph - h) / 2 - 1;
				g.drawImage(image, x, y, w, h, this);
				showX = x;
				showY = y;
				
				if (listener != null) {
					listener.postScaled();
				}
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
				g2d.setStroke(new BasicStroke(3f));
				g2d.drawRect(showX + bx, showY + by, bw, bh);
				
				if (bb.landmark != null) {
					drawLandmarks(g2d, bb, scaleFactor);
				}
				
				if (bb.plateNbr != null && bb.plateNbr.length() > 0) {
					g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
					g2d.drawString(bb.plateNbr, showX+bx, showY + by + bh + 25);
				}
				else {
					int s = imageFile.indexOf("__");
					if (s != -1) {
						int e = imageFile.lastIndexOf("__");
						if (e != -1) {
							String sub = imageFile.substring(s+2, e);
							if (!sub.equalsIgnoreCase("NoPlate")) {
								String tokens[] = sub.split("_");
								g2d.setColor(Color.WHITE);
								g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
								g2d.drawString(tokens[1], showX+bx, showY + by + bh + 25);
							}
						}
					}		
				}
				
				
				if (image != null) {
					int iw = image.getWidth();
					int ih = image.getHeight();
					if (bb.x + bb.w < iw && bb.y + bb.h < ih) {
						BufferedImage sub = image.getSubimage(bb.x, bb.y, bb.w, bb.h);
						this.listener.postSelectedImage(sub, bb);
					}
				}

			} else {
				g2d.setStroke(new BasicStroke(1f));
				g2d.setColor(Color.WHITE);
				g2d.drawRect(showX + bx, showY + by, bw, bh);
//				if (bb.extras != null && !bb.extras.equals("")) {
//					drawLandmarks(g2d, bb, scaleFactor);
//				}
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
	
	void drawLandmarks(Graphics2D g2d, LabeledBoundingBox bb, double scaleFactor) {
		String tokens[] = bb.landmark.split(",");
		for (int i=0; i< 4; i++) {
			int p = i*2; 
			int x = (int)( Float.valueOf(tokens[p]) / scaleFactor);
			int y =  (int)(Float.valueOf(tokens[p+1]) / scaleFactor);
			int radius = (int) (3/ scaleFactor);
			g2d.drawOval(showX + x, showY + y, radius, radius);
			g2d.drawString("" + i, showX + x + radius, showY + y - radius);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		requestFocusInWindow();
		if (pressingCtrl) {
			if (workingBoudingBox != null ) {
				if (workingBoudingBox.extrasSize() < 8) {
					int x = e.getX();
					int y = e.getY();
					workingBoudingBox.addExtras(x, y, scaleFactor, showX, showY);
					hasChanged = true;
				}
			}
			repaint();  
		} 
		else {
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
						this.workingBoudingBox = bb;
						check(workingBoudingBox);
						BufferedImage sub = image.getSubimage(bb.x, bb.y, bb.w, bb.h);
						listener.postCorp(sub); 
						this.selectBoundingBoxIndex = -1;
						repaint();
						corpStatus = CorpStatus.unkonw;
					}
					else {
						// cancel selected box
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
		
		if (listener != null) {
			listener.postScaled();
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom(e.getWheelRotation());
	}

	@Override
	public void keyTyped(KeyEvent e) {
//		System.out.println(e.toString());
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_CONTROL) {
			pressingCtrl = !pressingCtrl;
			listener.postCtrlPressing(pressingCtrl);
			if (pressingCtrl) {
				if (selectBoundingBoxIndex == -1) {
					selectBoundingBoxIndex = tableModel.getRowCount() -1;
				}
				repaint();
				return;
			}
		}
		// last or selected bounding box
		if (workingBoudingBox == null) return;
		

		switch (key) {
		case 'A':
			moveLeft(workingBoudingBox);
			break;
		case 'S':
			moveDown(workingBoudingBox);
			break;
		case 'D':
			moveRight(workingBoudingBox);
			break;
		case 'W':
			moveUp(workingBoudingBox);
			break;
		case 'Y':
			expandLeft(workingBoudingBox);
			break;
		case 'U':
			expandRight(workingBoudingBox);
			break;
		case 'I':
			expandTop(workingBoudingBox);
			break;
		case 'O':
			expandBottom(workingBoudingBox);
			break;
		case 'E':
			expand(workingBoudingBox);
			break;				
		case 'H':
			shrinkLeft(workingBoudingBox);
			break;
		case 'J':
			shrinkRight(workingBoudingBox);
			break;
		case 'K':
			shrinkTop(workingBoudingBox);
			break;
		case 'L':
			shrinkBottom(workingBoudingBox);
			break;
		case 'R':
			shrink(workingBoudingBox);
			break;
		case 'B': shrinkWidth(workingBoudingBox); break;
		case 'N': shrinkHeight(workingBoudingBox); break;
		case 'M': expandWidth(workingBoudingBox); break;
		case ',': expandHeight(workingBoudingBox); break;
		}
		check(workingBoudingBox);
		repaint();
		if (workingBoudingBox.w > 0 && workingBoudingBox.h > 0) {
			BufferedImage sub = image.getSubimage(workingBoudingBox.x, workingBoudingBox.y, workingBoudingBox.w, workingBoudingBox.h);
			listener.postChange(sub);
			listener.postChangeLabel(selectBoundingBoxIndex, workingBoudingBox);
			hasChanged = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	void moveLeft(LabeledBoundingBox bb) {
		bb.x -= 1;
	}
	void moveDown(LabeledBoundingBox bb) {
		bb.y += 1;
	}
	void moveRight(LabeledBoundingBox bb){
		bb.x += 1;
	}

	void moveUp(LabeledBoundingBox bb){
		bb.y -= 1;
	}
	
	void check(LabeledBoundingBox bb) {
		if (bb.x < 0) bb.x = 0;
		if (bb.x >= imageWidth) bb.x = imageWidth - 1;
		if (bb.y < 0) bb.y = 0;
		if (bb.y >= imageHeight) bb.y = imageHeight - 1;
		if (bb.x + bb.w > imageWidth) bb.w = imageWidth - bb.x;
		if (bb.y + bb.h > imageHeight) bb.h = imageHeight - bb.y;
		if (bb.w < 1) bb.w = 1;
		if (bb.h < 1) bb.h = 1;
	}
	
	
	void shrinkLeft(LabeledBoundingBox bb) {
		bb.x += 1;
		bb.w -= 1;
	}
	void shrinkRight(LabeledBoundingBox bb){
		bb.w -= 1;
	}
	void shrinkTop(LabeledBoundingBox bb){
		bb.y += 1;
		bb.h -= 1;
	}
	void shrinkBottom(LabeledBoundingBox bb){
		bb.h -= 1;
	}
	void shrink(LabeledBoundingBox bb){
		bb.h -= 2;
		bb.w -= 2;
		bb.x += 1;
		bb.y += 1;
	}
	
	void shrinkHeight(LabeledBoundingBox bb) {
		bb.y +=1;
		bb.h -=2;
	}
	
	void shrinkWidth(LabeledBoundingBox bb) {
		bb.x += 1;
		bb.w -= 2;
	}
	
	void expand(LabeledBoundingBox bb) {
		bb.h += 2;
		bb.w += 2;
		bb.x -= 1;
		bb.y -= 1;
	}
	
	void expandHeight(LabeledBoundingBox bb) {
		bb.y -= 1;
		bb.h += 2;
	}
	
	void expandWidth(LabeledBoundingBox bb) {
		bb.x -= 1;
		bb.w += 2;
	}
	
	void expandLeft(LabeledBoundingBox bb) {

		bb.x -= 1;
		bb.w += 1;
	}
	void expandRight(LabeledBoundingBox bb){
		bb.w += 1;
	}
	void expandTop(LabeledBoundingBox bb){
		bb.y -= 1;
		bb.h += 1;
	}
	void expandBottom(LabeledBoundingBox bb){
		bb.h += 1;
	}
	
	@SuppressWarnings("serial")
	class LabelNamesPanel extends JPanel {
		
		ButtonGroup group = new ButtonGroup();
		
		int idx = -1;
		
		int s = 0;
		int e = 0;
		int gline = 0;
		GridBagConstraints gbc = new GridBagConstraints();
		
		public LabelNamesPanel() {
			setLayout(new GridBagLayout());
			gbc.anchor = GridBagConstraints.WEST; 
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridwidth = 1;
			 
			splitGroup(); 
		}
		
		void splitGroup() {
			int i = 0;
			int x = 0;
			int y = 0;
			
			int length = labelConfig.clazzNames.length;
			for (int split: labelConfig.splits) {
				e = split;
				for (; i < e && i<length; i++) {
					x = (i - s) % 4;
					y = gline + (i - s) / 4;
					gbc.gridy = y ;
					gbc.gridx = x ;
					JRadioButton rb = new JRadioButton(labelConfig.clazzNames[i]);
					rb.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
					rb.addActionListener(new LabelNamesListener(this, i));
					add(rb, gbc);
					group.add(rb);
				}				 
				s = e;
				gline += y;
				if (( e - s) % 4 < 3) gline++; //force new line for next group 
			}
			
			//for rest
			for (; i<length; i++) {
				x = (i - s) % 4;
				y = gline + (i - s) / 4;
				gbc.gridy = y ;
				gbc.gridx = x ;
				JRadioButton rb = new JRadioButton(labelConfig.clazzNames[i]);
				rb.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
				rb.addActionListener(new LabelNamesListener(this, i));
				add(rb, gbc);
				group.add(rb);
			}		
		}
		
		String selected() { return labelConfig.clazzNames[idx]; }
	}
	
	class LabelNamesListener implements ActionListener {
		
		LabelNamesPanel labelNamesPanel;
		
		int idx;

		public LabelNamesListener(LabelNamesPanel labelNamesPanel, int idx) {
			super();
			this.labelNamesPanel = labelNamesPanel;
			this.idx = idx;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			labelNamesPanel.idx = this.idx;
		}
	}

	public void cleanLandmarks() {
		if (workingBoudingBox == null || selectBoundingBoxIndex == -1) {
			return;
		}
		workingBoudingBox.clearLandmarks();
		boundingBoxes.get(selectBoundingBoxIndex).clearLandmarks();
		repaint();
		hasChanged = true;
	}

}
