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
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
public class BrandSelector extends JPanel  {
	
	private static BrandSelector _instance;

	private ModelChangeListener modelChangeListener;

	private BrandChangeListener brandChangeListener;

	private SubBrandChangeListener subBrandChangeListener;

	boolean initialized = false;
	
	private BrandSelector() {
//		this.cbOtherFullBrand = cbOtherFullBrand;
//		this.recentUsed = recentUsed;
		initCompoments();
		initData();
		
		brandChangeListener = new BrandChangeListener();
		cbBrand.addItemListener(brandChangeListener);
		subBrandChangeListener = new SubBrandChangeListener();
		cbSubBrand.addItemListener(subBrandChangeListener);
		modelChangeListener = new ModelChangeListener();
		cbModel.addItemListener(modelChangeListener);
		
	}

	public static synchronized BrandSelector  getInstance() {
		if (_instance == null ) {
			_instance = new BrandSelector();
		}
		return _instance;
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
		textfield.addKeyListener(new ComboListener(cbBrand, 0));

		cbSubBrand = new JComboBox<>(mSubBrand);
		cbSubBrand.setEditable(true);
		cbSubBrand.setPreferredSize(preferredSize);
		textfield = (JTextField)cbSubBrand.getEditor().getEditorComponent();
		textfield.addKeyListener(new ComboListener(cbSubBrand, 1));

		cbModel = new JComboBox<>(mModel);
		cbModel.setEditable(true);
		cbModel.setPreferredSize(preferredSize);
		textfield = (JTextField)cbModel.getEditor().getEditorComponent();
		textfield.addKeyListener(new ComboListener(cbModel, 2));

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

		//		JPanel panel2 = new JPanel();
		//		add(panel2, BorderLayout.SOUTH);
		//		
		//		ok = new JButton("Ok");
		//		cancel = new JButton("Cancel");
		//		
		//		panel2.add(ok);
		//		panel2.add(cancel); 


		
		//		ok.addActionListener(new ConfirmActionListener());
		//		cancel.addActionListener(new CancelActionListener());



		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		setLocation((screenWidth - this.getWidth()) / 2, (screenHeight - this.getHeight()) / 4);
		//		setLocation(x, y);

	}

	void initData() {
		if (initialized) return ;
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
		
		brandFilters.addAll(brands);
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
				selectedSubBrand.subBrand, selectedModel.model);
	}

	JComboBox<String> cbBrand, cbSubBrand, cbModel;

	DefaultComboBoxModel<String> mBrand, mSubBrand, mModel;

	Brand selectedBrand;
	SubBrand selectedSubBrand;
	Model selectedModel;
	int siModel = 0;

	JButton ok, cancel;

	List<Brand> brands = new ArrayList<>();
	List<SubBrand> subbrands = new ArrayList<>();
	List<Model>  models = new ArrayList<>();
	
	List<Brand> brandFilters = new ArrayList<>();
	List<SubBrand> subBrandFilters = new ArrayList<>();
	List<Model> modelFilters = new ArrayList<>();
	
	boolean confirmed = false; 
	class Brand {
		Integer brand;
		String brandNameCN;
		List<SubBrand> subBrands = new ArrayList<>();
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((brand == null) ? 0 : brand.hashCode());
			result = prime * result + ((brandNameCN == null) ? 0 : brandNameCN.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Brand other = (Brand) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (brand == null) {
				if (other.brand != null)
					return false;
			} else if (!brand.equals(other.brand))
				return false;
			if (brandNameCN == null) {
				if (other.brandNameCN != null)
					return false;
			} else if (!brandNameCN.equals(other.brandNameCN))
				return false;
			return true;
		}
		private BrandSelector getOuterType() {
			return BrandSelector.this;
		}
		
		
	}
	class SubBrand {
		Brand brand;
		Integer subBrand;
		String subBrandNameCN;
		List<Model> models = new ArrayList<>();
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((brand == null) ? 0 : brand.hashCode());
			result = prime * result + ((models == null) ? 0 : models.hashCode());
			result = prime * result + ((subBrand == null) ? 0 : subBrand.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SubBrand other = (SubBrand) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (brand == null) {
				if (other.brand != null)
					return false;
			} else if (!brand.equals(other.brand))
				return false;
			if (models == null) {
				if (other.models != null)
					return false;
			} else if (!models.equals(other.models))
				return false;
			if (subBrand == null) {
				if (other.subBrand != null)
					return false;
			} else if (!subBrand.equals(other.subBrand))
				return false;
			return true;
		}
		private BrandSelector getOuterType() {
			return BrandSelector.this;
		}
		
		

	}

	class Model {
		SubBrand subbrand;
		Integer model;
		String fullNameCNs;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((fullNameCNs == null) ? 0 : fullNameCNs.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Model other = (Model) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (fullNameCNs == null) {
				if (other.fullNameCNs != null)
					return false;
			} else if (!fullNameCNs.equals(other.fullNameCNs))
				return false;
			return true;
		}
		private BrandSelector getOuterType() {
			return BrandSelector.this;
		}
		
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
//			String item =  getFullBrandCode();
//			if (!recentUsed.containsKey(item)) {
//				recentUsed.put(item, cbModel.getItemAt(siModel));
//				cbOtherFullBrand.addItem(item);
//			}
//			cbOtherFullBrand.setSelectedItem(item);
		}
	}

	class ModelChangeListener implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				int si = cbModel.getSelectedIndex();
				if (si > -1) {
					cbBrand.removeItemListener(brandChangeListener);
					cbSubBrand.removeItemListener(subBrandChangeListener);
					
					siModel = si;
					selectedModel =  modelFilters.get(si);
					selectedSubBrand = selectedModel.subbrand;
					selectedBrand = selectedSubBrand.brand;
					
					String bn = selectedBrand.brandNameCN;
					if (!brandFilters.contains(selectedBrand)) {
						cbBrand.addItem(bn);
						brandFilters.add(selectedBrand);
					}
					cbBrand.setSelectedItem(bn);
					
					String sbn = selectedSubBrand.subBrandNameCN;
					if (!subBrandFilters.contains(selectedSubBrand)) {
						cbSubBrand.addItem(sbn);
						subBrandFilters.add(selectedSubBrand);
					}
					cbSubBrand.setSelectedItem(sbn);
					
					cbBrand.setSelectedItem(bn);
					cbSubBrand.setSelectedItem(sbn);
					cbBrand.addItemListener(brandChangeListener);
					cbSubBrand.addItemListener(subBrandChangeListener);
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
					selectedBrand =  brandFilters.get(si);
					cbSubBrand.removeItemListener(subBrandChangeListener);
					mSubBrand.removeAllElements();
					for (SubBrand sb: selectedBrand.subBrands) {
						mSubBrand.addElement(sb.subBrandNameCN);
					}
					cbSubBrand.addItemListener(subBrandChangeListener);
					subBrandFilters = selectedBrand.subBrands;
					 
					if (subBrandFilters.size() > 0) {
						selectedSubBrand = subBrandFilters.get(0);
						cbSubBrand.setSelectedIndex(0);
						cbModel.removeItemListener(modelChangeListener);
						mModel.removeAllElements();
						for (Model m: selectedSubBrand.models) {
							mModel.addElement(m.fullNameCNs);
						}						
						if (selectedSubBrand.models.size() > 0) {
							cbModel.setSelectedIndex(0);
						}
						cbModel.addItemListener(modelChangeListener);						 
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
					selectedSubBrand =  subBrandFilters.get(si);
					selectedBrand  = selectedSubBrand.brand;
					modelFilters = selectedSubBrand.models;
					cbBrand.removeItemListener(brandChangeListener);
//					cbBrand.removeAllItems();
					if (!brandFilters.contains(selectedBrand)) {
						cbBrand.addItem(selectedBrand.brandNameCN);
					}
					cbBrand.setSelectedItem(selectedBrand.brandNameCN);
					cbBrand.addItemListener(brandChangeListener);
					
					cbModel.removeItemListener(modelChangeListener);
					mModel.removeAllElements();
					for (Model m: selectedSubBrand.models) {
						mModel.addElement(m.fullNameCNs);
					} 
					cbModel.addItemListener(modelChangeListener);
				}
			}
		}
	}

	class ComboListener extends KeyAdapter {
		JComboBox<String> combox;
		String input ;
		int cbId = 0;
		public ComboListener(JComboBox<String> combox, int cbId) {
			super();
			this.combox = combox;
			this.cbId = cbId;
		}

		@Override
		public void keyReleased(KeyEvent e) { 
			if (e.isActionKey()	|| e.getKeyCode() == KeyEvent.VK_ENTER) {
				super.keyReleased(e);
			}
			else {
				input = ((JTextField)e.getSource()).getText().trim();
				if (input !=null & input.length() > 0) {
					if (cbId ==0) {
						autoCompBrand();
					}
					else if(cbId ==1) {
						autoCompSubBrand();
					}
					else {
						autoCompModel();
					}
				}
			}

			super.keyReleased(e);
		}

		void searchModel() {
			for (Model m: models) {
				if (m.fullNameCNs.indexOf(input.trim().toUpperCase()) !=-1) {
					cbBrand.setSelectedItem(m.subbrand.brand.brandNameCN);
					cbSubBrand.setSelectedItem(m.subbrand.subBrandNameCN);
					cbModel.setSelectedItem(m.fullNameCNs);
					cbModel.showPopup();
					break;
				}
			}
		}

		void autoCompBrand() {
			brandFilters.clear();
			mBrand.removeAllElements();
			for (Brand b: brands) {
				if (b.brandNameCN.indexOf(input)!=-1) {							
					brandFilters.add(b);
				}
			}
			System.out.println("brandFilters: " + brandFilters.size());
			for (Brand f: brandFilters) {
				mBrand.addElement(f.brandNameCN);
			}
			cbBrand.showPopup();
		}

		void autoCompSubBrand() {
			//			if (cbSubBrand.isPopupVisible()) return;
			subBrandFilters.clear();
			mSubBrand.removeAllElements();
			for (SubBrand sb: subbrands) {
				if (sb.subBrandNameCN.indexOf(input) != -1) {
					subBrandFilters.add(sb);
				}
			}
			for (SubBrand sb: subBrandFilters) {
				mSubBrand.addElement(sb.subBrandNameCN);
			}
			cbSubBrand.showPopup();
		}

		void autoCompModel() {
			modelFilters.clear();
			mModel.removeAllElements();
			for (Model m: models) {
				if (m.fullNameCNs.indexOf(input) != -1) {
					modelFilters.add(m);
				}
			}

			for (Model mf : modelFilters) {
				mModel.addElement(mf.fullNameCNs);
			}

			cbModel.showPopup();
		}

		void searchBrand() {
			for (Brand b: brands) {
				if (b.brandNameCN.indexOf(input.trim().toUpperCase())!=-1) {							
					cbBrand.setSelectedItem(b.brandNameCN);		
					cbBrand.showPopup();
					break;
				}
			}
		}

		void searchSubBrand() {
			for (SubBrand sb: subbrands) {
				if (sb.subBrandNameCN.indexOf(input.trim().toUpperCase()) != -1) {
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
