/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import dataset.Util;

/**
 * @author link
 *
 */
public class BrandPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	BorderLayout boderLayout = new BorderLayout(10, 10);

	JTable plateNoTable;

	JTable brandsTable;

	JPanel northPanel = new JPanel();
	JPanel centerPanel = new JPanel();
	JPanel eastPanel = new JPanel();

	GridLayout grid = new GridLayout(1, 6);
	JTextField txtfullBrand = new JTextField("1105003002");
	JButton btnSearch = new JButton("search");
	List<PlateEntiy> plateEntities = new ArrayList<>();

	DefaultTableModel tableModel; 
	 
	Connection conn ;
	
	Path root = Paths.get("/train-data/vehicle-brand-dataset");

	private DefaultTableModel tableModel1;
	
	String _plateNo ="";
	String _plateColor = "";

	private PreparedStatement pstm;

	private ResultSet updatableSet;
	
	private JButton btnCorrect = new JButton("correct");
	JButton btnDelete = new JButton("Remove All Sample");
	JButton btnNext = new JButton(">>");
	JButton btnPre = new JButton("<<");

	private String path;
	
	SimpleImagePanel imagePanel = new SimpleImagePanel();
	
	@SuppressWarnings("serial")
	public BrandPanel() {
		super();
		//		northPanel.setLayout(grid);
		northPanel.add(new JLabel("full brand code:"));
		northPanel.add(txtfullBrand);		
		northPanel.add(btnSearch);
		
		setLayout(boderLayout);
		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(eastPanel, BorderLayout.EAST);

		tableModel = new DefaultTableModel(null, new String []{ "Plate No", "plate color", "Count", "Corrected" }) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		plateNoTable = new JTable(tableModel);
		plateNoTable.setAutoscrolls(false);
		plateNoTable.setDragEnabled(false);
		plateNoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);	 
		JScrollPane scrollPane = new JScrollPane(plateNoTable);
		centerPanel.add(scrollPane);
		scrollPane.setSize(centerPanel.getSize());
		
		btnSearch.addActionListener(new SearchAction());
		plateNoTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				int msi = lsm.getMinSelectionIndex(); 
				if (msi == -1) return;
				String plateNo = (String)tableModel.getValueAt(msi, 0);
//				Boolean correted = (Boolean)tableModel.getValueAt(msi, 3);	
				if (!_plateNo.equals(plateNo)) {
					_plateNo = plateNo;
					_plateColor = (String)tableModel.getValueAt(msi, 1);
					
					while (tableModel1.getRowCount() > 0) {
						tableModel1.removeRow(0);
					}
					
					try {
						refreshBrandTable();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		
		tableModel1 = new DefaultTableModel(null, new String[] {"full brand code", "brand name", "count"}) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		
		eastPanel.setLayout(new BorderLayout());
		JPanel pn = new JPanel();
		eastPanel.add(pn, BorderLayout.NORTH);
		pn.add(btnCorrect);
		pn.add(btnDelete);
		
		JPanel pc = new JPanel();
		pc.setLayout(new GridLayout(2,1));
		eastPanel.add(pc, BorderLayout.CENTER);
		
		brandsTable = new JTable(tableModel1);
		JScrollPane scrollPane1 = new JScrollPane(brandsTable);
		brandsTable.setEnabled(true);
		pc.add(scrollPane1);
		pc.add(imagePanel);
		
		JPanel ps = new JPanel();
		eastPanel.add(ps, BorderLayout.SOUTH);
		
		ps.add(btnPre);
		ps.add(btnNext);
		
		btnCorrect.addActionListener(new ActionListener() {
			// correct
			@Override
			public void actionPerformed(ActionEvent e) {
				int sidx = brandsTable.getSelectionModel().getMinSelectionIndex();
				if (sidx == -1) return ;
				String code = (String)tableModel1.getValueAt(sidx, 0);
				int brand = Integer.parseInt(code.substring(0, 4));
				int subbrand = Integer.parseInt(code.substring( 4, 7));
				int model = Integer.parseInt(code.substring(7));
				try {
					correctBrand(_plateNo, _plateColor, brand, subbrand, model);
					int idx = plateNoTable.getSelectedRow();
					if (idx < tableModel.getRowCount() -1) {
						int from = idx++;
						int end = idx++;
						//plateNoTable.setRowSelectionInterval(idx, idx);
						//plateNoTable.changeSelection(from, end, false, false);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		btnDelete.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					updatableSet.first();
					do {
						String file = updatableSet.getString("path");
						Path p = Paths.get(file);
						if (Files.exists(p)) {
							Files.delete(p);
						}
						updatableSet.deleteRow();
					} while (updatableSet.next());
					updatableSet.close();
					
					int row = plateNoTable.getSelectedRow();
					tableModel.removeRow(row);
					if (row < tableModel.getRowCount() -1) {
						int end = row++;
						//plateNoTable.setRowSelectionInterval(row, row);
						//plateNoTable.changeSelection(row, end, false, false);
					}
					
					while (tableModel1.getRowCount() > 0) {
						tableModel1.removeRow(0);
					}
					
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		btnNext.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean found = false;
				try {
					while  (!found && updatableSet.next()) {
						path = updatableSet.getString("path");
						if (Files.exists(Paths.get(path))) {
							found = true;
							imagePanel.setName(path);				
							imagePanel.updateUI();
						}
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				
			}
			
		});
		
		btnPre.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean found = false;
				try {
					while  (!found && updatableSet.previous()) {
						try {
							path = updatableSet.getString("path");
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
						if (Files.exists(Paths.get(path))) {
							found = true;
							imagePanel.setName(path);				
							imagePanel.updateUI();
						}
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
	
	private void refreshBrandTable() throws SQLException {
		pstm.clearParameters();
		pstm.setString(1, _plateNo);
		pstm.setString(2, _plateColor);
		ResultSet rs = pstm.executeQuery();
		while (rs.next()) {
			BrandEntity be = new BrandEntity();
			int b = rs.getInt("vehicle_brand");
			int sb = rs.getInt("vehicle_sub_brand");
			int m = rs.getInt("vehicle_model");
			be.count = rs.getInt("total");

			be.fullBrandCode = String.format("%04d%03d%03d", b, sb, m);
			be.fullBrandName = rs.getString("fullNameCN");

			tableModel1.addRow(new Object[] {be.fullBrandCode, be.fullBrandName, be.count});
		}
		rs.close();

		Statement stm = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
		updatableSet = stm.executeQuery("select id, path, vehicle_brand, vehicle_sub_brand, "
				+ "vehicle_model, corrected from vehicle_dataset where plate_nbr='" + _plateNo + "' and plate_color=" + _plateColor);

		// rand 1 pictures
		boolean found = false;
		while  (!found && updatableSet.next()) {
			path = updatableSet.getString("path");
			if (Files.exists(Paths.get(path))) {
				found = true;
				imagePanel.setName(path);				
				imagePanel.updateUI();
			}
		}
	}
	
	public void initData() throws ClassNotFoundException, SQLException {
		conn = Util.createConn();
		String fullbrand = this.txtfullBrand.getText();
		int brand = Integer.parseInt(fullbrand.substring(0, 4));
		int subbrand = Integer.parseInt(fullbrand.substring(4, 7));
		int model = Integer.parseInt(fullbrand.substring(7,10));
		
		Statement stm = conn.createStatement();
		String sql = String.format("select count(1) total, plate_nbr, plate_color "
				+ " from vehicle_dataset where vehicle_brand=%d and vehicle_sub_brand=%d and vehicle_model=%d and corrected=0"
				+ " group by plate_nbr, plate_color order by count(1) desc ", brand, subbrand, model);
		
		ResultSet rs = stm.executeQuery(sql);
		while (rs.next()) {
			PlateEntiy pe = new PlateEntiy();
			pe.plateNo = rs.getString("plate_nbr");
			pe.plateColor = rs.getString("plate_color");
			pe.count = rs.getInt("total");
			
			plateEntities.add(pe);
			tableModel.addRow(new Object[] {pe.plateNo, pe.plateColor, pe.count, false});
		}
		rs.close();
//		plateNoTable.updateUI();
		
		pstm = conn.prepareStatement("select vehicle_brand, vehicle_sub_brand, "
				+ " vehicle_model, fullNameCN, count(1) as total"
				+ " from vehicle_dataset d left join brand_dictionary b "
				+ " on vehicle_brand=brand and vehicle_sub_brand = subbrand and vehicle_model=model"
				+ " where plate_nbr=? and plate_color=? group by  vehicle_brand, vehicle_sub_brand, vehicle_model,fullNameCN order by count(1)");
	}
	
	void correctBrand(String plateNo, String plateColor, int brand, int subBrand, int model) throws Exception {
		//update database
		String newCode = String.format("b%04d%03d%03d", brand, subBrand, model);
		this.updatableSet.first();	
		do {
			Path p = Paths.get(updatableSet.getString("path"));
			if (Files.exists(p)) {
				Path root = p.getParent().getParent().getParent().getParent();
				String baseName = p.getFileName().toString();
				String newName = newCode + baseName.substring(11);
				Path sub = root.resolve(String.format("%04d/%03d/%03d", brand, subBrand, model)); 
				if (Files.notExists(sub )) {
					Files.createDirectories(sub);
				}
				Path tp = sub.resolve(newName);
				Files.move(p, tp);
				updatableSet.updateInt("vehicle_brand", brand);
				updatableSet.updateInt("vehicle_sub_brand", subBrand);
				updatableSet.updateInt("vehicle_model", model);
				updatableSet.updateString("path", tp.toString()); 
				updatableSet.updateInt("corrected", 1);
				updatableSet.updateRow();
			}
			else {
				updatableSet.deleteRow();
			}
		}while (updatableSet.next()) ;
//		conn.commit();
		updatableSet.close();
		 
		int psi = this.plateNoTable.getSelectedRow();
		tableModel.setValueAt(true, psi, 3);
		while (tableModel1.getRowCount() > 0) {
			tableModel1.removeRow(0);
		}
		try {
			refreshBrandTable();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	class SearchAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				initData();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	class PlateEntiy {
		public String plateNo;
		public String plateColor;
		public int count;
		public boolean selected = false;
	}
	
	class BrandEntity {
		public String fullBrandCode;
		public String fullBrandName;
		public int count;
		
	}
	
	class SimpleImagePanel extends JPanel{

		String name; 

		private BufferedImage image;
 

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			try {   
				if (name != null) {
					image = ImageIO.read(new File(name));
				}
				g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
			} catch (IOException ex) {	            
			}
		}

		public void setName(String name) {
			this.name = name; 
		}
	}
}
