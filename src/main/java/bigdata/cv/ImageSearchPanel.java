/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import bigdata.cv.ImageCellRenderer.ImageFile;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.ACCID;
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher;
import net.semanticmetadata.lire.searchers.ImageSearchHits;
import net.semanticmetadata.lire.searchers.ImageSearcher;

/**
 * @author link
 *
 */
@SuppressWarnings("serial")
public class ImageSearchPanel extends Panel implements Tool {
	
	ImageTableModel imageModel = new ImageTableModel(20, 6);
	JTable imageTable;
	
	String indexDir  = "/home/link/vehicle-index";
	private JTextArea fileNameTextArea;
	private JLabel totalImageLabel;
	private JLabel costLabel;
	private IndexReader indexReader;
	
	SimpleImageFrame frame;

	public ImageSearchPanel() {
		try {
			initComponenets();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initComponenets() throws IOException {
		
		indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
		
		JPanel north = new JPanel();
		 
		JPanel south = new JPanel();

		setLayout(new BorderLayout(10, 10));
		add(north, BorderLayout.NORTH);
		add(south, BorderLayout.SOUTH);
		
		
		totalImageLabel = new JLabel(String.format("Total Images: %d", indexReader.numDocs()));
		costLabel = new JLabel();
		south.add(totalImageLabel);
		south.add(costLabel);
		
		imageTable = new JTable(imageModel);
		imageTable.setDefaultRenderer(ImageFile.class, new ImageCellRenderer());
		imageTable.setAutoscrolls(false);
		imageTable.setDragEnabled(false);
		imageTable.setTableHeader(null);
		JScrollPane scrollPane = new JScrollPane(imageTable);
		add(scrollPane, BorderLayout.CENTER);
		for (int i = 0; i < imageModel.getColumnCount(); i++) {
			imageTable.getColumnModel().getColumn(i).setCellRenderer(new ImageCellRenderer());
		}
		
		
		// north.setLayout(new BorderLayout());

		JLabel label1 = new JLabel("Search File:");
		fileNameTextArea = new JTextArea();
		fileNameTextArea.setColumns(80);
		fileNameTextArea.setEditable(false);
		fileNameTextArea.setDragEnabled(true);

		fileNameTextArea.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle("select image file to search ...");
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					FileNameExtensionFilter filter = new FileNameExtensionFilter(
							"JPG, PNG & GIF Images", "jpg", "gif", "png");
					chooser.setFileFilter(filter);

					if (chooser.showSaveDialog(ImageSearchPanel.this) == JFileChooser.APPROVE_OPTION) {
						try {
							fileNameTextArea.setText(chooser.getSelectedFile().getCanonicalPath());
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});

		north.add(label1);
		north.add(fileNameTextArea);
		label1.setLabelFor(fileNameTextArea);

		JButton searchButton = new JButton("Search");
		north.add(searchButton);
		searchButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					doSearch();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		JButton showImageButton = new JButton("Show");
		north.add(showImageButton);
		showImageButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				BufferedImage image;
				try {
					image = ImageIO.read(new File(fileNameTextArea.getText()));
					frame = new SimpleImageFrame(image);
//					frame.pack();
					frame.setVisible(true);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	@Override
	public void saveCurrentWork() {
	}

	@Override
	public void addButtons() {
	}

	void doSearch() throws Exception {
		int height = getWidth() / 8;
		imageTable.setRowHeight(height);
		BufferedImage image = ImageIO.read(new FileInputStream(fileNameTextArea.getText()));
		long s = System.currentTimeMillis();
		ImageSearchHits hits = getSearcher().search(image, indexReader);
		double cost = (System.currentTimeMillis() - s) * .001;
		costLabel.setText(String.format("search cost: %.3f (seconds)", cost));
		
		for (int i = 0; i < hits.length(); i++) {
			 Document doc = indexReader.document(hits.documentID(i));
			 String f = doc.get(DocumentBuilder.FIELD_NAME_IDENTIFIER);
			 int cols = imageModel.getColumnCount();
			 int row = i / cols;
			 int col = i % cols;
			 ImageFile val = new ImageFile();
			 val.image = ImageIO.read(new File(f));
			 val.baseName = Paths.get(f).getFileName().toString();
			 imageModel.setValueAt(val, row, col);		 
		}		
		imageModel.fireTableDataChanged();
	}
	
	private ImageSearcher getSearcher() {
       int numResults = 128; 
       return new GenericFastImageSearcher(numResults, ACCID.class);
    }
	
	class SimpleImageFrame extends JFrame {
		SimpleImagePanel simpleImagePanel;
		public SimpleImageFrame(BufferedImage image) throws HeadlessException {
			super();
			simpleImagePanel = new SimpleImagePanel(image);
			setSize(image.getWidth(), image.getHeight());
			setContentPane(simpleImagePanel);
			//getContentPane().setLayout(new BorderLayout(10, 10));
			//JPanel p1 = new JPanel();
			//getContentPane().add(p1, BorderLayout.CENTER);
			//p1.add(simpleImagePanel);
			pack();
		}
	}
}
