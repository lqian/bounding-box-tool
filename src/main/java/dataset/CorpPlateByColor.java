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

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
//import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;;

public class CorpPlateByColor {

	Path dataroot;

	Connection conn;

	List<String> colors;

	Map<String, Integer> labelMap = new HashMap<>();

	PreparedStatement pstm;

	static Random rand = new Random();

	AtomicBoolean putover = new AtomicBoolean(false);

	ExecutorService service = Executors.newFixedThreadPool(8);

	CountDownLatch latch;

	LinkedBlockingQueue<PlateData> queue = new LinkedBlockingQueue<>(5000);

	LinkedBlockingQueue<PlateData> completedQueue = new LinkedBlockingQueue<>(5000);

	AtomicInteger counter;

	boolean perspectiveTransform = true;

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
				+ " where  plate_color=?  limit 1000");

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

	public static int randXOffset(float w) {
		int halfCharSize = (int) (Math.floor(w/12) + 1);
		int rw = rand.nextInt(halfCharSize);
		int sign = rand.nextInt(2) == 0 ? -1: 1;
		return sign * rw;
	}

	public static int randYOffset(float height) {
		int sign = rand.nextInt(2) == 0 ? -1: 1;
		int halfCharSize =  (int) (Math.floor(height/4) + 1);
		if (sign < 0) {
			halfCharSize =  (int) (Math.floor(height/8) + 1);
		}
		int rw = rand.nextInt(halfCharSize);
		return sign * rw;
	}

	public static Mat augPlate(Mat img, Box vehicleBox, Box plateBox, boolean debug) {
		int rh = (rand.nextInt(18) + 1) /2;
		int rw = (rand.nextInt(20) + 1) /2;
		// expand  random 0 - 15 pixel outer
		int w = img.cols();
		int h = img.rows();
		plateBox.x -= vehicleBox.x;
		plateBox.y -= vehicleBox.y;

		plateBox.w += rw*2;
		plateBox.h += rh*2;
		plateBox.x -= rw;
		plateBox.y -= rh;
		
		if (plateBox.x < 0 || plateBox.y < 0 || plateBox.x + plateBox.w > w || plateBox.y + plateBox.h > h ) return null;

		Point p0 = new Point(plateBox.x + randXOffset(plateBox.w), plateBox.y + randYOffset(plateBox.h));
		Point p1 = new Point(plateBox.x + plateBox.w + randXOffset(plateBox.w), plateBox.y + randYOffset(plateBox.h));
		Point p2 = new Point(plateBox.x  + plateBox.w  + randXOffset(plateBox.w), plateBox.y + plateBox.h + randYOffset(plateBox.h));
		Point p3 = new Point(plateBox.x + randXOffset(plateBox.w) , plateBox.y + plateBox.h + randYOffset(plateBox.h));

		if (debug) {
			Scalar color = new Scalar(0, 15, 235);
			Point pt1 = new Point(plateBox.x, plateBox.y);
			Point pt2 = new Point(plateBox.x + plateBox.w, plateBox.y+ plateBox.h);
			Imgproc.rectangle(img, pt1, pt2, color);
			Imgproc.circle(img, p0, 3, color);
			Imgproc.circle(img, p1, 3, color);
			Imgproc.circle(img, p2, 3, color);
			Imgproc.circle(img, p3, 3, color);
//			HighGui.imshow("rect and points", img); 
		}

		if (p0.x < 0) p0.x = 0;
		if (p0.y < 0) p0.y = 0;
		if (p1.y < 0) p1.y = 0;
		if (p1.x > vehicleBox.w-1) p1.x = vehicleBox.w - 1;
		if (p2.x > vehicleBox.w - 1) p2.x = vehicleBox.w - 1;
		if (p2.y > vehicleBox.y - 1) p2.y = vehicleBox.y - 1;
		if (p3.x < 0) p3.x = 0;
		if (p3.y > vehicleBox.h - 1) p3.y = vehicleBox.h - 1;
		//random half-char size of width and 1/8 ~ 1/4 char-size of height

		Point dp0 = new Point(0, 0);
		Point dp1 = new Point(128, 0);
		Point dp2 = new Point(128, 32);
		Point dp3 = new Point(0, 32);

		List<Point>src = Arrays.asList(p0, p1, p2, p3);
		List<Point>dst = Arrays.asList(dp0, dp1, dp2, dp3);
		Mat dstMat = new Mat(32, 128, img.type());
		Mat perspectiveMmat = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(src),
				Converters.vector_Point2f_to_Mat(dst));
		Imgproc.warpPerspective(img, dstMat, perspectiveMmat, dstMat.size(), Imgproc.INTER_LINEAR);
		return dstMat;
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
		int innerCounter = 0;
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
							if (++innerCounter % 4 == 0) {
								Mat img = Imgcodecs.imread(source.toString());
								if (img.empty()) continue;
								Mat dst = augPlate(img, vehicleBox, plateBox, false);
								if (dst!=null && !dst.empty()) {
									int seq = counter.incrementAndGet();
									int sub = seq / 10000;
									int no = seq % 10000;								 
									pd.target = Paths.get(String.format("%s/%06d/%06d.jpg", pd.catalog, sub, no));
									Path dir = pd.target.getParent();
									if (Files.notExists(dir)) Files.createDirectories(dir);
									Imgcodecs.imwrite(pd.target.toString(), dst); 
									completedQueue.put(pd);
								}
							}
							else {
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
		nu.pattern.OpenCV.loadShared();
		System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
		
		Path target = Paths.get(args[0]);
		Connection cnn = Util.createConn();
		Path label = Paths.get(args[2]);
		new CorpPlateByColor(target, cnn, Integer.valueOf(args[1]), label).doDataset();
		cnn.close();
	}

}
