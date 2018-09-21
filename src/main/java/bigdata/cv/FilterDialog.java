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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;

/**
 * @author qian xiafei
 *
 */
@SuppressWarnings("serial")
public class FilterDialog extends JDialog {
	
	LabelPanel labelPanel;
	
	List<JCheckBox> clazzes = new ArrayList<>();
	
	public FilterDialog(LabelPanel labelPanel, boolean modal) {
//		super(mainWindow.getParent(), modal);
		this.labelPanel = labelPanel;
		
		initialize();
	}

	void initialize() {
		Container pane = getContentPane();
		getContentPane().setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.WEST; 
		
		gbc.gridwidth = 1;
		String[] clazzNames = labelPanel.labelConfig.clazzNames;
		for (int i = 0 ; i< clazzNames.length; ++i) {
			gbc.gridx = i % 8;
			gbc.gridy = i / 8;
			JCheckBox cb = new JCheckBox(clazzNames[i], false);
			cb.addActionListener(new FilterClazzListener());
			pane.add(cb, gbc);
			clazzes.add(cb);
		}
		
		gbc.fill = GridBagConstraints.PAGE_END;
		gbc.gridwidth = 8;
		gbc.gridx = 0;
		gbc.gridy = 2;
		
		JButton close = new JButton("Close");
		pane.add(close, gbc);
		
		close.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				labelPanel.clearFilter();
				for (JCheckBox cb: clazzes) {
					if (cb.isSelected())
						labelPanel.addClazzFilter(cb.getText());
				}
				
				FilterDialog.this.dispose();
			}
		});
		
		pack();
		setResizable(false);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		setLocation((screenWidth - this.getWidth()) / 2, (screenHeight - this.getHeight()) / 4);
	}

	class FilterClazzListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBox cb = (JCheckBox)e.getSource();
			if (cb.isSelected()) {
				labelPanel.addClazzFilter(cb.getText());
			}
			else {
				labelPanel.removeClazzFilter(cb.getText());
			}
		}
		
	}
}
