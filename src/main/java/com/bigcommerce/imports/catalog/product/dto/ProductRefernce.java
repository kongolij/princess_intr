package com.bigcommerce.imports.catalog.product.dto;

public class ProductRefernce {

//	"skuNumber": "8662884",
//	"parentProduct": "PA1000000454",
//	"type": "sparepart"
	private String	skuNumber;
	private String productNumber;
	private String parentProduct;
	private String type;


	
	public String getProductNumber() {
		return productNumber;
	}

	public void setProductNumber(String productNumber) {
		this.productNumber = productNumber;
	}

	public String getParentProduct() {
		return parentProduct;
	}

	public void setParentProduct(String parentProduct) {
		this.parentProduct = parentProduct;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSkuNumber() {
		return skuNumber;
	}

	public void setSkuNumber(String skuNumber) {
		this.skuNumber = skuNumber;
	}

	
}
