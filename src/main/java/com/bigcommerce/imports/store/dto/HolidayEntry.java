package com.bigcommerce.imports.store.dto;

import java.util.List;

public class HolidayEntry {

	private String name;
	private int day;
	private List<String> hours;
	private String month; // Add this for month tracking

	// Getters and setters
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

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

}
