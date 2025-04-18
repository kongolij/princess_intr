package com.bigcommerce.imports.catalog.product.dto;

import java.util.List;

public class Product {

	public String productNumber;
	public List<String> categories;
	public boolean active;
	public String paCountryOfOrigin;
	public List<Attribute> attributes;
	public List<Variant> variants;
	public List<Asset> assets;

	public String getProductNumber() {
		return productNumber;
	}

	public void setProductNumber(String productNumber) {
		this.productNumber = productNumber;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getPaCountryOfOrigin() {
		return paCountryOfOrigin;
	}

	public void setPaCountryOfOrigin(String paCountryOfOrigin) {
		this.paCountryOfOrigin = paCountryOfOrigin;
	}

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

}
