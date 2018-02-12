/**
 * 
 */
package dataset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author lqian
 *
 */
public class VehicleBrand {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Map<String, Integer> counter = new TreeMap<>();

		Path path = Paths.get("/dataset/vehicle-brand-dataset");

		Path targetDir = Paths.get("/dataset/vehicle-brand-dataset");

		if (Files.notExists(targetDir)) {
			Files.createDirectory(targetDir);
		}

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8&rewriteBatchedStatements=true",
				"root", "123456");
		// conn.setAutoCommit(false);

		// mkdirs
		String sql = "select distinct vehicle_brand, vehicle_sub_brand, vehicle_model from vehicle_dataset_origin";

		ResultSet rs = conn.createStatement().executeQuery(sql);
		while (rs.next()) {
			int brand = rs.getInt("vehicle_brand");
			int subBrand = rs.getInt("vehicle_sub_brand");
			int model = rs.getInt("vehicle_model");
			String subPath = String.format("%04d/%03d/%03d", brand, subBrand, model);
			if (Files.notExists(targetDir.resolve(subPath))) {
				Files.createDirectories(targetDir.resolve(subPath));
			}
		}
		rs.close();

		sql = "select path, plate_nbr, plate_color, vehicle_position, plate_position, "
				+ "vehicle_brand, vehicle_sub_brand, vehicle_model, vehicle_type,"
				+ "vehicle_color from vehicle_dataset_origin";

		String insertSql = "insert into vehicle_dataset (path, plate_nbr, plate_color, vehicle_position, plate_position, vehicle_brand, vehicle_sub_brand, vehicle_model, vehicle_type, vehicle_color )"
				+ " values (?,?,?,?,?,?,?,?,?,?) ";
		PreparedStatement pstm1 = conn.prepareStatement(sql);
		rs = pstm1.executeQuery();
		PreparedStatement pstm2 = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);

		int id = 0;
		while (rs.next()) {
			String rp = rs.getString("path");
			int brand = rs.getInt("vehicle_brand");
			int subBrand = rs.getInt("vehicle_sub_brand");
			int model = rs.getInt("vehicle_model");

			Path source = path.resolve(rp);

			String subPath = String.format("%04d/%03d/%03d", brand, subBrand, model);

			int subid = 0;
			if (counter.containsKey(subPath)) {
				subid = counter.get(subPath);
			}

			String normalName = Util.normal(brand, subBrand, model, subid);
			
			Path target = targetDir.resolve(normalName);
			if (Files.exists(source)) {
				Files.move(source, target);
				counter.put(subPath, ++subid);
				
				int i = 1;
				// pstm2.setLong(i++, id);
				pstm2.setString(i++, String.format("%s/%s", subPath, normalName));
				pstm2.setString(i++, rs.getString("plate_nbr"));
				pstm2.setString(i++, rs.getString("plate_color"));
				pstm2.setString(i++, rs.getString("vehicle_position"));
				pstm2.setString(i++, rs.getString("plate_position"));
				pstm2.setString(i++, rs.getString("vehicle_brand"));
				pstm2.setString(i++, rs.getString("vehicle_sub_brand"));
				pstm2.setString(i++, rs.getString("vehicle_model"));
				pstm2.setString(i++, rs.getString("vehicle_type"));
				pstm2.setString(i++, rs.getString("vehicle_color"));
				pstm2.addBatch();

				if (++id % 1000 == 0) {
					pstm2.executeBatch();
					// pstm2.clearBatch();
					System.out.println("move file num:" + id);
				}
			}
		}
		pstm2.executeBatch();
		System.out.println("move file num:" + id);
		pstm2.close();
		rs.close();

		conn.close();
	}
}
