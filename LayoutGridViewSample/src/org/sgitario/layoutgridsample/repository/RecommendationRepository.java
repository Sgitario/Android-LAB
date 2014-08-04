package org.sgitario.layoutgridsample.repository;

import java.util.List;

import org.sgitario.layoutgridsample.model.Feed;

/**
 * Repository to retrieve the user recommendations depending on the max and the max items to show.
 * 
 * @author jhilario
 */
public interface RecommendationRepository {
	/**
	 * Calculate the tile response with the user recommendations.
	 * 
	 * @param page Page to retrieve.
	 * @param maxItems Max items to retrieve.
	 * @return Tile response with user recommendations.
	 */
	public List<Feed> getTrendingTopics(int page, int maxItems);
	
}
