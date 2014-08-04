package org.sgitario.layoutgridsample.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.sgitario.layoutgridsample.model.Feed;

public class MockRecommendationRepository implements RecommendationRepository {

	private static List<String> IMAGE_URLS = new ArrayList<String>();
	private static List<String> IMAGE_PROFILE_URLS = new ArrayList<String>();
	private static List<String> NAMES_URLS = new ArrayList<String>();
	private static List<String> DESCRIPTIONS = new ArrayList<String>();	
	private static List<String> TOPIC_URLS = new ArrayList<String>();
	private static List<String> TITLES = new ArrayList<String>();
	
	private static Random RND = new Random(System.currentTimeMillis());
	
	static {
		// IMAGES
		IMAGE_URLS.add("http://designyoutrust.com/wp-content/uploads/2013/03/iron-man-3-movie-hd-wallpaper.jpg");
		IMAGE_URLS.add("http://wallpicshd.com/wp-content/uploads/2013/07/New-Wolverine-Movies-HD-Wallpaper-Full-HD.jpg");
		IMAGE_URLS.add("http://welivefilm.com/wp-content/uploads/2013/02/Snitch-2013-Movies-HD-Wallpapers-1080p1.jpg");
		IMAGE_URLS.add("http://www.wallsave.com/wallpapers/1366x768/movies-2012/239244/movies-amazing-spider-man-wide-hd-jootix-239244.jpg");
		IMAGE_URLS.add("http://www.tgraphic.com/userimages/new_module/TGraphic_com-Full-Wallpapers-Movies-Wallpaper_Various_Wallpaper_2560_2_L417.jpg");
		IMAGE_URLS.add("http://wallpapersblog.org/wp-content/uploads/avatar-full-hd-wallpaper-the-movie-jake-sully-2012.jpg");
		
		// NAMES
		NAMES_URLS.add("Joseph Marconi");
		NAMES_URLS.add("Nathan L");
		NAMES_URLS.add("Manuel Arteaga");
		NAMES_URLS.add("Jose Carvajal");
		NAMES_URLS.add("Kristan");
		NAMES_URLS.add("Puneet");
		NAMES_URLS.add("Fenix");
		
		// IMAGE PROFILE URLS
		IMAGE_PROFILE_URLS.add("http://big-share.com/wp-content/uploads/2014/01/profile.jpg");
		
		// DESCRIPTIONS
		DESCRIPTIONS.add("Yg disemprotkan wasit #WorldCup saat tendangan bebas adl Magic Spray berisi cairan nontoksik yg bisa hilang dlm 60dtk http://twitpic.com/e6exgy ");
		DESCRIPTIONS.add("Okay, time to move our #WorldCup conversation on. Mr Wayne Rooney. Should he be a pivotal part of #ENG going forward? Use #WCT");
		DESCRIPTIONS.add("#WorldCup fans who appeared to jump out of their wheelchairs investigated for ticket fraud http://fw.to/ulf6ojf  ");
		
		// TOPICS URL
		TOPIC_URLS.add("http://83.98.5.9/poc/youtube/vod?q=World+cup+football");
		
		// TITLES
		TITLES.add("Ranking the first 16 matches of the 2014 World Cup");
	}

	@Override
	public List<Feed> getTrendingTopics(int page, int numItems) {
		List<Feed> feeds = new ArrayList<Feed>();
		for (int index = 0; index < numItems; index++) {
			Feed feed = new Feed();
			feed.setId((page * numItems) + index);
			feed.setTitle("Title " + feed.getId());
			feed.setDescription("Description " + feed.getId());
			feed.setType("trending");
			feed.setImagePreviewUrl(getImage());
			
			Map<String, String> attrs = new HashMap<String, String>();
			attrs.put(Feed.POSITION, String.valueOf(feed.getId()));
			attrs.put("URL_TOPIC", getTopicUrl());
			feed.setAttrs(attrs);
			
			feeds.add(feed);
		}
		
		return feeds;
	}
	
	public String getImage() {
		return IMAGE_URLS.get(RND.nextInt(IMAGE_URLS.size()));
	}
	
	public String getName() {
		return NAMES_URLS.get(RND.nextInt(NAMES_URLS.size()));
	}
	
	public String getDescription() {
		return DESCRIPTIONS.get(RND.nextInt(DESCRIPTIONS.size()));
	}
	
	public String getProfileImage() {
		return IMAGE_PROFILE_URLS.get(RND.nextInt(IMAGE_PROFILE_URLS.size()));
	}
	
	public String getSource(List<String> filterBy) {
		return filterBy.get(RND.nextInt(filterBy.size()));
	}
	
	public String getTopicUrl() {
		return TOPIC_URLS.get(RND.nextInt(TOPIC_URLS.size()));
	}
	
	public String getTitle() {
		return TITLES.get(RND.nextInt(TITLES.size()));
	}
	
	public String getPreviewVideoUrl() {
		return "https://ia600504.us.archive.org/4/items/CaptainNedFijiDivingPart2/FijiSVCD2_512kb.mp4";
	}
}
