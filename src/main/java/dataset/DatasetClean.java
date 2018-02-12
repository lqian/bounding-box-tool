/**
 * 
 */
package dataset;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author link
 *
 */
public class DatasetClean {

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8&rewriteBatchedStatements=true",
				"root", "123456");
		Statement stm = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		ResultSet rs = stm.executeQuery("select id, path, vehicle_sub_brand, vehicle_model from vehicle_dataset");

		while (rs.next()) {
			File f = new File(rs.getString("path"));
			if (!f.exists()) {
				rs.deleteRow();
			}
			else if (f.isDirectory()) {
				delete(rs, f);
			} else if (f.isFile() && f.length() < 80000) {
				delete(rs, f);
			}
			else  {
				int subBrand = rs.getInt("vehicle_sub_brand");
				int model = rs.getInt("vehicle_model");
				
				if (subBrand == 0 || model == 0) {
					delete(rs, f);
				}
			}
			
		}
		rs.close();
	}

	private static void delete(ResultSet rs, File f) throws SQLException {
		f.delete();
		rs.deleteRow();
		System.out.println("delete file: " + f);
	}
}
