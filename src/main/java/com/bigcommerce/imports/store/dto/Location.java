package com.bigcommerce.imports.store.dto;

import java.util.List;
import java.util.Map;

public class Location {

	private int id;
	private String code;
	private String label;
	private String description;
	private String type_id;
	private boolean enabled;
	private Map<String, DayHours> operating_hours; // derived from workingDays
	private String time_zone;

	private boolean managed_by_external_source;

	private Address address;
	private boolean storefront_visibility;

	private List<HolidayEntry> special_hours;

	private boolean pickUp;
	private boolean showOnPDP;
	private boolean showBopisMessage;
	private double latitude;
	private double longitude;

	private Map<String, String> frMetafields;

	// Getters and Setters

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType_id() {
		return type_id;
	}

	public void setType_id(String type_id) {
		this.type_id = type_id;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, DayHours> getOperating_hours() {
		return operating_hours;
	}

	public void setOperating_hours(Map<String, DayHours> operating_hours) {
		this.operating_hours = operating_hours;
	}

	public String getTime_zone() {
		return time_zone;
	}

	public void setTime_zone(String time_zone) {
		this.time_zone = time_zone;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public boolean isStorefront_visibility() {
		return storefront_visibility;
	}

	public void setStorefront_visibility(boolean storefront_visibility) {
		this.storefront_visibility = storefront_visibility;
	}

	public List<HolidayEntry> getSpecial_hours() {
		return special_hours;
	}

	public void setSpecial_hours(List<HolidayEntry> special_hours) {
		this.special_hours = special_hours;
	}

	public boolean isPickUp() {
		return pickUp;
	}

	public void setPickUp(boolean pickUp) {
		this.pickUp = pickUp;
	}

	public boolean isShowOnPDP() {
		return showOnPDP;
	}

	public void setShowOnPDP(boolean showOnPDP) {
		this.showOnPDP = showOnPDP;
	}

	public boolean isShowBopisMessage() {
		return showBopisMessage;
	}

	public void setShowBopisMessage(boolean showBopisMessage) {
		this.showBopisMessage = showBopisMessage;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public Map<String, String> getFrMetafields() {
		return frMetafields;
	}

	public void setFrMetafields(Map<String, String> frMetafields) {
		this.frMetafields = frMetafields;
	}

	public boolean isManaged_by_external_source() {
		return managed_by_external_source;
	}

	public void setManaged_by_external_source(boolean managed_by_external_source) {
		this.managed_by_external_source = managed_by_external_source;
	}

}
