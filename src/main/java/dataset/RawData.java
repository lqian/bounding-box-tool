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
		if (tokens.length != 9) {
			RawData rw = new RawData();
			rw.path = tokens[0];
			rw.plateNo = tokens[1];
			rw.vehiclePos = Box.parse(tokens[2]);
			rw.platePos = Box.parse(tokens[3]);
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

}