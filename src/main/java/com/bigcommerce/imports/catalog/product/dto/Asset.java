package com.bigcommerce.imports.catalog.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Asset {

	private String type; // "image" or "manual"
	private String paDocumentsFileName; // matches schema
	private String description;
	public String documentId;

	public String documentLabel;

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

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getDocumentLabel() {
		return documentLabel;
	}

	public void setDocumentLabel(String documentLabel) {
		this.documentLabel = documentLabel;
	}

}
