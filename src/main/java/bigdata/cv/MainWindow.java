package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PriorityQueue;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

public class MainWindow {

	private JFrame frame;
	private JTextField txtDirectory;

	int totalFile;
	int totalBoudingBoxLabels;

	String[] imageFiles;

	PriorityQueue<String> labelFiles = new PriorityQueue<String>();

	LoadImageThread loadImageThread;

	JLabel valTotalImage;

	JLabel valTotalLabel;

	int currentImageIndex = -1;

	ImagePanel imagePanel = new ImagePanel();
	JPanel centerPanel;
	private JButton btnFirst;
	private JButton btnPreviouse;
	private JButton btnNext;
	private JButton btnLast;

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
	@SuppressWarnings("serial")
	public MainWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			// UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
		}

		initialize();

		centerPanel.add(imagePanel, BorderLayout.CENTER);

		btnFirst.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentImageIndex = 0;
				imagePanel.setImageFile(imageFiles[currentImageIndex]);
			}
		});
		
		btnNext.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentImageIndex < imageFiles.length - 1) {
					imagePanel.setImageFile(imageFiles[++currentImageIndex]);
				}
			}
		});
		
		this.btnPreviouse.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentImageIndex > 0) {
					imagePanel.setImageFile(imageFiles[--currentImageIndex]);
				}
			}
		});
		
		this.btnLast.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentImageIndex = imageFiles.length - 1;
				imagePanel.setImageFile(imageFiles[currentImageIndex]);
				
			}
		});
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(10, 10));

		JPanel northPanel = new JPanel();
		frame.getContentPane().add(northPanel, BorderLayout.NORTH);
		northPanel.setLayout(new BorderLayout(10, 5));

		JLabel lblNewLabel = new JLabel("Directory:");
		northPanel.add(lblNewLabel, BorderLayout.WEST);

		txtDirectory = new JTextField();
		txtDirectory.setEditable(false);
		northPanel.add(txtDirectory, BorderLayout.CENTER);
		txtDirectory.setColumns(100);

		JButton btnOpenFile = new JButton("Open");
		btnOpenFile.setAction(new OpenFileAction(txtDirectory));
		btnOpenFile.setText("Open DataSet");
		btnOpenFile.setToolTipText("click the button open image dataset directory");
		northPanel.add(btnOpenFile, BorderLayout.EAST);

		centerPanel = new JPanel();
		frame.getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));

		JPanel toolBoxPanel = new JPanel();
		frame.getContentPane().add(toolBoxPanel, BorderLayout.EAST);
		GridBagLayout gbl_toolBoxPanel = new GridBagLayout();
		gbl_toolBoxPanel.columnWidths = new int[] { 44, 30, 60, 60, 0 };
		gbl_toolBoxPanel.rowHeights = new int[] { 87, 0 };
		gbl_toolBoxPanel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
		gbl_toolBoxPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		toolBoxPanel.setLayout(gbl_toolBoxPanel);

		JList list = new JList();
		list.setLayoutOrientation(JList.VERTICAL_WRAP);
		list.setModel(new AbstractListModel() {
			String[] values = new String[] { "1231", "456", "789", "1111", "12123" };

			public int getSize() {
				return values.length;
			}

			public Object getElementAt(int index) {
				return values[index];
			}
		});
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setBorder(new LineBorder(new Color(0, 0, 0)));
		GridBagConstraints gbc_list = new GridBagConstraints();
		gbc_list.fill = GridBagConstraints.HORIZONTAL;
		gbc_list.gridwidth = 2;
		gbc_list.anchor = GridBagConstraints.NORTHWEST;
		gbc_list.insets = new Insets(0, 0, 0, 5);
		gbc_list.gridx = 0;
		gbc_list.gridy = 0;
		gbc_list.gridheight = 8;

		toolBoxPanel.add(list, gbc_list);

		JButton btnRemoveBoundingBox = new JButton("Remove");
		GridBagConstraints gbc_btnRemoveBoundingBox = new GridBagConstraints();
		gbc_btnRemoveBoundingBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnRemoveBoundingBox.insets = new Insets(0, 0, 0, 5);
		gbc_btnRemoveBoundingBox.gridx = 0;
		gbc_btnRemoveBoundingBox.gridy = 4;
		toolBoxPanel.add(btnRemoveBoundingBox, gbc_btnRemoveBoundingBox);

		JButton btnCleanBoundingBox = new JButton("Clear All");
		GridBagConstraints gbc_btnCleanBoundingBox = new GridBagConstraints();
		gbc_btnCleanBoundingBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnCleanBoundingBox.anchor = GridBagConstraints.WEST;
		gbc_btnCleanBoundingBox.gridx = 0;
		gbc_btnCleanBoundingBox.gridy = 5;
		toolBoxPanel.add(btnCleanBoundingBox, gbc_btnCleanBoundingBox);

		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.SOUTH);

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

		frame.pack();
	}

	private class OpenFileAction extends AbstractAction {

		private static final long serialVersionUID = 7402675318287934991L;

		JTextField txtDirectory;

		public OpenFileAction(JTextField txtDirectory) {
			this.txtDirectory = txtDirectory;
			putValue(NAME, "OpenFileAction");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				txtDirectory.setText(chooser.getSelectedFile().getAbsolutePath());

				loadImageThread = new LoadImageThread(chooser.getSelectedFile().toPath());
				loadImageThread.start();
			}
		}
	}

	class UpdateStatusThread extends Thread {
		LoadImageThread loadImageThread;

		public UpdateStatusThread(LoadImageThread loadImageThread) {
			super();
			this.loadImageThread = loadImageThread;
		}

		public void run() {
			try {
				loadImageThread.join();
				// totalFile = imageFiles.size();
				// totalBoudingBoxLabels = labelFiles.size();
			} catch (InterruptedException e) {
				e.printStackTrace();
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
				imageFiles = visitor.imageFiles.toArray(new String[totalFile ]);
				valTotalImage.setText(String.valueOf(totalFile));
				valTotalLabel.setText(String.valueOf(totalBoudingBoxLabels));
				if (labelFiles.size() == 0) {
					currentImageIndex = 0;
					imagePanel.setImageFile(imageFiles[0]);
				} else {
					// TODO skip the first un-label image

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
