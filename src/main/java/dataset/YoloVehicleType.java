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

/**
 * 从原始数据中导出YOLO对象检测数据格式
 * 
 * @author qian xiafei
 *
 */
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
			RawData rawData = RawData.parse(line);
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
	
	

}
