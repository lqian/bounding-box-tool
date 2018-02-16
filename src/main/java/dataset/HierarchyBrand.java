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
	static final String skip_train_val_data = "--skip-train-val-data";

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
				"select id from vehicle_dataset where vehicle_brand=? and vehicle_sub_brand=? and vehicle_model=?");
		ResultSet rs = stm
				.executeQuery("select vehicle_brand, vehicle_sub_brand, vehicle_model, count(1) from vehicle_dataset vd join brand_dictionary bd"
						+ " on vehicle_brand = brand and vehicle_sub_brand = subBrand and vehicle_model = model"
						+ " group by vehicle_brand, vehicle_sub_brand, vehicle_model");
		BufferedWriter train = Files.newBufferedWriter(Paths.get("train.list"));
		BufferedWriter valid = Files.newBufferedWriter(Paths.get("valid.list"));
		while (rs.next()) {
			int brand = rs.getInt(1);
			int subBrand = rs.getInt(2);
			int model = rs.getInt(3);
			int total = rs.getInt(4);
			fill(pstm, train, valid, brand, subBrand, model, total >= 3000);
			System.out.format("fill brand: %d subBrand: %d model: %d total:%d\n", brand, subBrand, model, total);

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
			long id = rs.getLong(1);
			if (bigger && counter <= 1000 || 
					counter % 3 == 0) {
				appendList(valid, brand, subBrand, model, id);
			} else {
				appendList(train, brand, subBrand, model, id);
			}
			counter++;
		}

	}

	private static void appendList(BufferedWriter writer, int brand, int subBrand, int model, long id) throws IOException {
		writer.write(Util.normal(brand, subBrand, model, id));
		writer.newLine();
	}

	static void createConfigData() throws ClassNotFoundException, SQLException, IOException {
		Connection conn = ExportBrand.createConn();
		BufferedWriter tree = Files.newBufferedWriter(Paths.get("brand.tree"));
		BufferedWriter list = Files.newBufferedWriter(Paths.get("brand.list"));
		BufferedWriter names = Files.newBufferedWriter(Paths.get("brand.names"));
//		String sql = "select distinct brand from vehicle_brand where brand=1028";
		String sql = "select distinct brand from vehicle_brand";
		ResultSet rs = conn.createStatement().executeQuery(sql);
		PreparedStatement pstm1 = conn.prepareStatement("select subBrandNameEng from brand_dictionary where brand=? limit 1");

		List<Integer> brands = new ArrayList<>();
		while (rs.next()) {
			int brand = rs.getInt(1);
			brands.add(brand);
//			list.write(String.format("b%04d000000\n", brand));
			tree.write(String.format("b%04d000000 -1\n", brand));
//			pstm1.setInt(1, brand);
//			ResultSet rs1 = pstm1.executeQuery();
//			if (rs1.next()) {
//				String subBrandNameEng = rs1.getString(1);
//				String[] tokens = subBrandNameEng.split("_", 4);
//				names.write(tokens[0] + "_" + tokens[1] + "_" + tokens[2]);
//				names.newLine();
//			}
//			else {
//				names.write(String.format("b%04d000000\n", brand));
//			}
//			rs1.close();
		}
		rs.close();
		tree.flush();
//		names.flush();
//		list.flush();
		int rows = brands.size();

		List<Integer> subBrands = new ArrayList<>();
		PreparedStatement pstm = conn.prepareStatement(
				"select distinct vb.subbrand,  subBrandNameEng from vehicle_brand vb, brand_dictionary bd  "
				+ "where vb.brand=? and vb.subbrand!=0 and vb.brand = bd.brand and vb.subbrand = bd.subbrand order by vb.subbrand ");
		for (int i = 0; i < brands.size(); i++) {
			subBrands.clear();
			int brand = brands.get(i);
			pstm.setInt(1, brand);
			rs = pstm.executeQuery();
			while (rs.next()) {
				int subBrand = rs.getInt(1);
				subBrands.add(subBrand);
				tree.write(String.format("b%04d%03d000 %d\n", brand, subBrand, i));
//				list.write(String.format("b%04d%03d000\n", brand, subBrand));
//				names.write(rs.getString(2)); names.newLine();
			}
			rs.close();
			 
			// model
			int batch = 0;
			PreparedStatement pstm2 = conn.prepareStatement(
					"select distinct vb.model,  fullNameEng from vehicle_brand vb left join brand_dictionary bd  "
					+ " on  vb.brand=bd.brand and  vb.subbrand = bd.subbrand and vb.model = bd.model"
					+ " where vb.brand=? and vb.subBrand=? and vb.model!=0"
					+ " order by model");
			ResultSet rs2;
			for (int j = 0; j < subBrands.size(); j++) {
				int subBrand = subBrands.get(j);
				pstm2.setInt(1, brand);
				pstm2.setInt(2, subBrand);
				rs2 = pstm2.executeQuery();

				while (rs2.next()) {
					int model = rs2.getInt(1);
					tree.write(String.format("b%04d%03d%03d %d\n", brand, subBrand, model, rows + j));
					list.write(String.format("b%04d%03d%03d\n", brand, subBrand, model));
					String name = rs2.getString(2);
					names.write(name == null? "unkonwn" : name);
					names.newLine();
					batch++;
				}
				rs2.close();
			}
			rows += batch + subBrands.size();
		}

		tree.flush();
		names.flush();
		list.flush();
		tree.close();
		conn.close();
	}
}
