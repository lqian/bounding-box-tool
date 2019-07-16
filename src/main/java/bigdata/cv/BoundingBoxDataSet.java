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
import static java.nio.file.Files.notExists;
import static java.nio.file.Files.walkFileTree;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author lqian
 *
 */
public class BoundingBoxDataSet extends DataSet {

	static Set<String> extensions = new TreeSet<String>();

	static {
		extensions.add("jpg");
		extensions.add("png");
		extensions.add("jpeg");
		extensions.add("gif");
	}


	Path darkNetLabels; // darknet style
	
	Path caffeMobileYolo; // SSD style

	// Path annotations; // faster rcnn styl

	
	List<String> darknetLabelFiles = new ArrayList<>();
	
	public BoundingBoxDataSet(Path home) throws IOException {
		this(home, true);
	}

	public BoundingBoxDataSet(Path home, boolean visitingDirectory) throws IOException {
		super();
		this.home = home;
		jpegImages = home.resolve("JPEGImages");

		if (notExists(jpegImages)) {
			createDirectories(jpegImages);
		}

		rawLabels = home.resolve("raw");
		if (notExists(rawLabels)) {
			createDirectories(rawLabels);
		}
		darkNetLabels = home.resolve("labels");
		if (notExists(darkNetLabels)) {
			createDirectories(darkNetLabels);
		}
		
		caffeMobileYolo = home.resolve("mobileYolo");
		if (notExists(caffeMobileYolo)) {
			createDirectories(caffeMobileYolo);
		}
		// annotations = home.resolve("Annotations");
		// if (notExists(annotations)) {
		// createDirectories(annotations);
		// }

		if (visitingDirectory ) {
			walkFileTree(jpegImages, new ImageFileVisitor());
			Collections.sort(imageFiles);
			walkFileTree(rawLabels, new RawLabelFileVisitor());
			Collections.sort(rawLabelFiles);
			walkFileTree(darkNetLabels, new LabelFileVisitor());
			Collections.sort(darknetLabelFiles);
		}
	}

	  
 

	public Path getDarknetLabel(String file) {
		return darkNetLabels.resolve(file);
	}
	
	public Path getCaffeMobileYolo(String file) {
		return this.caffeMobileYolo.resolve(file);
	}

	public BoundingBoxDataSet(String dir) throws IOException {
		this(Paths.get(dir));
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
}
