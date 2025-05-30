package com.bigcommerce.imports.catalog.product.inventory.dto;

public class SkuVariantInfo {

	private int productId;
	private int variantId;
	private boolean isSingleSkuProduct;

	public SkuVariantInfo(int productId, int variantId, boolean isSingleSkuProduct) {
		this.productId = productId;
		this.variantId = variantId;
		this.isSingleSkuProduct = isSingleSkuProduct;
	}

	public int getProductId() {
		return productId;
	}

	public int getVariantId() {
		return variantId;
	}

	public boolean isSingleSkuProduct() {
		return isSingleSkuProduct;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public void setVariantId(int variantId) {
		this.variantId = variantId;
	}

	public void setSingleSkuProduct(boolean isSingleSkuProduct) {
		this.isSingleSkuProduct = isSingleSkuProduct;
	}

}
