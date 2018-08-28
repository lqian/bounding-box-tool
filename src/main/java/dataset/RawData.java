package dataset;

class RawData {
	String path;
	String plateNo;
	String plateColor;
	Box vehiclePos;
	Box platePos;
	String brand, subBrand, model;
	int vehilceColor;
	int vehicleType;

	/**
	 * 数据输入的行是逗号分隔的依次为
	 * path、plateNo、vehiclePos、platePos、brand、subBrand、model、vehicleType、vehicleColor
	 * 
	 * @param line
	 * @return
	 */
	static RawData parse(String line) {

		String tokens[] = line.split(";", 9);
		if (tokens.length == 9) {
			RawData rw = new RawData();
			rw.path = tokens[0];
			rw.vehiclePos = Box.parse(tokens[1]);
			rw.platePos = Box.parse(tokens[2]);
			rw.plateNo = tokens[3];
			rw.brand = tokens[4];
			rw.subBrand = tokens[5];
			rw.model = tokens[6];
			rw.vehicleType = Integer.parseInt(tokens[7]);
			rw.vehilceColor = Integer.parseInt(tokens[8]);
			return rw;
		} else {
			return null;
		}

	}

	/**
	 * 数据输入的行是逗号分隔的依次为
	 * path、plateNo、plateColor, vehiclePos、platePos、brand、subBrand、model、vehicleType、vehicleColor
	 * 
	 * @param line
	 * @return
	 */
	static RawData parseBrandLine(String line) {

		String tokens[] = line.split(";", 10);
		if (tokens.length == 10) {
			RawData rw = new RawData();
			rw.path = tokens[0];
			rw.plateNo = tokens[1];
			rw.plateColor = tokens[2];
			rw.vehiclePos = Box.parse(tokens[3]);
			rw.platePos = Box.parse(tokens[4]);
			rw.brand = tokens[5];
			rw.subBrand = tokens[6];
			rw.model = tokens[7];
			rw.vehicleType = Integer.parseInt(tokens[8]);
			rw.vehilceColor = Integer.parseInt(tokens[9]);
			return rw;
		} else {
			return null;
		}

	}
}