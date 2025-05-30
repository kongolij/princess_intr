package com.bigcommerce.imports.catalog.product.prices.dto;

import java.math.BigDecimal;

public class VariantPrice {

	private String skuBr;
	private BigDecimal listPrice;
	private BigDecimal salePrice; // nullable

	public String getSkuBr() {
		return skuBr;
	}

	public void setSkuBr(String skuBr) {
		this.skuBr = skuBr;
	}

	public BigDecimal getListPrice() {
		return listPrice;
	}

	public void setListPrice(BigDecimal listPrice) {
		this.listPrice = listPrice;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

}
