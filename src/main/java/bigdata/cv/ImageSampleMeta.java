/**
 * 
 */
package bigdata.cv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author qian xiafei
 *
 */
public class ImageSampleMeta {
	
	String fileName;
	String labelName;
	
	int width, height;
	
	List<LabeledBoundingBox> labeledBoundBoxes;
	
	public static ImageSampleMeta read(String fileName) {
		int i = fileName.lastIndexOf(".");
		if (i != -1) {
			ImageSampleMeta meta = new ImageSampleMeta();
			meta.fileName = fileName;
			meta.labelName = fileName.substring(0, i) + ".label";
			return meta;
		}
		else {
			return null;
		}
	}
	
	public void syncLabelFile(String labelName, boolean owerwrite) {
		Path path = Paths.get(labelName);
		if (owerwrite || Files.notExists(path)) {
			try {
				BufferedWriter writer = Files.newBufferedWriter(path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			
		}
	}
	
	public void syncLabelFile() {
		syncLabelFile(labelName, true);
	}
}
