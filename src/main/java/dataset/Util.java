/**
 * 
 */
package dataset;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import dataset.ExportBrand.BrandEntity;

/**
 * @author link
 *
 */
public class Util {
	
	public static String normal(BrandEntity be, long id) {
		return  normal(be.brand, be.subBrand, be.model, id);
	}
	
	public static String normal(int brand, int subBrand, int model, long id) {
		return  String.format("%1$d/%2$d/%3$d/b%1$04d%2$03d%3$03d_%4$08d.jpg", brand, subBrand, model, id);
	}
	
	public static void  main(String[] args) {
		System.out.println(normal(1,2,3,4L));
	}
	
	public static Connection createConn() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/corpus?useUnicode=yes&characterEncoding=utf8&rewriteBatchedStatements=true&autoReconnect=true",
				"root", "");
		return conn;
	}
	

}
