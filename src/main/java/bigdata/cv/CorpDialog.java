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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

/**
 * @author lqian
 *
 */
@SuppressWarnings("serial")
public class CorpDialog extends JDialog implements ItemListener {

	DataSet source;

	DataSet target;

	JTextField sourceTextField;

	JTextField targetTextField;

	// JButton btnOpen;

	JButton btnConvert;

	JLabel lblStatus;

	ButtonGroup group = new ButtonGroup();

	String suffix;

	private JButton openSource;

	private JButton openTarget;

	public CorpDialog(Frame owner, boolean modal, DataSet dataSet) {
		super(owner, "convert label", modal);
		this.source = dataSet;
		initialize();

		initializeActions();
	}

	class OpenActionListener implements ActionListener {

		boolean isSource;

		public OpenActionListener(boolean isSource) {
			super();
			this.isSource = isSource;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(CorpDialog.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File selectedFile = chooser.getSelectedFile();
				try {
					if (isSource) {
						source = new DataSet(selectedFile.toPath());
						sourceTextField.setText(selectedFile.getAbsolutePath());
					} else {
						target = new DataSet(selectedFile.toPath());
						targetTextField.setText(selectedFile.getAbsolutePath());
					}
					int sl = sourceTextField.getText().length();
					int tl = targetTextField.getText().length();
					btnConvert.setEnabled(sl > 0 && tl > 0);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	void initializeActions() {
		openSource.addActionListener(new OpenActionListener(true));
		openTarget.addActionListener(new OpenActionListener(false));

		// raw label file to relative label file with txt extension name
		btnConvert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					corpInnerBoxes();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	void corpInnerBoxes() throws IOException {
		int counter = 0;
		for (String lf : source.rawLabelFiles) {
			List<LabeledBoundingBox> boxes = source.readBoundingBoxes(lf);
			if (boxes.size() > 0) {
				List<InnerBoxes> innerBoxes = parseInnerBoxes(boxes);
				if (innerBoxes.size() > 0) {
					String name = lf.replace(".label", "");
					String jpg = lf.replace(".label", ".jpg");
					BufferedImage image = source.readImage(jpg);
					if (image != null) {
						int i = 0;
						for (InnerBoxes ib : innerBoxes) {
							String tin = String.format("%s_%d.jpg", name, i);
							int x = ib.outter.x;
							int y = ib.outter.y;
							int w = ib.outter.w;
							int h = ib.outter.h;
							BufferedImage sub = image.getSubimage(x, y, w, h);
							target.saveImage(tin, sub);
							
							String tln = String.format("%s_%d.label", name, i);
							target.saveRawLabel(tln, w, h, ib.inners);
							
							if (counter++ % 10==0) {
								lblStatus.setText(""+ counter);
							}
						}
					}
				}
			}
		}
	}

	List<InnerBoxes> parseInnerBoxes(List<LabeledBoundingBox> boxes) {
		List<InnerBoxes> innerBoxes = new ArrayList<>();
		for (int i = 0; i < boxes.size(); i++) {
			LabeledBoundingBox outter = boxes.get(i);
			if (outter.labelName.equals(ImagePanel.clazzNames[0])) {
				InnerBoxes ib = new InnerBoxes();
				ib.outter = outter;
				ib.inners = new ArrayList<>();
				for (int j = 0; j < boxes.size(); j++) {
					if (i != j) {
						LabeledBoundingBox e = boxes.get(j);
						if (e.isWithin(ib.outter)) {
							e.x -= outter.x;
							e.y -= outter.y;
							ib.inners.add(e);
						}
					}
				}
				innerBoxes.add(ib);
			}
		}
		return innerBoxes;
	}

	class InnerBoxes {
		LabeledBoundingBox outter;
		List<LabeledBoundingBox> inners;
	}

	void initialize() {
		getContentPane().setLayout(new BorderLayout(10, 10));

		JPanel center = new JPanel(new SpringLayout());
		getContentPane().add(center, BorderLayout.CENTER);

		JLabel l1 = new JLabel("Source DataSet: ");
		center.add(l1);
		sourceTextField = new JTextField(50);
		sourceTextField.setEditable(false);
		l1.setLabelFor(sourceTextField);
		center.add(sourceTextField);
		openSource = new JButton("...");
		center.add(openSource);

		JLabel l2 = new JLabel("Target DataSet: ");
		center.add(l2);
		targetTextField = new JTextField(50);
		targetTextField.setEditable(false);
		l2.setLabelFor(targetTextField);
		center.add(targetTextField);
		openTarget = new JButton("...");
		center.add(openTarget);

		SpringUtilities.makeCompactGrid(center, 2, 3, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad

		if (source != null) {
			sourceTextField.setText(source.home.toString());
		}

		JPanel south = new JPanel();
		south.setLayout(new GridBagLayout());
		getContentPane().add(south, BorderLayout.SOUTH);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		lblStatus = new JLabel("0");
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 4;
		south.add(lblStatus, gbc);

		btnConvert = new JButton("Convert");
		btnConvert.setEnabled(false);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridwidth = 1;
		gbc.gridx = 4;
		south.add(btnConvert, gbc);

		pack();
		setResizable(false);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
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
