package bigdata.cv;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import bigdata.cv.ImageCellRenderer.ImageFile;


@SuppressWarnings("serial")
public class ImageTableModel extends AbstractTableModel {

	List<ImageFile> data = new ArrayList<>();

	int rowCount = 5;

	int columnCount = 8;


	public void removeLastRow() {
		for (int i=0; i< columnCount; i++) {
			data.remove(data.size() - 1); 
		};

	}

	public ImageTableModel(int rowCount, int columnCount) {
		super();
		this.rowCount = rowCount;
		this.columnCount = columnCount;
	}

	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	@Override
	public int getRowCount() {
		return rowCount;
	}

	@Override
	public int getColumnCount() {
		return columnCount;
	}

	@Override
	public Object getValueAt(int row, int col) {
		int idx = row * getColumnCount() + col;
		if (data.size() <= idx) {
			return null;
		}
		else 
		return data.get(row * getColumnCount() + col);
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		data.add((ImageFile)aValue);
		fireTableCellUpdated(row, col);
	}
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	public void removeSelected() {}

	void clean() {
		data.clear();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
}