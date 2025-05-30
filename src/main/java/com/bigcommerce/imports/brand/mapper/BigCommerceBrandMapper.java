package com.bigcommerce.imports.brand.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bigcommerce.imports.brand.dto.Brand;

public class BigCommerceBrandMapper {

	public static JSONObject mapBrandToBigCommerceJson(Brand brand) {
		JSONObject json = new JSONObject();

		json.put("name", brand.getDisplayName());

		if (brand.getTitleText() != null) {
			json.put("page_title", brand.getDisplayName());
		}

		List<String> keywords = new ArrayList<>();
		if (brand.getSeoKeywords() != null && !brand.getSeoKeywords().isBlank()) {
			keywords = Arrays.stream(brand.getSeoKeywords().split(",")).map(String::trim).filter(s -> !s.isEmpty())
					.toList();
		} else if (brand.getDisplayName() != null && !brand.getDisplayName().isBlank()) {
			keywords = List.of(brand.getDisplayName().trim());
		}

		json.put("meta_keywords", new JSONArray(keywords));

		if (brand.getSeoDescription() != null) {
			json.put("meta_description", brand.getSeoDescription());
		}

		if (brand.getDocumentsFileName() != null && !brand.getDocumentsFileName().isBlank()) {
			String imageUrl = "https://www.princessauto.com/ccstore/v1/images/?source=/file/collections/"
					+ brand.getDocumentsFileName().trim();
			json.put("image_url", imageUrl);
		}

//	        if (brand.getSeoURLSlug() != null && !brand.getSeoURLSlug().isBlank()) {
//	            JSONObject customUrl = new JSONObject();
//	            customUrl.put("url", "/" + brand.getSeoURLSlug().trim());
//	            customUrl.put("is_customized", true);
//	            json.put("custom_url", customUrl);
//	        }

		return json;
	}

	public static JSONObject buildBrandMetafieldJson(Brand brand, int brandId) {
		JSONObject metafield = new JSONObject();

		metafield.put("permission_set", "write_and_sf_access");
		metafield.put("namespace", "brand_external_id");
		metafield.put("key", "external_id");

		// Use ID or fallback to displayName
		String externalId = brand.getId() != null ? brand.getId().trim() : brand.getDisplayName();
		metafield.put("value", externalId != null ? externalId : "unknown");

		metafield.put("description", "External system reference ID");
		metafield.put("resource_id", brandId);

		return metafield;
	}
}
