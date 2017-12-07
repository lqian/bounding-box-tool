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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

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
 * @author lqian
 *
 */
public class ConvertDialog extends JDialog implements ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7812674426324601391L;

	DataSet dataSet;

	JTextField tfDatasetPath;

	JButton btnOpen;

	JButton btnConvert;

	JLabel lblStatus;

	JCheckBox onlyGenTrainAndValSet;

	ButtonGroup group = new ButtonGroup();

	String suffix;

	public ConvertDialog(Frame owner, boolean modal, DataSet dataSet) {
		super(owner, "convert label", modal);
		this.dataSet = dataSet;

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
					try {
						dataSet = new DataSet(chooser.getSelectedFile().toPath());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		// raw label file to relative label file with txt extension name
		btnConvert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				if (onlyGenTrainAndValSet.isSelected()) {
					List<String> list = dataSet.imageFiles;
					try {
						DatasetCensus rest = genTrainAndValSet(list, false);
						lblStatus.setText(String.format(" generate train set count: %d, val set count: %d",
								rest.trainCount, rest.valCount));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					try {
						int counter = 0;
						List<String> list = dataSet.rawLabelFiles;
						for (String file : list) {
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
		Path path = dataSet.resolve("itd.names");
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
		Path trainSet = dataSet.resolve("train.txt");
		Path valSet = dataSet.resolve("val.txt");
		BufferedWriter trainWriter = Files.newBufferedWriter(trainSet);
		BufferedWriter valWriter = Files.newBufferedWriter(valSet);

		for (int i = 0; i < labelList.size(); i++) {
			String labelFile = labelList.get(i);

			if (convertLabel(labelFile) == 0)
				continue;

			Path imagePath = dataSet.getImage(labelFile.replace("." + suffix, ".jpg"));
			if (Files.exists(imagePath)) {
				// if (i % 3 == 0) {
				trainWriter.write(imagePath.toString());
				trainWriter.newLine();
				tc++;
				// }
				// else {
				if (i % 3 == 0) {
					valWriter.write(imagePath.toString());
					valWriter.newLine();
					vc++;
				}
			} else {
				System.out.println("not exists image: " + imagePath);
			}
		}

		trainWriter.close();
		valWriter.close();
		dc.trainCount = tc;
		dc.valCount = vc;
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
	 * boundbox(x, y, w, h) -> darknet_boundbox(dx, dy, dw, dh) dx = ((2x + w) /
	 * 2 -1) / image width dy = (( 2y + h ) / 2 -1) / image height dw = w /
	 * image width dy = h / image height
	 * 
	 * @param sn
	 * @return
	 * @throws IOException
	 */
	int convertLabel(String sn) throws IOException {
		int c = 0;
		String tn = sn.replace(".label", ".txt");
		Path txt = dataSet.getDarknetLabel(tn);

		BufferedWriter writer = Files.newBufferedWriter(txt);
		BufferedReader reader = Files.newBufferedReader(dataSet.getRawLabel(sn));
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
					double x = ((bb.x * 2 + bb.w) / 2 - 1) * dw;
					double y = ((bb.y * 2 + bb.h) / 2 - 1) * dh;
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
		north.setLayout(gbl_north);

		tfDatasetPath = new JTextField();
		if (dataSet != null) {
			tfDatasetPath.setText(dataSet.home.toString());
		}
		GridBagConstraints gbc_tfDatasetPath = new GridBagConstraints();
		gbc_tfDatasetPath.fill = GridBagConstraints.BOTH;
		gbc_tfDatasetPath.gridwidth = 10;

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

		JRadioButton radion1 = new JRadioButton("lbl");
		JRadioButton radion2 = new JRadioButton("txt");
		JRadioButton radion3 = new JRadioButton("label");
		radion1.addItemListener(this);
		radion2.addItemListener(this);
		radion3.addItemListener(this);
		radion3.setSelected(true);

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
		JRadioButton s = (JRadioButton) e.getSource();
		suffix = s.getText();
	}
}
