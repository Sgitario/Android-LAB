package org.sgitario.layoutgridsample;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

public class PlaceHolder {
	public int itemIndex;
	public Object data;
	public Rect frame;
	public View view;
	public Bundle extras;

	public static PlaceHolder clone(PlaceHolder desc) {
		if (desc == null)
			return null;

		PlaceHolder fd = new PlaceHolder();
		fd.itemIndex = desc.itemIndex;
		fd.data = desc.data;
		fd.frame = new Rect(desc.frame);
		fd.view = desc.view;
		fd.extras = desc.extras;
		return fd;
	}
}
