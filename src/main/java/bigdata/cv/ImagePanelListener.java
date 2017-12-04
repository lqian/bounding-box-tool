package bigdata.cv;

import java.awt.image.BufferedImage;

/**
 * 
 * @author qian xiafei
 *
 */
public abstract class ImagePanelListener {
	
	/**
	 * post a the event after scaled image
	 */
	public abstract void postScaled();
	
	public abstract void postLabelFileSave(boolean update);
	
	public abstract void postCorp(BufferedImage image);
	
	public abstract void postSelectedImage(BufferedImage image);
	
	public abstract void postOpen();
	
	public abstract void postChange(BufferedImage image);

	public abstract void postChangeLabel(int selectBoundingBoxIndex, LabeledBoundingBox bb)  ;
}
