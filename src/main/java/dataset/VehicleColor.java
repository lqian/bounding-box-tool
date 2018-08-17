/**
 * 
 */
package dataset;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.*;
import static java.nio.file.Files.*;

/**
 * @author lqian
 *
 */
public class VehicleColor {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Path dataset = Paths.get(args[0]);
		BufferedReader reader = Files.newBufferedReader(Paths.get(args[1])); // read the meta list
		Path target = Paths.get(args[2]);
		
		if (notExists(target)) {
			createDirectories(target);
			System.out.println("create target directory: " + target);
		}

		//create all sub catalog
		for (int i = 0; i < 15; i++) {
			Path catalog = target.resolve(format("c%02d", i));
			if (Files.notExists(catalog)) {
				Files.createDirectories(catalog);
				System.out.println("create sub catalog: " + catalog);
			}
		} 
		
		int scanFrom = 0;
		if (args.length > 3) {
			scanFrom = Integer.parseInt(args[3]);
		}
		
		int scanEnd = 100000;
		if (args.length > 4) {
			scanEnd = Integer.parseInt(args[4]);
		}
		
		String line = null;
		for (int i=0; i< scanEnd; i++) {
			line = reader.readLine();
			if (line == null) break;
			if (i<scanFrom) continue;
			
			RawData rw = RawData.parse(line); 

			Path sourceSample = dataset.resolve(rw.path);
			Path targetSample = target.resolve(String.format("c%02d/%08d", rw.vehilceColor, i));
			if (exists(sourceSample) && notExists(targetSample)) { 
				copy(sourceSample, targetSample);
			}
		}

		reader.close();
	}

}
