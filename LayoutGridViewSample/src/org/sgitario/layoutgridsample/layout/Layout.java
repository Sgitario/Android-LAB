package org.sgitario.layoutgridsample.layout;

public class Layout {
	public static final int MAX = -1;
	
	private final int widthPercentage;
	private final int heightPercentage;
	
	public Layout(int widthPercentage, int heightPercentage) {
		this.widthPercentage = widthPercentage;
		this.heightPercentage = heightPercentage;
	}

	public int getWidthPercentage() {
		return widthPercentage;
	}

	public int getHeightPercentage() {
		return heightPercentage;
	}
}
