package bigdata.cv;

import static java.lang.Math.*;

/**
 * 
 * @author qian xiafei
 *
 */
public class LabeledBoundingBox {

	public int x, y;
	public int w, h;

	public String labelName; // label name of bounding box,

	public static LabeledBoundingBox wrap(String labelName, int x1, int y1, int x2, int y2, double scaleFactor,
			int showX, int showY) {
		LabeledBoundingBox bb = new LabeledBoundingBox();
		bb.labelName = labelName;
		bb.x = (int) floor(((x1 < x2 ? x1 : x2) - showX + 1) * scaleFactor);
		bb.y = (int) floor(((y1 < y2 ? y1 : y2)  - showY + 1) * scaleFactor);
		bb.w = (int) floor(abs(x1 - x2) * scaleFactor) + 1;
		bb.h = (int) floor(abs(y1 - y2) * scaleFactor) + 1;
		return bb;
	}

	@Override
	public String toString() {
		return String.format("%s,%d,%d,%d,%d", labelName, x, y, w, h);
	}
	
	public String boundingBoxString() {
		return String.format("%d,%d,%d,%d", x, y, w, h);
	}

	public static LabeledBoundingBox from(String line) {
		String[] tokens = line.split(",", 5);
		if (tokens.length == 5) {
			LabeledBoundingBox bb = new LabeledBoundingBox();
			bb.labelName = tokens[0];
			bb.x = Integer.valueOf(tokens[1]);
			bb.y = Integer.valueOf(tokens[2]);
			bb.w = Integer.valueOf(tokens[3]);
			bb.h = Integer.valueOf(tokens[4]);
			return bb;
		} else {
			return null;
		}
	}
}
