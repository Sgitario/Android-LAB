package org.sgitario.layoutgridsample.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Feed model shared by all fragment types. 
 * 
 * @author jhilario
 */
public class Feed {
	public static final String POSITION = "POSITION";
	public static final String ALERT = "ALERT";
	public static final String TRUE_VALUE = "TRUE";
	
	private long id;
	private String title;
	private String imagePreviewUrl;
	private String type;
	private String description;
	private Date published;
	private String socialUrl;
	private Map<String, String> attrs = new HashMap<String, String>();
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImagePreviewUrl() {
		return imagePreviewUrl;
	}

	public void setImagePreviewUrl(String imagePreviewUrl) {
		this.imagePreviewUrl = imagePreviewUrl;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getAttrs() {
		return attrs;
	}

	public void setAttrs(Map<String, String> attrs) {
		this.attrs = attrs;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getPublished() {
		return published;
	}

	public void setPublished(Date published) {
		this.published = published;
	}

	public String getSocialUrl() {
		return socialUrl;
	}

	public void setSocialUrl(String socialUrl) {
		this.socialUrl = socialUrl;
	}
}
