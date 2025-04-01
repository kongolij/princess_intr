package com.bigcommerce.imports.store.dto;

public class SpecialHour {

	private String label;
	private String date; // Format: yyyy-MM-dd
	private boolean open;
	private String opening;
	private String closing;
	private boolean all_day;
	private boolean annual;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public String getOpening() {
		return opening;
	}

	public void setOpening(String opening) {
		this.opening = opening;
	}

	public String getClosing() {
		return closing;
	}

	public void setClosing(String closing) {
		this.closing = closing;
	}

	public boolean isAll_day() {
		return all_day;
	}

	public void setAll_day(boolean all_day) {
		this.all_day = all_day;
	}

	public boolean isAnnual() {
		return annual;
	}

	public void setAnnual(boolean annual) {
		this.annual = annual;
	}
}
