package com.bigcommerce.imports.catalog.product.repository;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.ImportCategoryTreeFromCVS;
import com.bigcommerce.imports.catalog.clinet.BigCommerceApiClient;
import com.bigcommerce.imports.catalog.constants.AccessToken;
import com.bigcommerce.imports.catalog.constants.StoreHash;
import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.ProductCreationResult;

@Component 
public class BigCommerceRepository {

	public static ProductCreationResult  createProductWithVariants(String locale, JSONObject productJson) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				AccessToken.getStoreAccessTokenByLocale(locale), "catalog/products", "POST");
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

	
	
	public boolean assignProductToChannel( String locale, int productId, int channelId) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				AccessToken.getStoreAccessTokenByLocale(locale), "catalog/products/channel-assignments", "PUT");

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


	public boolean setProductCustomFields( String locale, JSONArray productCustomFields, int productId) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				AccessToken.getStoreAccessTokenByLocale(locale), "catalog/products/" +productId + "/custom-fields", "POST");

		

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
	
	public boolean setVariantMetafieldsInBatches(String locale, Map<String, JSONArray> variantMetafields) throws Exception {
	    // Combine all entries from map into one flat array
	    JSONArray combinedPayload = new JSONArray();
	    for (Map.Entry<String, JSONArray> entry : variantMetafields.entrySet()) {
	        JSONArray localeArray = entry.getValue();
	        for (int i = 0; i < localeArray.length(); i++) {
	            combinedPayload.put(localeArray.getJSONObject(i));
	        }
	    }

	    // Split into batches of 10
	    List<JSONArray> batches = splitIntoBatches(combinedPayload, 10);
	    boolean allSuccess = true;

	    for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
	        JSONArray batch = batches.get(batchIndex);

	        HttpURLConnection connection = BigCommerceApiClient.createRequest(
	                StoreHash.getStoreHashByLocale(locale),
	                AccessToken.getStoreAccessTokenByLocale(locale),
	                "catalog/variants/metafields", "POST");

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

	
	public boolean setVariantMetafields(String locale, Map<String,JSONArray> variantMetafields) throws Exception {
	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	            StoreHash.getStoreHashByLocale(locale),
	            AccessToken.getStoreAccessTokenByLocale(locale),
	            "catalog/variants/metafields", "POST");

	    
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

	public boolean createProductImages(String locale, JSONObject productImageJson, int productid) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				AccessToken.getStoreAccessTokenByLocale(locale),
				"catalog/products/" + productid + "/images", "POST");

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = productImageJson.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {

			try (Scanner scanner = new Scanner(connection.getInputStream())) {
				String responseBody = scanner.useDelimiter("\\A").next();
				JSONObject responseJson = new JSONObject(responseBody);
				Map<String, Integer> skuToVariantMap = extractSkuToVariantIdMap(responseJson);
				
				System.out.println("product created successfully!");

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

	public int createCategory(String locale, JSONArray categoryJson) throws Exception {

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				AccessToken.getStoreAccessTokenByLocale(locale),
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
		queryParams.put("tree_id:in", List.of(ImportCategoryTreeFromCVS.CATEGOU_TREE_ID));
		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				"catalog/trees/categories", "GET", queryParams);

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
		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				"catalog/trees/categories", "GET", queryParams);

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

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				AccessToken.getStoreAccessTokenByLocale(locale),
				"catalog/categories", "GET");

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
		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				"catalog/trees/categories", "GET", queryParams);

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
	    	
	    
	    
	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	            StoreHash.getStoreHashByLocale(locale),
	            "catalog/products",
	            "GET",
	            queryParams);

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

	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	            StoreHash.getStoreHashByLocale(locale),
	            "catalog/variants",
	            "GET",
	            queryParams);

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

	
	
	public void priceOverrides(String priceListId, JSONArray priceListPrices, String locale) throws Exception {

		Map<String, Object> queryParams = new HashMap<>();

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				"pricelists/" + priceListId + "/records", "PUT", queryParams);

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
	
	
	public void inventoryOverrides(JSONObject inventory, String locale) throws Exception {

		Map<String, Object> queryParams = new HashMap<>();

		HttpURLConnection connection = BigCommerceApiClient.createRequest(StoreHash.getStoreHashByLocale(locale),
				"inventory/adjustments/absolute", "PUT", queryParams);

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

}
