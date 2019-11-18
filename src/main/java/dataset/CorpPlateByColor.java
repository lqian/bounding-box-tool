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
import org.opencv.highgui.HighGui;
//import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;;

public class CorpPlateByColor {

	static int H = 32;
	static int W = 128;
	Path dataroot;
	String metaList;
	String datasetName;

	Connection conn;

	List<String> colors;
	List<String> provinces;

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

	/**
	 * remove int startSeq,
	 * @param target
	 * @param datasetName
	 * @param conn
	 * @param label
	 * @throws SQLException
	 * @throws IOException
	 */
	public CorpPlateByColor(Path target, String datasetName, Connection conn,  Path label) throws SQLException, IOException {
		super();
		this.dataroot = target;
		
		this.conn = conn;
		this.datasetName = datasetName;
		counter = new AtomicInteger(0);
		metaList = dataroot.resolve(datasetName +".list").toAbsolutePath().toString();
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
				+ " where  plate_color=?  and province=? limit 300000"); //3060000

		// 普通黑牌
		//		pstm = conn.prepareStatement("select path, plate_nbr, vehicle_position, plate_position from vehicle_dataset "
		//				+ "where plate_color=3 and instr(plate_nbr, '粤') = 1 and substring(plate_nbr, 1,1) =? limit 2000");

		colors = Arrays.asList("0", "1", "2", "3", "4", "5", "6");
		provinces = Arrays.asList("云", "京", "冀", "吉", "宁", "川", "新", "晋", "桂", "沪", "津", "浙", "渝", "湘", "琼", "甘", "皖",
				"粤", "苏", "蒙", "藏", "豫", "贵", "赣", "辽", "鄂", "闽", "陕", "青", "鲁", "黑");
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
		int workerNum = 12;
		latch = new CountDownLatch(workerNum);
		service.execute(new MetaWriter());
		for (int i=0; i<workerNum; i++) service.execute(new Worker());
		
		for (int i=0; i<2; i++) { //only  0,1 
			doublePlate(i);
		}
//		doublePlate();
//		for (int i=0; i<colors.size(); i++) {
//			colorCatalog(i);
//		}

		putover.set(true);
		pstm.close();
		latch.await();
		service.shutdown();
	}

	void appendData(ResultSet rs, Path catalog, int plateColor)  throws Exception {
		while (rs.next()) {
			PlateData pd = new PlateData();
			pd.catalog = catalog;
			pd.source = Paths.get( rs.getString("path"));
			pd.plateBox = Box.parse(rs.getString("plate_position"));
			pd.vehicleBox = Box.parse(rs.getString("vehicle_position"));
			pd.plateNbr = rs.getString("plate_nbr");
			pd.plateColor = plateColor;
			queue.put(pd);
		}
	}
	
	public void doublePlate(int id) throws Exception {
		Path catalog = dataroot.resolve(datasetName).resolve(String.format("%02d", id));
		pstm = conn.prepareStatement("select path, plate_nbr, vehicle_position, plate_position from vehicle_dataset "
				+ " where  plate_color=? and plate_height/plate_width> .3");
		pstm.setInt(1, id);
		ResultSet rs = pstm.executeQuery();
		appendData(rs, catalog, id);
		rs.close();
	}
	public void colorCatalog(int id) throws Exception {
		Path catalog = dataroot.resolve(datasetName).resolve(String.format("%02d", id));
		if (Files.notExists(catalog)) {
			Files.createDirectories(catalog);
		}
		String colorId = colors.get(id);
		if (colorId.equals("2") || colorId.equals("1")) {
			pstm = conn.prepareStatement("select path, plate_nbr, vehicle_position, plate_position from vehicle_dataset "
					+ " where  plate_color=?  and province=? limit 300000"); //3060000
			for (String p: provinces) {
				pstm.setString(1, colorId);
				pstm.setString(2, p);
				ResultSet rs = pstm.executeQuery();
				appendData(rs, catalog, id);
				rs.close();
			}
		}
		else {
			pstm = conn.prepareStatement("select path, plate_nbr, vehicle_position, plate_position from vehicle_dataset "
					+ " where  plate_color=? limit 300000"); 
			pstm.setString(1, colorId);
			ResultSet rs = pstm.executeQuery();
			appendData(rs, catalog, id);
			rs.close();
		}
	}

	/**
	 * @deprecated
	 * @param max
	 * @return
	 */
	public static int randXOffset(float max) {
		int sign = rand.nextInt(2) == 0 ? -1: 1;
		int halfCharSize = (int) (Math.floor(max/12) + 1);
		int rw = rand.nextInt(halfCharSize);
		
		return sign * rw;
	}

	/**
	 * @deprecated
	 * @param height
	 * @return
	 */
	public static int randYOffset(float height) {
		int sign = rand.nextInt(2) == 0 ? -1: 1;
		int halfCharSize =  (int) (Math.floor(height/4) + 1);
		if (sign < 0) {
			halfCharSize =  (int) (Math.floor(height/8) + 1);
		}
		int rw = rand.nextInt(halfCharSize);
		return sign * rw;
	}

	static float randomRange(float low, float hight) {
		int max = (int)Math.floor(hight - low);
		int r = rand.nextInt(max);
		return r - low;
	}
	public static Mat perspectiveTransAugment(Mat img, Box vehicleBox, Box plateBox, boolean debug) {
		
		// expand  random 0 - 15 pixel outer
		int w = img.cols();
		int h = img.rows();
		int rh = w / 10;
		int rw = h / 16;
		plateBox.x -= vehicleBox.x;
		plateBox.y -= vehicleBox.y; 
		
		plateBox.w += rw*2;
		plateBox.h += rh*2;
		plateBox.x -= rw;
		plateBox.y -= rh;
		
		if (plateBox.x < 0 || plateBox.y < 0 || plateBox.x + plateBox.w > w || plateBox.y + plateBox.h > h ) return null;

		Point p0 = new Point(plateBox.x + randomRange(-rw*2, rw), plateBox.y + randomRange(-rh*2, rh));
		Point p1 = new Point(plateBox.x + plateBox.w + randomRange(-rw, rw*2), plateBox.y + randomRange(-rh*2, rh));
		Point p2 = new Point(plateBox.x  + plateBox.w  + randomRange(-rw, rw*2), plateBox.y + plateBox.h + randomRange(-rh, rh*2));
		Point p3 = new Point(plateBox.x + randomRange(-rw*2, rw) , plateBox.y + plateBox.y + randomRange(-rh, rh*2));
		
		if (p0.x<0 || p0.y<0 || p1.x>w || p1.y < 0 || p2.x > 2 || p2.y>h || p3.x < 0 || p3.y > h) return null;
		
		if (debug) {
			Scalar color = new Scalar(0, 15, 235);
			Point pt1 = new Point(plateBox.x, plateBox.y);
			Point pt2 = new Point(plateBox.x + plateBox.w, plateBox.y+ plateBox.h);
			Imgproc.rectangle(img, pt1, pt2, color);
			Imgproc.circle(img, p0, 3, color);
			Imgproc.circle(img, p1, 3, color);
			Imgproc.circle(img, p2, 3, color);
			Imgproc.circle(img, p3, 3, color);
			HighGui.imshow("rect and points", img); 
		}

//		if (p0.x < 0) p0.x = 0;
//		if (p0.y < 0) p0.y = 0;
//		if (p1.y < 0) p1.y = 0;
//		if (p1.x > vehicleBox.w-1) p1.x = vehicleBox.w - 1;
//		if (p2.x > vehicleBox.w - 1) p2.x = vehicleBox.w - 1;
//		if (p2.y > vehicleBox.y - 1) p2.y = vehicleBox.y - 1;
//		if (p3.x < 0) p3.x = 0;
//		if (p3.y > vehicleBox.h - 1) p3.y = vehicleBox.h - 1;
		//random half-char size of width and 1/8 ~ 1/4 char-size of height

		Point dp0 = new Point(0, 0);
		Point dp1 = new Point(W, 0);
		Point dp2 = new Point(W, H);
		Point dp3 = new Point(0, H);

		List<Point>src = Arrays.asList(p0, p1, p2, p3);
		List<Point>dst = Arrays.asList(dp0, dp1, dp2, dp3);
		Mat dstMat = new Mat(H, W, img.type());
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
		int plateColor;
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
								Mat dst = perspectiveTransAugment(img, vehicleBox, plateBox, false);
								if (dst!=null && !dst.empty()) {
									int seq = counter.incrementAndGet();
									int sub = seq / 10000;
									int no = seq % 10000;								 
									pd.target = Paths.get(String.format("%s/%06d/%06d-%s.jpg", pd.catalog, sub, no, pd.plateNbr));
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
									pd.target = Paths.get(String.format("%s/%06d/%06d-%s.jpg", pd.catalog, sub, no, pd.plateNbr));
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

		String convertAsLabel(String plateNbr, int plateColor) {
			StringBuilder label = new StringBuilder();
			int len = plateNbr.length();
			for (int i=0; i<len && i < 8; i++) {
				label.append(labelMap.get(plateNbr.substring(i, i+1)) + " ");	
			}
			for (int i=0; i<8-len; i++) label.append("0 ");
			return String.format("%s %02d", label.toString(), plateColor);
		}

		@Override
		public void run() {
			int c = 0;
			try (BufferedWriter meta = Files.newBufferedWriter(Paths.get(metaList))){
				while(latch.getCount() > 0) {
					PlateData pd = null;
					try {
						while ((pd = completedQueue.poll(100, TimeUnit.MICROSECONDS))!= null) {
							meta.write(dataroot.relativize(pd.target).toString() + " " + convertAsLabel(pd.plateNbr, pd.plateColor));
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
		if (Files.notExists(target)) {
			Files.createDirectories(target);
		}
		String datasetName = args[1];
		Connection cnn = Util.createConn();
		Path label = Paths.get(args[2]);
		new CorpPlateByColor(target, datasetName, cnn, label).doDataset();
		cnn.close();
	}

}
