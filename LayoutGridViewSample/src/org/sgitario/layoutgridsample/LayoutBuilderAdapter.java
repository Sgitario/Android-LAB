package org.sgitario.layoutgridsample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.sgitario.layoutgridsample.builder.LayoutBuilderFactory;
import org.sgitario.layoutgridsample.layout.Layout;
import org.sgitario.layoutgridsample.layout.LayoutBuilder;
import org.sgitario.layoutgridsample.model.Feed;

import android.graphics.Rect;
import android.util.Log;

public class LayoutBuilderAdapter implements LayoutAdapter {

	private static final String TAG = LayoutBuilderAdapter.class.toString();
	
	private HashMap<Object, PlaceHolder> map = new HashMap<Object, PlaceHolder>();
	private int maxRight = 0;
	private int width;
	private int height;
	
	public void prepareLayout(List<Object> items){
		List<Feed> data = new ArrayList<Feed>();
		for (Object item : items) {
			data.add((Feed) item);
		}
		
		map.clear();
		
		// Start at the top left corner.
		// ColumnAvailability columnsAvailability = new ColumnAvailability(width, height); // Column X: remains Y top space available.
		List<Rect> availablePlaceHolders = new ArrayList<Rect>();
		
		// Dependending on the number of data, the columns will be calculated.
		LayoutBuilder builder = null;
		int left = 0;
		int dataIndex = 0;
		for (int index = 0; index < data.size(); index++) {
			// Request for layout
			if (builder == null 
					|| dataIndex >= builder.getNumItems()) {
				builder = LayoutBuilderFactory.get(data.subList(index, data.size()));
				dataIndex = 0;
			}
			
			// Layout needs
			Layout layout = builder.getLayoutFor(dataIndex);
			int itemWidth = (width * layout.getWidthPercentage() / 100);
			int itemHeight = (height * layout.getHeightPercentage() / 100);
			
			// De we have available placeholders?
			Rect placeholder = null;
			for (Rect availablePlaceHolder : availablePlaceHolders) {
				if (((availablePlaceHolder.right - availablePlaceHolder.left) >= itemWidth) 
						&& ((availablePlaceHolder.bottom - availablePlaceHolder.top) >= itemHeight)) {
							// can be used
					placeholder = availablePlaceHolder;
				}
			}
			
			if (placeholder != null) {
				availablePlaceHolders.remove(placeholder);
				
				int placeHolderWidth = placeholder.right - placeholder.left;
				int placeHolderHeight = placeholder.bottom - placeholder.top;
				
				// Split to down
				if (placeHolderHeight > itemHeight) {
					int newHeight = placeHolderHeight - itemHeight;
					
					availablePlaceHolders.add(new Rect(placeholder.left, placeholder.top + itemHeight, 
							placeholder.right, placeholder.top + itemHeight + newHeight));
				}
				
				// Split to right
				if (placeHolderWidth > itemWidth) {
					int newWidth = placeHolderWidth - itemWidth;
					
					availablePlaceHolders.add(new Rect(placeholder.left + itemWidth, placeholder.top, 
							placeholder.left + itemWidth + newWidth, placeholder.bottom));
				}
			} else {
				// Generate a new column
				placeholder = new Rect();
				placeholder.left = left;
				placeholder.top = 0;
				placeholder.right = left + itemWidth;
				placeholder.bottom = itemHeight;
				
				if (itemHeight < height) {
					availablePlaceHolders.add(new Rect(placeholder.left, itemHeight, placeholder.right, height));
				}
				
				left = placeholder.right;
			}
			
			// Generate a new column
			PlaceHolder p = new PlaceHolder();
			p.itemIndex = index;
			p.data = data.get(index);
			
			Rect r = new Rect();
			r.left = placeholder.left;
			r.top = placeholder.top;
			r.right = placeholder.left + itemWidth;
			r.bottom = placeholder.top + itemHeight;
			
			p.frame = r;
			map.put(p.data, p);
			
			dataIndex++;
			
			// Update accumulative width
			maxRight = Math.max(maxRight, r.right);
		}
	}

	public HashMap<Object, PlaceHolder> getItemProxies(
			int viewPortLeft, int viewPortTop) {

		Rect viewport = new Rect(viewPortLeft, 
								viewPortTop, 
								viewPortLeft + width, 
								viewPortTop + height);
		
		HashMap<Object, PlaceHolder> ret = new HashMap<Object, PlaceHolder>();

		Iterator<Entry<Object, PlaceHolder>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Object, PlaceHolder> pairs = it.next();
			PlaceHolder p = (PlaceHolder) pairs.getValue();
			if ( Rect.intersects(p.frame, viewport) ) {
				ret.put(pairs.getKey(), p);
			}
		}		
		
		return ret;
		
	}
	
	public PlaceHolder getPlaceHolderForItem(Object item) {
		Log.d(TAG, " returing item: " + map.get(item));
		return map.get(item);
	}

	public int getContentWidth() {
		return Math.max(maxRight, width);
	}

	public int getContentHeight() {
		return 0;
	}

	public PlaceHolder getItemAt(float x, float y) {
		PlaceHolder returnValue = null;

		for(PlaceHolder item : map.values()) {
			if(item.frame.contains((int)x, (int)y)) {
                returnValue =  item;
            }
	      
	    }
		
		return returnValue;
	}
	
	public void setDimensions(int measuredWidth, int measuredHeight) {
		this.width = measuredWidth;
		this.height = measuredHeight;
	}
}
