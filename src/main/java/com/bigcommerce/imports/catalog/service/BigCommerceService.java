package com.bigcommerce.imports.catalog.service;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.client.BigCommerceApiClient;
import com.bigcommerce.imports.catalog.constants.AccessToken;
import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
import com.bigcommerce.imports.catalog.constants.CommonConstants;
import com.bigcommerce.imports.catalog.constants.LocaleConstants;
import com.bigcommerce.imports.catalog.constants.StoreHash;
import com.bigcommerce.imports.catalog.dto.CategoryNode;
import com.bigcommerce.imports.catalog.mapper.BigCommerceCategoryMapper;

import io.micrometer.common.util.StringUtils;

@Component
public class BigCommerceService {

	private List<Integer> fetchAllCategoryIdsFromTree() throws Exception {

		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;
		int treeId = BigCommerceStoreConfig.CATEGORY_TREE_ID;

		List<Integer> categoryIds = new ArrayList<>();
		String treeUrl = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/trees/" + treeId
				+ "/categories";

		HttpURLConnection connection = (HttpURLConnection) new URL(treeUrl).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("X-Auth-Token", accessToken);
		connection.setRequestProperty("Accept", "application/json");

		int responseCode = connection.getResponseCode();
		if (responseCode != 200) {
			throw new RuntimeException("Failed to fetch category tree. Response code: " + responseCode);
		}

		String response;
		try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
			response = scanner.useDelimiter("\\A").next();
		}

		JSONObject jsonResponse = new JSONObject(response);
		JSONArray data = jsonResponse.getJSONArray("data");
		for (int i = 0; i < data.length(); i++) {
			JSONObject category = data.getJSONObject(i);
			categoryIds.add(category.getInt("id"));
		}

		return categoryIds;
	}

	public List<Integer> getCategoryIdsByTreeId() throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;
		int treeId = BigCommerceStoreConfig.CATEGORY_TREE_ID;

		String url = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/trees/" + treeId + "/categories";

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("X-Auth-Token", accessToken);
		connection.setRequestProperty("Accept", "application/json");

		int responseCode = connection.getResponseCode();
		if (responseCode != 200) {
			throw new RuntimeException("Failed to fetch categories. HTTP code: " + responseCode);
		}

		String response;
		try (Scanner scanner = new Scanner(connection.getInputStream())) {
			response = scanner.useDelimiter("\\A").next();
		}

		JSONObject jsonResponse = new JSONObject(response);
		JSONArray data = jsonResponse.getJSONArray("data");

		List<Integer> allCategoryIds = new ArrayList<>();
		for (int i = 0; i < data.length(); i++) {
			JSONObject category = data.getJSONObject(i);
			collectCategoryIds(category, allCategoryIds);
		}

		return allCategoryIds;
	}

	private void collectCategoryIds(JSONObject category, List<Integer> result) {
		result.add(category.getInt("id"));

		if (category.has("children")) {
			JSONArray children = category.getJSONArray("children");
			for (int i = 0; i < children.length(); i++) {
				JSONObject child = children.getJSONObject(i);
				collectCategoryIds(child, result);
			}
		}
	}

	public Map<String, Integer> getExternalToInternalCategoryMap() throws Exception {

		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;
		String namespaceEncoded = URLEncoder.encode(CommonConstants.CATEGORY_EXTERNAL_ID_NAMESPACE,
				StandardCharsets.UTF_8.toString());

		Map<String, Integer> categoryMap = new HashMap<>();

		List<Integer> categoryIds = fetchAllCategoryIdsFromTree();

		for (Integer categoryId : categoryIds) {
			String metafieldUrl = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/categories/"
					+ categoryId + "/metafields" + "?namespace=" + namespaceEncoded + "&key="
					+ CommonConstants.CATEGORY_EXTERNAL_ID_KEY;

			HttpURLConnection connection = (HttpURLConnection) new URL(metafieldUrl).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("X-Auth-Token", accessToken);
			connection.setRequestProperty("Accept", "application/json");

			int responseCode = connection.getResponseCode();
			if (responseCode != 200)
				continue;

			String response;
			try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
				response = scanner.useDelimiter("\\A").next();
			}

			JSONObject jsonResponse = new JSONObject(response);
			JSONArray data = jsonResponse.getJSONArray("data");
			for (int i = 0; i < data.length(); i++) {
				JSONObject metafield = data.getJSONObject(i);
				String namespace = metafield.getString("namespace");
				String key = metafield.getString("key");
				if ("external_id".equals(key)) {
					String value = metafield.getString("value");
					categoryMap.put(value, categoryId);
				}
			}
		}

		return categoryMap;
	}

	public Map<String, Integer> getBCExternalToInternalCategoryMap(String locale) throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		Map<String, Integer> categoryMap = new HashMap<>();
		int page = 1;
		boolean hasNext = true;

		while (hasNext) {

			String namespaceEncoded = URLEncoder.encode(CommonConstants.CATEGORY_EXTERNAL_ID_NAMESPACE+ "_tree_" + BigCommerceStoreConfig.CATEGORY_TREE_ID, StandardCharsets.UTF_8.toString());
//			String namespaceEncoded = URLEncoder.encode(CommonConstants.CATEGORY_EXTERNAL_ID_NAMESPACE,
//					StandardCharsets.UTF_8.toString());

			String url = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/categories/metafields"
					+ "?namespace=" + namespaceEncoded + "&key=" + CommonConstants.CATEGORY_EXTERNAL_ID_KEY
					+ "&limit=250&page=" + page;

			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("X-Auth-Token", accessToken);
			connection.setRequestProperty("Accept", "application/json");

			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				throw new RuntimeException("Failed to fetch data. Response code: " + responseCode);
			}

			String response;
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				response = scanner.useDelimiter("\\A").next();
			}

			JSONObject jsonResponse = new JSONObject(response);
			JSONArray data = jsonResponse.getJSONArray("data");

			for (int i = 0; i < data.length(); i++) {
				JSONObject metafield = data.getJSONObject(i);
				String externalId = metafield.getString("value");
				int internalId = metafield.getInt("resource_id");
				categoryMap.put(externalId, internalId);
			}

			JSONObject pagination = jsonResponse.getJSONObject("meta").getJSONObject("pagination");
			int totalPages = pagination.getInt("total_pages");
			hasNext = page < totalPages;
			page++;
		}

		return categoryMap;
	}

	public Map<String, Integer> getBCExternalToInternalBrandMap(String locale) throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;
		Map<String, Integer> brandMap = new HashMap<>();
		int page = 1;
		boolean hasNext = true;

		while (hasNext) {
			String namespaceEncoded = URLEncoder.encode(CommonConstants.BRAND_EXTERNAL_ID_NAMESPACE,
					StandardCharsets.UTF_8.toString());

			String url = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/brands/metafields"
					+ "?namespace=" + namespaceEncoded + "&key=" + CommonConstants.BRAND_EXTERNAL_ID_KEY
					+ "&limit=250&page=" + page;

			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("X-Auth-Token", accessToken);
			connection.setRequestProperty("Accept", "application/json");

			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				throw new RuntimeException("Failed to fetch brand metafields. HTTP code: " + responseCode);
			}

			String response;
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				response = scanner.useDelimiter("\\A").next();
			}

			JSONObject jsonResponse = new JSONObject(response);
			JSONArray data = jsonResponse.getJSONArray("data");

			for (int i = 0; i < data.length(); i++) {
				JSONObject metafield = data.getJSONObject(i);
				String externalId = metafield.getString("value");
				int internalId = metafield.getInt("resource_id");
				brandMap.put(externalId, internalId);
			}

			JSONObject pagination = jsonResponse.getJSONObject("meta").getJSONObject("pagination");
			int totalPages = pagination.getInt("total_pages");
			hasNext = page < totalPages;
			page++;
		}

		return brandMap;
	}

	public Map<Integer, List<Integer>> getCategoryPathMapFromTree() throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;
		int treeID = BigCommerceStoreConfig.CATEGORY_TREE_ID;

		String url = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/trees/" + treeID + "/categories";

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("X-Auth-Token", accessToken);
		connection.setRequestProperty("Accept", "application/json");

		int responseCode = connection.getResponseCode();
		if (responseCode != 200) {
			throw new RuntimeException("Failed to fetch category tree. Response code: " + responseCode);
		}

		String response;
		try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
			response = scanner.useDelimiter("\\A").next();
		}

		JSONObject jsonResponse = new JSONObject(response);
		JSONArray data = jsonResponse.getJSONArray("data");

		Map<Integer, List<Integer>> categoryPathMap = new HashMap<>();
		Queue<JSONObject> queue = new LinkedList<>();

		for (int i = 0; i < data.length(); i++) {
			queue.add(data.getJSONObject(i));
		}

		while (!queue.isEmpty()) {
			JSONObject category = queue.poll();
			int id = category.getInt("id");

			List<Integer> path = new ArrayList<>();
			JSONArray pathArray = category.optJSONArray("path");
			if (pathArray != null) {
				for (int i = 0; i < pathArray.length(); i++) {
					path.add(pathArray.getInt(i));
				}
			}
			path.add(id); // include self
			categoryPathMap.put(id, path);

			JSONArray children = category.optJSONArray("children");
			if (children != null) {
				for (int i = 0; i < children.length(); i++) {
					queue.add(children.getJSONObject(i));
				}
			}
		}

		return categoryPathMap;
	}

	public Map<Integer, List<Integer>> getCategoryPathMap() throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		Map<Integer, List<Integer>> categoryPathMap = new HashMap<>();
		Map<String, Integer> slugToIdMap = new HashMap<>();

		int page = 1;
		boolean hasNext = true;

		List<JSONObject> allCategories = new ArrayList<>();

		// Fetch all categories
		while (hasNext) {
			String url = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/categories?limit=250&page="
					+ page;

			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("X-Auth-Token", accessToken);
			connection.setRequestProperty("Accept", "application/json");

			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				throw new RuntimeException("Failed to fetch categories. Response code: " + responseCode);
			}

			String response;
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				response = scanner.useDelimiter("\\A").next();
			}

			JSONObject jsonResponse = new JSONObject(response);
			JSONArray data = jsonResponse.getJSONArray("data");

			for (int i = 0; i < data.length(); i++) {
				JSONObject category = data.getJSONObject(i);
				allCategories.add(category);
				slugToIdMap.put(category.getString("name_slug"), category.getInt("id"));
			}

			JSONObject pagination = jsonResponse.getJSONObject("meta").getJSONObject("pagination");
			int totalPages = pagination.getInt("total_pages");
			hasNext = page < totalPages;
			page++;
		}

		// Build path map
		for (JSONObject category : allCategories) {
			int entityId = category.getInt("id");
			String path = category.optString("path", "");

			if (path.isEmpty())
				continue;

			String[] segments = path.split("/");
			List<Integer> pathIds = new ArrayList<>();

			for (String segment : segments) {
				if (!segment.isBlank()) {
					Integer id = slugToIdMap.get(segment);
					if (id != null) {
						pathIds.add(id);
					}
				}
			}

			categoryPathMap.put(entityId, pathIds);
		}

		return categoryPathMap;
	}

	public Map<String, Integer> getFlattenedCategoryNameToIdMap(String locale, int treeId) throws Exception {
		Map<String, Integer> categoryMap = new HashMap<>();

		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken,
				"catalog/trees/" + treeId + "/categories", "GET");

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray rootNodes = responseJson.getJSONArray("data");

				// Recursively traverse all nodes and build the map
				for (int i = 0; i < rootNodes.length(); i++) {
					JSONObject root = rootNodes.getJSONObject(i);
					traverseCategoryTree(root, categoryMap);
				}

			}
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Failed to fetch category tree. Code: " + responseCode);
				System.err.println("Details: " + errorResponse);
			}
		}

		return categoryMap;
	}

	private void traverseCategoryTree(JSONObject node, Map<String, Integer> map) {
		String name = node.optString("name");
		int id = node.optInt("id");

		if (name != null && !name.isBlank()) {
			map.put(name.trim(), id);
		}

		if (node.has("children")) {
			JSONArray children = node.getJSONArray("children");
			for (int i = 0; i < children.length(); i++) {
				traverseCategoryTree(children.getJSONObject(i), map);
			}
		}
	}

	public Map<String, Integer> getCategoryNameToIdMap(String locale, int treeId) throws Exception {
		Map<String, Integer> categoryMap = new HashMap<>();

		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken,
				"catalog/trees/" + treeId + "/categories", "GET");

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray data = responseJson.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONObject cat = data.getJSONObject(i);
					String name = cat.optString("name");
					int id = cat.optInt("id");
					if (name != null && !name.isBlank()) {
						categoryMap.put(name.trim(), id);
					}
				}
			}
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Failed to fetch categories. Code: " + responseCode);
				System.err.println("Details: " + errorResponse);
			}
		}

		return categoryMap;
	}

	public void importCategoryTreeInThreads(List<CategoryNode> categoryTree, String locale, int treeId,
			Map<String, Integer> categoriesForTheChannel) throws Exception {
//		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//		List<Future<?>> futures = new ArrayList<>();
//
		for (CategoryNode rootCategory : categoryTree) {
//			futures.add(executor.submit(() -> {
			try {
				importCategoryRecursive(rootCategory, 0, null, 1, treeId, locale, categoriesForTheChannel);
			} catch (Exception e) {
				System.err.println("❌ Error importing category [" + rootCategory.getId() + "]: " + e.getMessage());
				e.printStackTrace();
			}
//			}));
		}
//
//        // Wait for all threads to finish
//		for (Future<?> future : futures) {
//			future.get(); // Will throw ExecutionException if a thread failed
//		}
//
//		executor.shutdown();
	}

	public void importCategoryTree(List<CategoryNode> categoryTree, String locale, int treeId,
			Map<String, Integer> categoriesForTheChannel) throws Exception {
		for (CategoryNode rootCategory : categoryTree) {
			importCategoryRecursive(rootCategory, 0, null, 1, treeId, locale, categoriesForTheChannel); // Start with
																										// root //
																										// categories at
																										// level 1
		}
	}

	private void importCategoryRecursive(CategoryNode categoryNode, int parentCategoryId, String parentPath, int level,
			int treeId, String locale, Map<String, Integer> categoriesForTheChannel) throws Exception {
		String externalId = categoryNode.getId();
		int currentCategoryId;

		JSONArray categoryJson = BigCommerceCategoryMapper.mapCategoryToBigCommerce(categoryNode, parentCategoryId,
				parentPath, level, treeId);

		if (categoriesForTheChannel.containsKey(externalId)) {
			currentCategoryId = categoriesForTheChannel.get(externalId);
			JSONObject existing =null;
			try {
			       existing = fetchExistingCategory(currentCategoryId, locale);
			}catch (Exception  e) {  
				    System.out.println("⚠️ Category ID " + currentCategoryId + " not found — trying to recreate");
		            currentCategoryId = createCategory(categoryJson, locale);
		            if (currentCategoryId != 0) {
						// set new metaData
						createCategoryMetaDataBatch(categoryNode, currentCategoryId, locale);

						String newImageFileName = CommonConstants.CATEGORY_IMAGE_URL + categoryNode.getImageFileName();
						if (!StringUtils.isEmpty(newImageFileName) || !"NULL".equalsIgnoreCase(newImageFileName)) {
							byte[] imageBytes = fetchImageBytesIfValid(newImageFileName, null, currentCategoryId, locale);
							if (imageBytes != null) {
								uploadCategoryImageToBigCommerce(currentCategoryId, newImageFileName, imageBytes, locale);
								System.out.println("📷 Uploaded image for new category ID: " + currentCategoryId);
							}
						} else {
							System.out.println("📷 xxxxxxxxxxx No image for new category ID: " + currentCategoryId);
						}
						 return;
					}
			}		
			JSONObject updated = categoryJson.getJSONObject(0);

			String expectedFrName = categoryNode.getLocalizedName().get(LocaleConstants.FR);
			String existingFrName = fetchFrenchMetafieldValue(currentCategoryId, LocaleConstants.FR);

			boolean needsUpdate = categoryNeedsUpdate(existing, updated);
			boolean frenchNameChanged = existingFrName == null || !existingFrName.equals(expectedFrName);

			if (needsUpdate) {
				JSONObject patchPayload = new JSONObject();

				if (!existing.optString("name").equals(updated.optString("name"))) {
					patchPayload.put("name", updated.optString("name"));
				}

				if (existing.optInt("parent_id", -1) != updated.optInt("parent_id", -2)) {
					patchPayload.put("parent_id", updated.optInt("parent_id", -2));
				}

				if (existing.has("is_visible") && updated.has("is_visible")
						&& existing.optBoolean("is_visible") != updated.optBoolean("is_visible")) {
					patchPayload.put("is_visible", updated.optBoolean("is_visible"));
				}

				if (!patchPayload.isEmpty()) {
					JSONArray patchArray = new JSONArray();
					patchArray.put(patchPayload);
					safeUpdateCategoryWithRetry(currentCategoryId, patchArray, locale, 2);
				} else {
					System.out.println("⏭️ Skipped main update (no change) for category ID: " + currentCategoryId);
				}

				// update metadata
				if (frenchNameChanged) {
					System.out.println("🔁 Updating French name metafield for category ID: " + currentCategoryId);
					updateFrenchNameMetafield(currentCategoryId, expectedFrName, locale);
				}

				// ✅ IMAGE LOGIC
		        String existingImageUrl = existing.optString("image_url", null);
		        String newImageFileName = CommonConstants.CATEGORY_IMAGE_URL+ categoryNode.getImageFileName();

		        if (!StringUtils.isEmpty(newImageFileName) || !"NULL".equalsIgnoreCase(newImageFileName)) { 
		            try {
		                byte[] imageBytes = downloadImageBytes(newImageFileName);
		                if (imageBytes != null) {
		                    boolean shouldUpload = false;

		                    if (existingImageUrl == null || existingImageUrl.isBlank()) {
		                        shouldUpload = true;
		                    } else {
		                        String existingFileName = existingImageUrl.substring(existingImageUrl.lastIndexOf('/') + 1);
		                        if (!existingFileName.equalsIgnoreCase(newImageFileName)) {
		                            deleteCategoryImage(currentCategoryId, locale);
		                            shouldUpload = true;
		                        }
		                    }

		                    if (shouldUpload) {
		                        uploadCategoryImageToBigCommerce(currentCategoryId, newImageFileName, imageBytes, locale);
		                        System.out.println("📷 Uploaded image for category ID: " + currentCategoryId);
		                    }
		                }
		            } catch (Exception e) {
		                System.err.println("⚠️ Failed image handling for category ID: " + currentCategoryId + " → " + e.getMessage());
		            }
		        }else {
		        	
						System.out.println("📷 yyyyy  No image for new category ID: " + currentCategoryId);
					
		        }

			}

		} else {
//			System.out.println("🆕 Creating new category External ID: " + externalId);
			currentCategoryId = createCategory(categoryJson, locale);

			if (currentCategoryId != 0) {
				// set new metaData
				createCategoryMetaDataBatch(categoryNode, currentCategoryId, locale);

				String newImageFileName = CommonConstants.CATEGORY_IMAGE_URL + categoryNode.getImageFileName();
				if (!StringUtils.isEmpty(newImageFileName) || !"NULL".equalsIgnoreCase(newImageFileName)) {
					byte[] imageBytes = fetchImageBytesIfValid(newImageFileName, null, currentCategoryId, locale);
					if (imageBytes != null) {
						uploadCategoryImageToBigCommerce(currentCategoryId, newImageFileName, imageBytes, locale);
						System.out.println("📷 Uploaded image for new category ID: " + currentCategoryId);
					}
				} else {
					System.out.println("📷 xxxxxxxxxxx No image for new category ID: " + currentCategoryId);
				}

//				categoriesForTheChannel.put(externalId, currentCategoryId);
			}
		}

		String newParentPath = parentPath != null ? parentPath + "/" + categoryNode.getSlug() : categoryNode.getSlug();

		for (CategoryNode childCategory : categoryNode.getChildren()) {
			importCategoryRecursive(childCategory, currentCategoryId, newParentPath, level + 1, treeId, locale,
					categoriesForTheChannel);
		}
	}

	// Recursive method to import category and its children
	private void importCategoryRecursive(CategoryNode categoryNode, int categoryId, String parentPath, int level,
			int treeId, String locale) throws Exception {

		JSONArray categoryJson = BigCommerceCategoryMapper.mapCategoryToBigCommerce(categoryNode, categoryId,
				parentPath, level, treeId);
		int newCategoryId = createCategory(categoryJson, locale);
		// add metadata
		if (newCategoryId != 0) {
			createCategoryMetaDataBatch(categoryNode, newCategoryId, locale);
//			createCategoryMetaData(categoryNode, newCategoryId,locale );
		}

		String newParentPath = parentPath != null ? parentPath + "/" + categoryNode.getSlug() : categoryNode.getSlug();

		for (CategoryNode childCategory : categoryNode.getChildren()) {
			importCategoryRecursive(childCategory, newCategoryId, newParentPath, level + 1, treeId, locale); // Increment
																												// level
																												// for
																												// each
			// child
		}
	}

//	private Integer findExistingCategoryId(String externalId, String storeHash, String accessToken) throws Exception {
//	    String url = "catalog/categories/metafields?namespace=Category Sync&key=external_id&value=" + URLEncoder.encode(externalId, "UTF-8");
//	    HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken, url, "GET");
//
//	    int responseCode = connection.getResponseCode();
//	    if (responseCode == 200) {
//	        try (Scanner scanner = new Scanner(connection.getInputStream())) {
//	            String responseBody = scanner.useDelimiter("\\A").next();
//	            JSONObject json = new JSONObject(responseBody);
//	            JSONArray data = json.optJSONArray("data");
//	            if (data != null && !data.isEmpty()) {
//	                return data.getJSONObject(0).getInt("resource_id"); // this is the category ID
//	            }
//	        }
//	    }
//	    return null; // Not found
//	}

	private void importCategory(Map<String, CategoryNode> categoryTree, CategoryNode categoryNode, int categoryId,
			String parentPath, int level, int treeId, String locale) throws Exception {

		JSONArray categoryJson = BigCommerceCategoryMapper.mapCategoryToBigCommerce(categoryNode, categoryId,
				parentPath, level, treeId);
		int newCategoryId = createCategory(categoryJson, locale);
		// add metadata
		if (newCategoryId != 0) {
			createCategoryMetaData(categoryNode, newCategoryId, locale);
		}

		String newParentPath = parentPath != null ? parentPath + "/" + categoryNode.getSlug() : categoryNode.getSlug();

		for (CategoryNode childCategory : categoryNode.getChildren()) {
			importCategoryRecursive(childCategory, newCategoryId, newParentPath, level + 1, treeId, locale); // Increment
																												// level
																												// for
																												// each
			// child
		}
	}

	public int createCategory(JSONArray categoryJson, String locale) throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String acessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, acessToken,
				"catalog/trees/categories", "POST");
		// Send the request with the category JSON
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = categoryJson.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}
		// Handle the response
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {

			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				System.out.println("Category " + responseJson.getJSONArray("data").getJSONObject(0).getString("name")
						+ " created successfully!");
				return responseJson.getJSONArray("data").getJSONObject(0).getInt("category_id");

			}
		} else {

			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.out.println("Failed to create Category " + categoryJson.get(0).toString());
				System.err.println("Failed to create product. Response code: " + responseCode);

			} catch (Exception e) {
				System.err.println("Error occurred while reading the error stream: " + e.getMessage());
				throw new Exception(e);
			}

			return 0; // Return 0 in case of failure
		}
	}

	private JSONObject fetchExistingCategory(int categoryId, String locale) throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken,
				"catalog/categories/" + categoryId, "GET");

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject jsonResponse = new JSONObject(responseBody);
				return jsonResponse.getJSONObject("data");
			}
		} else {
			throw new RuntimeException(
					"❌ Failed to fetch existing category ID: " + categoryId + ", response code: " + responseCode);
		}
	}

	private String fetchFrenchMetafieldValue(int categoryId, String locale) throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		HttpURLConnection conn = BigCommerceApiClient.createRequest(storeHash, accessToken,
				"catalog/categories/" + categoryId + "/metafields", "GET");

		int responseCode = conn.getResponseCode();
		if (responseCode == 200) {
			try (Scanner scanner = new Scanner(conn.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray data = responseJson.optJSONArray("data");
				for (int i = 0; i < data.length(); i++) {
					JSONObject meta = data.getJSONObject(i);
					if (CommonConstants.CATEGORY_NAME_LOCALIZATION_NAMESPACE.equals(meta.optString("namespace"))
							&& LocaleConstants.FR.equals(meta.optString("key"))) {
						return meta.optString("value");
					}
				}
			}
		} else {
			System.err.println(
					"⚠️ Failed to fetch metafields for category ID " + categoryId + " (HTTP " + responseCode + ")");
		}

		return null;
	}

	private boolean categoryNeedsUpdate(JSONObject existing, JSONObject updated) throws Exception {

		// check if BC category name, parent_id, visible or image url are changed
		if (!existing.optString("name").equals(updated.optString("name")))
			return true;

		if (existing.optInt("parent_id", -1) != updated.optInt("parent_id", -2))
			return true;

		if (existing.optBoolean("is_visible", true) != updated.optBoolean("is_visible", true))
			return true;

		if (StringUtils.isEmpty(existing.optString("image_url")))
			return true;

		return false;
	}

	private void safeUpdateCategoryWithRetry(int categoryId, JSONArray categoryJson, String locale, int maxRetries)
			throws Exception {
		int retry = 0;
		while (retry < maxRetries) {
			try {
				updateCategory(categoryId, categoryJson, locale);
				return;
			} catch (Exception e) {
				if (e.getMessage().contains("parent category with the id")
						&& e.getMessage().contains("was not found")) {
					System.out.println("⏳ Waiting for parent to be available in the tree... Retry: " + retry);
					Thread.sleep(1000); // wait 1 second
					retry++;
				} else {
					throw e; // rethrow if it's a different error
				}
			}
		}
		throw new RuntimeException("❌ Parent category not available after retries.");
	}

	private void updateCategory(int categoryId, JSONArray categoryJson, String locale) throws Exception {
		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken,
				"catalog/categories/" + categoryId, "PUT");

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = categoryJson.getJSONObject(0).toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			System.out.println("✅ Category updated successfully (ID: " + categoryId + ")");
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Failed to update category ID " + categoryId + ". Error: " + errorResponse);
			} catch (Exception e) {
				System.err.println("❌ Error reading error stream for updateCategory: " + e.getMessage());
				throw e;
			}
		}
	}

	private void updateFrenchNameMetafield(int categoryId, String frName, String locale) throws Exception {
		JSONObject meta = new JSONObject();
		meta.put("key", LocaleConstants.FR);
		meta.put("value", frName);
		meta.put("namespace", CommonConstants.CATEGORY_NAME_LOCALIZATION_NAMESPACE);
		meta.put("permission_set", "write_and_sf_access");
		meta.put("description", frName);
		meta.put("resource_id", categoryId);

		JSONArray metaArray = new JSONArray();
		metaArray.put(meta);

		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String token = AccessToken.getStoreAccessTokenByLocale(locale);

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, token,
				"catalog/categories/metafields", "POST");

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = metaArray.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			System.out.println("✅ Metafield updated successfully for category ID: " + categoryId);
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Failed to update metafield for category ID " + categoryId + ": " + errorResponse);
			}
		}
	}

	private void createCategoryMetaDataBatch(CategoryNode categoryNode, int catId, String locale) throws Exception {
		JSONArray metafields = new JSONArray();

		// Localized names
		for (Map.Entry<String, String> entry : categoryNode.getLocalizedName().entrySet()) {
			JSONObject field = new JSONObject();
			field.put("key", entry.getKey());
			field.put("value", entry.getValue());
			field.put("namespace", CommonConstants.CATEGORY_NAME_LOCALIZATION_NAMESPACE);
			field.put("permission_set", "write_and_sf_access");
			field.put("description", entry.getValue());
			field.put("resource_id", catId); // 🔥 Required for batch
			metafields.put(field);
		}

		// External ID
		JSONObject externalId = new JSONObject();
		externalId.put("key", CommonConstants.CATEGORY_EXTERNAL_ID_KEY);
		externalId.put("value", categoryNode.getId());
		externalId.put("namespace",
				CommonConstants.CATEGORY_EXTERNAL_ID_NAMESPACE + "_tree_" + BigCommerceStoreConfig.CATEGORY_TREE_ID);
		externalId.put("permission_set", "write_and_sf_access");
		externalId.put("description", "Original category ID from source feed");
		externalId.put("resource_id", catId); // 🔥 Required for batch
		metafields.put(externalId);

		// ✅ Make batch call
		createBigCommerceCategoryMetaDataBatch(metafields, locale);
	}

	private void createCategoryMetaData(CategoryNode categoryNode, int catId, String locale) throws Exception {

		for (Map.Entry<String, String> entry : categoryNode.getLocalizedName().entrySet()) {
			JSONObject jsonObject = new JSONObject();

			jsonObject.put("key", entry.getKey());
			jsonObject.put("value", entry.getValue());
			jsonObject.put("namespace", "Category Name Localization");
			jsonObject.put("permission_set", "write_and_sf_access");
			jsonObject.put("description", entry.getValue());

			createBigCommerceCategoryMetaData(jsonObject, catId, locale);
		}

	}

	public boolean createBigCommerceCategoryMetaDataBatch(JSONArray metafields, String locale) throws Exception {

		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken,
				"catalog/categories/metafields", "POST");

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = metafields.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
//				System.out.println("✅ Batch metafields created successfully.");
				return true;
			}
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Batch metafield creation failed. Code: " + responseCode);
				System.err.println("Error: " + errorResponse);
			}
			return false;
		}
	}

	public boolean createBigCommerceCategoryMetaData(JSONObject categoryJson, int catID, String locale)
			throws Exception {
		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String acessToken = AccessToken.getStoreAccessTokenByLocale(locale);

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, acessToken,
				"catalog/categories/" + catID + "/metafields", "POST");
		// Send the request with the category JSON
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = categoryJson.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}
		// Handle the response
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {

			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				System.out.println("Category metadata created successfully!");

				return true;

			}
		} else {

			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("Failed to create product. Response code: " + responseCode);
				System.err.println("Error message: " + connection.getResponseMessage());
				System.err.println("Error details: " + errorResponse);
			} catch (Exception e) {
				System.err.println("Error occurred while reading the error stream: " + e.getMessage());
				throw new Exception(e);
			}

			return false; // Return 0 in case of failure
		}
	}

	public int findCategoryByUrlPath(String urlPath, String locale) throws Exception {
		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String acessToken = AccessToken.getStoreAccessTokenByLocale(locale);
		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, acessToken, "catalog/categories",
				"GET");

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray categories = responseJson.getJSONArray("data");

				// Loop through categories and find the one matching the urlPath
				for (int i = 0; i < categories.length(); i++) {
					JSONObject category = categories.getJSONObject(i);
					String categoryPath = category.getJSONObject("custom_url").getString("url");
					if (categoryPath.equals(urlPath)) {
						return category.getInt("id"); // Return the matching category
					}
				}
			}
		}
		System.out.println("Category with path '" + urlPath + "'  found.");
		return 0;
	}

	public List<Integer> fetchAndFlattenCategories(int treeId, String locale) throws Exception {

		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken,
				"catalog/trees/" + treeId + "/categories", "GET");
		List<Integer> categoryIds = new ArrayList<>();

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray categories = responseJson.getJSONArray("data");

				// Loop through categories and find the one matching the urlPath
				for (int i = 0; i < categories.length(); i++) {
					JSONObject category = categories.getJSONObject(i);
					int categoryId = category.getInt("id");
					categoryIds.add(categoryId);

				}
			}
		}

		return categoryIds;
	}

	private byte[] fetchImageBytesIfValid(String imageUrl, String existingImageUrl, int categoryId, String locale) {
		try {
			if (imageUrl == null || imageUrl.isBlank())
				return null;

			byte[] imageBytes = downloadImageBytes(imageUrl);
			if (imageBytes == null)
				return null;

			boolean shouldUpload = false;

			if (existingImageUrl == null || existingImageUrl.isBlank()) {
				shouldUpload = true;
			} else {
				String existingFileName = existingImageUrl.substring(existingImageUrl.lastIndexOf('/') + 1);
				String newFileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

				if (!existingFileName.equalsIgnoreCase(newFileName)) {
					deleteCategoryImage(categoryId, locale);
					shouldUpload = true;
				}
			}

			return shouldUpload ? imageBytes : null;
		} catch (Exception e) {
			System.err.println("⚠️ Failed image handling for category ID: " + categoryId + " → " + e.getMessage());
			return null;
		}
	}

	private byte[] downloadImageBytes(String imageFileName) {
		try {
			String imageUrl = imageFileName;
			URL url = new URL(imageUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");

			try (InputStream inputStream = connection.getInputStream()) {
				return inputStream.readAllBytes();
			}
		} catch (Exception e) {
			System.err.println("❌ Failed to download image: " + imageFileName + " → " + e.getMessage());
			return null;
		}
	}

	public void uploadCategoryImageToBigCommerce(int categoryId, String fileName, byte[] imageBytes, String locale)
			throws Exception {

		String storeHash = BigCommerceStoreConfig.STORE_HASH;
		String accessToken = BigCommerceStoreConfig.ACCESS_TOKEN;

		String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
		String lineFeed = "\r\n";
		String urlString = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/categories/" + categoryId
				+ "/image";

		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("X-Auth-Token", accessToken);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		try (OutputStream outputStream = connection.getOutputStream();
				DataOutputStream writer = new DataOutputStream(outputStream)) {

			// Start boundary
			writer.writeBytes("--" + boundary + lineFeed);

			// File part headers
			writer.writeBytes(
					"Content-Disposition: form-data; name=\"image_file\"; filename=\"" + fileName + "\"" + lineFeed);
			writer.writeBytes("Content-Type: application/octet-stream" + lineFeed);
			writer.writeBytes(lineFeed);

			// File content
			writer.write(imageBytes);
			writer.writeBytes(lineFeed);

			// End boundary
			writer.writeBytes("--" + boundary + "--" + lineFeed);
			writer.flush();
		}
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			System.out.println("✅ Category image uploaded successfully for categoryId: " + categoryId);
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorBody = errorScanner.useDelimiter("\\A").hasNext() ? errorScanner.next() : "";
				System.err.println("❌ Failed to upload category image. Status: " + responseCode);
				System.err.println("❌ Error: " + errorBody);
			}
		}
	}

	public void deleteCategoryImage(int categoryId, String locale) throws Exception {
		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

		String url = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/catalog/categories/" + categoryId
				+ "/image";
		HttpURLConnection connection = BigCommerceApiClient.createRequest(storeHash, accessToken, url, "DELETE");

		int responseCode = connection.getResponseCode();
		if (responseCode == 204) {
			System.out.println("🗑️ Deleted existing image for category ID: " + categoryId);
		} else {
			System.err.println("❌ Failed to delete image for category ID " + categoryId + ". Status: " + responseCode);
		}
	}

	public void deleteCategories(List<Integer> categoryIds, String locale) throws Exception {
		String storeHash = StoreHash.getStoreHashByLocale(locale);
		String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

		for (Integer categoryId : categoryIds) {
			HttpURLConnection deleteConn = BigCommerceApiClient.createRequest(storeHash, accessToken,
					"catalog/categories/" + categoryId, "DELETE");

			int status = deleteConn.getResponseCode();
			if (status == 204) {
				System.out.println("Deleted category ID: " + categoryId);
			} else {
				System.err.println("Failed to delete category ID: " + categoryId + ", HTTP status: " + status);
			}
		}
	}

	public Integer createBigCommerceBrand(JSONObject brandJson) throws Exception {
		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/brands", "POST");

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = brandJson.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);

				if (responseJson.has("data")) {
					int brandId = responseJson.getJSONObject("data").getInt("id");
					System.out.printf("✅ Brand created: %s (ID: %d)%n", brandJson.getString("name"), brandId);
					return brandId;
				} else {
					System.err.println("⚠️ Brand created but no ID returned.");
					return null;
				}
			}
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Failed to create brand. Response code: " + responseCode);
				System.err.println("Error details: " + errorResponse);
			}
			return null;
		}
	}

	public boolean createBrandMetafields(int brandId, JSONArray metafields) throws Exception {
		String endpoint = "catalog/brands/metafields";
		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, endpoint, "POST");

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = metafields.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				System.out.println("✅ Metafields created for brand " + brandId);
				return true;
			}
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Failed to create metafields for brand " + brandId + ". Code: " + responseCode);
				System.err.println("Error details: " + errorResponse);
			}
			return false;
		}
	}

	public List<String> getAllBrandNames() throws Exception {
		List<String> allNames = new ArrayList<>();
		int page = 1;
		int limit = 50; // max supported by BC

		while (true) {
			String endpoint = String.format("catalog/brands?page=%d&limit=%d", page, limit);
			HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
					BigCommerceStoreConfig.ACCESS_TOKEN, endpoint, "GET");

			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				break;
			}

			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject json = new JSONObject(responseBody);
				JSONArray data = json.getJSONArray("data");

				if (data.isEmpty())
					break;

				for (int i = 0; i < data.length(); i++) {
					JSONObject brand = data.getJSONObject(i);
					String name = brand.optString("name");
					if (name != null && !name.isBlank()) {
						allNames.add(name.trim());
					}
				}

				// If fewer than limit, we're done
				if (data.length() < limit)
					break;

				page++; // move to next page
			}
		}

		return allNames;
	}

	public boolean deleteBrandByName(String brandName) throws Exception {
		String encodedName = URLEncoder.encode(brandName, StandardCharsets.UTF_8);
		String endpoint = String.format("catalog/brands?name=%s", encodedName);

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, endpoint, "DELETE");

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
			System.out.println("🗑️ Brand deleted by name: " + brandName);
			return true;
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.printf("❌ Failed to delete brand '%s'. Code: %d%n", brandName, responseCode);
				System.err.println("Error details: " + errorResponse);
			}
			return false;
		}
	}

	private void buildPathMapRecursive(JSONObject category, Map<Integer, List<Integer>> map) {
		int id = category.getInt("id");

		List<Integer> path = new ArrayList<>();
		if (category.has("path")) {
			JSONArray pathArray = category.getJSONArray("path");
			for (int i = 0; i < pathArray.length(); i++) {
				path.add(pathArray.getInt(i));
			}
		}
		path.add(id); // Add current category at the end

		map.put(id, path);

		if (category.has("children")) {
			JSONArray children = category.getJSONArray("children");
			for (int i = 0; i < children.length(); i++) {
				buildPathMapRecursive(children.getJSONObject(i), map);
			}
		}
	}

}
