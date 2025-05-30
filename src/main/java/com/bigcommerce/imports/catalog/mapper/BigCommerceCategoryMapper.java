package com.bigcommerce.imports.catalog.mapper;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bigcommerce.imports.catalog.dto.CategoryNode;

public class BigCommerceCategoryMapper {

	public static JSONArray mapCategoryToBigCommerce(CategoryNode categoryNode, int parentCatId, String parentPath,
			int level, int treeId) {

		JSONArray categoriesArray = new JSONArray();
		JSONObject categoryJson = new JSONObject();

		// Basic category fields
		categoryJson.put("name", categoryNode.getName());
//		categoryJson.put("category_uuid", categoryNode.getId());
		categoryJson.put("parent_id", parentCatId);

		categoryJson.put("tree_id", treeId);

		// Set views and sort_order as specified
		categoryJson.put("views", 0); // Initial view count
		categoryJson.put("sort_order", level); // Dynamic sort order based on level or other criteria

		// `search_keywords`
		String name = categoryNode.getName();
		String searchKeywords = String.join(",", name.split("[\\s\\-_]+"));
		categoryJson.put("search_keywords", searchKeywords);

		// Visibility
		categoryJson.put("is_visible", categoryNode.isActive());

		categoriesArray.put(categoryJson);

		return categoriesArray;
	}

	public static JSONArray mapCategoryToBigCommerceMetaData(CategoryNode categoryNode, int parentCatId,
			String parentPath, int treeId, int level) {

		JSONArray categoriesArray = new JSONArray();
		JSONObject categoryJson = new JSONObject();

		// Basic category fields
		categoryJson.put("name", categoryNode.getName());
//		categoryJson.put("category_uuid", categoryNode.getId());
		categoryJson.put("parent_id", parentCatId);
		categoryJson.put("tree_id", treeId);

		// Set views and sort_order as specified
		categoryJson.put("views", 0); // Initial view count
		categoryJson.put("sort_order", level); // Dynamic sort order based on level or other criteria

		// `search_keywords`
		String name = categoryNode.getName();
		String searchKeywords = String.join(",", name.split("[\\s\\-_]+"));
		categoryJson.put("search_keywords", searchKeywords);

		// Populate `meta_description`
		String metaDescription = "Explore our " + categoryNode.getName() + " for a variety of options in "
				+ (parentPath != null ? parentPath : "our collection") + ".";
		categoryJson.put("meta_description", metaDescription);

		// Visibility
		categoryJson.put("is_visible", true);

		categoriesArray.put(categoryJson);

		return categoriesArray;
	}

}
