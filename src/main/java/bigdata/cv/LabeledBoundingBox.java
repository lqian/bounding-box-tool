package bigdata.cv;

/**
 * 
 * @author qian xiafei
 *
 */
public class LabeledBoundingBox {

	public int x, y;
	public int w, h;

	public String labelName; // label name of bounding box,

	public static LabeledBoundingBox wrap(String labelName, int x1, int y1, int x2, int y2) {
		LabeledBoundingBox bb = new LabeledBoundingBox();
		bb.labelName = labelName;
		bb.x = x1 < x2 ? x1 : x2;
		bb.y = y1 < y2 ? y1 : y2;
		bb.w = Math.abs(x1 - x2);
		bb.h = Math.abs(y1 - y2);
		return bb;
	}

	@Override
	public String toString() {
		return String.format("%s,%d,%d,%d,%d", labelName, x, y, w, h);
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
