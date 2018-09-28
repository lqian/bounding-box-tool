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

import static bigdata.cv.IconUtil.iconButton;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;

public class MainWindow implements WindowListener {

	
	Tool current = null;
	
	JFrame frame;
	
	CardLayout cardLayout = new CardLayout();
	JPanel cards = new JPanel(cardLayout);
	ClassificationPanel classificationPanel = new ClassificationPanel();
	BrandPanel brandPanel = new BrandPanel();
	LabelPanel labelPanel = new LabelPanel();
	ImageSearchPanel imageSearchPanel = new ImageSearchPanel();

	private JButton btnAbout;

	private JButton labelButton;

	private JButton classificationButton;

	private JButton brandButton;
	
	private JButton imageSearchButton;
	
	JToolBar toolBar = new JToolBar();	

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
		
		initialize();
		
		labelButton.doClick();
	} 
	
	void aboutButton() {
		toolBar.addSeparator();
		btnAbout = iconButton("about.gif", "");
		toolBar.add(btnAbout);
	}
	
	void updateButtonUI() {
		int c = toolBar.getComponentCount();
		for (int i=0; i<c; i++) {
			JComponent comp = (JComponent)toolBar.getComponent(i);
			comp.updateUI();			
		}
		toolBar.updateUI();
	}
	
	void initToolBar() {
		toolBar.removeAll();
		labelButton = iconButton("label.png", "swith label bounding tool");
		toolBar.add(labelButton);
		classificationButton = iconButton("classification.png", "open classification dataset");
		toolBar.add(classificationButton);
		brandButton = iconButton("vehicle.png", "correct vehicle brand");
		toolBar.add(brandButton);
		
		imageSearchButton = iconButton("image_search.png", "fast content base image search demo");
		toolBar.add(imageSearchButton);
		
		classificationButton.addActionListener( new ActionListener () {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					initToolBar();
					 //
					try {
						if (current != null) {
							current.saveCurrentWork();
						}
						classificationPanel.init(chooser.getSelectedFile().getAbsolutePath());
						cardLayout.show(cards, "classificationPanel");
						current = (Tool)classificationPanel;
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					aboutButton();
					current.addButtons();
					aboutButton();
					updateButtonUI();
				}
			}
		});
		
		brandButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {			
				BrandSelector brandSelector = BrandSelector.getInstance();
				int r = JOptionPane.showConfirmDialog(frame, brandSelector, 
						"select full brand code:" ,
						JOptionPane.OK_CANCEL_OPTION, 
						JOptionPane.PLAIN_MESSAGE);
				if (r == JOptionPane.OK_OPTION) {
					try {
						brandPanel.fullBrandCode = brandSelector.getFullBrandCode();
						brandPanel.initData();
						switchPanel("brandPanel");
					} catch (Exception e1) {
						e1.printStackTrace();
					}  
				}
			}

		});
		
		labelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switchPanel("labelPanel");
			}
		});
		
		imageSearchButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				switchPanel("imageSearchPanel");
			}
		});
	}
	
	void switchPanel(String name) {
		if (current != null) {
			current.saveCurrentWork();
		}
		cardLayout.show(cards, name);
		current = (Tool) labelPanel;
		
		initToolBar();
		current.addButtons();
		aboutButton();
		updateButtonUI();
	}

	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(10, 10));
		labelPanel.frame = frame;
		labelPanel.toolBar = toolBar;
		brandPanel.toolBar = toolBar;
		
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);
		frame.getContentPane().add(cards, BorderLayout.CENTER);
		
		cards.add(labelPanel, "labelPanel");
//		cards.add(annotationPanel, "annotationPanel");
		cards.add(classificationPanel, "classificationPanel");
		cards.add(brandPanel, "brandPanel");
		cards.add(imageSearchPanel, "imageSearchPanel");
		
		initToolBar();
		aboutButton();
		toolBar.updateUI();
		frame.pack();
	}
	

	@Override
	public void windowOpened(WindowEvent e) {
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		current.saveCurrentWork();
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
