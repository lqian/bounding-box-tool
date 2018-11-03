package dataset;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

public class PlateColor {
	
	Path target;
	
	Connection conn;
	
	ArrayList<Integer> colors = new ArrayList<>();
	
	PreparedStatement pstm;
	
	 Random rand = new Random();
	
	public PlateColor(Path target, Connection conn) throws SQLException {
		super();
		this.target = target;
		this.conn = conn;
		
		pstm = conn.prepareStatement("select path, vehicle_position, plate_position from vehicle_dataset "
				+ " where deprecated = 0 and plate_color=? limit 1000000" );
	}

	public void doDataset() throws Exception {
		String sql = "select distinct plate_color from vehicle_dataset where deprecated=0";
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while (rs.next()) {
			Integer color = rs.getInt("plate_color");
			Path catalog = target.resolve(String.format("pc%02d", color));
			if (Files.notExists(catalog)) {
				Files.createDirectories(catalog);
				colors.add(color);
			}
		}
		rs.close();
		
		for (Integer color: colors) {
			doColorCatalog(color);
		}
	}
	
	public void doColorCatalog(int colorId) throws Exception {
		Path catalog = target.resolve(String.format("pc%02d", colorId));
		pstm.setInt(1, colorId);
		ResultSet rs = pstm.executeQuery();
		int c = 0;
		while (rs.next()) {
			Path source = Paths.get( rs.getString("path"));
			Box plateBox = Box.parse(rs.getString("plate_position"));
			Box vehicleBox = Box.parse(rs.getString("vehicle_position"));
			if (Files.exists(source)) {
				BufferedImage img = ImageIO.read(source.toFile());
				BufferedImage plateImg = augmentPlate(img, vehicleBox, plateBox);
				if (plateImg != null) {
					ImageIO.write(plateImg, "JPG", catalog.resolve(String.format("%09d.jpg", c++)).toFile());
				}
			}
		}
		rs.close();
		System.out.format("generate %d samples for %02d catalog \n" , c, colorId);
	}

	
	 BufferedImage augmentPlate(BufferedImage img, Box vehicleBox, Box plateBox) {
		 int rh = (rand.nextInt(15) + 1)/2;
		 int rw = (rand.nextInt(20) + 1) /2;
		// expand  random 0 - 15 pixel outer
		 int w = img.getWidth();
		 int h = img.getHeight();
		 plateBox.w += rw*2;
		 plateBox.h += rh*2;
		 plateBox.x -= rw;
		 plateBox.y -= rh;
		 if (plateBox.x < 0) plateBox.x = 0;
		 if (plateBox.y < 0) plateBox.y = 0;
		 if (plateBox.x + plateBox.w > w ) plateBox.w = w - plateBox.x;
		 if (plateBox.y + plateBox.h > h ) plateBox.h = h - plateBox.h;
		 return (plateBox.h == 0 || plateBox.w ==0) ?  null :
			 img.getSubimage((int)plateBox.x, (int)plateBox.y, (int)plateBox.w, (int)plateBox.h);
	}

	public static void main(String[] args) throws Exception {
		Path target = Paths.get(args[0]);
		Connection cnn = Util.createConn();
		new PlateColor(target, cnn).doDataset();

	}

}
