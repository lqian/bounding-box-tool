package dataset;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class PlateColor {
	
	Path target;
	
	Connection conn;
	
	List<Integer> colors ;
	
	PreparedStatement pstm;
	
	Random rand = new Random();
	
	AtomicBoolean putover = new AtomicBoolean(false);
	
	ExecutorService service = Executors.newFixedThreadPool(8);
	
	CountDownLatch latch;
	
	LinkedBlockingQueue<PlateData> queue = new LinkedBlockingQueue<>(5000);
    AtomicInteger counter = new AtomicInteger();			
	public PlateColor(Path target, Connection conn) throws SQLException {
		super();
		this.target = target;
		this.conn = conn;
		
		pstm = conn.prepareStatement("select path, vehicle_position, plate_position from vehicle_dataset "
				+ " where deprecated = 0 and plate_color=? limit 1000000" );
	}

	public void doDataset() throws Exception {
//		String sql = "select distinct plate_color from vehicle_dataset where deprecated=0";
//		ResultSet rs = conn.createStatement().executeQuery(sql);
//		while (rs.next()) {
//			Integer color = rs.getInt("plate_color");
//			Path catalog = target.resolve(String.format("pc%02d", color));
//			if (Files.notExists(catalog)) {
//				Files.createDirectories(catalog);
//			}
//			colors.add(color);
//		}
//		rs.close();
		int workerNum = 8;
		latch = new CountDownLatch(workerNum);
		for (int i=0; i<workerNum; i++) service.execute(new Worker());
		colors = Arrays.asList(0,1,2,3,5);
		for (Integer color: colors) {
			pubColorCatalog(color);
		}
		putover.set(true);
		pstm.close();
		latch.await();
		service.shutdown();
	}
	
	public void pubColorCatalog(int colorId) throws Exception {
		Path catalog = target.resolve(String.format("pc%02d", colorId));
		pstm.setInt(1, colorId);
		ResultSet rs = pstm.executeQuery();
 		int c = 0;
		while (rs.next()) {
			PlateData pd = new PlateData();
			pd.catalog = catalog;
			pd.source = Paths.get( rs.getString("path"));
			pd.plateBox = Box.parse(rs.getString("plate_position"));
			pd.vehicleBox = Box.parse(rs.getString("vehicle_position"));
			queue.put(pd);
			if (++c % 10000 == 0) {
				System.out.format("put %d samples for %02d catalog \n" , c, colorId);
			}
		}
		rs.close();
		System.out.format("put %d samples for %02d catalog \n" , c, colorId);
	}

	
	 BufferedImage augmentPlate(BufferedImage img, Box vehicleBox, Box plateBox) {
		 int rh = (rand.nextInt(18) + 1) /2;
		 int rw = (rand.nextInt(20) + 1) /2;
		// expand  random 0 - 15 pixel outer
		 int w = img.getWidth();
		 int h = img.getHeight();
		 plateBox.x -= vehicleBox.x;
		 plateBox.y -= vehicleBox.y;
		 
		 plateBox.w += rw*2;
		 plateBox.h += rh*2;
		 plateBox.x -= rw;
		 plateBox.y -= rh;
		 if (plateBox.x < 0) plateBox.x = 0;
		 if (plateBox.y < 0) plateBox.y = 0;
		 if (plateBox.x + plateBox.w > w ) plateBox.w = w - plateBox.x -1;
		 if (plateBox.y + plateBox.h > h ) plateBox.h = h - plateBox.h -1;
		 
		 if (plateBox.x + plateBox.w > w || plateBox.y + plateBox.h > h ) return null;
		 return (plateBox.h == 0 || plateBox.w ==0) ?  null :
			 img.getSubimage((int)plateBox.x, (int)plateBox.y, (int)plateBox.w, (int)plateBox.h);
	}
	 class PlateData {
		 Path catalog;
		 Path source;
		 Box vehicleBox;
		 Box plateBox;
	 }
	 
	 class Worker implements Runnable {

		@Override
		public void run() {
			while (!putover.get()) {
				PlateData pd = null;
				try {
					while ((pd = queue.poll(100, TimeUnit.MICROSECONDS))!= null)
					{
						Path source = pd.source;
						Box plateBox = pd.plateBox;
						Box vehicleBox = pd.vehicleBox;
						if (Files.exists(source) && plateBox != null && vehicleBox != null) {
							BufferedImage img = ImageIO.read(source.toFile());
							if (img == null || img.getHeight() == 0 || img.getWidth() == 0) continue;
							BufferedImage plateImg = augmentPlate(img, vehicleBox, plateBox);
							if (plateImg != null) {
								ImageIO.write(plateImg, "JPG", pd.catalog.resolve(String.format("%09d.jpg", counter.incrementAndGet())).toFile()); 
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			latch.countDown();
		}
	 }
	 
	public static void main(String[] args) throws Exception {
		Path target = Paths.get(args[0]);
		Connection cnn = Util.createConn();
		new PlateColor(target, cnn).doDataset();
		cnn.close();
	}
}
