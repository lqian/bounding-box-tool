/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package bigdata.cv;

import static bigdata.cv.IconUtil.icon;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class MainWindow implements WindowListener {

	LabelConfig labelConfig;

	JFrame frame;

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

	JMenuBar menuBar = new JMenuBar();

	JMenu fileMenu = new JMenu("File");

	JMenu editMenu = new JMenu("Edit");

	JMenu utilMenu = new JMenu("Utility");

	JMenu aboutMenu = new JMenu("About");

	JButton btnFirst;
	JButton btnPreviouse;
	JButton btnNext;
	JButton btnAutoNext;
	JButton btnLast;
	JLabel lblFileName;
	JLabel lblCurrentImageIndex;

	JTable tblBoudingBox;
	DefaultTableModel tableModel;
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
	private JButton btnAbout;
	
	JButton btnOpenClazz;

	boolean filtered = false;

	Set<String> filterClazz = new HashSet<>();

	FilterDialog filterDialog;

	private JButton btnAutoForward;
	
	CardLayout cardLayout = new CardLayout();
	JPanel cards = new JPanel(cardLayout);
	JPanel annotationPanel = new JPanel(new BorderLayout());
	ClassificationPanel classificationPanel = new ClassificationPanel();
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
					window.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
					window.frame.addWindowListener(window);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws IOException
	 */
	public MainWindow() throws IOException {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			// UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			System.out.println(e);
		}
		labelConfig = new LabelConfig();
		initialize();
		initActions();
	}

	void initActions() {
		imagePanel.labelConfig = labelConfig;
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
				tableModel.setValueAt(bb.boundingBoxString(), row, 1);
			}
		};

		tblBoudingBox.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					ListSelectionModel lsm = (ListSelectionModel) e.getSource();
					int msi = lsm.getMinSelectionIndex();
					imagePanel.selectBoundingBox(msi);
					imagePanel.requestFocusInWindow();
				}
			}
		});

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
				filterDialog.setVisible(true);
				filterDialog.setAlwaysOnTop(true);
				filterDialog.setLocationRelativeTo(frame);
			}

		});
		
		this.btnOpenClazz.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					 //
					try {
						classificationPanel.init(chooser.getSelectedFile().getAbsolutePath());
						cardLayout.show(cards, "classificationPanel");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
				}
			}
		});
	}

	void buildClassificationPanel() {
		
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

	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(10, 10));
		
		
		JToolBar toolBar = new JToolBar();
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);
		btnOpen = new JButton(icon("open.gif", "open dataset"));
		btnOpen.setToolTipText("open dataset");
		btnOpen.addActionListener(new OpenFileAction());
		toolBar.add(btnOpen);

		btnSave = new JButton(icon("save.gif", "save labeled bounding box to file for current image"));
		btnSave.setToolTipText("save labeled bounding box to file for current image");
		btnSave.setEnabled(false);
		toolBar.add(btnSave);

		btnDelete = new JButton(icon("delete.gif", "delete current image and label file"));
		toolBar.add(btnDelete);
		btnDelete.setEnabled(false);
		btnDelete.setToolTipText("delete current image and label file");

		toolBar.addSeparator();
		btnConvert = new JButton(icon("convert.gif", "convert the labeled bounding box file to annother format"));
		toolBar.add(btnConvert);
		btnConvert.setToolTipText("convert the labeled bounding box file to annother format");

		btnCorp = new JButton(icon("corp.gif", "corp inner labeld bounding fox within vehicle box"));
		btnCorp.setToolTipText("corp inner labeld bounding fox within vehicle box");
		toolBar.add(btnCorp);

		btnFindByName = new JButton(icon("findByName.gif", "find image via its name"));
		btnFindByName.setToolTipText("find image via its name");
		toolBar.add(btnFindByName);

		btnFindByClazz = new JButton(icon("findByClazz.gif", "find image via an given clazz"));
		btnFindByClazz.setToolTipText("find image via an given clazz");
//		toolBar.add(btnFindByClazz);

		btnFilter = new JButton(icon("filter.gif", "filter one or more label class and export to annother dataset"));
		btnFilter.setToolTipText("filter one or more label class and export to annother dataset");
		toolBar.add(btnFilter);

		btnAutoForward = new JButton(icon("auto_forward.gif", "auto forward to next image without bounding box"));
		btnAutoForward.setToolTipText("auto forward to next image without bounding box");
		toolBar.add(btnAutoForward);
		// btnClearFilter = new JButton(icon("clear_filter.gif", "clear current
		// label class filter"));
		// btnClearFilter.setToolTipText("clear current label class filter");
		// toolBar.add(btnClearFilter);
		
		toolBar.addSeparator();
		btnOpenClazz = new JButton(icon("open-clazz-dataset.png", "open classification dataset"));
		btnOpenClazz.setToolTipText("open classification dataset");
		toolBar.add(btnOpenClazz);

		toolBar.addSeparator();
		btnAbout = new JButton(icon("about.gif", ""));
		toolBar.add(btnAbout);

		
		frame.getContentPane().add(cards, BorderLayout.CENTER);
		
		cards.add(annotationPanel, "annotationPanel");
		cards.add(classificationPanel, "classificationPanel");
		
		btnRemoveBoundingBox = new JButton("Remove");
		btnCleanBoundingBox = new JButton("Clear All");

		
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

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		buttonPane.add(btnRemoveBoundingBox);
		buttonPane.add(btnCleanBoundingBox);
		toolBoxPanel.add(buttonPane, BorderLayout.SOUTH);

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

		JPanel navPanel = new JPanel();
		panel.add(navPanel);
		navPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		btnFirst = new JButton("|<");
		navPanel.add(btnFirst);

		btnPreviouse = new JButton("<<");
		navPanel.add(btnPreviouse);

		btnNext = new JButton(">>");
		navPanel.add(btnNext);

		btnLast = new JButton(">|");
		navPanel.add(btnLast);

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

		filterDialog = new FilterDialog(MainWindow.this, true);

		frame.pack();
	}

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
				loadImageThread = new LoadImageThread(chooser.getSelectedFile().toPath());
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
		imagePanel.load(imageFiles.get(currentImageIndex));
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
				CardLayout layout = (CardLayout)frame.getContentPane().getLayout();
				layout.show(frame.getContentPane(), "annotationPanel");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {

	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (this.imagePanel.hasChanged) {
			imagePanel.saveLabelsToFile();
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {

	}

	@Override
	public void windowIconified(WindowEvent e) {

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
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
}
