/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
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
		// imageTable.set
		// imageTable.setSelectionMode(selectionMode);
		for (int i = 0; i < dataModel.getColumnCount(); i++) {
			imageTable.getColumnModel().getColumn(i).setCellRenderer(new ImageCellRenderer());
		}

		JScrollPane scrollPane = new JScrollPane(imageTable);
		add(scrollPane, BorderLayout.CENTER);

		imageTable.setRowHeight(200);

		imageTable.setCellSelectionEnabled(true);

		imageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		imageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					int row = imageTable.getSelectedRow();
					int col = imageTable.getSelectedColumn();
					ImageFile imageFile = (ImageFile)dataModel.getValueAt(row, col);
					imageFile.selected = true;
					dataModel.fireTableCellUpdated(row, col);
				}
			}

		});

		// imageTable.
	}

	void init(String dir) throws IOException {
		path = Paths.get(dir);

		Files.newDirectoryStream(path).forEach(p -> {
			if (p.getFileName().toString().endsWith("jpg")) {
				fileNames.add(p.getFileName().toString());
			}
		});

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
	
	void pageDown() {
		currentIndex += dataModel.getRowCount() * dataModel.getColumnCount();
		dataModel.clean();
		
	}
	
	void pageUp() {
		currentIndex -= dataModel.getRowCount() * dataModel.getColumnCount();
		dataModel.clean();
	}

	class ImageTableModel extends AbstractTableModel {

		List<Object> data = new ArrayList<>();
		
		int rowCount;
		
		int columnCount;

		public void setColumnCount(int columnCount) {
			this.columnCount = columnCount;
		}

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
//			fireTableCellUpdated(row, col);
		}
		public void setRowCount(int rowCount) {
			this.rowCount = rowCount;
		}
		
		void clean() {
			data.clear();
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	}

	class ImageCellRenderer extends JPanel implements TableCellRenderer {

		BufferedImage image;
		ImageFile imageFile;

		public ImageCellRenderer() {
//			setOpaque(true);
			setLayout(new BorderLayout());
			add(checkBox, BorderLayout.SOUTH);

		}

		void setImage(BufferedImage c) {
			ImagePanel imagePanel = new ImagePanel(c);

			// add(imagePanel, BorderLayout.CENTER);
			imagePanel.updateUI();
			// imagePanel.repaint();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			imageFile = (ImageFile) value;
			checkBox.setText(imageFile.baseName);
//			setImage(imageFile.image);
			image = imageFile.image;
			updateUI();
			// setHeight(200);
			return this;
		}

		public boolean isSelected() {
			return checkBox.isSelected();
		}

		JCheckBox checkBox = new JCheckBox();

		@Override
		public void paint(Graphics g) {
			int pw = getWidth();
			int ph = getHeight();
			if (imageFile.selected) {
				g.setColor(Color.blue);
				g.fillRect(0, 0, pw, ph);
			}
			else {
				setOpaque(true);
				g.setColor(getBackground());
				g.fillRect(0, 0, pw, ph);
			}
//			
			
			
			if (image != null) {
				
				int iw = image.getWidth();
				int ih = image.getHeight();
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
		boolean selected = false;
		String baseName;
		BufferedImage image;
	}

}
