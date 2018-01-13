/**
 * 
 */
package bigdata.cv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author lqian
 *
 */
public class VehicleBrandDataset {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Path path = Paths.get("image-directory");

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8", "root", "123456");

		String sql = "select id, path, vehicle_brand, vehicle_sub_brand, vehicle_model from vehicle_dataset";
		PreparedStatement prepareStatement = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
		ResultSet rs = prepareStatement
				.executeQuery();
		int j = 0;
		while (rs.next()) {
			String rp = rs.getString("path");
			int brand = rs.getInt("vehicle_brand");
			int subBrand = rs.getInt("vehicle_sub_brand");
			int model = rs.getInt("vehicle_model");
			
			Path source = path.resolve(rp);
			String normalName = String.format("%04d%03d%03d_%08d.JPG", brand, subBrand, model, j++);
			Path target = path.resolve("" + brand).resolve("" + subBrand).resolve(normalName);
//			Files.move(source, target);
			rs.updateString("path", String.format("%d/%d/%s", brand, subBrand, normalName));
			rs.updateRow();
		}

		rs.close();

		conn.close();
	}
}
