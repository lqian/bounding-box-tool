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
		Path dataset  = Paths.get(args[0]);
		if (Files.notExists(dataset)) { 
			System.err.println("cannot find the dataset " + dataset);
			
			System.exit(1);  
		}
		String sourceList = args[1];
		
		Path trainList = dataset.resolve("train.list");
		Path valList = dataset.resolve("val.list");
		Path labelDir = dataset.resolve("labels");
		if (Files.notExists(labelDir)) {
			Files.createDirectories(labelDir);
		}
				
		BufferedWriter trainWriter = Files.newBufferedWriter(trainList);
		BufferedWriter valWriter = Files.newBufferedWriter(valList);
		BufferedReader reader = Files.newBufferedReader(dataset.resolve(sourceList));
		String line = null;
		int counter  = 0;
		while ( (line = reader.readLine()) != null) {
			RawData rawData = parse(line);
			if (rawData != null) {				
				 Path samplePath = dataset.resolve(rawData.path);
				 if (Files.notExists(samplePath))  {
					 System.out.println("cannot find file: " + samplePath);
					 continue;				 
				 }
				BufferedImage image = ImageIO.read(samplePath.toFile());
				 float w = image.getWidth();
				 float h = image.getHeight();				 
							 
				 int i = rawData.path.lastIndexOf(".");
				 if (i != -1) {					 
					 String labelName = rawData.path.substring(0, i) + ".txt";
					 labelName = labelName.replaceFirst("JPEGImages\\/", "");
					 Path subPath = labelDir.resolve(Paths.get(labelName).subpath(0, 2));
					 if (Files.notExists(subPath)) Files.createDirectories(subPath);
					 
					 // yolo coordinates
					 float yx = ((2 * rawData.vehiclePos.x + w ) /2 -1 ) / w;
					 float yy = ((2 * rawData.vehiclePos.y + h ) /2 -1 ) / h;
					 float yw = rawData.vehiclePos.w / w;
					 float yh = rawData.vehiclePos.h / h;
					 
					 String outLine = String.format("%d %f %f %f %f", rawData.vehicleType, yx, yy, yw, yh);
					 BufferedWriter out = Files.newBufferedWriter(labelDir.resolve(labelName));
					 out.write(outLine);
					 out.flush();
					 out.close();
					 
					 String sample = samplePath.toString();
					 if (++counter % 6==0) {
						 valWriter.write(sample); 
						 valWriter.newLine();
						 System.out.format("%8d append val list %s \n", counter, sample);
					 }
					 else {
						 trainWriter.write(sample); 
						 trainWriter.newLine();
						 System.out.format("%8d append train list %s \n", counter, sample);
					 } 
					 //System.out.format(" convert %8d samples \n", counter);
					  
				 }
			}
		}
		valWriter.flush();
		valWriter.close();
		trainWriter.flush();
		trainWriter.close();
	}
	
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
