package com.bigcommerce.imports.catalog.product.dto;

import java.util.HashMap;
import java.util.Map;

public class ProductOptionResolution {
	
	public Map<String, Integer> optionNameToId = new HashMap<>();
    public Map<String, Map<String, Integer>> labelToValueIdMap = new HashMap<>();
	public Map<String, Integer> getOptionNameToId() {
		return optionNameToId;
	}
	public void setOptionNameToId(Map<String, Integer> optionNameToId) {
		this.optionNameToId = optionNameToId;
	}
	public Map<String, Map<String, Integer>> getLabelToValueIdMap() {
		return labelToValueIdMap;
	}
	public void setLabelToValueIdMap(Map<String, Map<String, Integer>> labelToValueIdMap) {
		this.labelToValueIdMap = labelToValueIdMap;
	}

}
