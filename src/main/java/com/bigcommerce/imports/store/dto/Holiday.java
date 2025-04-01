package com.bigcommerce.imports.store.dto;

import java.util.List;

public class Holiday {

	private String name;
	private int day;
	private List<String> hours; // e.g., ["10:00|17:00"]

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public List<String> getHours() {
		return hours;
	}

	public void setHours(List<String> hours) {
		this.hours = hours;
	}
}
