/**
 * 
 */
package bigdata.cv;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
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
public class VehicleColorDataset {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int shrinkRate = 1;

		Path path = Paths.get("image-directory");
		Path targetPath = Paths.get("vehicle-color-dataset");

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8", "root", "123456");

		int maxColorId = 14;
		for (int i = 0; i <= maxColorId; i++) {
			Path sub = targetPath.resolve("" + i);
			if (Files.notExists(sub)) {
				Files.createDirectory(sub);
			}
			;

			String sql = "select path, vehicle_position from vehicle_dataset where vehicle_color=" + i + " limit 7000";
			ResultSet rs = conn.createStatement().executeQuery(sql);
			int j = 0;
			while (rs.next() && j < 6000) {
				String rp = rs.getString("path");
				String pos = rs.getString("vehicle_position");
				LabeledBoundingBox lbb = LabeledBoundingBox.from(pos);

				BufferedImage image = ImageIO.read(path.resolve(rp).toFile());
				if (lbb.isValid()) {
					BufferedImage vehicleImage = image.getSubimage(lbb.x, lbb.y, lbb.w, lbb.h);
					if (shrinkRate > 1) {
						BufferedImage scaleImage = new BufferedImage(lbb.w / shrinkRate, lbb.h / shrinkRate,
								image.getType());
						AffineTransform transform = new AffineTransform();
						transform.setToScale(1.0 / shrinkRate, 1.0 / shrinkRate);
						AffineTransformOp imageOp = new AffineTransformOp(transform, null);
						imageOp.filter(image, scaleImage);

						vehicleImage = scaleImage;
					}
					ImageIO.write(vehicleImage, "JPG", sub.resolve("" + j + ".JPG").toFile());
					j++;
				}
			}

			rs.close();
		}

		conn.close();
	}

}
