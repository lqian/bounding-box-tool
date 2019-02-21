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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
@SuppressWarnings({"serial", "unchecked"})
public class CorpDialog extends JDialog implements ItemListener {

	DataSet source;

	DataSet target;

	JTextField sourceTextField;

	JTextField targetTextField;
	
	JTextField threadNumTextField = new JTextField("8");

	// JButton btnOpen;

	JButton corpBtn;

	JLabel lblStatus;

	ButtonGroup group = new ButtonGroup();

	String suffix;

	private JButton openSource;

	private JButton openTarget;

	LabelConfig labelConfig;

	JComboBox<String> clazzList;

	String selectedOutterClazz;

	JCheckBox innerClazz;
	
	AtomicInteger counter = new AtomicInteger(0);
	
	CountDownLatch latch;
	
	LinkedBlockingQueue<String> queue ;

	public CorpDialog(Frame owner, boolean modal, DataSet dataSet, LabelConfig labelConfig) {
		super(owner, "corp dataset", modal);
		this.source = dataSet;
		this.labelConfig = labelConfig;
		selectedOutterClazz = labelConfig.clazzNames[0];
		
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
						target = new DataSet(selectedFile.toPath(), false);
						targetTextField.setText(selectedFile.getAbsolutePath());
					}
					int sl = sourceTextField.getText().length();
					int tl = targetTextField.getText().length();
					corpBtn.setEnabled(sl > 0 && tl > 0);
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
		corpBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					corpInnerBoxes();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		clazzList.addActionListener(new ActionListener() {

			
			public void actionPerformed(ActionEvent e) {
				JComboBox<String> cb = (JComboBox<String>) e.getSource();
				selectedOutterClazz = (String) cb.getSelectedItem();
			}
		});
	}
	
    void corpSample(String labelFile) {
    	try {
			List<LabeledBoundingBox> boxes = source.readBoundingBoxes(labelFile);
			if (boxes.size() > 0) {
				List<ObjectBoundingBox> innerBoxes = parseObjectBoxes(boxes);
				if (innerBoxes.size() > 0) {
					String name = labelFile.replace(".label", "");
					String jpg = labelFile.replace(".label", ".jpg");
					BufferedImage image = source.readImage(jpg);
					if (image != null) {
						int i = 0;
						for (ObjectBoundingBox ib : innerBoxes) {
							String tin = String.format("%s_%d.jpg", name, i);
							int x = ib.outter.x;
							int y = ib.outter.y;
							int w = ib.outter.w;
							int h = ib.outter.h;
							BufferedImage sub = image.getSubimage(x, y, w, h);
							target.saveImage(tin, sub);

							if (innerClazz.isSelected()) {
								String tln = String.format("%s_%d.label", name, i++);
								target.saveRawLabel(tln, w, h, ib.inners);
							}
							counter.getAndIncrement();
							
						}
					}
				}
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
    }
    
    class CorpRunnable implements Runnable  {
    	@Override
		public void run() {
			String lf;
			try {
				while ((lf = queue.poll(1, TimeUnit.SECONDS)) != null ) {
					corpSample(lf);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			finally {
				latch.countDown();
			}
		}
    }

	void corpInnerBoxes() throws IOException {
		counter.set(0);
		lblStatus.setText("" + counter.get());
		queue = new LinkedBlockingQueue<>();
		queue.addAll(source.rawLabelFiles);
		int threadNum = Integer.parseInt(threadNumTextField.getText());
		latch = new CountDownLatch(threadNum);
		
		ExecutorService service = Executors.newFixedThreadPool(threadNum);
		for (int i=0; i< threadNum; i++) {
			service.submit(new CorpRunnable());
		}
		
		new Thread() {

			@Override
			public void run() {
				try {
					while (!queue.isEmpty()) {
						Thread.sleep(5000);
						lblStatus.setText("corp samples" + counter.get());						
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}.start();
		corpBtn.setEnabled(false);
		try {
			latch.await();
			service.shutdown();
			lblStatus.setText("corp samples" + counter.get());
			corpBtn.setEnabled(true);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	List<ObjectBoundingBox> parseObjectBoxes(List<LabeledBoundingBox> boxes) {
		List<ObjectBoundingBox> objectBoxes = new ArrayList<>();
		for (int i = 0; i < boxes.size(); i++) {
			LabeledBoundingBox outter = boxes.get(i);
			if (outter.labelName.equals(selectedOutterClazz)) {
				ObjectBoundingBox ib = new ObjectBoundingBox();
				ib.outter = outter;
				ib.inners = new ArrayList<>();
				if (innerClazz.isSelected()) {
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
				}
				objectBoxes.add(ib);
			}
		}
		return objectBoxes;
	}

	class ObjectBoundingBox {
		LabeledBoundingBox outter;
		List<LabeledBoundingBox> inners;
	}

	void initialize() {
		getContentPane().setLayout(new BorderLayout(10, 10));

		JPanel center = new JPanel(new SpringLayout());
		getContentPane().add(center, BorderLayout.CENTER);

		JLabel l1 = new JLabel("Source DataSet");
		center.add(l1);
		sourceTextField = new JTextField(50);
		sourceTextField.setEditable(false);
		l1.setLabelFor(sourceTextField);
		center.add(sourceTextField);
		openSource = new JButton("...");
		center.add(openSource);

		JLabel l2 = new JLabel("Target DataSet");
		center.add(l2);
		targetTextField = new JTextField(50);
		targetTextField.setEditable(false);
		l2.setLabelFor(targetTextField);
		center.add(targetTextField);
		openTarget = new JButton("...");
		center.add(openTarget);

		JLabel l3 = new JLabel("Outter clazz");
		center.add(l3);
		clazzList = new JComboBox<String>(labelConfig.clazzNames);
		clazzList.setEditable(false);
		l3.setLabelFor(clazzList);
		center.add(clazzList);
		innerClazz = new JCheckBox("inner boxes");
//		innerClazz.setToolTipText("checked me include inner boxes");
		innerClazz.setSelected(true);
		center.add(innerClazz);
		
		
		JLabel l4 = new JLabel("Corp Thread Number");
		center.add(l4);
		l4.setLabelFor(threadNumTextField);
		center.add(threadNumTextField);
		corpBtn = new JButton("Corp");
		corpBtn.setEnabled(false);
		center.add(corpBtn);
		
		SpringUtilities.makeCompactGrid(center, 4, 3, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad

		if (source != null) {
			sourceTextField.setText(source.home.toString());
		}

		JPanel south = new JPanel();
		getContentPane().add(south, BorderLayout.SOUTH);
		lblStatus = new JLabel("");
		south.add(lblStatus);


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
