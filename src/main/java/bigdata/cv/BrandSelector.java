/**
 * 
 */
package bigdata.cv;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import dataset.Util;

/**
 * @author link
 *
 */
@SuppressWarnings("serial")
public class BrandSelector extends JFrame implements WindowStateListener {
	
	private ModelChangeListener modelChangeListener;

	private BrandChangeListener brandChangeListener;

	private SubBrandChangeListener subBrandChangeListener;

	public BrandSelector(JComboBox<String> cbOtherFullBrand, Map<String, String> recentUsed) {
		this.cbOtherFullBrand = cbOtherFullBrand;
		this.recentUsed = recentUsed;
		initCompoments();
		initData();
	}

	void initCompoments() {
		
		mBrand = new DefaultComboBoxModel<>();
		mSubBrand = new DefaultComboBoxModel<>();
		mModel = new DefaultComboBoxModel<>();
		
		cbBrand = new JComboBox<>(mBrand);
		cbBrand.setEditable(true);
		Dimension preferredSize = new Dimension(300, 24);
		cbBrand.setPreferredSize(preferredSize);
		JTextField textfield = (JTextField)cbBrand.getEditor().getEditorComponent();
		textfield.addKeyListener(new ComboListener(0));
		
		cbSubBrand = new JComboBox<>(mSubBrand);
		cbSubBrand.setEditable(true);
		cbSubBrand.setPreferredSize(preferredSize);
		textfield = (JTextField)cbSubBrand.getEditor().getEditorComponent();
		textfield.addKeyListener(new ComboListener(1));
		
		cbModel = new JComboBox<>(mModel);
		cbModel.setEditable(true);
		cbModel.setPreferredSize(preferredSize);
		textfield = (JTextField)cbModel.getEditor().getEditorComponent();
		textfield.addKeyListener(new ComboListener(2));
		
		//TODO combox search feature
		SpringLayout layout = new SpringLayout();
		JPanel panel1 = new JPanel();
		panel1.setLayout(layout);	
		
		setLayout(new BorderLayout());
		add(panel1, BorderLayout.CENTER);
		
		JLabel l1 = new JLabel("Brand:", JLabel.TRAILING);
		panel1.add(l1);
		panel1.add(cbBrand);
		l1.setLabelFor(cbBrand);
		
		JLabel l2 = new JLabel("Sub Brand:", JLabel.TRAILING);
		panel1.add(l2);
		panel1.add(cbSubBrand);
		l2.setLabelFor(cbSubBrand);
		
		JLabel l3 = new JLabel("Model:", JLabel.TRAILING);
		panel1.add(l3);
		panel1.add(cbModel);
		l3.setLabelFor(cbModel);
		SpringUtilities.makeCompactGrid(panel1, 3, 2, 6, 6, 6, 6);
		
		JPanel panel2 = new JPanel();
		add(panel2, BorderLayout.SOUTH);
		
		ok = new JButton("Ok");
		cancel = new JButton("Cancel");
		
		panel2.add(ok);
		panel2.add(cancel);
		
		pack();
		
		brandChangeListener = new BrandChangeListener();
		cbBrand.addItemListener(brandChangeListener);
		subBrandChangeListener = new SubBrandChangeListener();
		cbSubBrand.addItemListener(subBrandChangeListener);
		modelChangeListener = new ModelChangeListener();
		cbModel.addItemListener(modelChangeListener);
		ok.addActionListener(new ConfirmActionListener());
		cancel.addActionListener(new CancelActionListener());
		
		addWindowStateListener(this);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		setLocation((screenWidth - this.getWidth()) / 2, (screenHeight - this.getHeight()) / 4);
//		setLocation(x, y);
		
	}
	
	void initData() {
		ResultSet rs;
		try {
			Connection conn = Util.createConn();
			rs = conn.createStatement().executeQuery("select brand, brandNameCN, subbrand, subBrandNameCN, model, "
					+ "fullNameCN from brand_dictionary order by brand, subbrand, model");
			while (rs.next()) {
				String bn = rs.getString("brandNameCN");
				String sbn = rs.getString("subBrandNameCN");
				int b = rs.getInt("brand");
				int sb = rs.getInt("subbrand");
				int m = rs.getInt("model");
				String fullName = rs.getString("fullNameCN");
				Brand brand = addBrandIfNotExists(bn, b);
				SubBrand subBrand = addSubBrandIfNotExists(brand, sbn, sb);
				addModel(subBrand, fullName, m);
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	Brand addBrandIfNotExists(String name, int b) {
		Brand nb = new Brand();
		nb.brand = b;
		nb.brandNameCN = name;
		
		if (brands.size() > 0) {
			Brand ob = brands.get(brands.size() - 1);
			if (ob.brand != b) {
				brands.add(nb);
				mBrand.addElement(name);
				return nb;
			}
			else {
				return ob;
			}
		}
		else {
			brands.add(nb);
			mBrand.addElement(name);
			return nb;
		}
	}
	
	SubBrand addSubBrandIfNotExists(Brand brand, String subName, int sb) {
		SubBrand nsb = new SubBrand();
		nsb.brand = brand;
		nsb.subBrand = sb;
		nsb.subBrandNameCN = subName;
		
		if (brand.subBrands.size() > 0) {
			SubBrand e = brand.subBrands.get(brand.subBrands.size() -1);
			if (e.subBrand != sb ) {
				brand.subBrands.add(nsb);
				subbrands.add(nsb);
//				mSubBrand.addElement(subName);
				return nsb;
			}
			else {
				return e;
			}
		}
		else {
			brand.subBrands.add(nsb);
			subbrands.add(nsb);
//			mSubBrand.addElement(subName);
			return nsb;
		}
	}
	
	void addModel(SubBrand subBrand, String fullName, int model) {
		Model m = new Model();
		m.model = model;
		m.fullNameCNs = fullName;				
		m.subbrand = subBrand;
//		mModel.addElement(fullName);
		subBrand.models.add(m);
		models.add(m);
	}
	
	public String getFullBrandCode() {
		return String.format("%04d%03d%03d", selectedBrand.brand, 
				selectedSubBrand.subBrand, selectedSubBrand.models.get(siModel).model);
	}
	 
	JComboBox<String> cbBrand, cbSubBrand, cbModel;
	
	DefaultComboBoxModel<String> mBrand, mSubBrand, mModel;
	
	Brand selectedBrand;
	SubBrand selectedSubBrand;
	int siModel = 0;
	
	JButton ok, cancel;
	
	List<Brand> brands = new ArrayList<>();
	List<SubBrand> subbrands = new ArrayList<>();
	List<Model>  models = new ArrayList<>();
	
	boolean confirmed = false;
	
	JComboBox<String> cbOtherFullBrand;
	Map<String, String> recentUsed;

	@Override
	public void windowStateChanged(WindowEvent e) {
	}
	
	class Brand {
		Integer brand;
		String brandNameCN;
		List<SubBrand> subBrands = new ArrayList<>();
	}
	class SubBrand {
		Brand brand;
		Integer subBrand;
		String subBrandNameCN;
		List<Model> models = new ArrayList<>();
		
	}
	
	class Model {
		SubBrand subbrand;
		Integer model;
		String fullNameCNs;
	}

	class CancelActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			BrandSelector.this.setVisible(false);
			confirmed = false;
		}
		
	}
	class ConfirmActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			siModel = cbModel.getSelectedIndex();
			if (siModel < 0) siModel = 0;			
			BrandSelector.this.setVisible(false);
			String item =  getFullBrandCode();
			if (!recentUsed.containsKey(item)) {
				recentUsed.put(item, cbModel.getItemAt(siModel));
				cbOtherFullBrand.addItem(item);
			}
			 cbOtherFullBrand.setSelectedItem(item);
		}
	}

	class ModelChangeListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				int si = cbModel.getSelectedIndex();
				if (si > -1) {
					siModel = si;
				}
			}
		}
	}

	class BrandChangeListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				 int si = cbBrand.getSelectedIndex();
				if (si > -1) {
					selectedBrand = brands.get(si);
					mSubBrand.removeAllElements();
					for (SubBrand sb: selectedBrand.subBrands) {
						mSubBrand.addElement(sb.subBrandNameCN);
					}
					mModel.removeAllElements();
					
					if (selectedBrand.subBrands.size() > 0) {
						selectedSubBrand = selectedBrand.subBrands.get(0);
						
						for (Model m: selectedSubBrand.models) {
							mModel.addElement(m.fullNameCNs);
						}
						siModel = 0;
					}
				}
			 }
		}
	}

	class SubBrandChangeListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				int si = cbSubBrand.getSelectedIndex();
				if (si > -1) {
					mModel.removeAllElements();
					selectedSubBrand = selectedBrand.subBrands.get(si);
					for (Model m: selectedSubBrand.models) {
						mModel.addElement(m.fullNameCNs);
					}
					siModel = 0;
				}
			 }
		}
	}
	
	class ComboListener extends KeyAdapter {
		
		long ts = 0;
		long delay = 0;
		int cbId = 0;
		
		String input = null;

		
		public ComboListener(int cbId) {
			super();
			this.cbId = cbId;
		}

		@Override
		public void keyReleased(KeyEvent e) {
			long curr = System.currentTimeMillis();
			if ( ts == 0 ) {
				ts = curr;
				delay = 0;
			}
			else {
				delay = curr - ts;
				ts = curr;
			}
			if (delay < 200 ) return;
			
			String text = ((JTextField)e.getSource()).getText();
			if (text != null && !text.isEmpty()) {
				if (!text.equals(input) ) {
					input = text;
					System.out.println(text);
					if (cbId ==0) {
						searchBrand();
					}
					else if(cbId ==1) {
						searchSubBrand();
					}
					else {
						searchModel();
					}
					ts = 0;
					delay = 0;
				}
			}
			super.keyReleased(e);
		}
		
		void searchModel() {
			for (Model m: models) {
				if (m.fullNameCNs.indexOf(input) !=-1) {
					cbBrand.setSelectedItem(m.subbrand.brand.brandNameCN);
					cbSubBrand.setSelectedItem(m.subbrand.subBrandNameCN);
					cbModel.setSelectedItem(m.fullNameCNs);
					cbModel.showPopup();
					break;
				}
			}
		}

		void searchBrand() {
			for (Brand b: brands) {
				if (b.brandNameCN.indexOf(input)!=-1) {							
					cbBrand.setSelectedItem(b.brandNameCN);		
					cbBrand.showPopup();
					break;
				}
			}
		}
		
		void searchSubBrand() {
			for (SubBrand sb: subbrands) {
				if (sb.subBrandNameCN.indexOf(input) != -1) {
					cbBrand.setSelectedItem(sb.brand.brandNameCN);
					cbSubBrand.showPopup();
					cbSubBrand.setSelectedItem(sb.subBrandNameCN);
//					cbBrand.removeItemListener(brandChangeListener);
//					cbBrand.addItemListener(brandChangeListener);
					break;
				}
			}
		}
	}
}


