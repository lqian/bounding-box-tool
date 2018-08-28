package dataset;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;

public class App1 {

	public static void main(String[] args) throws IOException {
		Path p = Paths.get("/home/link/vehicle-brand-dataset");
		Files.walkFileTree(p, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				String name = dir.getFileName().toString();
				if (name.length() < 4 && name.startsWith("0")) {
				String  newName  = Integer.valueOf(name) +"";
				Files.move(dir, dir.getParent().resolve(newName));
				}
				return super.postVisitDirectory(dir, exc);
			}
			
		});

	}

}
