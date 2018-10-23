/**
 * 
 */
package bigdata.cv;

import static bigdata.cv.IconUtil.icon;
import static bigdata.cv.IconUtil.iconButton;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;


/**
 * @author link
 *
 */
@SuppressWarnings({"serial"})
public class LabelPanel extends JPanel implements Tool {
	
	public LabelPanel() {
		initialComponents();
		initActions();
	}

	void initialComponents() {
		
		btnOpen = iconButton("open.gif", "open dataset");
		btnOpen.addActionListener(new OpenFileAction());
		 

		btnSave = iconButton("save.gif", "save labeled bounding box to file for current image");
		btnSave.setEnabled(false);
		btnDelete = iconButton("delete.gif", "delete current image and label file");
		btnDelete.setEnabled(false);

		btnConvert = new JButton(icon("convert.gif", "convert the labeled bounding box file to annother format"));
		btnConvert.setToolTipText("convert the labeled bounding box file to annother format");

		btnCorp = new JButton(icon("corp.gif", "corp inner labeld bounding fox within vehicle box"));
		btnCorp.setToolTipText("corp inner labeld bounding fox within vehicle box");

		btnFindByName = new JButton(icon("findByName.gif", "find image via its name"));
		btnFindByName.setToolTipText("find image via its name");

		btnFindByClazz = new JButton(icon("findByClazz.gif", "find image via an given clazz"));
		btnFindByClazz.setToolTipText("find image via an given clazz");
		//				toolBar.add(btnFindByClazz);

		btnFilter = new JButton(icon("filter.gif", "filter one or more label class and export to annother dataset"));
		btnFilter.setToolTipText("filter one or more label class and export to annother dataset");
		 
		btnAutoForward = new JButton(icon("auto_forward.gif", "auto forward to next image without bounding box"));
		btnAutoForward.setToolTipText("auto forward to next image without bounding box");
		 
		setLayout(new BorderLayout(10, 10)); 
		
		// btnClearFilter = new JButton(icon("clear_filter.gif", "clear current
		// label class filter"));
		// btnClearFilter.setToolTipText("clear current label class filter");
		// toolBar.add(btnClearFilter); 

		btnRemoveBoundingBox = iconButton("remove_one.png", "remove one bounding label");
		btnCleanBoundingBox = iconButton("remove_all.png", "remove all bounding labels");

		add(annotationPanel, BorderLayout.CENTER);
		JPanel centerPanel = new JPanel();
		annotationPanel.add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(5, 5));

		String[] columnNames = { "Label", "Bounding Box" };
		tableModel = new DefaultTableModel(null, columnNames) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		JPanel toolBoxPanel = new JPanel();
		annotationPanel.add(toolBoxPanel, BorderLayout.EAST);
		toolBoxPanel.setLayout(new BorderLayout(5, 5));
		JPanel centerToolBox = new JPanel();
		toolBoxPanel.add(centerToolBox, BorderLayout.CENTER);
		GridBagLayout gblCenterToolBox = new GridBagLayout();
		centerToolBox.setLayout(gblCenterToolBox);

		gblCenterToolBox.columnWidths = new int[] { 432, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gblCenterToolBox.rowHeights = new int[] { 22, 0, 0 };
		gblCenterToolBox.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				Double.MIN_VALUE };
		gblCenterToolBox.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };

		tblBoudingBox = new JTable(tableModel);
		tblBoudingBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblBoudingBox.getColumnModel().getColumn(0).setPreferredWidth(5);

		JScrollPane scrollPane = new JScrollPane(tblBoudingBox);
		GridBagConstraints gbcScrollPane = new GridBagConstraints();
		gbcScrollPane.anchor = GridBagConstraints.SOUTH;
		gbcScrollPane.fill = GridBagConstraints.BOTH;
		gbcScrollPane.gridx = 0;
		gbcScrollPane.gridy = 0;
		centerToolBox.add(scrollPane, gbcScrollPane);

		JPanel corpPanel = new JPanel();
		GridBagConstraints gbcCorpPanel = new GridBagConstraints();
		gbcCorpPanel.anchor = GridBagConstraints.NORTH;
		gbcCorpPanel.fill = GridBagConstraints.BOTH;
		gbcCorpPanel.gridx = 0;
		gbcCorpPanel.gridy = 1;
		gbcCorpPanel.gridheight = 1;
		centerToolBox.add(monitorPanel, gbcCorpPanel);

		corpPanel.setLayout(new BorderLayout(10, 10));

		centerPanel.add(imagePanel, BorderLayout.CENTER);
		imagePanel.tableModel = tableModel;

		// status panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		centerPanel.add(panel, BorderLayout.SOUTH);

		JPanel statusPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) statusPanel.getLayout();
		flowLayout.setHgap(10);
		panel.add(statusPanel);


		btnFirst =iconButton("first.png", "skip to first sample of current dataset");
		btnPreviouse = iconButton("previous.png", "skip to previous sample of current dataset");
		btnNext = iconButton("next.png", "skip to next sample of current dataset");
		btnLast = iconButton("last.png", "skip to last sample of current dataset");

		JLabel lblTotalImage = new JLabel("Total Image:");
		statusPanel.add(lblTotalImage);

		valTotalImage = new JLabel("NaN");
		statusPanel.add(valTotalImage);

		JLabel lblTotalLabel = new JLabel("BoundingBox Files:");
		statusPanel.add(lblTotalLabel);

		valTotalLabel = new JLabel("NaN");
		statusPanel.add(valTotalLabel);

		lblFileName = new JLabel("");
		statusPanel.add(lblFileName);

		lblCurrentImageIndex = new JLabel("");
		statusPanel.add(lblCurrentImageIndex);

		this.lblScalePercent = new JLabel("");
		statusPanel.add(lblScalePercent);

		lblResolution = new JLabel("");
		statusPanel.add(lblResolution);
	}
	
	 
	void initActions() {
		imagePanel.listener = new ImagePanelListener() {

			@Override
			public void postLabelFileSave(boolean update) {
				if (!update) {
					int labels = Integer.valueOf(valTotalLabel.getText());
					labels++;
					valTotalLabel.setText(String.valueOf(labels));
				}
			}

			@Override
			public void postScaled() {
				String text = String.format("%.0f", 100 / imagePanel.scaleFactor);
				lblScalePercent.setText(text + "%");

			}

			@Override
			public void postCorp(BufferedImage image) {
				monitorPanel.setImage(image);
				tblBoudingBox.getSelectionModel().clearSelection();
				btnSave.setEnabled(true);
			}

			@Override
			public void postSelectedImage(BufferedImage image) {
				monitorPanel.setImage(image);
			}

			@Override
			public void postOpen() {
				lblResolution.setText(String.format("%dX%d", imagePanel.imageWidth, imagePanel.imageHeight));
				btnSave.setEnabled(false);
			}

			@Override
			public void postChange(BufferedImage image) {
				monitorPanel.setImage(image);
			}

			@Override
			public void postChangeLabel(int row, LabeledBoundingBox bb) {
				if (tableModel.getRowCount() > 0)
					tableModel.setValueAt(bb.boundingBoxString(), row, 1);
			}
		};

		tblBoudingBox.getSelectionModel().addListSelectionListener(boundingBoxListSelectionListener);
		
		btnRemoveBoundingBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int index = tblBoudingBox.getSelectedRow();
				imagePanel.removeBoundingBox(index);
				monitorPanel.clearImage();
			}
		});

		btnCleanBoundingBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				imagePanel.removeAllBoundingBox();
				monitorPanel.clearImage();
			}
		});

		btnFirst.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentImageIndex = 0;
				imagePanel.load(imageFiles.get(currentImageIndex));
				lblFileName.setText(imageFiles.get(currentImageIndex));
				monitorPanel.clearImage();
				lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
			}
		});

		btnNext.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentImageIndex < imageFiles.size() - 1) {
					if (filterClazz.size() > 0) {
						try {
							filterNext();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					} else {
						++currentImageIndex;
					}
					showCurrImage();
				}
			}
		});

		btnAutoForward.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				autoLocateImage();
			}
		});

		btnPreviouse.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				monitorPanel.clearImage();
				if (currentImageIndex > 0) {
					if (filterClazz.isEmpty()) {
						--currentImageIndex;
					} else {
						try {
							filterPre();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					showCurrImage();
				}
			}
		});

		btnLast.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentImageIndex = imageFiles.size() - 1;
				imagePanel.load(imageFiles.get(currentImageIndex));
				lblFileName.setText(imageFiles.get(currentImageIndex));
				lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
			}
		});

		ActionListener findActionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String image = JOptionPane.showInputDialog("image file name:");
				if (image != null) {
					for (int i = 0; i < imageFiles.size(); i++) {
						if (image.equals(imageFiles.get(i))) {
							currentImageIndex = i;
							imagePanel.load(imageFiles.get(currentImageIndex));
							lblFileName.setText(imageFiles.get(currentImageIndex));
							lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
						}
					}
				}
			}
		};

		btnFindByName.addActionListener(findActionListener);

		btnFindByClazz.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (filterClazz.isEmpty()) {
					DialogUtil.showWarningMsg("does not select any clazz in filter dialog");
				} else {
					findImageByClazz();
				}
			}

		});

		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imagePanel.saveLabelsToFile();
			}
		});

		btnDelete.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				String imageFile = imageFiles.get(currentImageIndex);
				String labelFile = "";
				int i = imageFile.lastIndexOf(".");
				if (i != -1) {
					labelFile = imageFile.substring(0, i) + ".label";
				}
				String msg = String.format("are you sure to delete %s and its label file:%s if exsited", imageFile,
						labelFile);
				int ret = showConfirmDialog(frame, msg, "confirm delete image", YES_NO_OPTION);
				if (ret == YES_OPTION) {
					deleteCurrentFiles();
					monitorPanel.clearImage();
				}
			}
		});

		ActionListener convertActionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ConvertDialog dialog = new ConvertDialog(frame, true, dataSet, labelConfig);
				dialog.setVisible(true);
				dialog.setAlwaysOnTop(true);
				dialog.setLocationRelativeTo(frame);
			}

		};
		btnConvert.addActionListener(convertActionListener);

		ActionListener corpActionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				CorpDialog dialog = new CorpDialog(frame, true, dataSet, labelConfig);
				dialog.setVisible(true);
				dialog.setAlwaysOnTop(true);
				dialog.setLocationRelativeTo(frame);
			}
		};

		btnCorp.addActionListener(corpActionListener);

		btnFilter.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FilterDialog filterDialog = new FilterDialog(LabelPanel.this, true);
				filterDialog.setVisible(true);
				filterDialog.setAlwaysOnTop(true);
				filterDialog.setLocationRelativeTo(frame);
			}
		}); 
	}

	JFrame frame;

	LabelConfig labelConfig;


	int totalFile;
	int totalBoudingBoxLabels;

	List<String> imageFiles;

	List<String> labelFiles;

	LoadImageThread loadImageThread;

	JLabel valTotalImage;

	JLabel valTotalLabel;

	JLabel lblScalePercent;

	JLabel lblResolution;

	int currentImageIndex = -1;

	ImagePanel imagePanel = new ImagePanel();
	MonitorPanel monitorPanel = new MonitorPanel();
	
	

	JButton btnFirst;
	JButton btnPreviouse;
	JButton btnNext;
	JButton btnAutoNext;
	JButton btnLast;
	JLabel lblFileName;
	JLabel lblCurrentImageIndex;

	JTable tblBoudingBox;
	DefaultTableModel tableModel;
	
	ListSelectionListener boundingBoxListSelectionListener = new ListSelectionListener() {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				int msi = lsm.getMinSelectionIndex();
				if (msi < 0) return ;
				imagePanel.selectBoundingBox(msi);
				imagePanel.requestFocusInWindow();
			}
		}
	};
	
	private JButton btnCleanBoundingBox;
	private JButton btnRemoveBoundingBox;

	DataSet dataSet;

	JButton moveLeft = new JButton("Move Left");
	JButton moveRight = new JButton("Move Right");
	JButton moveUp = new JButton("Move Up");
	JButton moveDown = new JButton("Move Down");

	JButton expandLeft = new JButton("Expand Left");
	JButton expandRight = new JButton("Expand Right");
	JButton expandTop = new JButton("Expand Top");
	JButton expandBottom = new JButton("Expand Bottom");

	JButton shrinkLeft = new JButton("Shrink Left");
	JButton shrinkRight = new JButton("Shrink Right");
	JButton shrinkTop = new JButton("Shrink Top");
	JButton shinkBottom = new JButton("Shrink Bottom");

	private JButton btnOpen;
	private JButton btnDelete;
	private JButton btnConvert;
	private JButton btnFindByName;
	private JButton btnFindByClazz;
	private JButton btnFilter;
	private JButton btnSave;
	private JButton btnCorp;
	
	JToolBar toolBar;
	JButton btnOpenClazz;

	boolean filtered = false;

	Set<String> filterClazz = new HashSet<>();


	private JButton btnAutoForward;
	JPanel annotationPanel = new JPanel(new BorderLayout());

	private class OpenFileAction extends AbstractAction {

		private static final long serialVersionUID = 7402675318287934991L;

		JTextField txtDirectory;

		public OpenFileAction() {
			putValue(NAME, "Open DateSet");
			putValue(SHORT_DESCRIPTION, "open a directory which contain image dataset");
		}

		public void actionPerformed(ActionEvent e) {
			imagePanel.enabled = false;
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				if (txtDirectory != null) {
					txtDirectory.setText(chooser.getSelectedFile().getAbsolutePath());
					monitorPanel.clearImage();
				}
				
				Path path = chooser.getSelectedFile().toPath();
				try {
					labelConfig = new LabelConfig(path.resolve("label.names"));
					imagePanel.labelConfig = labelConfig;
				} catch (IOException ioe) {
					throw new RuntimeException(ioe); 
				}
				
				loadImageThread = new LoadImageThread(path);
				loadImageThread.start();
			}
		}
	}

	private void autoLocateImage() {
		currentImageIndex = 0;
		if (labelFiles.size() > 0) {
			for (; currentImageIndex < imageFiles.size(); currentImageIndex++) {
				String imageFile = imageFiles.get(currentImageIndex);
				int i = imageFile.lastIndexOf(".");
				String labelFile = imageFile.substring(0, i) + ".label";
				if (Files.notExists(dataSet.getRawLabel(labelFile))) {
					break;
				}
			}
		}
		showCurrImage();
	}

	void findImageByClazz() {
		if (labelFiles.size() > 0) {
			try {
				boolean found = false;
				for (; !found && currentImageIndex < imageFiles.size(); currentImageIndex++) {
					String imageFile = imageFiles.get(currentImageIndex);
					int i = imageFile.lastIndexOf(".");
					String labelFile = imageFile.substring(0, i) + ".label";

					List<LabeledBoundingBox> lbbs = this.dataSet.readBoundingBoxes(labelFile);
					for (LabeledBoundingBox lbb : lbbs) {
						found = filterClazz.contains(lbb.labelName);
						if (found)
							break;
					}
					if (found)
						break;
				}
				if (found) {
					showCurrImage();
				}
				else {
					DialogUtil.showInfoMsg("find clazz error, see track log");
				}
			} catch (IOException e) {
				e.printStackTrace();
				DialogUtil.showWarningMsg("find clazz error, see track log");
			}
		}
	}

	private void showCurrImage() {
		if (currentImageIndex >= imageFiles.size())
			currentImageIndex = 0;
		monitorPanel.clearImage();
		ListSelectionModel selectionModel = tblBoudingBox.getSelectionModel();
		selectionModel.removeListSelectionListener(boundingBoxListSelectionListener);
		imagePanel.load(imageFiles.get(currentImageIndex));
		selectionModel.addListSelectionListener(boundingBoxListSelectionListener);
		lblFileName.setText(imageFiles.get(currentImageIndex));
		lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
	}

	void filterNext() throws IOException {
		boolean found = false;
		currentImageIndex++;
		for (; !found && currentImageIndex < imageFiles.size(); currentImageIndex++) {
			String imageFile = imageFiles.get(currentImageIndex);
			int i = imageFile.lastIndexOf(".");
			String labelFile = imageFile.substring(0, i) + ".label";
			List<LabeledBoundingBox> boxes = dataSet.readBoundingBoxes(labelFile);
			if (boxes != null) {
				for (LabeledBoundingBox box : boxes) {
					if (filterClazz.contains(box.labelName)) {
						found = true;
						break;
					}
				}
			}
			if (found) break;
		}
	}

	void filterPre() throws IOException {
		boolean found = false;
		if(currentImageIndex> 0)
			currentImageIndex--;
		for (; !found && currentImageIndex > 0; currentImageIndex--) {
			String imageFile = imageFiles.get(currentImageIndex);
			int i = imageFile.lastIndexOf(".");
			String labelFile = imageFile.substring(0, i) + ".label";
			List<LabeledBoundingBox> boxes = dataSet.readBoundingBoxes(labelFile);
			if (boxes != null) {
				for (LabeledBoundingBox box : boxes) {
					if (filterClazz.contains(box.labelName)) {
						found = true;
						break;
					}
				}
			}

			if (found) break;
		}
	}

	class LoadImageThread extends Thread {
		Path path;

		public LoadImageThread(Path path) {
			super();
			this.path = path;
		}

		public void run() {
			try {
				dataSet = new DataSet(path);
				totalFile = dataSet.imageFiles.size();
				imageFiles = dataSet.imageFiles;
				labelFiles = dataSet.rawLabelFiles;
				valTotalImage.setText(String.valueOf(totalFile));
				totalBoudingBoxLabels = dataSet.rawLabelFiles.size();
				valTotalLabel.setText(String.valueOf(totalBoudingBoxLabels));

				// locate the first un-labeled image
				imagePanel.dataSet = dataSet;
				autoLocateImage();
				btnDelete.setEnabled(true);
				imagePanel.enabled = true;
				// CardLayout layout = (CardLayout)frame.getContentPane().getLayout();
				// cardLayout.show(cards, "annotationPanel");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}



	public void clearFilter() {
		filtered = false;
		this.filterClazz.clear();
	}

	public void addClazzFilter(String clazz) {
		filterClazz.add(clazz);
	}

	public void removeClazzFilter(String clazz) {
		filterClazz.remove(clazz);
	}


	void deleteCurrentFiles() {
		String fileName = imageFiles.get(currentImageIndex);
		imageFiles.remove(currentImageIndex);
		try {
			Files.deleteIfExists(dataSet.getImage(fileName));
			int i = fileName.lastIndexOf(".");
			if (i != -1) {
				String labelFile = fileName.substring(0, i) + ".label";
				Files.deleteIfExists(dataSet.getRawLabel(labelFile));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		int maxIndex = imageFiles.size() - 1;
		if (currentImageIndex > maxIndex) {
			currentImageIndex = maxIndex;
		}
		imagePanel.load(imageFiles.get(currentImageIndex));
		lblFileName.setText(imageFiles.get(currentImageIndex));
		valTotalImage.setText("" + imageFiles.size());
	}

	@Override
	public void saveCurrentWork() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addButtons() {
		// labels
		toolBar.addSeparator();
		toolBar.add(btnOpen);
		toolBar.add(btnSave);
		toolBar.add(btnDelete);
		toolBar.addSeparator();
		toolBar.add(btnConvert);
		toolBar.add(btnCorp);
		toolBar.add(btnFindByName);
		toolBar.add(btnFilter);
		toolBar.addSeparator();
		toolBar.add(btnFirst);
		toolBar.add(btnPreviouse);
		toolBar.add(btnNext);
		toolBar.add(btnLast);
		toolBar.add(btnAutoForward);
		
		toolBar.addSeparator();
		toolBar.add(btnRemoveBoundingBox);
		toolBar.add(btnCleanBoundingBox);
		
	}

}
