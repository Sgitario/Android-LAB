package org.sgitario.layoutgridsample.builder;

import java.util.ArrayList;
import java.util.List;

import org.sgitario.layoutgridsample.fragments.ArticleTilesLayoutBuilder;
import org.sgitario.layoutgridsample.layout.DefaultTilesLayoutBuilder;
import org.sgitario.layoutgridsample.layout.LayoutBuilder;
import org.sgitario.layoutgridsample.model.Feed;

public class LayoutBuilderFactory {
	
	private static final List<LayoutBuilder> layoutBuilders = new ArrayList<LayoutBuilder>();
	private static final DefaultTilesLayoutBuilder defaultLayoutBuilder = new DefaultTilesLayoutBuilder();
	
	static {
		layoutBuilders.add(new ArticleTilesLayoutBuilder());
	}
	
	public static LayoutBuilder get(List<Feed> feeds) {
		LayoutBuilder result = defaultLayoutBuilder;
		
		for (LayoutBuilder layoutBuilder : layoutBuilders) {
			if (layoutBuilder.isFor(feeds) == true) {
				result = layoutBuilder;
			}
		}
		
		return result;
	}
}
