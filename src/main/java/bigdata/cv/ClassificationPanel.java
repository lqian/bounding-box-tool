/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

	ImageTableModel dataModel;
	JTable imageTable;
	
	JCheckBox autoDelete = new JCheckBox("Auto Delete Selected Image");
	
	BufferedWriter blacklist;

	private JSpinner spRows;

	private JSpinner spCols;

	private JScrollPane scrollPane;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3232368214975648611L;

	public ClassificationPanel() {
		super();

		setLayout(boderLayout); 

		// imageTable.
		
		add(toolPanel, BorderLayout.SOUTH);
		
		JButton btnFirst = new JButton("!<");
		JButton btnPageUp = new JButton("<<");
		JButton btnPageDown = new JButton(">>");
		JButton btnAuto = new JButton("->");
		
		JButton btnDelete = new JButton("XXX");
		btnDelete.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				dataModel.removeSelected();			}
			
		}); 
		
		toolPanel.add(btnFirst);
		toolPanel.add(btnPageUp);
		toolPanel.add(btnPageDown);
		toolPanel.add(btnAuto);
		toolPanel.add(btnDelete);
		toolPanel.add(autoDelete);
		
		
		JLabel lblRows = new JLabel("Rows:");
		toolPanel.add(lblRows);
		spRows = new JSpinner();
		toolPanel.add(spRows);
		spRows.setModel(new SpinnerNumberModel(5, 5, 9, 1));
		
		
		lblRows.setLabelFor(spRows);
		JLabel lblCols = new JLabel("Cols:");
		toolPanel.add(lblCols);
		spCols = new JSpinner();
		spCols.setModel(new SpinnerNumberModel(8, 6, 12, 1));
		lblCols.setLabelFor(spCols);
		toolPanel.add(spCols);
		
		spRows.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				
				
				int oc = dataModel.getRowCount();
				int nc = (Integer)spRows.getValue();
				if (oc != nc) {
					initTable(nc, (Integer) spCols.getValue());
				}
			}
			
		});
		
		
		spCols.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				dataModel.setColumnCount((Integer)spCols.getValue());
			}
			
		});
		
		
		
		btnFirst.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (autoDelete.isSelected()) {
					btnDelete.doClick();
				}
				currentIndex = 0;
				dataModel.clean();
				showImage();
			}
		});
		
		btnPageUp.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				
				int batch = dataModel.getRowCount() * dataModel.getColumnCount();
				if (currentIndex >= batch) {
					if (autoDelete.isSelected()) {
						btnDelete.doClick();
					}
					currentIndex -= batch;
					dataModel.clean();
					showImage();
					writeMeta();
				}
			}
			
		});
		
		btnPageDown.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int batch = dataModel.getRowCount() * dataModel.getColumnCount();
				if (currentIndex + batch < fileNames.size()) {
					if (autoDelete.isSelected()) {
						btnDelete.doClick();
					}
					currentIndex += batch;
					dataModel.clean();
					showImage();
					writeMeta();
				}
			}
		});
		
		btnAuto.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
	}

	private void initTable(int rows, int cols) {
		
		if (imageTable != null) {
			remove(imageTable);
		}
		
		if (scrollPane != null ) {
			remove(scrollPane);
		}
		
		dataModel = new ImageTableModel(rows, cols);
		
		imageTable = new JTable(dataModel);
		imageTable.setAutoscrolls(false);
		imageTable.setDragEnabled(false);
		imageTable.setTableHeader(null);
		// imageTable.set
		// imageTable.setSelectionMode(selectionMode);
		for (int i = 0; i < dataModel.getColumnCount(); i++) {
			imageTable.getColumnModel().getColumn(i).setCellRenderer(new ImageCellRenderer());
		}

		scrollPane = new JScrollPane(imageTable);
		add(scrollPane, BorderLayout.CENTER);

		imageTable.setRowHeight((getHeight()-30) / dataModel.getRowCount());

		imageTable.setCellSelectionEnabled(true);

		imageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

//		imageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
//
//			@Override
//			public void valueChanged(ListSelectionEvent e) {
//				if (!e.getValueIsAdjusting()) {
//					int row = imageTable.getSelectedRow();
//					int col = imageTable.getSelectedColumn();
//					ImageFile imageFile = (ImageFile)dataModel.getValueAt(row, col);
//					imageFile.selected = !imageFile.selected;
//					dataModel.fireTableCellUpdated(row, col);
//				}
//			}
//
//		});
		
		imageTable.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				int row = imageTable.rowAtPoint(e.getPoint());
				int col = imageTable.columnAtPoint(e.getPoint());
				ImageFile imageFile = (ImageFile)dataModel.getValueAt(row, col);
				imageFile.selected = !imageFile.selected;
				dataModel.fireTableCellUpdated(row, col);
				
			}
		});
		imageTable.updateUI();
	}

	void writeMeta() {
		Path meta = path.resolve("meta");
			
			try (BufferedWriter writer = Files.newBufferedWriter(meta)) {
				writer.write(currentIndex+"");
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	void init(String dir) throws IOException {
		currentIndex = 0;
		fileNames.clear();
		path = Paths.get(dir);
		
		Files.newDirectoryStream(path).forEach(p -> {
			if (p.getFileName().toString().endsWith("jpg")) {
				fileNames.add(p.getFileName().toString());
			}
		});

		Collections.sort(fileNames);
		
		initTable((Integer)spRows.getValue(), (Integer)spCols.getValue());

		Path meta = path.resolve("meta");
		if (Files.exists(meta)) {
			BufferedReader reader = Files.newBufferedReader(path.resolve("meta"));
			currentIndex = Integer.valueOf(reader.readLine());
			reader.close();
		}
		
		Path blp = path.resolve("blacklist");
		if (Files.notExists(blp)) {
			Files.createFile(blp);
		}
		
		blacklist = Files.newBufferedWriter(blp, StandardOpenOption.APPEND);

		showImage();
		updateUI();
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

		List<ImageFile> data = new ArrayList<>();
		
		int rowCount = 5;
		
		int columnCount = 8;
		
		ImageTableModel() {}
		
		public void removeLastRow() {
			for (int i=0; i< columnCount; i++) {
				
				data.remove(data.size() - 1);
//				fireTableCellUpdated(row, col);
			};
			
		}

		public ImageTableModel(int rowCount, int columnCount) {
			super();
			this.rowCount = rowCount;
			this.columnCount = columnCount;
		}

		public void setColumnCount(int columnCount) {
			this.columnCount = columnCount;
		}

		@Override
		public int getRowCount() {
			return rowCount;
		}

		@Override
		public int getColumnCount() {
			return columnCount;
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data.get(row * getColumnCount() + col);
		}

		@Override
		public void setValueAt(Object aValue, int row, int col) {
			data.add((ImageFile)aValue);
			fireTableCellUpdated(row, col);
		}
		public void setRowCount(int rowCount) {
			this.rowCount = rowCount;
		}
		
		public void removeSelected() {
			for (ImageFile e: data) {
				if (e.selected) {
					fileNames.remove(e.baseName);
					try {
						Files.deleteIfExists(path.resolve(e.baseName));
						blacklist.write(e.baseName);
						blacklist.newLine();
						
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			try {
				blacklist.flush();
				data.clear();
				showImage();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
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
			//add(checkBox, BorderLayout.SOUTH);

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
			image = imageFile.image;
			updateUI(); 
			return this;
		}

		 

		@Override
		public void paint(Graphics g) {
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
