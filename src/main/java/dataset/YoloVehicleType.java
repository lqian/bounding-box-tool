package dataset;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * 从原始数据中导出YOLO对象检测数据格式
 * 
 * @author qian xiafei
 *
 */
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
			RawData rawData = RawData.parse(line);
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
					 float yx = ((2 * rawData.vehiclePos.x + rawData.vehiclePos.w ) /2 -1 ) / w;
					 float yy = ((2 * rawData.vehiclePos.y + rawData.vehiclePos.h ) /2 -1 ) / h;
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

}
