/**
 * 
 */
package bigdata.cv;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author lqian
 *
 */
public class DataSetVisitor implements FileVisitor<Path> {

	static Set<String> extensions = new TreeSet<String>();
	
	List<String> imageFiles = new ArrayList<>();
	
	List<String> labelFiles = new ArrayList<>();

	static {
		extensions.add("jpg");
		extensions.add("png");
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
		if (attrs.isRegularFile()) {
			String name = path.getFileName().toString();
			int li = name.lastIndexOf(".");
			if (li > 0) {
				String extName = name.substring(li + 1).toLowerCase();
				if (extensions.contains(extName)) {
					imageFiles.add(name);
				}
				
				if ("label".equals(extName)) {
					labelFiles.add(name);
				}
			}
		}
		
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

}
