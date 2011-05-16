package com.progress.codeshare.esbservice.convertMessage;

public class FileStructure {

	public FileStructure() {
	};

	private String name;

	private String displayColumn;

	private int startPosition = 0;

	private int size = 0;

	private String type;

	private int precision = 1;

	private String groupType = "";

	private int groupStartPosition = 0;

	private int groupSize = 0;

	private String groupValue = "";

	private boolean typeGroup = false;

	private boolean removeTrim = true;
	
	private String typeGroupNames = "";
	
	public String getDisplayColumn() {
		return displayColumn;
	}

	public void setDisplayColumn(String displayColumn) {
		this.displayColumn = displayColumn;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(int startPosition) {
		this.startPosition = startPosition;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public boolean isTypeGroup() {
		return typeGroup;
	}

	public void setTypeGroup(String typeGroup) {

		if (typeGroup.equalsIgnoreCase("false")) {
			this.typeGroup = false;
		}

		if (typeGroup.equalsIgnoreCase("true")) {
			this.typeGroup = true;
		}

	}

	public int getGroupSize() {
		return groupSize;
	}

	public void setGroupSize(int groupSize) {
		this.groupSize = groupSize;
	}

	public int getGroupStartPosition() {
		return groupStartPosition;
	}

	public void setGroupStartPosition(int groupStartPosition) {
		this.groupStartPosition = groupStartPosition;
	}

	public String getGroupType() {
		return groupType;
	}

	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}

	public String getGroupValue() {
		return groupValue;
	}

	public void setGroupValue(String groupValue) {
		this.groupValue = groupValue;
	}

	public String getTypeGroupNames() {
		return typeGroupNames;
	}

	public void setTypeGroupNames(String typeGroupNames) {
		this.typeGroupNames = typeGroupNames;
	}

	public boolean isRemoveTrim() {
		return removeTrim;
	}

	public void setRemoveTrim(String removeTrim) {
		
		if (removeTrim.equalsIgnoreCase("false")) {
			this.removeTrim = false;
		}

		if (removeTrim.equalsIgnoreCase("true")) {
			this.removeTrim = true;
		}
		
	}
}
