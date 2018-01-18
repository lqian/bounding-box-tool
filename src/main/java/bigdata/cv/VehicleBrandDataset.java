/**
 * 
 */
package bigdata.cv;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author lqian
 *
 */
public class VehicleBrandDataset {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Path path = Paths.get("/dataset/");

		Path targetDir = Paths.get("/dataset/vehicle-dataset");

		if (Files.notExists(targetDir)) {
			Files.createDirectory(targetDir);
		}

		Files.walkFileTree(path.resolve("export"), new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.SKIP_SIBLINGS;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				int nc = dir.getNameCount();
				if (nc > 2) {
					Path end = dir.subpath(2, dir.getNameCount());
					Path sub = targetDir.resolve(end);
					if (Files.notExists(sub)) {
						Files.createDirectories(sub);
					}
				}
				return FileVisitResult.CONTINUE;
			}

		});

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8&rewriteBatchedStatements=true", "root", "123456");
//		conn.setAutoCommit(false);

		String sql = "select path, plate_nbr, plate_color, vehicle_position, plate_position, vehicle_brand, vehicle_sub_brand, vehicle_model, vehicle_type,\n"
				+ "vehicle_color from vehicle_dataset_origin";

		String insertSql = "insert into vehicle_dataset (path, plate_nbr, plate_color, vehicle_position, plate_position, vehicle_brand, vehicle_sub_brand, vehicle_model, vehicle_type, vehicle_color )"
				+ " values (?,?,?,?,?,?,?,?,?,?) ";
		PreparedStatement pstm1 = conn.prepareStatement(sql);
		ResultSet rs = pstm1.executeQuery();
		PreparedStatement pstm2 = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);

		int id = 0;		
		while (rs.next()) {
			String rp = rs.getString("path");
			int brand = rs.getInt("vehicle_brand");
			int subBrand = rs.getInt("vehicle_sub_brand");
			int model = rs.getInt("vehicle_model");

			Path source = path.resolve(rp);
			String normalName = String.format("%04d%03d%03d_%08d.JPG", brand, subBrand, model, id++);
			Path target = targetDir.resolve("" + brand).resolve("" + subBrand).resolve(normalName);
			if (Files.exists(source)) {
				Files.move(source, target);
			}

			int i = 1;
//			pstm2.setLong(i++, id);
			pstm2.setString(i++, String.format("%d/%d/%s", brand, subBrand, normalName));
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

			if (id % 1000 == 0) {
				pstm2.executeBatch();
//				pstm2.clearBatch();
				System.out.println("move file num:" + id);
			}
		}
		pstm2.executeBatch();
		System.out.println("move file num:" + id);
		conn.commit();
		pstm2.close();
		rs.close();

		conn.close();
	}
}
