package org.sgitario.layoutgridsample.layout;

import java.util.List;

import org.sgitario.layoutgridsample.model.Feed;

public interface LayoutBuilder {

	public boolean isFor(List<Feed> feeds);
	public int getNumItems();
	public Layout getLayoutFor(int position);
}
