package com.bigcommerce.imports.catalog.product.dto;

import java.util.List;

public class Variant {

	public String skuNumber;
    public boolean paSkuClearance;
    public String paAvailabilityCode;
    public boolean paLevy;
    public boolean paShippable;
    public String paProductStatus;
    public String paVendorNumber;
    public String paVendorPartNumber;
    public String paUPC;
    public double paHeight;
    public double paLength;
    public double paWidth;
    public double paWeight;
    public List<Attribute> attributes;
    public List<OptionValue> option_values;
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
	public boolean isPaLevy() {
		return paLevy;
	}
	public void setPaLevy(boolean paLevy) {
		this.paLevy = paLevy;
	}
	public boolean isPaShippable() {
		return paShippable;
	}
	public void setPaShippable(boolean paShippable) {
		this.paShippable = paShippable;
	}
	public String getPaProductStatus() {
		return paProductStatus;
	}
	public void setPaProductStatus(String paProductStatus) {
		this.paProductStatus = paProductStatus;
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

}
