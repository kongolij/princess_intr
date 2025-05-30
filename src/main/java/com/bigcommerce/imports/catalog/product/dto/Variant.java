package com.bigcommerce.imports.catalog.product.dto;

import java.util.List;

public class Variant {

//	"skuNumber": "8669293",
//	"active": true,
//	"paShippable": "Y-Yes",
//	"paSkuClearance": false,
//	"paArrivalDate": "",
//	"paAvailabilityCode": "",
//	"paSkuStatus": "A",
//	"paVendorNumber": "006883",
//	"paVendorPartNumber": "15RX324 BLACK",
//	"paUPC": "",
//	"paHeight": "0.2",
//	"paLength": "16.0",
//	"paWidth": "8.0",
//	"paWeight": "0.2",
//	"paCountryOfOrigin": "CN",

	public String skuNumber;
	public boolean active;
	public String paShippable;
	public boolean paSkuClearance;
	public String paArrivalDate;
	public String paAvailabilityCode;
	public String paSkuStatus;

	public String paVendorNumber;
	public String paVendorPartNumber;
	public String paUPC;
	public double paHeight;
	public double paLength;
	public double paWidth;
	public double paWeight;
	public String paCountryOfOrigin;

	public Video videos;

	public WebLink webLinks;

//	
//	"paProductStatus": "Active",
//	"paHeight": "0.2",
//	"paLength": "16.0",
//	"paWidth": "8.0",
//	"paWeight": "0.2",
//	"paCountryOfOrigin": "CN",

	public List<Attribute> attributes;
	public List<OptionValue> option_values;
	public List<Asset> assets;

	public String getSkuNumber() {
		return skuNumber;
	}

	public void setSkuNumber(String skuNumber) {
		this.skuNumber = skuNumber;
	}

	public boolean isPaSkuClearance() {
		return paSkuClearance;
	}

	public void setPaSkuClearance(boolean paSkuClearance) {
		this.paSkuClearance = paSkuClearance;
	}

	public String getPaAvailabilityCode() {
		return paAvailabilityCode;
	}

	public void setPaAvailabilityCode(String paAvailabilityCode) {
		this.paAvailabilityCode = paAvailabilityCode;
	}

	public String getPaShippable() {
		return paShippable;
	}

	public void setPaShippable(String paShippable) {
		this.paShippable = paShippable;
	}

	public String getPaSkuStatus() {
		return paSkuStatus;
	}

	public void setPaSkuStatus(String paSkuStatus) {
		this.paSkuStatus = paSkuStatus;
	}

	public String getPaVendorNumber() {
		return paVendorNumber;
	}

	public void setPaVendorNumber(String paVendorNumber) {
		this.paVendorNumber = paVendorNumber;
	}

	public String getPaVendorPartNumber() {
		return paVendorPartNumber;
	}

	public void setPaVendorPartNumber(String paVendorPartNumber) {
		this.paVendorPartNumber = paVendorPartNumber;
	}

	public String getPaUPC() {
		return paUPC;
	}

	public void setPaUPC(String paUPC) {
		this.paUPC = paUPC;
	}

	public double getPaHeight() {
		return paHeight;
	}

	public void setPaHeight(double paHeight) {
		this.paHeight = paHeight;
	}

	public double getPaLength() {
		return paLength;
	}

	public void setPaLength(double paLength) {
		this.paLength = paLength;
	}

	public double getPaWidth() {
		return paWidth;
	}

	public void setPaWidth(double paWidth) {
		this.paWidth = paWidth;
	}

	public double getPaWeight() {
		return paWeight;
	}

	public void setPaWeight(double paWeight) {
		this.paWeight = paWeight;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public List<OptionValue> getOption_values() {
		return option_values;
	}

	public void setOption_values(List<OptionValue> option_values) {
		this.option_values = option_values;
	}

	public List<Asset> getAssets() {
		return assets;
	}

	public void setAssets(List<Asset> assets) {
		this.assets = assets;
	}

	public String getPaCountryOfOrigin() {
		return paCountryOfOrigin;
	}

	public void setPaCountryOfOrigin(String paCountryOfOrigin) {
		this.paCountryOfOrigin = paCountryOfOrigin;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Video getVideos() {
		return videos;
	}

	public void setVideos(Video videos) {
		this.videos = videos;
	}

	public WebLink getWebLinks() {
		return webLinks;
	}

	public void setWebLinks(WebLink webLinks) {
		this.webLinks = webLinks;
	}

	public String getPaArrivalDate() {
		return paArrivalDate;
	}

	public void setPaArrivalDate(String paArrivalDate) {
		this.paArrivalDate = paArrivalDate;
	}

}
