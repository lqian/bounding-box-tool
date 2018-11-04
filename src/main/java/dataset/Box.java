package dataset;

class Box  {
	float x, y, w, h;

	public Box(int x, int y, int w, int h) {
		super();
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}
	
	public static Box parse(String str) {
		String tokens[] = str.split(",");
		if (tokens.length != 4) return null;
		return new Box(Integer.parseInt(tokens[0]), 
				Integer.parseInt(tokens[1]), 
				Integer.parseInt(tokens[2]), 
				Integer.parseInt(tokens[3]));
	}
}