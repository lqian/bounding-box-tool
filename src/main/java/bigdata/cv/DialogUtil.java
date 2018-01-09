/**
 * 
 */
package bigdata.cv;

import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * @author link
 *
 */
public class DialogUtil {

	public static void showWarningMsg(String text) {
		Toolkit.getDefaultToolkit().beep();
		JOptionPane optionPane = new JOptionPane(text, JOptionPane.WARNING_MESSAGE);
		JDialog dialog = optionPane.createDialog("Warning!");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
	}
	
	public static void showInfoMsg(String text) {
		Toolkit.getDefaultToolkit().beep();
		JOptionPane optionPane = new JOptionPane(text, JOptionPane.INFORMATION_MESSAGE);
		JDialog dialog = optionPane.createDialog("Info");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
	}
}
