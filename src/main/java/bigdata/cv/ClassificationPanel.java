/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * @author lqian
 *
 */
public class ClassificationPanel extends JPanel {

	BorderLayout boderLayout = new BorderLayout(10, 10);

	JPanel northPanel = new JPanel();

	JTable imageTable = new JTable();
	
	Path path;
	
	List<String> fileNames = new ArrayList<>();
	
	int currentIndex = 0 ;
	
	ImageTableModel dataModel = new ImageTableModel();

	/**
	 * 
	 */
	private static final long serialVersionUID = 3232368214975648611L;

	public ClassificationPanel(String dir) throws IOException {
		super();
		path = Paths.get(dir);
		Files.walk(path, FileVisitOption.FOLLOW_LINKS)
			.filter(p -> p.getFileName().endsWith(".jpg"))
			.forEach( p-> fileNames.add(p.getFileName().toString()));
		Collections.sort(fileNames);
		
		BufferedReader reader = Files.newBufferedReader(path.resolve("meta"));
		currentIndex = Integer.valueOf(reader.readLine());
		reader.close();
		
		setLayout(boderLayout);

		add(northPanel, BorderLayout.SOUTH);

		imageTable.setAutoscrolls(false);
		imageTable.setDragEnabled(false);
		imageTable.setModel(dataModel);
		imageTable.setDefaultRenderer(ImageFile.class, new ImageCellRenderer());
		
		showImage();
	}
	
	void showImage() {
		
	}
	
	class ImageTableModel extends AbstractTableModel {

		@Override
		public int getRowCount() {
			return 4;
		}

		@Override
		public int getColumnCount() {
			return 6;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			int idx =  currentIndex  + rowIndex * getColumnCount() + columnIndex;
			ImageFile imageFile = new ImageFile();
			imageFile.baseName = fileNames.get(idx);
			try {
				ImageIO.read(path.resolve(fileNames.get(idx)).toFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return imageFile;
		}
		
	}

	class ImageCellRenderer extends JPanel implements TableCellRenderer {

		public ImageCellRenderer() {
			setOpaque(true);
			setLayout(new BorderLayout());
			add(checkBox, BorderLayout.SOUTH);
			add(imagePanel, BorderLayout.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			ImageFile imageFile = (ImageFile) value;
			checkBox.setText(imageFile.baseName);
			imagePanel.image = imageFile.image;
			imagePanel.updateUI();
			return this;
		}

		public boolean isSelected() {
			return checkBox.isSelected();
		}

		JCheckBox checkBox;

		ImagePanel imagePanel = new ImagePanel();
	}

	class ImagePanel extends JPanel {
		BufferedImage image;

		@Override
		public void print(Graphics g) {
			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}
		}
	}

	class ImageFile {
		String baseName;
		BufferedImage image;
	}

}
