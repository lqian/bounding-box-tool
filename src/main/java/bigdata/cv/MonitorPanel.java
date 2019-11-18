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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * @author link
 *
 */
public class MonitorPanel extends JPanel {
	
	private static final long serialVersionUID = 2914151804347061483L;
	
	private BufferedImage image;
	
	LabeledBoundingBox bb;
	
	public void setImage(BufferedImage image) {
		this.image = image;
		repaint();
	}
	
	public void setLabeledBoundingBox(LabeledBoundingBox bb) {
		this.bb = bb;
	}
	
	public void clearImage() {
		this.image = null;
		this.bb = null;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		if (image != null) {
			int iw = image.getWidth();
			int ih = image.getHeight();
			int pw = this.getParent().getWidth();  // the width fill to parent component width
			double s = pw * 1. / iw;
			if (s < 1) {
				int sh = (int) Math.floor(ih * s);
				setSize(pw, sh);
				g.drawImage(image, 0, 0,  pw, sh, null);
			}
			else {
				setSize(iw, ih);
				g.drawImage(image, 0, 0,  null);
			}
			if (bb != null && bb.extrasSize() > 0) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.setColor(Color.MAGENTA);
				g2d.setStroke(new BasicStroke(3f));
				drawLandmarks(g2d, bb, s>1?1:s);
			}
		}
		else {
			g.setColor(this.getParent().getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}
	
	void drawLandmarks(Graphics2D g2d, LabeledBoundingBox bb, double scaleFactor) {
		String tokens[] = bb.landmark.split(",");
		for (int i=0; i<4; i++) {
			int p = i*2; 
			int x = (int)((Float.valueOf(tokens[p]) - bb.x)*scaleFactor);
			int y =  (int)((Float.valueOf(tokens[p+1]) - bb.y)*scaleFactor);
			int radius = (int) (3/ scaleFactor);
			g2d.drawOval(x, y, radius, radius);
			g2d.drawString("" + i, x + radius,  y - radius);
		}
	}
	
}
