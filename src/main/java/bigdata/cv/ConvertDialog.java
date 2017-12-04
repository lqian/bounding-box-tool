/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * @author qian xiafei
 *
 */
public class ConvertDialog extends JDialog implements ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7812674426324601391L;

	Path datasetPath;

	Path labelsPath;

	Path jpegImages;

	JTextField tfDatasetPath;

	JButton btnOpen;

	JButton btnConvert;

	JLabel lblStatus;
	
	JCheckBox onlyGenTrainAndValSet;
	
	JCheckBox samePath;
	
	ButtonGroup group = new ButtonGroup();
	
	String suffix;

	public ConvertDialog(Frame owner, boolean modal, Path datasetPath) {
		super(owner, "convert label", modal);
		this.datasetPath = datasetPath;
		if (datasetPath != null) {
			this.labelsPath = datasetPath.resolve("labels");
			this.jpegImages = datasetPath.resolve("JPEGImages");
		}

		initialize();

		initializeActions();
	}

	void initializeActions() {
		
		btnOpen.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog(ConvertDialog.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					tfDatasetPath.setText(chooser.getSelectedFile().getAbsolutePath());
					btnConvert.setEnabled(true);
					datasetPath = chooser.getSelectedFile().toPath();
					labelsPath = datasetPath.resolve("labels");
					jpegImages = datasetPath.resolve("JPEGImages");
				}
			}
		});

		// raw label file to relative label file with txt extension name
		btnConvert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (onlyGenTrainAndValSet.isSelected()) {
					File[] images  = jpegImages.toFile().listFiles();
					List<String> list = new ArrayList<>();
					Arrays.asList(images).forEach( f -> list.add(f.getName()) );;
					try {
						DatasetCensus rest = genTrainAndValSet(list, false);
						lblStatus.setText(String.format(" generate train set count: %d, val set count: %d",
								rest.trainCount, rest.valCount));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					PriorityQueue<String> rawLabelFiles = new PriorityQueue<>();
					DataSetVisitor visitor = new DataSetVisitor();
					try {
						int counter = 0;
						Files.walkFileTree(jpegImages, visitor);
						rawLabelFiles.addAll(visitor.labelFiles);
						ArrayList<String> list = new ArrayList<>();
						list.addAll(rawLabelFiles);
						String file;
						while (!rawLabelFiles.isEmpty()) {
							file = rawLabelFiles.poll();
							counter += convertLabel(file);
							if (counter % 1000 == 0) {
								lblStatus.setText(counter + "");
							}
						}
						
						DatasetCensus rest = genTrainAndValSet(list, true);
						genClassNameFile();
						lblStatus.setText(String.format("convert labeles: %d, train set count: %d, val set count: %d",
								counter, rest.trainCount, rest.valCount));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
	}

	void genClassNameFile() throws IOException {
		Path path = datasetPath.resolve("itd.names");
		BufferedWriter writer = Files.newBufferedWriter(path);
		for (String name : ImagePanel.clazzNames_Eng) {
			writer.write(name);
			writer.newLine();
		}
		writer.close();
	}

	/**
	 * 1:2 ratio for train set and validate set
	 * 
	 * @param rawLabelFiles
	 * @throws IOException
	 */
	DatasetCensus genTrainAndValSet(List<String> labelList, boolean fromLabel) throws IOException {
		DatasetCensus dc = new DatasetCensus();
		int tc = 0, vc = 0;
		Collections.shuffle(labelList);
		Path trainSet = datasetPath.resolve("train.txt");
		Path valSet = datasetPath.resolve("val.txt");
		BufferedWriter trainWriter = Files.newBufferedWriter(trainSet);
		BufferedWriter valWriter = Files.newBufferedWriter(valSet);
		
		for (int i = 0; i < labelList.size(); i++) {
			String labelFile = labelList.get(i);
//			Path labelPath = samePath.isSelected() ?  jpegImages.resolve(labelFile.replace(".jpg", "."+ suffix )): 
//				    labelsPath.resolve(labelFile.replace(".jpg", "."+ suffix ));
//			
//			BufferedReader reader = Files.newBufferedReader(labelPath);
//			String line =null;
//			boolean zero = false;
//			while (!zero && (line = reader.readLine()) != null) {
//				String tokens[] = line.split("\\s");
//				double w = Double.valueOf(tokens[2]);
//				double h = Double.valueOf(tokens[3]);
//				zero =  (w==0 || h ==0);
//			}
//			reader.close();
//			
//			if (zero) {
//				continue;
//			}
			
		   if (convertLabel(labelFile) == 0) continue;
			
			Path imagePath = jpegImages.resolve(labelFile.replace("." + suffix, ".jpg"));
			if (Files.exists(imagePath)) {
//			if (i % 3 == 0) {
				trainWriter.write(imagePath.toString());
				trainWriter.newLine();
				tc++;
//			}
//			else {
			if  (i % 3 == 0) {
				valWriter.write(imagePath.toString());
				valWriter.newLine();
				vc++;
			}
			}
			else {
				System.out.println("not exists image: " + imagePath);
			}
		}

		trainWriter.close();
		valWriter.close();
		dc.trainCount = tc;
		dc.valCount  = vc;
		return dc;
	}

	int getClassIdByName(String className) {
		int i = 0;
		for (; i < ImagePanel.clazzNames.length; i++) {
			if (className.equalsIgnoreCase(ImagePanel.clazzNames[i]))
				break;
		}
		return i;
	}

	/**
	 * boundbox(x, y, w, h) -> darknet_boundbox(dx, dy, dw, dh)
	 * dx =  ((2x + w) /  2 -1) / image width
	 * dy = (( 2y + h ) / 2 -1) / image height
	 * dw = w / image width
	 * dy = h / image height
	 * @param sn
	 * @return
	 * @throws IOException
	 */
	int convertLabel(String sn) throws IOException {
		int c = 0;
		String tn = sn.replace(".label", ".txt");
		Path txt = labelsPath.resolve(tn);

		BufferedWriter writer = Files.newBufferedWriter(txt);
		BufferedReader reader = Files.newBufferedReader(jpegImages.resolve(sn));
		String line = reader.readLine();
		int width = 0, height = 0;
		if (line != null) {
			String tokens[] = line.split(",", 2);
			if (tokens.length == 2) {
				width = Integer.valueOf(tokens[0]);
				height = Integer.valueOf(tokens[1]);
			} else {
				throw new RuntimeException("invalid width and height [" + line + "] in label:" + sn);
			}

			while ((line = reader.readLine()) != null) {
				LabeledBoundingBox bb = LabeledBoundingBox.from(line);
				if (bb != null) {
					double dw = 1.0 / width;
					double dh = 1.0 / height;
					double x = ((bb.x * 2 + bb.w) / 2  - 1 ) *  dw;
					double y = ((bb.y * 2 + bb.h) / 2 - 1) *dh;
					double w = bb.w * dw;
					double h = bb.h * dh;

					writer.write(String.format("%d %f %f %f %f", getClassIdByName(bb.labelName), x, y, w, h));
					writer.newLine();
					c++;
				}
			}
			reader.close();
		}
		writer.close();
		return c;
	}

	void initialize() {
		JPanel north = new JPanel();
		getContentPane().add(north, BorderLayout.NORTH);
		GridBagLayout gbl_north = new GridBagLayout();
		// gbl_north.columnWidths = new int[]{432, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		// gbl_north.rowHeights = new int[]{22, 0, 0};
		// gbl_north.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
		// 0.0, 0.0, 0.0, Double.MIN_VALUE};
		// gbl_north.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		north.setLayout(gbl_north);

		tfDatasetPath = new JTextField();
		if (datasetPath != null) {
			tfDatasetPath.setText(datasetPath.toString());
		}
		GridBagConstraints gbc_tfDatasetPath = new GridBagConstraints();
		gbc_tfDatasetPath.fill = GridBagConstraints.BOTH;
		gbc_tfDatasetPath.gridwidth = 10;

		// gbc_textField.insets = new Insets(0, 0, 5, 0);

		btnOpen = new JButton("Open");
		btnOpen.setToolTipText("Open Dataset");
		GridBagConstraints gbc_btnOpen = new GridBagConstraints();
		gbc_btnOpen.anchor = GridBagConstraints.WEST;
		gbc_btnOpen.gridx = 0;
		gbc_btnOpen.gridy = 0;
		north.add(btnOpen, gbc_btnOpen);

		gbc_tfDatasetPath.anchor = GridBagConstraints.CENTER;
		gbc_tfDatasetPath.gridx = 1;
		gbc_tfDatasetPath.gridy = 0;
		gbc_tfDatasetPath.gridwidth = 8;
		north.add(tfDatasetPath, gbc_tfDatasetPath);
		tfDatasetPath.setColumns(100);
		tfDatasetPath.setEditable(false);

		btnConvert = new JButton("Convert");
		btnConvert.setEnabled(false);
		GridBagConstraints gbc_btnConvert = new GridBagConstraints();
		gbc_btnConvert.anchor = GridBagConstraints.EAST;
		gbc_btnConvert.gridwidth = 1;
		gbc_btnConvert.gridx = 9;
		gbc_btnConvert.gridy = 0;
		north.add(btnConvert, gbc_btnConvert);

		JRadioButton radion1 =new JRadioButton("lbl"); 
		JRadioButton radion3 =new JRadioButton("label"); 
		JRadioButton radion2 =new JRadioButton("txt"); 
		radion1.addItemListener(this);
		radion2.addItemListener(this);
		radion3.addItemListener(this);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 0;
		gbc.gridy = 1;
		north.add(radion1, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 1;
		north.add(radion2, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 1;
		north.add(radion3, gbc);
		
		group.add(radion1);
		group.add(radion2);
		group.add(radion3);
		
		samePath = new JCheckBox("label file and image in the same directory");
		gbc.gridx = 3;
		gbc.gridy = 1;
		north.add(samePath, gbc);
		
		onlyGenTrainAndValSet = new JCheckBox("only generate Train and Validate Set");
		GridBagConstraints gbc_checkBox = new GridBagConstraints();
		gbc_checkBox.anchor = GridBagConstraints.EAST;
		gbc_checkBox.gridwidth = 4;
		gbc_checkBox.gridx = 4;
		gbc_checkBox.gridy = 1;
		north.add(onlyGenTrainAndValSet, gbc_checkBox);
		
		JPanel sourth = new JPanel();
		getContentPane().add(sourth, BorderLayout.SOUTH);

		lblStatus = new JLabel("0");
		lblStatus.setEnabled(false);
		sourth.add(lblStatus);
		pack();
		setResizable(false);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // 得到屏幕的尺寸
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		setLocation((screenWidth - this.getWidth()) / 2, (screenHeight - this.getHeight()) / 4);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		JRadioButton s = (JRadioButton)e.getSource();
		suffix = s.getText();
	}
}
