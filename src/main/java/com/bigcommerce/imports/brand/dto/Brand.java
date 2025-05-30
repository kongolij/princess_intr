package com.bigcommerce.imports.brand.dto;

public class Brand {

	private String id;
	private String displayName;
	private String documentsFileName;
	private String altText;
	private String titleText;
	private Boolean active;
	private String seoDescription;
	private String seoKeywords;
	private String seoTitle;
	private String seoURLSlug;
	private String fixedChildProducts;

	// Getters and setters

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDocumentsFileName() {
		return documentsFileName;
	}

	public void setDocumentsFileName(String documentsFileName) {
		this.documentsFileName = documentsFileName;
	}

	public String getAltText() {
		return altText;
	}

	public void setAltText(String altText) {
		this.altText = altText;
	}

	public String getTitleText() {
		return titleText;
	}

	public void setTitleText(String titleText) {
		this.titleText = titleText;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getSeoDescription() {
		return seoDescription;
	}

	public void setSeoDescription(String seoDescription) {
		this.seoDescription = seoDescription;
	}

	public String getSeoKeywords() {
		return seoKeywords;
	}

	public void setSeoKeywords(String seoKeywords) {
		this.seoKeywords = seoKeywords;
	}

	public String getSeoTitle() {
		return seoTitle;
	}

	public void setSeoTitle(String seoTitle) {
		this.seoTitle = seoTitle;
	}

	public String getSeoURLSlug() {
		return seoURLSlug;
	}

	public void setSeoURLSlug(String seoURLSlug) {
		this.seoURLSlug = seoURLSlug;
	}

	public String getFixedChildProducts() {
		return fixedChildProducts;
	}

	public void setFixedChildProducts(String fixedChildProducts) {
		this.fixedChildProducts = fixedChildProducts;
	}
}
