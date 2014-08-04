package org.sgitario.layoutgridsample.model;

import java.util.Date;

public class SocialInfo {
	private String title;
	private String profileUserUrl;
	private String username;
	private String description;
	private String source;
	private Date when;
	
	public String getProfileUserUrl() {
		return profileUserUrl;
	}
	
	public void setProfileUserUrl(String profileUserUrl) {
		this.profileUserUrl = profileUserUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getWhen() {
		return when;
	}

	public void setWhen(Date when) {
		this.when = when;
	}
}
