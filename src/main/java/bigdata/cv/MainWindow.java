package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

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
import static javax.swing.JOptionPane.*;

public class MainWindow {

	private JFrame frame;
	private JTextField txtDirectory;

	int totalFile;
	int totalBoudingBoxLabels;

	List<String> imageFiles = new ArrayList<String>();

	PriorityQueue<String> labelFiles = new PriorityQueue<String>();

	LoadImageThread loadImageThread;

	JLabel valTotalImage;

	JLabel valTotalLabel;

	int currentImageIndex = -1;

	ImagePanel imagePanel = new ImagePanel();
	private JButton btnFirst;
	private JButton btnPreviouse;
	private JButton btnNext;
	private JButton btnLast;
	private JLabel lblFileName;

	JTable tblBoudingBox;
	DefaultListModel<String> listModel = new DefaultListModel<String>();
	private JButton btnCleanBoundingBox;
	private JButton btnRemoveBoundingBox;
	private JButton btnOpen;
	private JButton btnDelPicture;
	private JButton btnAbout;
	private JButton btnConvert;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
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

		tblBoudingBox.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					ListSelectionModel lsm = (ListSelectionModel) e.getSource();
					int msi = lsm.getMinSelectionIndex();
					imagePanel.selectBoundingBox(msi);
				}
			}

		});

		btnRemoveBoundingBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int index = tblBoudingBox.getSelectedRow();
				imagePanel.removeBoundingBox(index);
			}
		});

		btnCleanBoundingBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				imagePanel.removeAllBoundingBox();
			}
		});

		btnFirst.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentImageIndex = 0;
				imagePanel.load(imageFiles.get(currentImageIndex));
				lblFileName.setText(imageFiles.get(currentImageIndex));
			}
		});

		btnNext.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentImageIndex < imageFiles.size() - 1) {
					imagePanel.load(imageFiles.get(++currentImageIndex));
					lblFileName.setText(imageFiles.get(currentImageIndex));
				}
			}
		});

		this.btnPreviouse.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentImageIndex > 0) {
					imagePanel.load(imageFiles.get(--currentImageIndex));
					lblFileName.setText(imageFiles.get(currentImageIndex));
				}
			}
		});

		this.btnLast.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentImageIndex = imageFiles.size() - 1;
				imagePanel.load(imageFiles.get(currentImageIndex));
				lblFileName.setText(imageFiles.get(currentImageIndex));
			}
		});
	}

	void deleteCurrentFiles() {
		String fileName = imageFiles.get(currentImageIndex);
		imageFiles.remove(currentImageIndex);
		try {
			Files.deleteIfExists(Paths.get(fileName));
			int i = fileName.lastIndexOf(".");
			if (i != -1) {
				String labelFile = fileName.substring(0, i) + ".label";
				Files.deleteIfExists(Paths.get(labelFile));
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
		btnDelPicture.setAction(new AbstractAction() {

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
				}
			}

		});
		btnDelPicture.setText("Delete Picutre");

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
		@SuppressWarnings("serial")
		DefaultTableModel tableModel = new DefaultTableModel(null, columnNames) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		JPanel toolBoxPanel = new JPanel();
		frame.getContentPane().add(toolBoxPanel, BorderLayout.EAST);
		toolBoxPanel.setLayout(new BorderLayout(5, 5));

		tblBoudingBox = new JTable(tableModel);
		tblBoudingBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblBoudingBox.getColumnModel().getColumn(0).setPreferredWidth(5);
		JScrollPane scrollPane = new JScrollPane(tblBoudingBox);
		toolBoxPanel.add(scrollPane, BorderLayout.CENTER);

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
				}
				loadImageThread = new LoadImageThread(chooser.getSelectedFile().toPath());
				loadImageThread.start();
			}
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
				DataSetVisitor visitor = new DataSetVisitor(labelFiles);
				Files.walkFileTree(path, visitor);
				totalFile = visitor.imageFiles.size();
				imageFiles.addAll(visitor.imageFiles);
//				imageFiles.addAll(Arrays.asList(visitor.imageFiles.toArray(new String[totalFile])));
				valTotalImage.setText(String.valueOf(totalFile));
				totalBoudingBoxLabels = visitor.labelFiles.size();
				valTotalLabel.setText(String.valueOf(totalBoudingBoxLabels));

				// locate the first un-labeled image
				currentImageIndex = 0;
				if (labelFiles.size() > 0) {
					Iterator<String> iter = visitor.labelFiles.iterator();
					while (iter.hasNext() && currentImageIndex < totalFile) {
						String labelFile = iter.next();
						String imageFile = imageFiles.get(currentImageIndex);
						int i = imageFile.lastIndexOf(".");
						if (i != -1) {
							if (!labelFile.substring(0, i).equals(imageFile.substring(0, i))) {
								break;
							}
						}
						currentImageIndex++;
					}
				}
				imagePanel.load(imageFiles.get(currentImageIndex));
				lblFileName.setText(imageFiles.get(currentImageIndex));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
