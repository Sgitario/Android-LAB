package org.sgitario.layoutgridsample.model;

import java.util.List;

public class UserSettings {
	private String username;
	private List<TopicKey> selectedTopics;
	private List<String> selectedSocialNetworks;
	private String selectedTopic;
	private String location;
	private String dayOfWeek;
	private String time;
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public List<TopicKey> getSelectedTopics() {
		return selectedTopics;
	}

	public void setSelectedTopics(List<TopicKey> selectedTopics) {
		this.selectedTopics = selectedTopics;
	}

	public List<String> getSelectedSocialNetworks() {
		return selectedSocialNetworks;
	}

	public void setSelectedSocialNetworks(List<String> selectedSocialNetworks) {
		this.selectedSocialNetworks = selectedSocialNetworks;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getSelectedTopic() {
		return selectedTopic;
	}

	public void setSelectedTopic(String selectedTopic) {
		this.selectedTopic = selectedTopic;
	}
}
