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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PriorityQueue;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import sun.net.www.content.audio.x_aiff;

/**
 * @author qian xiafei
 *
 */
public class ConvertDialog extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7812674426324601391L;
	
	
	Path datasetPath;
	
	JTextField tfDatasetPath;
	
	JButton btnOpen;
	
	JButton btnConvert;
	
	JLabel lblStatus;

	public ConvertDialog(Frame  owner, boolean modal, Path datasetPath) {
		super(owner, "convert label", modal);
		this.datasetPath = datasetPath;
		
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
				}
			}
		});
		
		btnConvert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				PriorityQueue<String> labelFiles = new PriorityQueue<>();
				DataSetVisitor visitor = new DataSetVisitor(labelFiles);
				try {
					int counter = 0;
					Files.walkFileTree(datasetPath, visitor);
					BufferedWriter writer = Files.newBufferedWriter(datasetPath.resolve("relative-position-label.txt"));
					String file ;
					while (!labelFiles.isEmpty()) {
						file = labelFiles.poll();
						Path path = datasetPath.resolve(file);
						
						BufferedReader reader = Files.newBufferedReader(path);
						String line = reader.readLine();
						
						//read image width & height from first line
						int width = 0, height = 0;
						if (line != null) {
							String tokens[] = line.split(",", 2);
							if (tokens.length == 2) {
								width = Integer.valueOf(tokens[0]);
								height = Integer.valueOf(tokens[1]);
							}
							else {
								throw new RuntimeException("invalid withd and height["+ line + "] in label:" +  file);
							}
							
							while ((line = reader.readLine()) != null) {
								LabeledBoundingBox bb = LabeledBoundingBox.from(line);
								if (bb != null) {
									double x = bb.x * 1.0 / width;
									double y = bb.y * 1.0 / height;
									double w = bb.w * 1.0 / width;
									double h = bb.h * 1.0 / height;
									
									writer.write(String.format("%s,%s,%f,%f,%f,%f", 
											file.replaceFirst("\\.label", ".jpg"),
											bb.labelName, x, y, w, h));
									writer.newLine();
									
									if (++counter % 1000 == 0) {
										lblStatus.setText(counter+"");
									}
								}
							}
						}
						reader.close();
					}
					writer.flush();
					lblStatus.setText("convert labeles: " + counter);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
	
	void initialize() {
		JPanel north = new JPanel();
		getContentPane().add(north, BorderLayout.NORTH);
		GridBagLayout gbl_north = new GridBagLayout();
//		gbl_north.columnWidths = new int[]{432, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//		gbl_north.rowHeights = new int[]{22, 0, 0};
//		gbl_north.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
//		gbl_north.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		north.setLayout(gbl_north);
		
		tfDatasetPath = new JTextField();
		if (datasetPath != null) { 
			tfDatasetPath.setText(datasetPath.toString());
		}
		GridBagConstraints gbc_tfDatasetPath = new GridBagConstraints();
		gbc_tfDatasetPath.fill = GridBagConstraints.BOTH;
		gbc_tfDatasetPath.gridwidth = 10;
		
//		gbc_textField.insets = new Insets(0, 0, 5, 0);
		
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
		
		JPanel sourth = new JPanel();
		getContentPane().add(sourth, BorderLayout.SOUTH);
		
		lblStatus = new JLabel("0");
		lblStatus.setEnabled(false);
		sourth.add(lblStatus);
		pack();
		setResizable(false);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); //得到屏幕的尺寸 
	    int screenWidth = screenSize.width;
	    int screenHeight = screenSize.height;
	    setLocation((screenWidth - this.getWidth()) / 2, (screenHeight - this.getHeight())/4);
	       
	}
}
