package dataset;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

public class ExportPlate {

	Connection conn;

	Path target;

	Map<String, Path> prefixs = new HashMap<>();

	ExecutorService service = Executors.newFixedThreadPool(12);

	public ExportPlate(String target, int threadNums) throws Exception {
		this.target = Paths.get(target);
		service = Executors.newFixedThreadPool(threadNums);
		conn = Util.createConn();
	}

	void export() throws Exception {
		String sql = "select id, path, plate_nbr, plate_position from vehicle_dataset where plate_nbr != '车牌'"
				+ "and plate_nbr not regexp '^[ABCDEFGHIJKLMNOPQRSTUVXYZ].+' limit 10000";
 
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while (rs.next()) {
			long id = rs.getLong("id");
			String path = rs.getString("path");
			String pr = rs.getString("plate_nbr");
			String pp = rs.getString("plate_position");

			String prefix = pr.substring(0, 1);
			if (prefix.equalsIgnoreCase("W")) {
				prefix = "WJ";
			}

			Path sub = target.resolve(prefix);
			if (!prefixs.containsKey(prefix)) {
				Files.createDirectories(sub);
				prefixs.put(prefix, sub);
			}

			service.submit(new Task(id, Paths.get(path), sub, pr, pp));
		}
	}

	class Task implements Runnable {

		long id;
		Path source;
		Path sub;
		String plateNbr;
		String platePosistion;

		public Task(long id, Path source, Path sub, String plateNbr, String platePosistion) {
			super();
			this.id = id;
			this.source = source;
			this.sub = sub;
			this.plateNbr = plateNbr;
			this.platePosistion = platePosistion;
		}

		@Override
		public void run() {
			try {
				BufferedImage image = ImageIO.read(source.toFile());
				String normalName = String.format("%s_%08d.JPG", plateNbr, id);
				Path target = sub.resolve(sub).resolve(normalName);
				if (Files.exists(target)) return;
				String tokens[] = platePosistion.split("[\\s,]", 4);
				if (tokens.length == 4) {
					AffineTransform affineTransform = new AffineTransform();
					int x = Integer.valueOf(tokens[0]);
					int y = Integer.valueOf(tokens[1]);
					int w = Integer.valueOf(tokens[2]);
					int h = Integer.valueOf(tokens[3]);
					if (x>5 && y>5) { 
						x -=5;
						y -=5;
						if (x + w + 10 < image.getWidth()) {
							w +=10;
							if (y + h +10 < image.getHeight()) {
								h +=10;
								BufferedImage subImage = image.getSubimage(x, y, w, h);
 
								ImageIO.write(subImage, "jpg", target.toFile());
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws NumberFormatException, Exception {
		new ExportPlate(args[0], Integer.valueOf(args[1])).export();
	}
}
