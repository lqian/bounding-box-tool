/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
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

import bigdata.cv.ImageCellRenderer.ImageFile;

/**
 * @author lqian
 *
 */
public class ClassificationPanel extends JPanel implements Tool {

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
				dataModel.removeSelected();			
			}

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

	@SuppressWarnings("serial")
	private void initTable(int rows, int cols) {
		if (imageTable != null) {
			remove(imageTable);
		}

		if (scrollPane != null ) {
			remove(scrollPane);
		}

		dataModel = new ImageTableModel(rows, cols) {
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
		};

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


	@Override
	public void saveCurrentWork() {

	}

	@Override
	public void addButtons() {
		
	}

}
