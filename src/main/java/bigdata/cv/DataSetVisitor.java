/**
 * 
 */
package bigdata.cv;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author lqian
 *
 */
public class DataSetVisitor implements FileVisitor<Path> {

	static Set<String> extensions = new TreeSet<String>();
	
	PriorityQueue<String> imageFiles = new PriorityQueue<String>(new StringComparator());
	
	PriorityQueue<String> labelFiles;

	static {
		extensions.add("jpg");
//		extensions.add("png");
	}
	
	public DataSetVisitor(PriorityQueue<String> labelFiles) {
		super();
		this.labelFiles = labelFiles;
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
