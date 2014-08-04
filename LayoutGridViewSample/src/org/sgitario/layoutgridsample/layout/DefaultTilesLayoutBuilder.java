package org.sgitario.layoutgridsample.layout;

import java.util.List;

import org.sgitario.layoutgridsample.layout.LayoutBuilder;
import org.sgitario.layoutgridsample.model.Feed;

public class DefaultTilesLayoutBuilder implements LayoutBuilder {
	
	@Override
	public boolean isFor(List<Feed> feeds) {
		return true;
	}

	@Override
	public Layout getLayoutFor(int position) {
		return new Layout(33, 33);
	}

	@Override
	public int getNumItems() {
		return 1;
	}

}
