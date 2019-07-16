package bigdata.cv;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.notExists;
import static java.nio.file.Files.walkFileTree;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class MTCNNDataSet extends DataSet { 


	// Path annotations; // faster rcnn styl
	public MTCNNDataSet(Path home) throws IOException {
		this(home, true);
	}

	public MTCNNDataSet(Path home, boolean visitingDirectory) throws IOException {
		super();
		this.home = home;
		jpegImages = home.resolve("JPEGImages");

		if (notExists(jpegImages)) {
			createDirectories(jpegImages);
		}

		rawLabels = home.resolve("mtcnn");
		if (notExists(rawLabels)) {
			createDirectories(rawLabels);
		}
		 
		// annotations = home.resolve("Annotations");
		// if (notExists(annotations)) {
		// createDirectories(annotations);rawLabels
		// }

		if (visitingDirectory ) {
			walkFileTree(jpegImages, new ImageFileVisitor());
			Collections.sort(imageFiles);
			walkFileTree(rawLabels, new RawLabelFileVisitor());
			Collections.sort(rawLabelFiles);
		}
	}

	public MTCNNDataSet(String dir) throws IOException {
		this(Paths.get(dir));
	}
 

	
}
