package com.bigcommerce.imports.store.dto;

import java.util.List;

public class WorkingDay {

	private String day;
	private List<String> workingHours; // Format: ["09:00|17:00"]

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public List<String> getWorkingHours() {
		return workingHours;
	}

	public void setWorkingHours(List<String> workingHours) {
		this.workingHours = workingHours;
	}
}
