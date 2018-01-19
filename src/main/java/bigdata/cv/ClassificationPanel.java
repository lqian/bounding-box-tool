/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * @author lqian
 *
 */
public class ClassificationPanel extends JPanel {

	BorderLayout boderLayout = new BorderLayout(10, 10);

	JPanel toolPanel = new JPanel();


	Path path;

	List<String> fileNames = new ArrayList<>();

	int currentIndex = 0;

	ImageTableModel dataModel = new ImageTableModel();
	JTable imageTable;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3232368214975648611L;

	public ClassificationPanel() {
		super();

		setLayout(boderLayout);
		
		add(toolPanel, BorderLayout.SOUTH);
		imageTable = new JTable(dataModel);
		imageTable.setAutoscrolls(false);
		imageTable.setDragEnabled(false);
		imageTable.setTableHeader(null);
		for (int i=0; i< dataModel.getColumnCount(); i++) {
			imageTable.getColumnModel().getColumn(i).setCellRenderer(new ImageCellRenderer());
		}
		
		JScrollPane scrollPane = new JScrollPane(imageTable);
		add(scrollPane, BorderLayout.CENTER);
		 
		imageTable.setRowHeight(200);
	}

	void init(String dir) throws IOException {
		path = Paths.get(dir);

		Files.newDirectoryStream(path).forEach(p -> fileNames.add(p.getFileName().toString()));

		// Files.walk(path).filter(
		// p -> p.getFileName().endsWith("jpg"))
		// .forEach(p -> fileNames.add(p.getFileName().toString()));
		Collections.sort(fileNames);

		Path meta = path.resolve("meta");
		if (Files.exists(meta)) {
			BufferedReader reader = Files.newBufferedReader(path.resolve("meta"));
			currentIndex = Integer.valueOf(reader.readLine());
			reader.close();
		}

		showImage();
	}

	void showImage() {
		int rows = dataModel.getRowCount();
		int cols = dataModel.getColumnCount();
		imageTable.setDefaultRenderer(ImageFile.class, new ImageCellRenderer());
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				ImageFile val = new ImageFile();
				val.baseName = fileNames.get(currentIndex + i * cols + j);
				try {
					val.image = ImageIO.read(path.resolve(val.baseName).toFile());
					dataModel.setValueAt(val, i, j);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
		
		
	}

	class ImageTableModel extends AbstractTableModel {
		
		List<Object> data = new ArrayList<>();
		
		@Override
		public int getRowCount() {
			return 4;
		}

		@Override
		public int getColumnCount() {
			return 6;
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data.get(row * getColumnCount() + col);
		}

		@Override
		public void setValueAt(Object aValue, int row, int col) {
			data.add(aValue);
			fireTableCellUpdated(row, col);
		}
	}
	
	class ImageCellRenderer extends JPanel implements TableCellRenderer {

		BufferedImage image;
		public ImageCellRenderer() {
			setOpaque(true);
			setLayout(new BorderLayout());
			add(checkBox, BorderLayout.SOUTH);
			
		}
		
		void setImage(BufferedImage c) {
			ImagePanel imagePanel = new ImagePanel(c);
			
			add(imagePanel, BorderLayout.CENTER);
			imagePanel.updateUI();
			//imagePanel.repaint();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			ImageFile imageFile = (ImageFile) value;
			checkBox.setText(imageFile.baseName);
			setImage(imageFile.image); 
			//image = imageFile.image;
			// updateUI();
			//setHeight(200);
			return this;
		}

		public boolean isSelected() {
			return checkBox.isSelected();
		}

		JCheckBox checkBox = new JCheckBox();

		/*@Override
		public void paint(Graphics g) {
			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}
		}*/
	}

	class ImagePanel extends JPanel {
		BufferedImage image;
		
		ImagePanel(BufferedImage image) {
			this.image = image;
		}

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
