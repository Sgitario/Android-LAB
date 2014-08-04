package org.sgitario.layoutgridsample;

import java.util.HashMap;
import java.util.List;

public interface LayoutAdapter {
	public void prepareLayout(List<Object> data);
	public void setDimensions(int measuredWidth, int measuredHeight);
	public PlaceHolder getItemAt(float x, float y);
	public int getContentHeight();
	public int getContentWidth();
	public PlaceHolder getPlaceHolderForItem(Object item);
	public HashMap<Object, PlaceHolder> getItemProxies(
			int viewPortLeft, int viewPortTop);
}
