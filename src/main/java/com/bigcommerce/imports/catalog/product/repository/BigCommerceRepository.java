package com.bigcommerce.imports.catalog.product.repository;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.client.BigCommerceApiClient;
import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
//import com.bigcommerce.imports.catalog.constants.StoreHash;
import com.bigcommerce.imports.catalog.product.dto.ProductCreationResult;
import com.bigcommerce.imports.catalog.product.inventory.dto.SkuVariantInfo;

@Component
public class BigCommerceRepository {

	//used on new product creation
	public ProductCreationResult createProductWithVariants(String locale, JSONObject productJson) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/products", "POST");
		// Send the request with the category JSON
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = productJson.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		// Handle the response
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {

			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				System.out.println("product created successfully!");

				Map<String, Integer> skuToVariantIdMap = extractSkuToVariantIdMap(responseJson);
				int productId = responseJson.getJSONObject("data").getInt("id");

				return new ProductCreationResult(productId, skuToVariantIdMap);
//				return responseJson.getJSONObject("data").getInt("id");

			}
		} else {
			// Print response for debugging
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("Failed to create product. Response code: " + responseCode);
				System.err.println("Error message: " + connection.getResponseMessage());
				System.err.println("Error details: " + errorResponse);
			} catch (Exception e) {
				System.err.println("Error occurred while reading the error stream: " + e.getMessage());
				throw new Exception(e);
			}
			return null; // Return 0 in case of failure
		}
	}
	
	

	// used on new produc creation
	public boolean assignProductToChannel(String locale, int productId, int channelId) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/products/channel-assignments", "PUT");

		JSONArray productChannelArray = new JSONArray();
		JSONObject productChannelJson = new JSONObject();
		productChannelJson.put("product_id", productId);
		productChannelJson.put("channel_id", channelId);
		productChannelArray.put(productChannelJson);

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = productChannelArray.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
			return true;
		} else {
			// Print response for debugging
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

	// used on new prouct creation
	public boolean setProductCustomFields(String locale, JSONArray productCustomFields, int productId)
			throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/products/" + productId + "/custom-fields", "POST");

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = productCustomFields.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {

			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				System.out.println("product assigned successfully!");
				return true;
			}
		} else {
			// Print response for debugging
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

	// used in new product creartion
	public boolean setVariantMetafieldsInBatches(String locale, Map<String, JSONArray> variantMetafields,
			JSONArray varaintStaticAttributeJson) throws Exception {
		// Combine all entries from map into one flat array
		JSONArray combinedPayload = new JSONArray();
		for (Map.Entry<String, JSONArray> entry : variantMetafields.entrySet()) {
			JSONArray localeArray = entry.getValue();
			for (int i = 0; i < localeArray.length(); i++) {
				combinedPayload.put(localeArray.getJSONObject(i));
			}
		}

		// 2. Add static (non-localized) variant metafields
		for (int i = 0; i < varaintStaticAttributeJson.length(); i++) {
			combinedPayload.put(varaintStaticAttributeJson.getJSONObject(i));
		}

		// Split into batches of 10
		List<JSONArray> batches = splitIntoBatches(combinedPayload, 10);
		boolean allSuccess = true;

		for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
			JSONArray batch = batches.get(batchIndex);

			HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
					BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/variants/metafields", "POST");

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = batch.toString().getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				try (Scanner scanner = new Scanner(connection.getInputStream())) {
					String responseBody = scanner.useDelimiter("\\A").next();
					System.out.println("✅ Batch " + (batchIndex + 1) + "/" + batches.size() + " created successfully!");
				}
			} else {
				allSuccess = false;
				try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
					String errorResponse = errorScanner.useDelimiter("\\A").next();
					System.err.println("❌ Batch " + (batchIndex + 1) + " failed. Response code: " + responseCode);
					System.err.println("Error message: " + connection.getResponseMessage());
					System.err.println("Error details: " + errorResponse);
				} catch (Exception e) {
					System.err.println("⚠️ Error reading error stream: " + e.getMessage());
					throw new Exception(e);
				}
			}
		}

		return allSuccess;
	}
	
	public boolean setProductMetafieldsInBatches(Map<String, JSONArray> productMetafields) throws Exception {
	    // 1. Flatten all localized metafields into one payload
	    JSONArray combinedPayload = new JSONArray();

	    for (Map.Entry<String, JSONArray> entry : productMetafields.entrySet()) {
	        JSONArray localeArray = entry.getValue();
	        for (int i = 0; i < localeArray.length(); i++) {
	            combinedPayload.put(localeArray.getJSONObject(i));
	        }
	    }

	    if (combinedPayload.length() == 0) {
	        System.out.println("⚠️ No product metafields to submit.");
	        return true;
	    }

	    // 2. Split into batches of 10
	    List<JSONArray> batches = splitIntoBatches(combinedPayload, 10);
	    boolean allSuccess = true;

	    for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
	        JSONArray batch = batches.get(batchIndex);

	        HttpURLConnection connection = BigCommerceApiClient.createRequest(
	                BigCommerceStoreConfig.STORE_HASH,
	                BigCommerceStoreConfig.ACCESS_TOKEN,
	                "catalog/products/metafields",
	                "POST"
	        );

	        connection.setRequestProperty("Content-Type", "application/json");
	        connection.setDoOutput(true);

	        try (OutputStream os = connection.getOutputStream()) {
	            byte[] input = batch.toString().getBytes("utf-8");
	            os.write(input, 0, input.length);
	        }

	        int responseCode = connection.getResponseCode();

	        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
	            try (Scanner scanner = new Scanner(connection.getInputStream())) {
	                String responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
	                System.out.println("✅ Batch " + (batchIndex + 1) + "/" + batches.size() + " submitted successfully.");
	            }
	        } else {
	            allSuccess = false;
	            try (Scanner scanner = new Scanner(connection.getErrorStream())) {
	                String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
	                System.err.println("❌ Batch " + (batchIndex + 1) + " failed. Response code: " + responseCode);
	                System.err.println("Error response: " + errorResponse);
	            } catch (Exception e) {
	                System.err.println("⚠️ Error reading error stream: " + e.getMessage());
	            }
	        }
	    }

	    return allSuccess;
	}



	//used for pricing and invnetoyr
	public Map<String, Pair<Integer, Integer>> getVariantProductAndIdsBySkus(List<String> skus) throws IOException {
	    String joinedSkus = String.join(",", skus);
	    String url = "https://api.bigcommerce.com/stores/" + BigCommerceStoreConfig.STORE_HASH
	            + "/v3/catalog/variants?sku=" + URLEncoder.encode(joinedSkus, StandardCharsets.UTF_8);

	    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
	    conn.setRequestProperty("X-Auth-Token", BigCommerceStoreConfig.ACCESS_TOKEN);
	    conn.setRequestProperty("Accept", "application/json");

	    int responseCode = conn.getResponseCode();
	    if (responseCode != 200) {
	        System.err.println("❌ Failed to fetch variants, code: " + responseCode);
	        return Collections.emptyMap();
	    }

	    String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	    JSONObject body = new JSONObject(json);
	    JSONArray data = body.getJSONArray("data");

	    Map<String, Pair<Integer, Integer>> skuToProductVariantId = new HashMap<>();
	    for (int i = 0; i < data.length(); i++) {
	        JSONObject variant = data.getJSONObject(i);
	        String sku = variant.getString("sku");
	        int variantId = variant.getInt("id");
	        int productId = variant.getInt("product_id");

	        skuToProductVariantId.put(sku, Pair.of(productId, variantId));
	    }

	    return skuToProductVariantId;
	}
	
	public Map<String, SkuVariantInfo> getVariantProductAndIdsBySkusUpdated(List<String> skus) throws IOException {
	    String joinedSkus = String.join(",", skus);
	    String url = "https://api.bigcommerce.com/stores/" + BigCommerceStoreConfig.STORE_HASH
	            + "/v3/catalog/variants?sku=" + URLEncoder.encode(joinedSkus, StandardCharsets.UTF_8);

	    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
	    conn.setRequestProperty("X-Auth-Token", BigCommerceStoreConfig.ACCESS_TOKEN);
	    conn.setRequestProperty("Accept", "application/json");

	    int responseCode = conn.getResponseCode();
	    if (responseCode != 200) {
	        System.err.println("❌ Failed to fetch variants, code: " + responseCode);
	        return Collections.emptyMap();
	    }

	    String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	    JSONObject body = new JSONObject(json);
	    JSONArray data = body.getJSONArray("data");

	    Map<String, SkuVariantInfo> skuToInfo = new HashMap<>();
	    for (int i = 0; i < data.length(); i++) {
	        JSONObject variant = data.getJSONObject(i);
	        String sku = variant.getString("sku");
	        int variantId = variant.getInt("id");
	        int productId = variant.getInt("product_id");

	        boolean hasOptions = variant.optJSONArray("option_values") != null &&
	                             variant.getJSONArray("option_values").length() > 0;

	        boolean isSingleSku = !hasOptions;

	        skuToInfo.put(sku, new SkuVariantInfo(productId, variantId, isSingleSku));
	    }

	    return skuToInfo;
	}
	
	// not used 
	public Map<String, Integer> getVariantIdsBySkus(List<String> skus) throws IOException {
		String joinedSkus = String.join(",", skus);
		String url = "https://api.bigcommerce.com/stores/" + BigCommerceStoreConfig.STORE_HASH
				+ "/v3/catalog/variants?sku=" + URLEncoder.encode(joinedSkus, StandardCharsets.UTF_8);

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("X-Auth-Token", BigCommerceStoreConfig.ACCESS_TOKEN);
		conn.setRequestProperty("Accept", "application/json");

		int responseCode = conn.getResponseCode();
		if (responseCode != 200) {
			System.err.println("Failed to fetch variants, code: " + responseCode);
			return Collections.emptyMap();
		}

		String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		JSONObject body = new JSONObject(json);
		JSONArray data = body.getJSONArray("data");

		Map<String, Integer> skuToVariantId = new HashMap<>();
		for (int i = 0; i < data.length(); i++) {
			JSONObject variant = data.getJSONObject(i);
			skuToVariantId.put(variant.getString("sku"), variant.getInt("id"));
		}

		return skuToVariantId;
	}

	public boolean setVariantMetafields(String locale, Map<String, JSONArray> variantMetafields) throws Exception {
		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/variants/metafields", "POST");

		JSONArray combinedPayload = new JSONArray();

		for (Map.Entry<String, JSONArray> entry : variantMetafields.entrySet()) {
			JSONArray localeArray = entry.getValue();
			for (int i = 0; i < localeArray.length(); i++) {
				combinedPayload.put(localeArray.getJSONObject(i));
			}
		}

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = combinedPayload.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				System.out.println("✅ Variant metafields created successfully!");
				return true;
			}
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.err.println("❌ Failed to create variant metafields. Response code: " + responseCode);
				System.err.println("Error message: " + connection.getResponseMessage());
				System.err.println("Error details: " + errorResponse);
			} catch (Exception e) {
				System.err.println("⚠️ Error occurred while reading the error stream: " + e.getMessage());
				throw new Exception(e);
			}
			return false;
		}
	}

	public void uploadProductImageToBigCommerce(int productId, String fileName, byte[] imageBytes) throws Exception {

		String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
		String lineFeed = "\r\n";
		String urlString = "https://api.bigcommerce.com/stores/" + BigCommerceStoreConfig.STORE_HASH
				+ "/v3/catalog/products/" + productId + "/images";

		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("X-Auth-Token", BigCommerceStoreConfig.ACCESS_TOKEN);
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
			System.out.println("✅ Product image uploaded successfully for categoryId: " + productId);
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorBody = errorScanner.useDelimiter("\\A").hasNext() ? errorScanner.next() : "";
				System.err.println("❌ Failed to upload produycr image. Status: " + responseCode);
				System.err.println("❌ Error: " + errorBody);
			}
		}
	}

	public void uploadVariantImageToBigCommerce(int productId, int variantId, String fileName, byte[] imageBytes)
			throws Exception {
		String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
		String lineFeed = "\r\n";

		String urlString = "https://api.bigcommerce.com/stores/" + BigCommerceStoreConfig.STORE_HASH
				+ "/v3/catalog/products/" + productId + "/variants/" + variantId + "/image";

		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("X-Auth-Token", BigCommerceStoreConfig.ACCESS_TOKEN);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		try (OutputStream outputStream = connection.getOutputStream();
				DataOutputStream writer = new DataOutputStream(outputStream)) {

			writer.writeBytes("--" + boundary + lineFeed);
			writer.writeBytes(
					"Content-Disposition: form-data; name=\"image_file\"; filename=\"" + fileName + "\"" + lineFeed);
			writer.writeBytes("Content-Type: image/jpeg" + lineFeed);
			writer.writeBytes(lineFeed);
			writer.write(imageBytes);
			writer.writeBytes(lineFeed);

			writer.writeBytes("--" + boundary + "--" + lineFeed);
			writer.flush();
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
				String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
				System.out.println("✅ Variant image uploaded: " + response);
			}
		} else {
			try (Scanner scanner = new Scanner(connection.getErrorStream(), "utf-8")) {
				String error = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
				System.err.println(
						"❌ Failed to upload variant image (variantId=" + variantId + "). Code: " + responseCode);
				System.err.println("Error: " + error);
			}
		}
	}

	public int createCategory(String locale, JSONArray categoryJson) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/trees/categories", "POST");
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
			// Print response for debugging
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = errorScanner.useDelimiter("\\A").next();
				System.out.println("Failed to create category. Response code: " + responseCode);
//                System.out.println("Error details: " + errorResponse);
			}
			return 0; // Return 0 in case of failure
		}
	}

	public Map<String, Integer> getCategoryNames(String locale) throws Exception {
		Map<String, Integer> categoryIds = new HashMap<>();

		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("tree_id:in", List.of(BigCommerceStoreConfig.CATEGORY_TREE_ID));
		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/trees/categories", "GET", queryParams);

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray categories = responseJson.getJSONArray("data");

				// Loop through categories and find ones with matching names
				for (int i = 0; i < categories.length(); i++) {
					JSONObject category = categories.getJSONObject(i);
					categoryIds.put(category.getString("name"), category.getInt("category_id"));
				}
			}
		} else {
			System.out.println("Failed to retrieve categories. HTTP response code: " + responseCode);
		}

		return categoryIds; // Return list of matching category IDs
	}

	public List<Integer> findCategoryByCatName(String locale, List<String> catNames) throws Exception {
		List<Integer> categoryIds = new ArrayList<>();

		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("tree_id:in", List.of(3));
		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/trees/categories", "GET", queryParams);

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray categories = responseJson.getJSONArray("data");

				// Loop through categories and find ones with matching names
				for (int i = 0; i < categories.length(); i++) {
					JSONObject category = categories.getJSONObject(i);
					String name = category.getString("name");
					System.out.println(" ---> nammm ---> " + name + " " + catNames);
					// Check if this category name is in the list of `catNames`
					if (catNames.contains(name)) {
						categoryIds.add(category.getInt("category_id")); // Add matching category ID to list
					}
				}
			}
		} else {
			System.out.println("Failed to retrieve categories. HTTP response code: " + responseCode);
		}

		return categoryIds; // Return list of matching category IDs
	}

	public static int findCategoryByUrlPath(String locale, String urlPath) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/categories", "GET");

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

	public List<Integer> findCategoryByUUID(String locale, List<String> uuids) throws Exception {

		List<Integer> categoryIds = new ArrayList<>();
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("category_uuid:in", uuids);
		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/trees/categories", "GET", queryParams);

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray categories = responseJson.getJSONArray("data");
				for (int i = 0; i < categories.length(); i++) {
					JSONObject category = categories.getJSONObject(i);
					categoryIds.add(category.getInt("id"));
				}
			}
		}
		return categoryIds;
	}

	public JSONObject getProductBySKU(String prodsku, String locale) throws Exception {
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("sku", prodsku);
		queryParams.put("include", "variants");

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/products", "GET", queryParams);

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray productsResponse = responseJson.getJSONArray("data");

				if (productsResponse.length() > 0) {
					JSONObject product = productsResponse.getJSONObject(0);
					return product; // Return the first matching variant ID
				} else {
					System.out.println(" falied to look up product " + prodsku);
					return null;
				}
//	                throw new Exception("No variant found with SKU: " + prodsku);

			}
		} else {
			System.out.println(" falied to look up product " + prodsku);
			return null;
//	        throw new Exception("Failed to fetch variant data. HTTP response code: " + responseCode);
		}
	}

	public int getVariantBySku(String skuId, String locale) throws Exception {
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("sku", skuId);

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "catalog/variants", "GET", queryParams);

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				JSONArray variants = responseJson.getJSONArray("data");

				if (variants.length() > 0) {
					JSONObject variant = variants.getJSONObject(0);
					return variant.getInt("id"); // Return the first matching variant ID
				} else {
					throw new Exception("No variant found with SKU: " + skuId);
				}
			}
		} else {
			throw new Exception("Failed to fetch variant data. HTTP response code: " + responseCode);
		}
	}

	public boolean updateVariantPrice(int productId, int variantId, BigDecimal listPrice, BigDecimal salePrice) {
		
		try {
			String url = "https://api.bigcommerce.com/stores/" + BigCommerceStoreConfig.STORE_HASH
					+ "/v3/catalog/products/" + productId + "/variants/" + variantId;
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

			connection.setRequestMethod("PUT");
			connection.setRequestProperty("X-Auth-Token", BigCommerceStoreConfig.ACCESS_TOKEN);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			JSONObject payload = new JSONObject();
			payload.put("price", listPrice);
			if (salePrice != null) {
				payload.put("sale_price", salePrice);
			}

			try (OutputStream os = connection.getOutputStream()) {
				os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
			}
			
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
				System.out.println("✅ Variant price updated successfully!");
				return true;
			} else {
				   String errorResponse = "";
				    try (InputStream errorStream = connection.getErrorStream()) {
				        if (errorStream != null) {
				            errorResponse = new BufferedReader(new InputStreamReader(errorStream))
				                    .lines()
				                    .collect(Collectors.joining("\n"));
				        } else {
				            errorResponse = "(No error stream returned)";
				        }
				    } catch (Exception e) {
				        System.err.println("⚠️ Error occurred while reading error stream: " + e.getMessage());
				    }

				    System.err.println("❌ Failed to update variant price.");
				    System.err.println("   ➤ HTTP Code:        " + responseCode);
				    System.err.println("   ➤ HTTP Message:     " + connection.getResponseMessage());
				    System.err.println("   ➤ Request URL:      " + connection.getURL());
				    System.err.println("   ➤ Request Method:   " + connection.getRequestMethod());
				    System.err.println("   ➤ Error Response:   " + errorResponse);
				    return false;
			}
		} catch (IOException e) {
			System.err.println("❌ Exception while updating variant price: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public void priceOverrides(String priceListId, JSONArray priceListPrices, String locale) throws Exception {

		Map<String, Object> queryParams = new HashMap<>();

		HttpURLConnection connection = BigCommerceApiClient.createRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, "pricelists/" + priceListId + "/records", "PUT", queryParams);

		// Send the request with the category JSON
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = priceListPrices.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			return;
		} else {
			System.err.println("Failed to override prices. HTTP response code: " + responseCode);

			// Attempt to read the error stream to get additional information
			try (Scanner scanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = scanner.useDelimiter("\\A").next();
				System.err.println("Error response message: " + errorResponse);
			} catch (Exception e) {
				System.err.println("Unable to retrieve error message from response: " + e.getMessage());
			}
		}

	}
	
	
	public void inventoryOverrides(JSONObject inventory) throws Exception {

		Map<String, Object> queryParams = new HashMap<>();

		HttpURLConnection connection = BigCommerceApiClient.createRequest(
				BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.ACCESS_TOKEN, 
				"inventory/adjustments/absolute", 
				"PUT", queryParams);

		// Send the request with the category JSON
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = inventory.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			return;
		} else {
			System.err.println("Failed to override prices. HTTP response code: " + responseCode);

			// Attempt to read the error stream to get additional information
			try (Scanner scanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = scanner.useDelimiter("\\A").next();
				System.err.println("Error response message: " + errorResponse);
			} catch (Exception e) {
				System.err.println("Unable to retrieve error message from response: " + e.getMessage());
			}
		}

	}

	public void activateInventoryTrackingFromSkuMap(Map<String, SkuVariantInfo> skuToIdsMap) throws Exception {
	    Map<String, Object> queryParams = new HashMap<>();
	    JSONObject activateInvTracking = new JSONObject();
	    
	    for (Map.Entry<String, SkuVariantInfo> entry : skuToIdsMap.entrySet()) {
	        String sku = entry.getKey();
	        SkuVariantInfo info = entry.getValue();

	        JSONObject payload = new JSONObject();
	        String url;
	        
	        if (info.isSingleSkuProduct()) {
	            // Product-level tracking
	            payload.put("inventory_tracking", "product");
	          
	        } else {
	            // Variant-level tracking
	            payload.put("inventory_tracking", "variant");
	          
	        }
	        url = "catalog/products/" + info.getProductId();

	        try {
	            HttpURLConnection connection = BigCommerceApiClient.createRequest(
	                BigCommerceStoreConfig.STORE_HASH,
	                BigCommerceStoreConfig.ACCESS_TOKEN,
	                url,
	                "PUT",
	                new HashMap<>()
	            );

	            try (OutputStream os = connection.getOutputStream()) {
	                byte[] input = payload.toString().getBytes("utf-8");
	                os.write(input, 0, input.length);
	            }

	            int responseCode = connection.getResponseCode();
	            if (responseCode == HttpURLConnection.HTTP_OK) {
	                System.out.printf("✅ Inventory tracking enabled for SKU: %s (Product ID: %d, Variant ID: %d)%n",
	                        sku, info.getProductId(), info.getVariantId());
	            } else {
	                System.err.printf("❌ Failed to enable inventory tracking for SKU: %s (Product ID: %d)%n",
	                        sku, info.getProductId());
	                try (Scanner scanner = new Scanner(connection.getErrorStream())) {
	                    String errorResponse = scanner.useDelimiter("\\A").next();
	                    System.err.println("🔍 Error details: " + errorResponse);
	                } catch (Exception e) {
	                    System.err.println("⚠️ Could not read error response: " + e.getMessage());
	                }
	            }

	        } catch (Exception ex) {
	            System.err.printf("💥 Exception while enabling inventory tracking for SKU: %s%n", sku);
	            ex.printStackTrace();
	        }
	    }
	}
	
	public static Map<String, Integer> extractSkuToVariantIdMap(JSONObject responseJson) {
		Map<String, Integer> skuToIdMap = new HashMap<>();
		try {
			JSONObject data = responseJson.getJSONObject("data");
			JSONArray variants = data.getJSONArray("variants");

			for (int i = 0; i < variants.length(); i++) {
				JSONObject variant = variants.getJSONObject(i);
				String sku = variant.getString("sku");
				int variantId = variant.getInt("id");
				skuToIdMap.put(sku, variantId);
			}

		} catch (Exception e) {
			System.err.println("Failed to extract SKU to Variant ID map: " + e.getMessage());
		}
		return skuToIdMap;
	}

	public List<JSONArray> splitIntoBatches(JSONArray fullArray, int batchSize) {
		List<JSONArray> batches = new ArrayList<>();
		JSONArray currentBatch = new JSONArray();

		for (int i = 0; i < fullArray.length(); i++) {
			currentBatch.put(fullArray.getJSONObject(i));

			if (currentBatch.length() == batchSize) {
				batches.add(currentBatch);
				currentBatch = new JSONArray();
			}
		}

		if (currentBatch.length() > 0) {
			batches.add(currentBatch);
		}

		return batches;
	}

	public Map<String, Integer> getStoreCodeToLocationIdMap() throws IOException {
	    Map<String, Integer> storeCodeToLocationId = new HashMap<>();
	    int page = 1;
	    boolean hasMorePages = true;

	    while (hasMorePages) {
	        String url = "https://api.bigcommerce.com/stores/" + BigCommerceStoreConfig.STORE_HASH +
	                     "/v3/inventory/locations?page=" + page + "&limit=1000";

	        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
	        conn.setRequestProperty("X-Auth-Token", BigCommerceStoreConfig.ACCESS_TOKEN);
	        conn.setRequestProperty("Accept", "application/json");

	        int responseCode = conn.getResponseCode();
	        if (responseCode != 200) {
	            System.err.println("❌ Failed to fetch locations. Code: " + responseCode);
	            return Collections.emptyMap();
	        }

	        String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	        JSONObject response = new JSONObject(json);
	        JSONArray data = response.getJSONArray("data");
	        
	        System.out.println(" ----- " + data.length());

	        for (int i = 0; i < data.length(); i++) {
	            JSONObject location = data.getJSONObject(i);
	            String code = location.optString("code"); // e.g., "store-49"
	            int id = location.getInt("id");
	            boolean matched = false;
	         // ✅ Handle default location explicitly
	            if ("Default location".equalsIgnoreCase(location.optString("label"))) {
	                storeCodeToLocationId.put("1", id);  // Map internal feed store code "1" to this location
	                matched = true;
	            }
	            
	            if (code != null && code.startsWith("store-")) {
	                String storeNumber = code.replace("store-", "");
	                storeCodeToLocationId.put(storeNumber, id);
	                matched = true;
	            }
	         // ❗️ Log unmatched locations
	            if (!matched) {
	                System.out.println("⚠️ Unmatched location:");
	                System.out.println("    → ID: " + id);
	                System.out.println("    → Code: " + code);
	               
	            }
	        }

	        // Check if there are more pages
	        JSONObject meta = response.optJSONObject("meta");
	        if (meta != null) {
	            JSONObject pagination = meta.optJSONObject("pagination");
	            int currentPage = pagination.optInt("current_page", page);
	            int totalPages = pagination.optInt("total_pages", page);
	            hasMorePages = currentPage < totalPages;
	            page++;
	        } else {
	            hasMorePages = false; // Safety fallback
	        }
	    }

	    return storeCodeToLocationId;
	}

	private JSONObject createBigCommerceInventoryJson(String skuId) {
	    JSONObject inventoryItem = new JSONObject();

	    JSONObject identity = new JSONObject();
	    identity.put("sku", skuId);
	    
	    inventoryItem.put("identity", identity);
	    inventoryItem.put("safety_stock", 10);
	    inventoryItem.put("warning_level", 20);
//	    inventoryItem.put("is_in_stock", quantity > 0);
	    inventoryItem.put("bin_picking_number", 12);

	    JSONObject payload = new JSONObject();
	    payload.put("settings", new JSONArray().put(inventoryItem));

	    return payload;
	}

}
