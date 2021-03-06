/**
 * 
 */
package dataset;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

/**
 * @author lqian
 *
 */
public class ExportBrand {
	
	static ExecutorService service = Executors.newFixedThreadPool(6);

	static Path home;
	
	

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws Exception {
		home = args.length > 0 ? Paths.get(args[0]) : Paths.get("vehicle-brand-dataset");

		Connection conn = Util.createConn();
		// conn.setAutoCommit(false);

		String summarizeSQL = "select distinct vehicle_brand, vehicle_sub_brand, vehicle_model from vehicle_dataset";
		Statement stm = conn.createStatement();
		ResultSet rs = stm.executeQuery(summarizeSQL);
		ArrayList<BrandEntity> list = new ArrayList<>();
		while (rs.next()) {
			BrandEntity be = new BrandEntity();
			be.brand = rs.getInt(1);
			be.subBrand = rs.getInt(2);
			be.model = rs.getInt(3);
			list.add(be);
		}
		rs.close();
		stm.close();

		for (BrandEntity be : list) {
			Path dir = home.resolve(be.brand + "").resolve(be.subBrand + "").resolve(be.model + "");
			if (Files.notExists(dir)) {
				Files.createDirectories(dir);
			}
		}

		String sql = "select id, path, vehicle_position from vehicle_dataset where vehicle_brand=? and vehicle_sub_brand=? and vehicle_model=?";
		PreparedStatement pstm = conn.prepareStatement(sql);
		for (BrandEntity be : list) {
			pstm.setInt(1, be.brand);
			pstm.setInt(2, be.subBrand);
			pstm.setInt(3, be.model);

			rs = pstm.executeQuery();
			AtomicInteger count = new AtomicInteger(0);
			rs.last();
			int rows = rs.getRow();
			if (rows > 0) {
				CountDownLatch latch = new CountDownLatch(rows);
				rs.first();
				do {
					long id = rs.getLong("id");
					String path = rs.getString("path");
					String vehiclePosition = rs.getString("vehicle_position");
					service.submit(new Corp(be, path, id, vehiclePosition, latch, count));
				} while (rs.next());
				latch.await();
				rs.close();
			}
			System.out.format("corp %d/ %d image for %s %n", count.get(), rows, be);
		}
		pstm.close();
		
	}

	

	static class Corp implements Runnable {
		
		BrandEntity brandEntity;
		String path;
		long id;
		String vehiclePosition;
		AtomicInteger count;
		CountDownLatch latch;

		public Corp(BrandEntity brandEntity, String path, long id, String vehiclePosition, CountDownLatch latch, AtomicInteger count) {
			super();
			this.brandEntity = brandEntity;
			this.path = path;
			this.id = id;
			this.vehiclePosition = vehiclePosition;
			this.latch = latch;
			this.count = count;
		}


		@Override
		public void run() {
			try {

				File file = new File(path);
				if (file.isFile()) {
					long len = file.length();
					if (len <= 80000) {

					} else {
						File tf = home.resolve(Util.normal(brandEntity, id)).toFile();
						if (!tf.exists()) {
							BufferedImage image = ImageIO.read(file);
							String tokens[] = vehiclePosition.split("[\\s,]", 4);
							if (tokens.length == 4) {
								int x = Integer.valueOf(tokens[0]);
								int y = Integer.valueOf(tokens[1]);
								int w = Integer.valueOf(tokens[2]);
								int h = Integer.valueOf(tokens[3]);
								BufferedImage subImage = image.getSubimage(x, y, w, h);
								ImageIO.write(subImage, "jpg", tf);
								if (count != null)
								count.incrementAndGet();
							}
						}
					}
				}
			} catch (IOException e) {
				System.err.println(e.toString());
			}
			latch.countDown();
		}
	}

	static class BrandEntity implements Comparable<BrandEntity> {
		int brand;
		int subBrand;
		int model;

		
		
		public BrandEntity() {
			super();
		}

		public BrandEntity(int brand, int subBrand, int model) {
			super();
			this.brand = brand;
			this.subBrand = subBrand;
			this.model = model;
		}

		@Override
		public String toString() {
			return "[brand=" + brand + ", subBrand=" + subBrand + ", model=" + model + "]";
		}

		@Override
		public int compareTo(BrandEntity o) {
			int db = brand - o.brand;
			if (db == 0) {
				int ds = subBrand - o.subBrand;
				if (ds == 0) {
					int dm = model - o.model;
					return dm > 0 ? 1 : dm == 0 ? 0 : -1;
				} else {
					return ds > 0 ? 1 : -1;
				}
			} else
				return db > 0 ? 1 : -1;
		}
	}
}
