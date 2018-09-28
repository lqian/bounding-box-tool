/**
 * 
 */
package bigdata.cv;

import static bigdata.cv.IconUtil.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.DefaultTableModel;

import dataset.Util;

/**
 * @author link
 *
 */

@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
public class BrandPanel extends JPanel implements Tool {
	
	JFrame frame;

	JTable plateNoTable;

	JTable brandsTable;

	String fullBrandCode;

	JToolBar toolBar;

	GridLayout grid = new GridLayout(1, 6);
	JTextField txtfullBrand = new JTextField("1120016002");
	//	JButton btnSearch = new JButton("search");
	JButton btnRemoveInvalid ;
	List<PlateEntiy> plateEntities = new ArrayList<>();

	DefaultTableModel tableModel; 

	Connection conn = null;

	Path root = Paths.get("/train-data/vehicle-brand-dataset");

	private DefaultTableModel dmBrand;

	String _plateNo = "";
	String _plateColor = "";

	private PreparedStatement pstm;

	private ResultSet updatableSet;

	private JButton btnCorrect;
	JButton btnDeleteAll;
	JButton btnDeleteOne;
	JButton btnNext = new JButton(">>");
	JButton btnPre = new JButton("<<");

	//	Set<String> recentUsed = new HashSet<>();
	Map<String, String> recentUsed = new HashMap<>();


	JComboBox<String>  cbOtherFullBrand = new JComboBox<>();
	DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<>();

	JTextField otherFullBrand = new JTextField("6000001000");
	JButton btnOther;
	private String path;

	JLabel lblStatus = new JLabel();

	SimpleImagePanel imagePanel = new SimpleImagePanel();

	BrandSelector  brandSelector;

	public BrandPanel()  {
		super();
		txtfullBrand.setColumns(10);
		otherFullBrand.setColumns(10);

		btnRemoveInvalid = iconButton("check.png", "Remove Invalid Path Sample");
		btnDeleteAll = iconButton("remove_all.png", "remove all samples in the list");

		btnCorrect = iconButton("correct.png", "use a select item to correct the samples");

		btnDeleteOne = iconButton("remove_one.png", "remove one brand from the list");

		btnOther = iconButton("use_another.png", "use brand selected from combxo");

		JPanel weastPanel = new JPanel();
		weastPanel.setLayout(new BorderLayout(10, 10));

		//		JPanel leftNorthPanel = new JPanel();
		JPanel eastPanel = new JPanel();
		JPanel southPanel = new JPanel();


		setLayout(new BorderLayout(10, 10));
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				weastPanel, eastPanel);
		splitPane.setResizeWeight(.75);
		add(splitPane, BorderLayout.CENTER);
		add(southPanel, BorderLayout.SOUTH);

		southPanel.add(lblStatus);


		tableModel = new DefaultTableModel(null, new String []{ "Plate No", "plate color", "Count", "Corrected" }) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		plateNoTable = new JTable(tableModel);
		plateNoTable.setAutoscrolls(true);
		plateNoTable.setDragEnabled(false);
		plateNoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);	 
		JScrollPane scrollPane = new JScrollPane(plateNoTable);

		weastPanel.add(scrollPane, BorderLayout.CENTER);



		//		btnSearch.addActionListener(new SearchAction());
		btnRemoveInvalid.addActionListener(new RemoveInvalidPathListener());

		plateNoTable.getSelectionModel().addListSelectionListener(new PlateNoTableSelectionListener() );

		dmBrand = new DefaultTableModel(null, 
				new String[] {"full brand code", "brand name", "count"}) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		cbOtherFullBrand.setModel(cbModel);
		cbOtherFullBrand.setEditable(true);
		cbOtherFullBrand.setPreferredSize(new Dimension(200, 24));
		cbOtherFullBrand.setRenderer(new ComboxRender());
		cbOtherFullBrand.getEditor().getEditorComponent().addMouseListener( new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
//					brandSelector.setVisible(true);
					int r = JOptionPane.showConfirmDialog(frame, brandSelector, "Brand Selector", JOptionPane.OK_CANCEL_OPTION);
					if (r == JOptionPane.OK_OPTION) {
						String fc = brandSelector.getFullBrandCode();						
						if (cbModel.getIndexOf(fc) != -1) {
							cbModel.addElement(fc);
						}
						cbOtherFullBrand.setSelectedItem(fc);
					}
				}
			}
		});

		JButton moreBrand = new JButton(icon("more.png", "show more brand"));
		moreBrand.addActionListener(new MoreActionListener() );

		JButton markAsCorrectButton = iconButton("mark_as_correct.png", "mark all samples as correct");
		JButton markAsUnknown = iconButton("keep_as_unknown.png", "keep all samples as unknow");
		eastPanel.setLayout(new BorderLayout());
		JPanel pn = new JPanel();
		eastPanel.add(pn, BorderLayout.NORTH);
		pn.add(btnCorrect);
		pn.add(markAsCorrectButton);
		pn.add(btnDeleteAll);
		pn.add(btnDeleteOne);
		pn.add(markAsUnknown);
		pn.add(cbOtherFullBrand);
		pn.add(btnOther);
		pn.add(moreBrand);

		JPanel pc = new JPanel();
		pc.setLayout(new GridLayout(2,1));
		eastPanel.add(pc, BorderLayout.CENTER);

		brandsTable = new JTable(dmBrand);
		JScrollPane scrollPane1 = new JScrollPane(brandsTable);
		brandsTable.setEnabled(true);
		brandsTable.addMouseListener( new BrandTableMouseListener());
		brandsTable.getSelectionModel().addListSelectionListener( new BrandsTableListSelectionListener());

		pc.add(scrollPane1);
		pc.add(imagePanel);

		JPanel ps = new JPanel();
		eastPanel.add(ps, BorderLayout.SOUTH);

		ps.add(btnPre);
		ps.add(btnNext);

		btnCorrect.addActionListener(new CorrectActionListener());
		markAsCorrectButton.addActionListener(new MarkAsCorrectAction());
		btnDeleteAll.addActionListener(new DeleteAllAction() );
		btnDeleteOne.addActionListener( new DeleteOnAction());
		btnOther.addActionListener(new OtherAction() );
		btnNext.addActionListener(new NextAction());
		btnPre.addActionListener(new PreActionListener());
		markAsUnknown.addActionListener(new MarkAsUnknowAction());

		 
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
			dmBrand.addRow(new Object[] {be.fullBrandCode, be.fullBrandName, be.count});
		}
		rs.close();

		int r = dmBrand.getRowCount();
		if (r > 0) {
			brandsTable.setRowSelectionInterval(r-1, r-1);
		}

		Statement stm = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
		updatableSet = stm.executeQuery("select id, path, vehicle_brand, vehicle_sub_brand, "
				+ "vehicle_model, corrected, deprecated from vehicle_dataset where plate_nbr='" + _plateNo + "' and plate_color=" + _plateColor);

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
		while (tableModel.getRowCount() > 0) {
			tableModel.removeRow(0);
		}
		//this.plateNoTable.removeAll();

		conn = Util.createConn();

		int brand = Integer.parseInt(fullBrandCode.substring(0, 4));
		int subbrand = Integer.parseInt(fullBrandCode.substring(4, 7));
		int model = Integer.parseInt(fullBrandCode.substring(7,10));

		Statement stm = conn.createStatement();
		String sql = String.format("select count(1) total, plate_nbr, plate_color "
				+ " from vehicle_dataset where vehicle_brand=%d and vehicle_sub_brand=%d and vehicle_model=%d and corrected=0 "
				+ " and deprecated=0 "
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
				+ " where plate_nbr=? and plate_color=? and deprecated = 0 group by  vehicle_brand, vehicle_sub_brand, vehicle_model,fullNameCN order by count(1)");
	}

	boolean putNewBrand(String code) {
		int brand = Integer.parseInt(code.substring(0, 4));
		int subbrand = Integer.parseInt(code.substring(4, 7));
		int model = Integer.parseInt(code.substring(7,10));

		try {
			String sql = String.format("select fullNameCN from brand_dictionary where brand=%d and subbrand=%d and model=%d", brand, subbrand, model);
			ResultSet rs = conn.createStatement().executeQuery(sql);
			boolean r = rs.next();
			if (r) {
				String fullNameCN = rs.getString(1);
				recentUsed.put(code, fullNameCN);
			}
			rs.close();
			return r;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;

	}

	void correctBrand(String plateNo, String plateColor, int brand, int subBrand, int model) throws Exception {
		//update database
		String newCode = String.format("b%04d%03d%03d", brand, subBrand, model);
		this.updatableSet.first();	
		do {
			Path p = Paths.get(updatableSet.getString("path"));
			int ob = updatableSet.getInt("vehicle_brand");
			int osb = updatableSet.getInt("vehicle_sub_brand");
			int m = updatableSet.getInt("vehicle_model");
			int d = updatableSet.getInt("deprecated");

			if (d == 1) continue;
			if (Files.exists(p)) {
				if (!( ob == brand  && osb == subBrand && m == model  )) { 
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
				}
				updatableSet.updateInt("corrected", 1);
				updatableSet.updateRow();
			}
			else {
				updatableSet.deleteRow();
			}
		} while (updatableSet.next()) ;
		//		conn.commit();
		updatableSet.close();

		int psi = this.plateNoTable.getSelectedRow();
		tableModel.setValueAt(true, psi, 3);
		while (dmBrand.getRowCount() > 0) {
			dmBrand.removeRow(0);
		}
		try {
			refreshBrandTable();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	class MarkAsCorrectAction implements  ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				updatableSet.beforeFirst();
				while (updatableSet.next()) {
					Path p = Paths.get(updatableSet.getString("path"));
					if (Files.exists(p)) {
						updatableSet.updateInt("corrected", 1);
						updatableSet.updateRow();
					}
					else {
						updatableSet.deleteRow();
					}
				}

				updatableSet.close();

				int idx = plateNoTable.getSelectedRow();
				tableModel.setValueAt(true, idx, 3);

				if (idx < tableModel.getRowCount() -1) {
					int next = ++idx;
					plateNoTable.setRowSelectionInterval(next, next);
					//plateNoTable.changeSelection(from, end, false, false);
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	class MarkAsUnknowAction implements  ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				updatableSet.beforeFirst();
				while (updatableSet.next()) {
					Path p = Paths.get(updatableSet.getString("path"));
					if (Files.exists(p)) {
						updatableSet.updateInt("deprecated", 2);
						updatableSet.updateRow();
					}
					else {
						updatableSet.deleteRow();
					}
				}

				updatableSet.close();

				int idx = plateNoTable.getSelectedRow();
				tableModel.setValueAt(true, idx, 3);

				if (idx < tableModel.getRowCount() -1) {
					int next = ++idx;
					plateNoTable.setRowSelectionInterval(next, next);
					//plateNoTable.changeSelection(from, end, false, false);
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}


	class CorrectActionListener implements  ActionListener {
		// correct
		@Override
		public void actionPerformed(ActionEvent e) {
			int sidx = brandsTable.getSelectionModel().getMinSelectionIndex();
			if (sidx == -1) return ;
			String code = (String)dmBrand.getValueAt(sidx, 0);
			int brand = Integer.parseInt(code.substring(0, 4));
			int subbrand = Integer.parseInt(code.substring(4, 7));
			int model = Integer.parseInt(code.substring(7));
			try {
				correctBrand(_plateNo, _plateColor, brand, subbrand, model);
				int idx = plateNoTable.getSelectedRow();
				if (idx < tableModel.getRowCount() -1) {
					int next = ++idx;
					plateNoTable.setRowSelectionInterval(next, next);
					//plateNoTable.changeSelection(from, end, false, false);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
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

	class PlateNoTableSelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			ListSelectionModel lsm = (ListSelectionModel) e.getSource();
			int msi = lsm.getMinSelectionIndex(); 
			if (msi == -1) return;
			String plateNo = (String)tableModel.getValueAt(msi, 0);
			//			Boolean correted = (Boolean)tableModel.getValueAt(msi, 3);	
			if (!_plateNo.equals(plateNo)) {
				_plateNo = plateNo;
				_plateColor = (String)tableModel.getValueAt(msi, 1);

				while (dmBrand.getRowCount() > 0) {
					dmBrand.removeRow(0);
				}

				try {
					refreshBrandTable();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	class RemoveInvalidPathListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			Thread t = new Thread() {

				@Override
				public void run() {
					String fullbrand = txtfullBrand.getText();
					int brand = Integer.parseInt(fullbrand.substring(0, 4));
					int subbrand = Integer.parseInt(fullbrand.substring(4, 7));
					int model = Integer.parseInt(fullbrand.substring(7,10));

					try {
						if (conn == null) {
							conn = Util.createConn();
						}
						Statement stm = BrandPanel.this.conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
						ResultSet rs = stm.executeQuery(String.format("select id, path from vehicle_dataset where vehicle_brand=%d and vehicle_sub_brand=%d and vehicle_model=%d", brand, subbrand, model));
						while (rs.next()) {
							Path p = Paths.get(rs.getString("path"));
							if (Files.notExists(p)) {
								rs.deleteRow();
								lblStatus.setText(p.toString());
							}
						}
						rs.close();
						stm.close();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			};
			t.start();
		}
	}

	class MoreActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			brandSelector.setVisible(true);	
			//			brandSelector.pack();
		}
	}
	class BrandTableMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {


		} 
	}

	class BrandsTableListSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			ListSelectionModel lsm = (ListSelectionModel) e.getSource();
			int msi = lsm.getMinSelectionIndex(); 
			if (msi == -1 || updatableSet == null) return;
			String tb = (String) dmBrand.getValueAt(msi, 0);
			try {
				if (!updatableSet.isClosed()) {
					updatableSet.beforeFirst();
					skipNext(tb);
				}
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

	class DeleteAllAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				updatableSet.first();
				do {
					//					String file = updatableSet.getString("path");
					//					Path p = Paths.get(file);
					//					if (Files.exists(p)) {
					//						Files.delete(p);
					//					}
					updatableSet.updateInt("deprecated", 1);
					updatableSet.updateRow();
				} while (updatableSet.next());
				updatableSet.close();

				while (dmBrand.getRowCount() > 0) {
					dmBrand.removeRow(0);
				}
				int row = plateNoTable.getSelectedRow();
				tableModel.removeRow(row);

				if (row < tableModel.getRowCount() -1) {
					int next = (row++);
					plateNoTable.setRowSelectionInterval(next, next);
					//plateNoTable.changeSelection(row, end, false, false);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	class OtherAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			String code = (String)cbOtherFullBrand.getSelectedItem();

			if (!recentUsed.containsKey(code) ) {
				putNewBrand(code);					
				cbOtherFullBrand.addItem(code);					
			}

			int brand = Integer.parseInt(code.substring(0, 4));
			int subbrand = Integer.parseInt(code.substring(4, 7));
			int model = Integer.parseInt(code.substring(7));
			try {
				correctBrand(_plateNo, _plateColor, brand, subbrand, model);
				int idx = plateNoTable.getSelectedRow();
				if (idx < tableModel.getRowCount() -1) {
					int next = ++idx;

					plateNoTable.setRowSelectionInterval(next, next);
					//plateNoTable.changeSelection(from, end, false, false);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	class PreActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			String tb = (String) dmBrand.getValueAt(brandsTable.getSelectedRow(), 0);
			boolean found = false;
			try {
				while  (!found && updatableSet.previous()) {
					int b = updatableSet.getInt("vehicle_brand");
					int sb = updatableSet.getInt("vehicle_sub_brand");
					int m = updatableSet.getInt("vehicle_model");
					String fc = String.format("%04d%03d%03d", b, sb, m);
					if (fc.equals(tb) ) {
						path = updatableSet.getString("path");
						if (Files.exists(Paths.get(path))) {
							found = true;
							imagePanel.setName(path);				
							imagePanel.updateUI();
						}
					}
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	void skipNext(String tb) {
		boolean found = false;
		try {
			while  (!found && updatableSet.next()) {
				int b = updatableSet.getInt("vehicle_brand");
				int sb = updatableSet.getInt("vehicle_sub_brand");
				int m = updatableSet.getInt("vehicle_model");
				String fc = String.format("%04d%03d%03d", b, sb, m);

				if (fc.equals(tb) ) {
					path = updatableSet.getString("path");
					if (Files.exists(Paths.get(path))) {
						found = true;
						imagePanel.setName(path);				
						imagePanel.updateUI();
					}
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	class NextAction implements ActionListener  {

		@Override
		public void actionPerformed(ActionEvent e) {
			String tb = (String) dmBrand.getValueAt(brandsTable.getSelectedRow(), 0);
			skipNext(tb);
		}
	}


	class DeleteOnAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			int sidx = brandsTable.getSelectionModel().getMinSelectionIndex();
			if (sidx == -1) return ;
			String code = (String)dmBrand.getValueAt(sidx, 0);
			int brand = Integer.parseInt(code.substring(0, 4));
			int subbrand = Integer.parseInt(code.substring(4, 7));
			int model = Integer.parseInt(code.substring(7));

			try {
				updatableSet.first();
				do {
					int ob = updatableSet.getInt("vehicle_brand");
					int osb = updatableSet.getInt("vehicle_sub_brand");
					int om = updatableSet.getInt("vehicle_model");

					if (brand == ob && subbrand == osb && model == om) {
						updatableSet.updateInt("deprecated", 1);
						updatableSet.updateRow();
					}
				} while (updatableSet.next());
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			dmBrand.removeRow(sidx);
		}
	}


	class ComboxRender  extends BasicComboBoxRenderer  {

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			String val = (String) list.getModel().getElementAt(index);
			if (val != null && !"null".equals(val)) {
				int i = val.indexOf("|");
				if (i == -1 && recentUsed.containsKey(val)) {
					value = val + "| " + recentUsed.get(val) ;
				}
			}

			return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		}
	}

	@Override
	public void saveCurrentWork() {
	}

	@Override
	public void addButtons() {
		toolBar.addSeparator();
		toolBar.add(btnRemoveInvalid); 
	}

}
