/*
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package bigdata.cv;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.*;
import static java.nio.file.Files.walkFileTree;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

/**
 * @author lqian
 *
 */
public class DataSet {

	static Set<String> extensions = new TreeSet<String>();

	static {
		extensions.add("jpg");
		extensions.add("png");
	}

	Path home;

	Path jpegImages;

	Path rawLabel;

	Path darkNetLabels; // darknet style

	// Path annotations; // faster rcnn styl

	List<String> imageFiles = new ArrayList<>();
	List<String> rawLabelFiles = new ArrayList<>();
	List<String> darknetLabelFiles = new ArrayList<>();

	public DataSet(Path home) throws IOException {
		super();
		this.home = home;
		jpegImages = home.resolve("JPEGImages");

		if (notExists(jpegImages)) {
			createDirectories(jpegImages);
		}

		rawLabel = home.resolve("raw");
		if (notExists(rawLabel)) {
			createDirectories(rawLabel);
		}
		darkNetLabels = home.resolve("labels");
		if (notExists(darkNetLabels)) {
			createDirectories(darkNetLabels);
		}
		// annotations = home.resolve("Annotations");
		// if (notExists(annotations)) {
		// createDirectories(annotations);
		// }

		walkFileTree(jpegImages, new ImageFileVisitor());
		Collections.sort(imageFiles);
		walkFileTree(rawLabel, new RawLabelFileVisitor());
		Collections.sort(rawLabelFiles);
		walkFileTree(darkNetLabels, new LabelFileVisitor());
		Collections.sort(darknetLabelFiles);
	}

	public Path getRawLabel(String file) {
		return rawLabel.resolve(file);
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

	public Path getImage(String file) {
		return jpegImages.resolve(file);
	}

	public Path getDarknetLabel(String file) {
		return darkNetLabels.resolve(file);
	}

	public Path resolve(String file) {
		return home.resolve(file);
	}

	public DataSet(String dir) throws IOException {
		this(Paths.get(dir));
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
						imageFiles.add(name);
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

	class LabelFileVisitor extends ImageFileVisitor implements FileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (attrs.isRegularFile()) {
				String name = file.getFileName().toString();
				int li = name.lastIndexOf(".");
				if (li > 0) {
					String extName = name.substring(li + 1).toLowerCase();
					if ("txt".equals(extName)) {
						darknetLabelFiles.add(name);
					}
				}
			}
			return FileVisitResult.CONTINUE;
		}
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
