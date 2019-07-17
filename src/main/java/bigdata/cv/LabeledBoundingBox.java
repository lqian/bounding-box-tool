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

import static java.lang.Math.abs;
import static java.lang.Math.floor;

import java.util.ArrayList;

/**
 * 
 * @author qian xiafei
 *
 */
public class LabeledBoundingBox {

	public int x, y;
	public int w, h;
	
	public String  extras = "" ;

	public String labelName; // label name of bounding box,

	public static LabeledBoundingBox wrap(String labelName, int x1, int y1, int x2, int y2, double scaleFactor,
			int showX, int showY) {
		LabeledBoundingBox bb = new LabeledBoundingBox();
		bb.labelName = labelName;
		bb.x = (int) floor(((x1 < x2 ? x1 : x2) - showX + 1) * scaleFactor);
		bb.y = (int) floor(((y1 < y2 ? y1 : y2) - showY + 1) * scaleFactor);
		bb.w = (int) floor(abs(x1 - x2) * scaleFactor) + 1;
		bb.h = (int) floor(abs(y1 - y2) * scaleFactor) + 1;
		return bb;
	}
	
	public static LabeledBoundingBox wrap(String labelName, int x1, int y1, int x2, int y2, double scaleFactor,
			int showX, int showY, String extras) {
		LabeledBoundingBox bb = new LabeledBoundingBox();
		bb.labelName = labelName;
		bb.x = (int) floor(((x1 < x2 ? x1 : x2) - showX + 1) * scaleFactor);
		bb.y = (int) floor(((y1 < y2 ? y1 : y2) - showY + 1) * scaleFactor);
		bb.w = (int) floor(abs(x1 - x2) * scaleFactor) + 1;
		bb.h = (int) floor(abs(y1 - y2) * scaleFactor) + 1;
		bb.extras = extras;
		return bb;
	}
	

	@Override
	public String toString() {
		if (extras == null || extras.equals("")) {
			return String.format("%s,%d,%d,%d,%d", labelName, x, y, w, h);
		}
		else {
			return String.format("%s,%d,%d,%d,%d,%s", labelName, x, y, w, h, extras);
		}
	}

	public String boundingBoxString() {
		return String.format("%d,%d,%d,%d", x, y, w, h);
	} 

	public static LabeledBoundingBox from(String line) {
		String[] tokens = line.trim().split(",", 6);
		LabeledBoundingBox bb = new LabeledBoundingBox();
		if (tokens.length >= 5) {
			bb.labelName = tokens[0];
			bb.x = Integer.valueOf(tokens[1]);
			bb.y = Integer.valueOf(tokens[2]);
			bb.w = Integer.valueOf(tokens[3]);
			bb.h = Integer.valueOf(tokens[4]);
		}
		if (tokens.length == 6){
			bb.extras = tokens[5].trim();
		}
		return tokens.length >=5 ? bb : null;
	}
	
	public static LabeledBoundingBox fromPos(String line) {
		String[] tokens = line.split(",", 4);
		if (tokens.length == 4) {
			LabeledBoundingBox bb = new LabeledBoundingBox();
			bb.x = Integer.valueOf(tokens[0]);
			bb.y = Integer.valueOf(tokens[1]);
			bb.w = Integer.valueOf(tokens[2]);
			bb.h = Integer.valueOf(tokens[3]);
			return bb;
		} else {
			return null;
		}
	}
	
	public int extrasSize() {
		return extras == null ? 0: extras.split(",").length;
	}

	public boolean isWithin(LabeledBoundingBox other) {
		return x >= other.x && y >= other.y 
				&& x + w <= other.x + other.w 
				&& y + h <= other.y + other.h;
	}

	public boolean isValid() {
		return x >= 0 && y >= 0 && w > 0 && h > 0;
	}

	public void addExtras(int x, int y, double scaleFactor, int showX, int showY) {
		float lx = (float) ((x - showX) * scaleFactor - 1 );
		float ly = (float) ((y - showY) * scaleFactor - 1 );
		if (extrasSize() <2) {
			extras +=  String.format("%.4f,%.4f", lx, ly);
		}
		else {
			extras +=  String.format(",%.4f,%.4f", lx, ly);
		}
		
	}
}
