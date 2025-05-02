package com.bigcommerce.imports.catalog.product.inventory.dto;

public class InventoryRecord {

	private String sku;
	private int storeId;
	private int availableQty;
	
	
	 public InventoryRecord(String sku, int storeId, int availableQty) {
	        this.sku = sku;
	        this.storeId = storeId;
	        this.availableQty = availableQty;
	    }
	 
	 
	public String getSku() {
		return sku;
	}
	public void setSku(String sku) {
		this.sku = sku;
	}
	public int getStoreId() {
		return storeId;
	}
	public void setStoreId(int storeId) {
		this.storeId = storeId;
	}
	public int getAvailableQty() {
		return availableQty;
	}
	public void setAvailableQty(int availableQty) {
		this.availableQty = availableQty;
	}
	
	
	
	
}
