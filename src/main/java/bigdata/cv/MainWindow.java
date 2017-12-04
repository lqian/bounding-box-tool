package bigdata.cv;

import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;

import java.awt.BorderLayout;
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
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
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
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class MainWindow implements WindowListener{

	private JFrame frame;
	private JTextField txtDirectory;

	int totalFile;
	int totalBoudingBoxLabels;

	List<String> imageFiles;

	List<String> labelFiles;

	LoadImageThread loadImageThread;

	JLabel valTotalImage;

	JLabel valTotalLabel;
	
	JLabel lblScalePercent;
	
	JLabel lblResolution ;

	int currentImageIndex = -1;

	ImagePanel imagePanel = new ImagePanel();
	MonitorPanel  monitorPanel = new MonitorPanel();
	
	private JButton btnFirst;
	private JButton btnPreviouse;
	private JButton btnNext;
	private JButton btnAutoNext;
	private JButton btnLast;
	JButton btnFind;
	private JLabel lblFileName;
	JLabel lblCurrentImageIndex;

	JTable tblBoudingBox;
	DefaultTableModel tableModel;
	private JButton btnCleanBoundingBox;
	private JButton btnRemoveBoundingBox;
	private JButton btnOpen;
	private JButton btnDelPicture;
	private JButton btnAbout;
	private JButton btnConvert;
	
	Path datasetPath;

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
	 */
	public MainWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			// UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
		}

		initialize();
		initActions();
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
			}

			@Override
			public void postSelectedImage(BufferedImage image) {
				monitorPanel.setImage(image);
			}

			@Override
			public void postOpen() {
				lblResolution.setText(String.format("%dX%d", imagePanel.imageWidth, imagePanel.imageHeight));
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
					imagePanel.load(imageFiles.get(++currentImageIndex));
					lblFileName.setText(imageFiles.get(currentImageIndex));
					monitorPanel.clearImage();
					lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
				}
			}
		});
		
		btnAutoNext.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				monitorPanel.clearImage();
				autoLocateImage() ;
				lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
			}
			
		});

		btnPreviouse.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				monitorPanel.clearImage();
				if (currentImageIndex > 0) {
					imagePanel.load(imageFiles.get(--currentImageIndex));
					lblFileName.setText(imageFiles.get(currentImageIndex));
					lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
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
		
		btnFind.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String image = JOptionPane.showInputDialog("image file name:");
				if (image != null) {
					for (int i=0; i < imageFiles.size(); i++) {
						if (image.equals(imageFiles.get(i))) {
							currentImageIndex = i;
							imagePanel.load(imageFiles.get(currentImageIndex));
							lblFileName.setText(imageFiles.get(currentImageIndex));
							lblCurrentImageIndex.setText(String.valueOf(currentImageIndex));
						}
					}
				}
			}
		});
		
		btnDelPicture.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				String imageFile = imageFiles.get(currentImageIndex);
				String labelFile = "";
				int i = imageFile.lastIndexOf(".");
				if (i != -1) {
					labelFile = imageFile.substring(0, i) + ".label";
				}
				String msg = String.format("are you sure to delete %s and its label file:%s if exsited", imageFile, labelFile);
				int ret = showConfirmDialog(frame, msg, "confirm delete image", YES_NO_OPTION);
				if (ret == YES_OPTION) {
					deleteCurrentFiles();
					monitorPanel.clearImage();
				}
			}

		});
		
		btnConvert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ConvertDialog dialog = new ConvertDialog(frame, true, datasetPath);
				dialog.setVisible(true);
				dialog.setAlwaysOnTop(true);
				dialog.setLocationRelativeTo(frame);
			}
			
		});
	}

	void deleteCurrentFiles() {
		String fileName = imageFiles.get(currentImageIndex);
		imageFiles.remove(currentImageIndex);
		try {
			Files.deleteIfExists(datasetPath.resolve(fileName));
			int i = fileName.lastIndexOf(".");
			if (i != -1) {
				String labelFile = fileName.substring(0, i) + ".label";
				Files.deleteIfExists(datasetPath.resolve(labelFile));
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

	/**
	 * Initialize the contents of the frame.
	 */
	
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(10, 10));

		JToolBar toolBar = new JToolBar();
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);
		btnOpen = new JButton("Open DataSet");
		btnOpen.setAction(new OpenFileAction());
		toolBar.add(btnOpen);

		btnDelPicture = new JButton();
		toolBar.add(btnDelPicture);
		btnDelPicture.setText("Delete Picutre");
		btnDelPicture.setEnabled(false);

		btnConvert = new JButton("Convert");
		toolBar.add(btnConvert);

		btnAbout = new JButton("About");
		toolBar.add(btnAbout);

		JButton btnOpenFile = new JButton("Open");
		btnOpenFile.setAction(new OpenFileAction(txtDirectory));
		btnOpenFile.setText("Open DataSet");
		btnOpenFile.setToolTipText("click the button open image dataset directory");

		btnRemoveBoundingBox = new JButton("Remove");
		btnCleanBoundingBox = new JButton("Clear All");

		JPanel centerPanel = new JPanel();
		frame.getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(5, 5));

		String[] columnNames = { "Label", "Bounding Box" };
		tableModel = new DefaultTableModel(null, columnNames) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		JPanel toolBoxPanel = new JPanel();
		frame.getContentPane().add(toolBoxPanel, BorderLayout.EAST);
		toolBoxPanel.setLayout(new BorderLayout(5, 5));
		JPanel centerToolBox = new JPanel();
		toolBoxPanel.add(centerToolBox, BorderLayout.CENTER);
		GridBagLayout gblCenterToolBox = new GridBagLayout();
		centerToolBox.setLayout(gblCenterToolBox);
		
		gblCenterToolBox.columnWidths = new int[]{432, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gblCenterToolBox.rowHeights = new int[]{22, 0, 0};
		gblCenterToolBox.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gblCenterToolBox.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		
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
		
//		JPanel corpNorth = new JPanel();
//		corpNorth.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
//		corpNorth.add(moveUp);
//		corpNorth.add(expandTop);
//		corpNorth.add(shrinkTop);
//
//		JPanel corpSouth = new JPanel(); 
//		corpSouth.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
//		corpSouth.add(moveDown);
//		corpSouth.add(expandBottom);
//		corpSouth.add(shinkBottom);
//		
//		JPanel corpWest = new JPanel();
//		corpWest.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
//		corpWest.add(moveLeft);
//		corpWest.add(expandLeft);
//		corpWest.add(shrinkLeft);
//		
//		corpPanel.add(corpNorth, BorderLayout.NORTH);
//		corpPanel.add(corpSouth, BorderLayout.SOUTH);
//		corpPanel.add(corpWest, BorderLayout.WEST);
//		corpPanel.add(monitorPanel, BorderLayout.CENTER);

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
		
		btnAutoNext = new JButton("->>");
		navPanel.add(btnAutoNext);

		btnLast = new JButton(">|");
		navPanel.add(btnLast);
		
		btnFind = new JButton("Find");
		navPanel.add(btnFind);

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
		
		frame.pack();
	}

	private class OpenFileAction extends AbstractAction {

		private static final long serialVersionUID = 7402675318287934991L;

		JTextField txtDirectory;

		public OpenFileAction() {
			putValue(NAME, "Open DateSet");
			putValue(SHORT_DESCRIPTION, "open a directory which contain image dataset");
		}

		public OpenFileAction(JTextField txtDirectory) {
			this.txtDirectory = txtDirectory;
			putValue(NAME, "Open DateSet");
			putValue(SHORT_DESCRIPTION, "open a directory which contain image dataset");
		}

		public void actionPerformed(ActionEvent e) {
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
				if (Files.notExists(datasetPath.resolve(labelFile))) {
					break;
				}
			}
		}
		if (currentImageIndex >= imageFiles.size()) 
			currentImageIndex = 0;
		imagePanel.load(imageFiles.get(currentImageIndex));
		lblFileName.setText(imageFiles.get(currentImageIndex));
	}
	

	class LoadImageThread extends Thread {
		Path path;

		public LoadImageThread(Path path) {
			super();
			this.path = path;
		}

		public void run() {
			try {
				DataSetVisitor visitor = new DataSetVisitor();
				Files.walkFileTree(path, visitor);
				Collections.sort(visitor.imageFiles);
				Collections.sort(visitor.labelFiles);
				totalFile = visitor.imageFiles.size();
				imageFiles = visitor.imageFiles;
				labelFiles =  visitor.labelFiles;
				valTotalImage.setText(String.valueOf(totalFile));
				totalBoudingBoxLabels = visitor.labelFiles.size();
				valTotalLabel.setText(String.valueOf(totalBoudingBoxLabels));

				// locate the first un-labeled image
				datasetPath = path;
				imagePanel.datasetPath = path;
				autoLocateImage();
				btnDelPicture.setEnabled(true);
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
}
