package com.bigcommerce.imports.store.dto;

import java.util.List;
import java.util.Map;

public class StoreHours {

	private String storeId;
	private String name;
	private String timeZone;
	private List<WorkingDay> workingDays;
	private Map<String, List<Holiday>> holidays;

	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public List<WorkingDay> getWorkingDays() {
		return workingDays;
	}

	public void setWorkingDays(List<WorkingDay> workingDays) {
		this.workingDays = workingDays;
	}

	public Map<String, List<Holiday>> getHolidays() {
		return holidays;
	}

	public void setHolidays(Map<String, List<Holiday>> holidays) {
		this.holidays = holidays;
	}

}
