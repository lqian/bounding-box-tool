/**
 * 
 */
package bigdata.cv;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.imageio.ImageIO;

/**
 * @author lqian
 *
 */
public class ExportVehicleBrand {
	
	static Path home; 

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws Exception {
		home = args.length > 0 ? Paths.get(args[0]) : Paths.get("vehicle-brand-dataset");
		
		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8&rewriteBatchedStatements=true",
				"root", "123456");
		// conn.setAutoCommit(false);

		String summarizeSQL = "select distinct vehicle_brand, vehicle_sub_brand, vehicle_model from vehicle_dataset";
		Statement stm = conn.createStatement();
		ResultSet rs = stm.executeQuery(summarizeSQL);
		ArrayList<BrandEntity> list = new ArrayList<>();
		while (rs.next()) {
			BrandEntity be = new BrandEntity();
			be.brand = rs.getString(1);
			be.subBrand = rs.getString(2);
			be.model = rs.getString(3);
			list.add(be);
		}
		rs.close();
		stm.close();

		for (BrandEntity be : list) {
			Path dir = home.resolve(be.brand).resolve(be.subBrand).resolve(be.model);
			if (Files.notExists(dir)) {
				Files.createDirectories(dir);
			}
		}
		
		
		String sql = "select id, path, vehicle_position from vehicle_dataset where vehicle_brand=? and vehicle_sub_brand=? and vehicle_model=? limit 2000";
		PreparedStatement pstm = conn.prepareStatement(sql);
		for (BrandEntity be : list) {
			pstm.setString(1, be.brand);
			pstm.setString(2, be.subBrand);
			pstm.setString(3, be.model);
			
			rs = pstm.executeQuery();
			int count = 0;
			while (rs.next()) {
				long id = rs.getLong("id");
				String path = rs.getString("path");
				String vehiclePosition = rs.getString("vehicle_position");
				if (corpVehicle(be, path, id, vehiclePosition)) { count++ ;}
				
			}
			rs.close();
			System.out.format("corp %05d image for %s %n", count, be);
		}
		pstm.close();
	}

	private static boolean corpVehicle(BrandEntity be, String path, long id, String vehiclePosition)  {
		try {
			File file = new File(path);
			BufferedImage image = ImageIO.read(file);
			String tokens[] = vehiclePosition.split("[\\s,]", 4);
			if (tokens.length ==4) {
				int x = Integer.valueOf(tokens[0]);
				int y = Integer.valueOf(tokens[1]);
				int w = Integer.valueOf(tokens[2]);
				int h = Integer.valueOf(tokens[3]);
				BufferedImage subImage = image.getSubimage(x, y, w, h);
				File tf = home.resolve(be.brand).resolve(be.subBrand).resolve(be.model).resolve(String.format("%08d.jpg", id)).toFile();
				ImageIO.write(subImage, "jpg", tf);
				return true;
			}

		}catch (IOException e) {
			System.err.println(e.toString());
		}

		return false;
	}

	static class BrandEntity {
		String brand;
		String subBrand;
		String model;
		@Override
		public String toString() {
			return "[brand=" + brand + ", subBrand=" + subBrand + ", model=" + model + "]";
		}
	}
}
