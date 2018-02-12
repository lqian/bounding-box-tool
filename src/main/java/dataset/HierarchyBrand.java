/**
 * 
 */
package dataset;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author link
 *
 */
public class HierarchyBrand {

	static final String skip_config_data = "--skip-config-data";
	static final String skip_train_val_data = "--skip-train-val-data";;

	/**
	 * @param args
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws Exception {
		List<String> options = new ArrayList<>();
		if (args.length > 0) {
			options = Arrays.asList(args);
		}

		if (!options.contains(skip_config_data)) {
			createConfigData();
		}

		if (!options.contains(skip_train_val_data)) {
			createTrainValList();
		}

	}

	private static void createTrainValList() throws Exception {
		Connection conn = ExportBrand.createConn();
		Statement stm = conn.createStatement();
		PreparedStatement pstm = conn.prepareStatement(
				"select path from vehicle_dataset where vehicle_brand=? and vehicle_sub_brand=? and vehicle_model=?");
		ResultSet rs = stm
				.executeQuery("select vehicle_brand, vehicle_sub_brand, vehicle_model, count(1) from vehicle_dataset"
						+ " group by vehicle_brand, vehicle_sub_brand, vehicle_model");
		BufferedWriter train = Files.newBufferedWriter(Paths.get("train.list"));
		BufferedWriter valid = Files.newBufferedWriter(Paths.get("valid.list"));
		while (rs.next()) {

			int brand = rs.getInt(1);
			int subBrand = rs.getInt(2);
			int model = rs.getInt(3);
			int total = rs.getInt(4);
			fill(pstm, train, valid, brand, subBrand, model, total >= 3000);

		}
		rs.close();
		conn.close();
		train.flush();
		train.close();
		valid.flush();
		valid.close();

	}

	private static void fill(PreparedStatement pstm, BufferedWriter train, BufferedWriter valid, int brand,
			int subBrand, int model, boolean bigger) throws Exception {
		String token = String.format("/%04d/", brand);
		pstm.setInt(1, brand);
		pstm.setInt(2, subBrand);
		pstm.setInt(3, model);
		ResultSet rs = pstm.executeQuery();
		int counter = 0;

		while (rs.next()) {
			if (bigger && counter <= 1000 || counter % 3 == 0) {
				appendList(valid, token, rs);
			} else {
				appendList(train, token, rs);
			}
			counter++;
		}

	}

	static void appendList(BufferedWriter writer, String token, ResultSet rs) throws SQLException, IOException {
		String path = rs.getString(1);
		int i = path.indexOf(token);
		String rpath = path.substring(i);
		if (i != -1) {
			writer.write(rpath);
			writer.newLine();
		}
	}

	 

	static void createConfigData() throws ClassNotFoundException, SQLException, IOException {
		Connection conn = ExportBrand.createConn();
		BufferedWriter writer = Files.newBufferedWriter(Paths.get("brand.tree"));
		String sql = "select distinct brand from vehicle_brand where brand=1028";
		ResultSet rs = conn.createStatement().executeQuery(sql);

		List<Integer> brands = new ArrayList<>();
		while (rs.next()) {
			int brand = rs.getInt(1);
			brands.add(brand);
			writer.write(String.format("b%04d000000 -1\n", brand));
		}
		rs.close();

		int rows = brands.size();

		List<Integer> subBrands = new ArrayList<>();
		PreparedStatement pstm = conn.prepareStatement(
				"select distinct subbrand from vehicle_brand where brand=? and subbrand!=0 order by subbrand");
		for (int i = 0; i < brands.size(); i++) {
			subBrands.clear();
			int brand = brands.get(i);
			pstm.setInt(1, brand);
			rs = pstm.executeQuery();
			while (rs.next()) {
				int subBrand = rs.getInt(1);
				subBrands.add(subBrand);
				writer.write(String.format("b%04d%03d000 %d\n", brand, subBrand, i));
			}

			// model
			int batch = 0;
			PreparedStatement pstm2 = conn.prepareStatement(
					"select model from vehicle_brand where brand=? and subBrand=? and model!=0 order by model");
			ResultSet rs2;
			for (int j = 0; j < subBrands.size(); j++) {
				int subBrand = subBrands.get(j);
				pstm2.setInt(1, brand);
				pstm2.setInt(2, subBrand);
				rs2 = pstm2.executeQuery();

				while (rs2.next()) {
					int model = rs2.getInt(1);
					writer.write(String.format("b%04d%03d%03d %d\n", brand, subBrand, model, rows + j));
					batch++;
				}
				rs2.close();
			}
			rows += batch + subBrands.size();
		}

		writer.flush();
		writer.close();
		conn.close();
	}
}
