package com.bigcommerce.imports.catalog.product.dto;

public class Asset {

	private String type; // "image" or "manual"
	private String paDocumentsFileName; // matches schema
	private String description;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getPaDocumentsFileName() {
		return paDocumentsFileName;
	}
	public void setPaDocumentsFileName(String paDocumentsFileName) {
		this.paDocumentsFileName = paDocumentsFileName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}



}
