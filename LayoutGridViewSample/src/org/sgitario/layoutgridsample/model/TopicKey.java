package org.sgitario.layoutgridsample.model;

public class TopicKey extends Selectable {
	private String topicUrl;
	
	public TopicKey() {
	}
	
	public TopicKey(String name, String imageUrl) {
		this(name, imageUrl, false);
	}
	
	public TopicKey(String name, String imageUrl, boolean selected) {
		super(name, selected);
		
		this.setTopicUrl(imageUrl);
	}

	public String getTopicUrl() {
		return topicUrl;
	}

	public void setTopicUrl(String topicUrl) {
		this.topicUrl = topicUrl;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof String) {
			return this.getName().equals(o);
		} else {
			return super.equals(o);
		}
	}
}
