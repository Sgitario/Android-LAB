package org.sgitario.layoutgridsample.model;

public class Selectable {
	private long id;
	private String name;
	private boolean selected;
	
	public Selectable() {
	}
	
	public Selectable(String name) {
		this(name, false);
	}
	
	public Selectable(String name, boolean selected) {
		this.name = name;
		this.selected = selected;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
