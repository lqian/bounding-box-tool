package dataset;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VehicleObjectDetect {
	Path meta;  	// meta path, file line content is json format
	Path cfg;  		// path to store yolo style train, val, label list
	
	BufferedWriter trainList;
	BufferedWriter valList;
	BufferedWriter labels;
	Map<String, String> types = new HashMap<>();
	public VehicleObjectDetect(Path meta, Path cfg) throws Exception {
		super();
		this.meta = meta;
		this.cfg = cfg;
		
		trainList = Files.newBufferedWriter(cfg.resolve("train.list"));
		valList = Files.newBufferedWriter(cfg.resolve("val.list"));
		labels = Files.newBufferedWriter(cfg.resolve("labels"));
	} 
	
	void execute() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true);
		BufferedReader reader = Files.newBufferedReader(meta);
		String line = null;
		int c = 0;
		while ((line = reader.readLine()) != null) {
			LineJson json = mapper.readValue(line, LineJson.class);
			Path samplePath = Paths.get(json.image);
			List<ImageResult> results = json.seemooResult.imageResults;
			if (Files.exists(samplePath) && results != null && results.size() > 0) {
				BufferedImage image = ImageIO.read(samplePath.toFile());
				float iw = image.getWidth();
				float ih = image.getHeight();
				Path p = Paths.get(json.image.replaceFirst("\\.jpg", ".txt").replaceFirst("JPEGImages", "labels") );
				Path dir = p.getParent();
				
//				if (Files.notExists(dir)) {
//					Files.createDirectories(dir);
//				}
				c++;
				List<Vehicle> vehicles = results.get(0).vehicles;
				if (vehicles !=null && vehicles.size() > 0) {
					BufferedWriter label = Files.newBufferedWriter(p);
					for (Vehicle v: vehicles) {
						List<Integer> rect = v.detect.body.Rect;
						Top top = v.recognize.type.topList.get(0);
						// center of box relative to samples' width and height
						float yx = ((2 * rect.get(0) + rect.get(2)) / 2 -1 ) / iw;  
						float yy = ((2 * rect.get(1) + rect.get(3)) / 2 -1 ) / ih;
						float yw = rect.get(2) / iw;
						float yh = rect.get(3) / ih;
						String outLine = String.format("%d %f %f %f %f", Integer.parseInt(top.code)-1, yx, yy, yw, yh);
						label.write(outLine);
						label.newLine();
					}
					label.close();
				}
				if (c % 1000 == 0) {
					System.out.format("convert %d samples \n", c);
				}
				if (c % 10 == 0) {
					valList.write(samplePath.toString());
					valList.newLine();
				}
				else {
					trainList.write(samplePath.toString());
					trainList.newLine();
				}
			}
		}
		
		reader.close();
		valList.close();
		trainList.close();
		System.out.format("convert %d train and %d valid samples \n",  c- c/10, c/10 );
	}

	public static void main(String[] args) throws Exception {
		Path meta = Paths.get(args[0]);
		Path cfg = Paths.get(".");
		
		VehicleObjectDetect vod = new VehicleObjectDetect(meta, cfg);
		vod.execute();
		
	}
	
	public static class LineJson {
		String 	image;
		SeemooResult seemooResult;
		
		public String getImage() {
			return image;
		}
		public void setImage(String image) {
			this.image = image;
		}
		public SeemooResult getSeemooResult() {
			return seemooResult;
		}
		public void setSeemooResult(SeemooResult seemooResult) {
			this.seemooResult = seemooResult;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SeemooResult {
		@JsonProperty("Code")
		int code;
		
		@JsonProperty("Message")
		String message;
		
		@JsonProperty("ImageResults")
		List<ImageResult> imageResults = new ArrayList<ImageResult>();

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public List<ImageResult> getImageResults() {
			return imageResults;
		}

		public void setImageResults(List<ImageResult> imageResults) {
			this.imageResults = imageResults;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static  class ImageResult {
		@JsonProperty("Code")
		int code;
		
		@JsonProperty("Message")
		String message;
		
		@JsonProperty("Vehicles")
		List<Vehicle> vehicles;

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public List<Vehicle> getVehicles() {
			return vehicles;
		}

		public void setVehicles(List<Vehicle> vehicles) {
			this.vehicles = vehicles;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static  class Vehicle {
		@JsonProperty("Detect")
		Detect detect;
		
		@JsonProperty("Recognize")
		Recognize recognize;

		public Detect getDetect() {
			return detect;
		}

		public void setDetect(Detect detect) {
			this.detect = detect;
		}

		public Recognize getRecognize() {
			return recognize;
		}

		public void setRecognize(Recognize recognize) {
			this.recognize = recognize;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static  class Recognize {
		@JsonProperty("mistake")
		int Mistake;
		@JsonProperty("Type")
		RecognObj type;
		@JsonProperty("Color")
		RecognObj color;
		@JsonProperty("Plate")
		PlateRecog plate;
		public int getMistake() {
			return Mistake;
		}
		public void setMistake(int mistake) {
			Mistake = mistake;
		}
		public RecognObj getType() {
			return type;
		}
		public void setType(RecognObj type) {
			this.type = type;
		}
		public RecognObj getColor() {
			return color;
		}
		public void setColor(RecognObj color) {
			this.color = color;
		}
		public PlateRecog getPlate() {
			return plate;
		}
		public void setPlate(PlateRecog plate) {
			this.plate = plate;
		}
	}
	
	public static class Detect {
		@JsonProperty("Code")
		String code;
		@JsonProperty("Message")
		String message;
		@JsonProperty("Car")
		RectObj car;
		@JsonProperty("Body")
		RectObj body;
		@JsonProperty("Plate")
		RectObj plate;
		@JsonProperty("Window")
		RectObj window;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public RectObj getCar() {
			return car;
		}
		public void setCar(RectObj car) {
			this.car = car;
		}
		public RectObj getPlate() {
			return plate;
		}
		public void setPlate(RectObj plate) {
			this.plate = plate;
		}
		public RectObj getWindow() {
			return window;
		}
		public void setWindow(RectObj window) {
			this.window = window;
		}
		public RectObj getBody() {
			return body;
		}
		public void setBody(RectObj body) {
			this.body = body;
		}
		 
	}
	
	public static class RectObj {
		@JsonProperty("Score")
		int Score;
		@JsonProperty("Rect")
		List<Integer> Rect;
		public int getScore() {
			return Score;
		}
		public void setScore(int score) {
			Score = score;
		}
		public List<Integer> getRect() {
			return Rect;
		}
		public void setRect(List<Integer> rect) {
			Rect = rect;
		}
		 
	}
	
	public static class RecognObj {
		
		@JsonProperty("Code")
		String code;
		@JsonProperty("Message")
		String message;
		@JsonProperty("TopList")
		List<Top> topList;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public List<Top> getTopList() {
			return topList;
		}
		public void setTopList(List<Top> topList) {
			this.topList = topList;
		}
		 
	}
	
	// "Plate":{"Code":0,"Message":"succ","Color":{"Code":"2","Name":"蓝","Score":99},"Type":2,"Flag":1,"Shelter":false,"Destain":false,"Licence":"桂A98849","Score":99}
	public static class PlateRecog {
		@JsonProperty("Code")
		String code;
		@JsonProperty("Message")
		String message;
		@JsonProperty("Type")
		int type;
		@JsonProperty("Flag")
		int flag;	
		@JsonProperty("Color")
		Top color;
		@JsonProperty("Shelter")
		boolean shelter;
		@JsonProperty("Destain")
		boolean destain;
		@JsonProperty("Licence")
		String licence;
		@JsonProperty("Score")
		int score;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public int getType() {
			return type;
		}
		public void setType(int type) {
			this.type = type;
		}
		public int getFlag() {
			return flag;
		}
		public void setFlag(int flag) {
			this.flag = flag;
		}
		public Top getColor() {
			return color;
		}
		public void setColor(Top color) {
			this.color = color;
		}
		public boolean isShelter() {
			return shelter;
		}
		public void setShelter(boolean shelter) {
			this.shelter = shelter;
		}
		public boolean isDestain() {
			return destain;
		}
		public void setDestain(boolean destain) {
			this.destain = destain;
		}
		public String getLicence() {
			return licence;
		}
		public void setLicence(String licence) {
			this.licence = licence;
		}
		public int getScore() {
			return score;
		}
		public void setScore(int score) {
			this.score = score;
		}
		 
	}
	
	public static  class Top {
		@JsonProperty("Code")
		String code;
		@JsonProperty("Name")
		String name;
		@JsonProperty("Score")
		int score;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getScore() {
			return score;
		}
		public void setScore(int score) {
			this.score = score;
		}
	}
}
