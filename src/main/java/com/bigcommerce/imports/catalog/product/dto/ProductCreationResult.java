package com.bigcommerce.imports.catalog.product.dto;

import java.util.Map;

public class ProductCreationResult {

	private final int productId;
	private final Map<String, Integer> skuToVariantIdMap;

	public ProductCreationResult(int productId, Map<String, Integer> skuToVariantIdMap) {
		this.productId = productId;
		this.skuToVariantIdMap = skuToVariantIdMap;
	}

	public int getProductId() {
		return productId;
	}

	public Map<String, Integer> getSkuToVariantIdMap() {
		return skuToVariantIdMap;
	}
}
