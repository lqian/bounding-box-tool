package dataset;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class YoloVehicleType {

	public static void main(String[] args) throws IOException {
		String sourceList = args[0];
		Path targetPath  = Paths.get(args[1]);
		Path trainList = targetPath.resolve("train.list");
		Path valList = targetPath.resolve("val.list");
		Path lableDir = targetPath.resolve("labels");
				
		BufferedWriter trainWriter = Files.newBufferedWriter(trainList);
		BufferedWriter valWriter = Files.newBufferedWriter(valList);
		BufferedReader reader = Files.newBufferedReader(Paths.get(sourceList));
		String line = null;
		int counter  = 0;
		while ( (line = reader.readLine()) != null) {
			RawData rawData = parse(line);
			if (rawData != null) {
				 BufferedImage image = ImageIO.read(new File(rawData.path));
				 float w = image.getWidth();
				 float h = image.getHeight();				 
							 
				 int i = rawData.path.lastIndexOf(".");
				 if (i != -1) {
					 String labelName = rawData.path.substring(0, i) + ".txt";
					 
					 // yolo coordinates
					 float yx = ((2 * rawData.vehiclePos.x + w ) /2 -1 ) / w;
					 float yy = ((2 * rawData.vehiclePos.y + h ) /2 -1 ) / h;
					 float yw = rawData.vehiclePos.w / w;
					 float yh = rawData.vehiclePos.h / h;
					 
					 String outLine = String.format("%d %f %f %f %f", rawData.vehicleType, yx, yy, yw, yh);
					 BufferedWriter out = Files.newBufferedWriter(lableDir.resolve(labelName));
					 out.write(outLine);
					 out.flush();
					 out.close();
					 
					 if (++counter % 6==0) {
						 valWriter.write(outLine); 
						 valWriter.newLine();
					 }
					 else {
						 trainWriter.write(outLine); 
						 trainWriter.newLine();
					 }
				 }
			}
		}
	}
	
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
		}
		else {
			return null;
		}
		 
	}
	
	static class RawData {
		String path;
		String plateNo;
		String plateColor;
		Box vehiclePos;
		Box platePos;
		String brand, subBrand, model;
		int vehilceColor;
		int vehicleType;
	}
	
	static class Box  {
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
			return new Box(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]));
		}
	}

}
