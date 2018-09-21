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

import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * @author qian xiafei
 *
 */
public class IconUtil {
	
	public static  ImageIcon icon(String name, String description) {
		java.net.URL imgURL = IconUtil.class.getClass().getResource("/icons/" + name);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + name);
			return null;
		}
	}
	
	public static JButton iconButton(String name, String desc) {
		JButton button = new JButton(icon(name, desc));
		button.setToolTipText(desc);
		return button;
	}

}
