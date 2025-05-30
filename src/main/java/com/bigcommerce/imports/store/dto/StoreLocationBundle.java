package com.bigcommerce.imports.store.dto;

import java.util.Map;

public class StoreLocationBundle {

	private Location location;
	private Map<String, String> metafields;

	public StoreLocationBundle(Location location, Map<String, String> metafields) {
		this.location = location;
		this.metafields = metafields;
	}

	public Location getLocation() {
		return location;
	}

	public Map<String, String> getMetafields() {
		return metafields;
	}
}
