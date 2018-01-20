/**
 * 
 */
package bigdata.cv;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import javax.imageio.ImageIO;

/**
 * @author lqian
 *
 */
public class VehicleTypeDataset {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int shrinkRate = 1;
		int maxTypeId = 10;

		Path path = Paths.get("/dataset/vehicle-dataset");
		Path targetPath = Paths.get("/home/link/vehicle-type-dataset");
		
		if (notExists(targetPath)) {
			createDirectory(targetPath);
		}
		
		for (int i = 0; i <= maxTypeId; i++) {
			Path sub = targetPath.resolve("" + i);
			if (Files.notExists(sub)) {
				Files.createDirectory(sub);
			}
		}

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8", "root", "123456");

		for (int i = 0; i <= maxTypeId; i++) {
			Path sub = targetPath.resolve("" + i);
			String sql = "select path, vehicle_position from vehicle_dataset where vehicle_type=" + i + " limit 7000";
			ResultSet rs = conn.createStatement().executeQuery(sql);
			int j = 0;
			while (rs.next() && j < 7000) {
				String rp = rs.getString("path");
				String pos = rs.getString("vehicle_position");
				LabeledBoundingBox lbb = LabeledBoundingBox.fromPos(pos);

				Path source = path.resolve(rp);
				if (notExists(source))
					continue;
				
				BufferedImage image = ImageIO.read(source.toFile());
				if (lbb.isValid() && lbb.x + lbb.w <= image.getWidth() && lbb.y + lbb.h <= image.getHeight()) {
					BufferedImage vehicleImage = image.getSubimage(lbb.x, lbb.y, lbb.w, lbb.h);
					if (shrinkRate != 1) {
						BufferedImage scaleImage = new BufferedImage(lbb.w / shrinkRate, lbb.h / shrinkRate,
								image.getType());
						AffineTransform transform = new AffineTransform();
						transform.setToScale(1.0 / shrinkRate, 1.0 / shrinkRate);
						AffineTransformOp imageOp = new AffineTransformOp(transform, null);
						imageOp.filter(vehicleImage, scaleImage);

						vehicleImage = scaleImage;
					}
					ImageIO.write(vehicleImage, "JPG", sub.resolve(String.format("%d_%04d.JPG", i, j)).toFile());
					j++;
				}
			}

			rs.close();
			System.out.format("find image: %d for vehicle type: %d %n", j, i);
		}

		conn.close();
	}

}
