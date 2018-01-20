/**
 * 
 */
package bigdata.cv;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileVisitOption;
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

		int maxColorId = 14;
		Path path = Paths.get("/dataset/vehicle-dataset");
		Path targetPath = Paths.get("/home/link/vehicle-color-dataset");

		for (int i = 0; i <= maxColorId; i++) {
			Path sub = targetPath.resolve("" + i);
			if (Files.notExists(sub)) {
				Files.createDirectories(sub);
			}
		}

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8", "root", "123456");

		for (int i = 4; i <= maxColorId; i++) {
			Path sub = targetPath.resolve("" + i);

			String sql = "select path, vehicle_position from vehicle_dataset where vehicle_color=" + i + " limit 7000";
			ResultSet rs = conn.createStatement().executeQuery(sql);
			int j = 0;
			while (rs.next() && j < 7000) {
				String rp = rs.getString("path");
				String pos = rs.getString("vehicle_position");
				LabeledBoundingBox lbb = LabeledBoundingBox.fromPos(pos);

				try {
					BufferedImage image = ImageIO.read(path.resolve(rp).toFile());
					if (lbb.isValid() && lbb.x + lbb.w <= image.getWidth() 
							&& lbb.y  + lbb.h <= image.getHeight()) {
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
						ImageIO.write(vehicleImage, "JPG", sub.resolve(String.format("%d_%04d.jpg", i, j)).toFile());
						j++;
					}
				} catch (IOException e) {
					System.err.println("can not read image: " + path.resolve(rp));
				}
			}

			rs.close();
			System.out.println("color id: " + i + " " + Files.walk(sub, 1, FileVisitOption.FOLLOW_LINKS).count());
		}
		conn.close();
	}

}
