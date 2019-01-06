/**
 * 
 */
package dataset;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author link
 *
 */
public class MultiLabelsDataset {	

	List<String> labelNames ;
	Connection connection;
	Path dataset;
	int trainSamples = 100000;
	int validSamples = 10000;
	int threadNums = 16;

	LinkedBlockingQueue<Sample> samples;
	CountDownLatch latch;
	AtomicInteger trainCounter ;
	AtomicInteger validCounter ;

	LinkedBlockingQueue<Sample> filteredSamples =  new LinkedBlockingQueue<>();


	void setup() throws SQLException {
		trainCounter = new AtomicInteger(0);
		validCounter = new AtomicInteger(0);
		labelNames = new ArrayList<>();
		labelNames.add("vehicle_type");
		labelNames.add("vehicle_color");
		Statement stm = connection.createStatement();
		int max = (int) ((trainSamples + trainSamples) * 1.2);

		samples = new LinkedBlockingQueue<>(max);
		String sql = "select path, " + toColumns(labelNames) + " from vehicle_dataset limit " + max;
		ResultSet rs = stm.executeQuery(sql);		
		List<Sample> list = new ArrayList<>();
		while (rs.next()) {
			Sample sample = new Sample();
			sample.path = rs.getString("path");
			for (String ln : labelNames) {
				sample.labels.add(rs.getInt(ln));
			}
			list.add(sample);
		}
		rs.close();
		Collections.shuffle(list);
		samples.addAll(list);		

	}

	String toColumns(List<String> labelNames) {
		String str = labelNames.toString();
		int len = str.length();
		return str.substring(1, len-1);
	}

	void execute() throws Exception {
		latch = new CountDownLatch(threadNums);
		ExecutorService threadPool = Executors.newFixedThreadPool(threadNums);
		for (int i=0 ; i<threadNums; i++) {
			threadPool.submit(new Runnable() {

				@Override
				public void run() {
					Sample sample = null;
					try {
						while (trainCounter.get() < trainSamples 
								&& validCounter.get() < validSamples 
								&& (sample = samples.poll(1, TimeUnit.SECONDS)) != null ) {
							if (Files.exists(Paths.get(sample.path))) {
								filteredSamples.add(sample);
							}
						}
					} catch (InterruptedException e) {					 
						e.printStackTrace();
					}
					finally {
						latch.countDown();
					}

				}});
		}
		latch.await();
		threadPool.shutdown();

		// take and write train samples
		List<Sample> train = new ArrayList<>();
		filteredSamples.drainTo(train, trainSamples);
		write("train.list", train);
		
		// take and write valid samples
		List<Sample> valid = new ArrayList<>();
		filteredSamples.drainTo(valid, validSamples);
		write("valid.list", valid);
	}

	void write(String file, List<Sample> samples) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(dataset.resolve(file));
		for (Sample s: samples) {
			writer.write(s.toLine());
		}
		writer.flush();
		writer.close();
		System.out.format("write %d to %s \n", samples.size(), file);
	}

	class Sample {
		String path;
		List<Integer> labels = new ArrayList<>();

		String toLine() {
			String line = path;
			for (Integer label: labels) {
				line += ";" + label;
			}
			return line + "\n";

		}
	}

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
		MultiLabelsDataset mlds = new MultiLabelsDataset();
		mlds.connection = Util.createConn();
		mlds.dataset = Paths.get("mlds");
		if (Files.notExists(mlds.dataset)) {
			Files.createDirectories(mlds.dataset);
		}
		mlds.setup();
		mlds.execute();

	}

}
