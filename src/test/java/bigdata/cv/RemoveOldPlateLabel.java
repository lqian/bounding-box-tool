/**
 * 
 */
package bigdata.cv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author link
 *
 */
public class RemoveOldPlateLabel {

	public static void main(String[] args) throws Exception {
		Path target = Paths.get("label-without-plate");
		if (Files.notExists(target)) {
			Files.createDirectories(target);
		}
		long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2017-11-22 21:23:08").getTime();
		List<Path> oldLabels = new ArrayList<Path>();
		Path start = Paths.get("/home/link/Intelligence-Transport-Dateset/JPEGImages");
		Files.walkFileTree(start, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("label") && attrs.lastModifiedTime().toMillis() <= timestamp)
					oldLabels.add(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});

		for (Path path: oldLabels) {
			removePlate(path, target);
		}
	}

	static void removePlate(Path labelFile,Path targetDir) {
		List<String> boundingBoxes = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(labelFile)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				LabeledBoundingBox bb = LabeledBoundingBox.from(line);
				if (bb == null || (bb != null && !"车辆".equalsIgnoreCase(bb.labelName)))
					boundingBoxes.add(line);
			}

		} catch (IOException e) {
			System.out.print(e);
		}
		
		try (BufferedWriter writer = Files.newBufferedWriter(targetDir.resolve(labelFile.getFileName()))) {
			for (String line : boundingBoxes) {
				writer.write(line);
				writer.newLine();
			}
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
