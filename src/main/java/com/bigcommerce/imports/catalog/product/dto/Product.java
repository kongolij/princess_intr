package com.bigcommerce.imports.catalog.product.dto;

import java.util.List;

public class Product {

	public String productNumber;
	public List<Category> categories;

	public DisplayName displayName;
	public LongDescription longDescription;
	public SharedApplications paSharedApplications;
	public SharedFeatures paSharedFeatures;
	
	
	public boolean active;
	public boolean paLevy;
	public String paAvailabilityCode;
	public boolean paProductClearance;
	public String paProductStatus;
//	public String paCountryOfOrigin;

	public String brand;
	public List<Replace> replaces;
	public List<ProductRefernce> references;

	public List<Attribute> attributes;
	public List<Variant> variants;
	public List<Asset> assets;

	public String getProductNumber() {
		return productNumber;
	}

	public void setProductNumber(String productNumber) {
		this.productNumber = productNumber;
	}
	
	

	public DisplayName getDisplayName() {
		return displayName;
	}

	public void setDisplayName(DisplayName displayName) {
		this.displayName = displayName;
	}

	public LongDescription getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(LongDescription longDescription) {
		this.longDescription = longDescription;
	}

	public SharedApplications getPaSharedApplications() {
		return paSharedApplications;
	}

	public void setPaSharedApplications(SharedApplications paSharedApplications) {
		this.paSharedApplications = paSharedApplications;
	}

	public SharedFeatures getPaSharedFeatures() {
		return paSharedFeatures;
	}

	public void setPaSharedFeatures(SharedFeatures paSharedFeatures) {
		this.paSharedFeatures = paSharedFeatures;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public List<Category> getCategories() {
		return categories;
	}

	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}

//	public String getPaCountryOfOrigin() {
//		return paCountryOfOrigin;
//	}
//
//	public void setPaCountryOfOrigin(String paCountryOfOrigin) {
//		this.paCountryOfOrigin = paCountryOfOrigin;
//	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public List<Variant> getVariants() {
		return variants;
	}

	public void setVariants(List<Variant> variants) {
		this.variants = variants;
	}

	public List<Asset> getAssets() {
		return assets;
	}

	public void setAssets(List<Asset> assets) {
		this.assets = assets;
	}

	public boolean isPaLevy() {
		return paLevy;
	}

	public void setPaLevy(boolean paLevy) {
		this.paLevy = paLevy;
	}

	public String getPaAvailabilityCode() {
		return paAvailabilityCode;
	}

	public void setPaAvailabilityCode(String paAvailabilityCode) {
		this.paAvailabilityCode = paAvailabilityCode;
	}

	public boolean isPaProductClearance() {
		return paProductClearance;
	}

	public void setPaProductClearance(boolean paProductClearance) {
		this.paProductClearance = paProductClearance;
	}

	public String getPaProductStatus() {
		return paProductStatus;
	}

	public void setPaProductStatus(String paProductStatus) {
		this.paProductStatus = paProductStatus;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public List<Replace> getReplaces() {
		return replaces;
	}

	public void setReplaces(List<Replace> replaces) {
		this.replaces = replaces;
	}

	public List<ProductRefernce> getReferences() {
		return references;
	}

	public void setReferences(List<ProductRefernce> references) {
		this.references = references;
	}

}
