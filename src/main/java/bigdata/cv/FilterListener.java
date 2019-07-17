/**
 * 
 */
package bigdata.cv;

/**
 * @author lqian
 *
 */
public interface FilterListener {
	
	public void clearFilter();
	
	public void addClazzFilter(String text);
	
	public String[] clazzNames();
	
	public void removeClazzFilter(String text);
}
