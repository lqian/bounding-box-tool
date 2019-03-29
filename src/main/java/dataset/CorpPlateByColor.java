package dataset;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class CorpPlateByColor {

	Path dataroot;

	Connection conn;

	List<String> colors;
	
	Map<String, Integer> labelMap = new HashMap<>();

	PreparedStatement pstm;

	Random rand = new Random();

	AtomicBoolean putover = new AtomicBoolean(false);

	ExecutorService service = Executors.newFixedThreadPool(8);

	CountDownLatch latch;

	LinkedBlockingQueue<PlateData> queue = new LinkedBlockingQueue<>(5000);

	LinkedBlockingQueue<PlateData> completedQueue = new LinkedBlockingQueue<>(5000);

	AtomicInteger counter;

	public CorpPlateByColor(Path target, Connection conn, int startSeq, Path label) throws SQLException, IOException {
		super();
		this.dataroot = target;
		this.conn = conn;
		counter = new AtomicInteger(0);
		
		BufferedReader reader = Files.newBufferedReader(label);
		String key;
		int id =0;
		while ((key = reader.readLine())!=null) {
			labelMap.put(key, id++);
		}
		System.out.println("put " + id + " keys");
		
//		pstm = conn.prepareStatement("select path, plate_nbr, vehicle_position, plate_position from vehicle_dataset "
//				+ " where substring(plate_nbr, 1,1) =? and plate_color=1 limit 100" );
		
		pstm = conn.prepareStatement("select path, plate_nbr, vehicle_position, plate_position from vehicle_dataset "
				+ " where  plate_color=?  limit 3000000");
		
		
	// 普通黑牌
//		pstm = conn.prepareStatement("select path, plate_nbr, vehicle_position, plate_position from vehicle_dataset "
//				+ "where plate_color=3 and instr(plate_nbr, '粤') = 1 and substring(plate_nbr, 1,1) =? limit 2000");

		colors = Arrays.asList("0", "1", "2", "3", "4", "5", "6");
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
		service.execute(new MetaWriter());
		for (int i=0; i<workerNum; i++) service.execute(new Worker());
		for (int i=0; i<colors.size(); i++) {
			provinceCatalog(i);
		}
		 
		putover.set(true);
		pstm.close();
		latch.await();
		service.shutdown();
	}

	public void provinceCatalog(int id) throws Exception {
		Path catalog = dataroot.resolve(String.format("%02d", id));
		if (Files.notExists(catalog)) {
			Files.createDirectories(catalog);
		}
		String province = colors.get(id);
		pstm.setString(1, province);
		ResultSet rs = pstm.executeQuery();
		int c = 0;
		while (rs.next()) {
			PlateData pd = new PlateData();
			pd.catalog = catalog;
			pd.source = Paths.get( rs.getString("path"));
			pd.plateBox = Box.parse(rs.getString("plate_position"));
			pd.vehicleBox = Box.parse(rs.getString("vehicle_position"));
			pd.plateNbr = rs.getString("plate_nbr");
			queue.put(pd);
		}
		rs.close();
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
		String plateNbr;
		Path catalog;
		Path source;
		Path target;
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
								int seq = counter.incrementAndGet();
								int sub = seq / 10000;
								int no = seq % 10000;								 
								pd.target = Paths.get(String.format("%s/%06d/%06d.jpg", pd.catalog, sub, no));
								Path dir = pd.target.getParent();
								if (Files.notExists(dir)) Files.createDirectories(dir);
								ImageIO.write(plateImg, "JPG", pd.target.toFile()); 
								completedQueue.put(pd);
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

	
	class MetaWriter implements Runnable {
		
		String convertAsLabel(String plateNbr) {
			StringBuilder label = new StringBuilder();
			int len = plateNbr.length();
			for (int i=0; i<len && i < 8; i++) {
				label.append(labelMap.get(plateNbr.substring(i, i+1)) + " ");	
			}
			for (int i=0; i<8-len; i++) label.append("0 ");
			return label.toString();
		}
		@Override
		public void run() {
			int c = 0;
			try (BufferedWriter meta = Files.newBufferedWriter(Paths.get("meta.list"))){
				while(latch.getCount() > 0) {
					PlateData pd = null;
					try {
						while ((pd = completedQueue.poll(100, TimeUnit.MICROSECONDS))!= null) {
							meta.write(dataroot.relativize(pd.target).toString() + " " + convertAsLabel(pd.plateNbr));
							meta.newLine();
							c++;
							
							if (c % 1000 == 0) {
								System.out.format("write %d samples for catalog \n" , c);
							}
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			System.out.println("write lines to meta: " + c);
		}
	}

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws  
	 */
	public static void main(String[] args) throws Exception {
		Path target = Paths.get(args[0]);
		Connection cnn = Util.createConn();
		Path label = Paths.get(args[2]);
		new CorpPlateByColor(target, cnn, Integer.valueOf(args[1]), label).doDataset();
		cnn.close();
	}

}
