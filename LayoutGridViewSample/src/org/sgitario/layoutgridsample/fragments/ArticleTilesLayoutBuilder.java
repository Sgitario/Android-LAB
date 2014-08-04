package org.sgitario.layoutgridsample.fragments;

import java.util.List;

import org.sgitario.layoutgridsample.layout.Layout;
import org.sgitario.layoutgridsample.layout.LayoutBuilder;
import org.sgitario.layoutgridsample.model.Feed;

import android.util.SparseArray;

/**
 * This layout will apply when: 
 * 
 * Pos: 0 (33% and 65%) -> It's an article
 * 
 * @author jhilario
 *
 */
public class ArticleTilesLayoutBuilder implements LayoutBuilder {
	
	private SparseArray<Layout> matrix = new SparseArray<Layout>();
	
	public ArticleTilesLayoutBuilder() {
		matrix.put(0, new Layout(33, 100));
	}

	/**
	 * If among the next feeds, there is an article content.
	 */
	@Override
	public boolean isFor(List<Feed> feeds) {
		
		return feeds != null && !feeds.isEmpty()
				&& feeds.get(0).getType().equalsIgnoreCase("news");
	}

	@Override
	public int getNumItems() {
		return matrix.size();
	}

	@Override
	public Layout getLayoutFor(int position) {
		return matrix.get(position);
	}

}