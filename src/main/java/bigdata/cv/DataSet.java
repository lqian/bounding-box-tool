/**
 * 
 */
package bigdata.cv;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.newBufferedWriter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import bigdata.cv.DataSet.ImageFileVisitor;

/**
 * @author lqian
 *
 */
public abstract class DataSet {
	
	static Set<String> extensions = new TreeSet<String>();

	static {
		extensions.add("jpg");
		extensions.add("png");
		extensions.add("jpeg");
		extensions.add("gif");
	}
	
	
	Path home;
	Path jpegImages;
	Path rawLabels;
	
	
	List<String> imageFiles = new ArrayList<>();
	List<String> rawLabelFiles = new ArrayList<>();
	
	public Path getRawLabel(String file) {
		return rawLabels.resolve(file);
	}
	public List<LabeledBoundingBox> readBoundingBoxes(String file) throws IOException {
		Path path = getRawLabel(file);
		List<LabeledBoundingBox> boxes = new ArrayList<LabeledBoundingBox>();
		if (exists(path)) {
			BufferedReader reader = Files.newBufferedReader(path);
			String line = null;
			while ((line = reader.readLine()) != null) {
				LabeledBoundingBox bb = LabeledBoundingBox.from(line);
				if (bb != null)
					boxes.add(bb);
			}
			reader.close();
		}
		return boxes;
	}
	
	
	class ImageFileVisitor implements FileVisitor<Path> {

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (attrs.isRegularFile()) {
				String name = file.getFileName().toString();
				int li = name.lastIndexOf(".");
				if (li > 0) {
					String extName = name.substring(li + 1).toLowerCase();
					if (extensions.contains(extName)) {
						imageFiles.add(jpegImages.relativize(file).toString());
					}
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}
	
	
	public void saveRawLabel(String file, int w, int h, List<LabeledBoundingBox> boxes) throws IOException {
		Path path = getRawLabel(file);
		BufferedWriter writer = newBufferedWriter(path);
		writer.write(w + "," + h);
		writer.newLine();
		for (LabeledBoundingBox box : boxes) {
			writer.write(box.toString());
			writer.newLine();
		}
		writer.close();
	}

	public boolean saveImage(String file, BufferedImage image) throws IOException {
		return ImageIO.write(image, "jpg", jpegImages.resolve(file).toFile());
	}

	public BufferedImage readImage(String file) throws IOException {
		Path image = getImage(file);
		return exists(image) ? ImageIO.read(image.toFile()) : null;
	}

	 

	public Path getImage(String file) {
		return jpegImages.resolve(file);
	}

	public Path resolve(String file) {
		return home.resolve(file);
	}
	
	
	class RawLabelFileVisitor extends ImageFileVisitor implements FileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (attrs.isRegularFile()) {
				String name = file.getFileName().toString();
				int li = name.lastIndexOf(".");
				if (li > 0) {
					String extName = name.substring(li + 1).toLowerCase();
					if ("label".equals(extName)) {
						rawLabelFiles.add(name);
					}
				}
			}
			return FileVisitResult.CONTINUE;
		}
	}
	

}
