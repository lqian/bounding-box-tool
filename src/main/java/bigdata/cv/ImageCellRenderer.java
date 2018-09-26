package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;


@SuppressWarnings("serial")
public class ImageCellRenderer extends JPanel implements TableCellRenderer {

	BufferedImage image;
	ImageFile imageFile;

	public ImageCellRenderer() {
		//			setOpaque(true);
		setLayout(new BorderLayout());
		//add(checkBox, BorderLayout.SOUTH);

	}

	void setImage(BufferedImage c) {
		SimpleImagePanel imagePanel = new SimpleImagePanel(c);

		// add(imagePanel, BorderLayout.CENTER);
		imagePanel.updateUI();
		// imagePanel.repaint();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		
		imageFile = (ImageFile) value;
		if (imageFile != null) {
			image = imageFile.image;
			updateUI(); 
		}
		return this;
	}



	@Override
	public void paint(Graphics g) {
		if (imageFile == null ) return ;
		int pw = getWidth();
		int ph = getHeight();
		
		if (imageFile.selected) {
			g.setColor(Color.BLUE);
			g.fillRect(0, 0, pw, ph);
		}
		else {
			setOpaque(true);
			g.setColor(getBackground());
			g.fillRect(0, 0, pw, ph);
		}
		//			


		if (image != null) {

			int iw = image.getWidth() - 6;
			int ih = image.getHeight()-6;
			int x = 0, y = 0;
			double sw = iw * 1.0 / ph;
			double sh = ih * 1.0 / pw;
			double scaleFactor = sh > sw ? sh : sw;
			if (scaleFactor < 1) {
				scaleFactor = 1;
			}

			{
				int w = (int) (iw / scaleFactor);
				int h = (int) (ih / scaleFactor);
				x = (pw - w) / 2 - 1;
				y = (ph - h) / 2 - 1;
				g.drawImage(image, x, y, w, h, this);
			}
		}
	}

	
	public static class ImageFile {
		boolean selected = false;
		String baseName;
		BufferedImage image;
	}

}