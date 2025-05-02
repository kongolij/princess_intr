package com.bigcommerce.imports.store.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.client.BigCommerceApiClient;
import com.bigcommerce.imports.catalog.constants.AccessToken;
import com.bigcommerce.imports.catalog.constants.CommonConstants;
import com.bigcommerce.imports.catalog.constants.StoreHash;
import com.bigcommerce.imports.catalog.dto.CategoryNode;
import com.bigcommerce.imports.catalog.mapper.BigCommerceCategoryMapper;
import com.bigcommerce.imports.store.constants.StoreCommonConstants;
import com.bigcommerce.imports.store.dto.Location;
import com.bigcommerce.imports.store.dto.StoreLocationBundle;
import com.bigcommerce.imports.store.mapper.BigCommerceLocationMapper;

import io.micrometer.common.util.StringUtils;

@Component
public class BigCommerceStoreLocationService {

	
	public void importStores(List<Location> locations) throws Exception {
		
	}
	
	
	/**
	 * Entry point to import store locations using a map of StoreLocationBundle
	 */
	public void importStoresToBc(Map<String, StoreLocationBundle> locationMap) throws Exception {
	    if (locationMap == null || locationMap.isEmpty()) {
	        System.out.println("⚠️ No store locations to import.");
	        return;
	    }

	    for (Map.Entry<String, StoreLocationBundle> entry : locationMap.entrySet()) {
	        String key = entry.getKey();
	        StoreLocationBundle bundle = entry.getValue();

	        if (bundle == null || bundle.getLocation() == null) {
	            System.err.println("⚠️ Skipping null bundle or location for key: " + key);
	            continue;
	        }

	        System.out.println("🚚 Importing store location for key: " + key + ", code: " + bundle.getLocation().getCode());
	        importSingleLocation(bundle); // ✅ pass the whole bundle now
	    }
	}
	
	private void importSingleLocation(StoreLocationBundle bundle) throws Exception {
	    Location location = bundle.getLocation();
	    Map<String, String> metafields = bundle.getMetafields(); // ✅ now from the bundle

	    
	    String locationCode = location.getCode();

	    JSONArray locationPayload = BigCommerceLocationMapper.mapLocationToBigCommerce(Collections.singletonList(location));

	    boolean created = createBigCommerceLocations(locationPayload, "en");
	    if (!created) {
	        System.err.println("❌ Skipping metafield step due to failed location creation for: " + locationCode);
	        return;
	    }

	    Integer locationId = getLocationIdByCode(locationCode, "en");
	    if (locationId == null) {
	        System.err.println("❌ Could not retrieve ID for newly created location: " + locationCode);
	        return;
	    }
	    
	    // create pickup method
	    createPickupMethod(locationId,  "en");
	    
	    if (metafields != null && !metafields.isEmpty()) {
	        createMetafieldsForLocation(locationId, metafields, "en");
	    } else {
	        System.out.println("ℹ️ No metafields provided for location: " + locationCode);
	    }
	}
	
	
	
	
	
	
	
	public boolean createBigCommerceLocations(JSONArray locationPayloadArray, String locale) throws Exception {
	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	        storeHash, accessToken,
	        "inventory/locations",
	        "POST"
	    );

	    connection.setRequestProperty("Content-Type", "application/json");

	    try (OutputStream os = connection.getOutputStream()) {
	        byte[] input = locationPayloadArray.toString().getBytes("utf-8");
	        os.write(input, 0, input.length);
	    }

	    int responseCode = connection.getResponseCode();

	    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
	        try (Scanner scanner = new Scanner(connection.getInputStream())) {
	            String responseBody = scanner.useDelimiter("\\A").next();
	            System.out.println("✅ Locations created successfully:");
	            System.out.println(responseBody);
	        }
	        return true;
	    } else {
	        try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	            String errorResponse = errorScanner.useDelimiter("\\A").next();
	            System.err.println("❌ Failed to create locations. Code: " + responseCode);
	            System.err.println("Error: " + errorResponse);
	        }
	        return false;
	    }
	}
	
	
	public Integer getLocationIdByCode(String locationCode, String locale) throws Exception {
	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    String endpoint = "inventory/locations?location_code:in=" + URLEncoder.encode(locationCode, "UTF-8");

	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	        storeHash, accessToken,
	        endpoint,
	        "GET"
	    );

	    int responseCode = connection.getResponseCode();

	    if (responseCode == HttpURLConnection.HTTP_OK) {
	        try (Scanner scanner = new Scanner(connection.getInputStream())) {
	            String responseBody = scanner.useDelimiter("\\A").next();
	            JSONObject json = new JSONObject(responseBody);

	            JSONArray data = json.getJSONArray("data");
	            if (data.length() > 0) {
	                return data.getJSONObject(0).getInt("id");
	            } else {
	                System.err.println("⚠️ No location found for code: " + locationCode);
	                return null;
	            }
	        }
	    } else {
	        try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	            String errorResponse = errorScanner.useDelimiter("\\A").next();
	            System.err.println("❌ Failed to fetch location. Code: " + responseCode);
	            System.err.println("Error: " + errorResponse);
	        }
	        return null;
	    }
	}
	
	public Map<String, Integer> getLocationIdsByCodes(List<String> locationCodes, String locale) throws Exception {
	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    Map<String, Integer> result = new LinkedHashMap<>();

	    for (String locationCode : locationCodes) {
	        String endpoint = "inventory/locations?location_code:in=" + URLEncoder.encode(locationCode, "UTF-8");

	        HttpURLConnection connection = BigCommerceApiClient.createRequest(
	                storeHash, accessToken, endpoint, "GET");

	        int responseCode = connection.getResponseCode();

	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            try (Scanner scanner = new Scanner(connection.getInputStream())) {
	                String responseBody = scanner.useDelimiter("\\A").next();
	                JSONObject json = new JSONObject(responseBody);

	                JSONArray data = json.getJSONArray("data");
	                if (data.length() > 0) {
	                    int id = data.getJSONObject(0).getInt("id");
	                    result.put(locationCode, id);
//	                    System.out.printf("✅ Found location [%s] with ID: %d\n", locationCode, id);
	                } else {
	                    System.out.printf("⚠️ No location found for code: %s\n", locationCode);
	                    result.put(locationCode, null);
	                }
	            }
	        } else {
	            try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	                String errorResponse = errorScanner.useDelimiter("\\A").next();
	                System.err.printf("❌ Failed to fetch location for code: %s | HTTP %d\n", locationCode, responseCode);
	                System.err.println("Error: " + errorResponse);
	            }
	            result.put(locationCode, null);
	        }
	    }

	    return result;
	}
	private void createMetafieldsForLocation(Integer locationId, Map<String, String> metafields, String locale) throws Exception {
	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    JSONArray metafieldArray = new JSONArray();

	    for (Map.Entry<String, String> entry : metafields.entrySet()) {
	        JSONObject metafield = new JSONObject();
	        metafield.put("namespace", StoreCommonConstants.STORE_DATA_NAMESPACE);
	        metafield.put("key", entry.getKey());
	        metafield.put("value", entry.getValue());
	        metafield.put("value_type", "string");
	        metafield.put("resource_id", locationId);
	        metafield.put("permission_set", StoreCommonConstants.PERMISION_SET);

	        
	       
	        
	        metafieldArray.put(metafield);
	    }

	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	        storeHash, accessToken,
	        "inventory/locations/metafields",
	        "POST"
	    );

	    
	    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
	    

	    try (OutputStream os = connection.getOutputStream()) {
	        byte[] input = metafieldArray.toString().getBytes("utf-8");
	        os.write(input, 0, input.length);
	    }

	    int responseCode = connection.getResponseCode();

	    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
	        System.out.println("✅ Metafields created for location ID: " + locationId);
	    } else {
	        try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	            String errorResponse = errorScanner.useDelimiter("\\A").next();
	            System.err.println("❌ Failed to create metafields. Code: " + responseCode);
	            System.err.println("Error: " + errorResponse);
	        }
	    }
	}

	private void createPickupMethod(Integer locationId,  String locale) throws Exception {
	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    JSONArray pickupMethodsArray = new JSONArray();


	
	    JSONObject pickupMethod = new JSONObject();
	    pickupMethod.put("location_id", locationId);
	    pickupMethod.put("display_name", "In-Store Pickup" );
	    pickupMethod.put("collection_instructions", "Instructioins here" );
	    pickupMethod.put("collection_time_description", "Ready for pick up in 4 hours");
	   


	    pickupMethodsArray.put(pickupMethod);

	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	        storeHash,
	        accessToken,
	        "pickup/methods",
	        "POST"
	    );

	    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

	    try (OutputStream os = connection.getOutputStream()) {
	        byte[] input = pickupMethodsArray.toString().getBytes("utf-8");
	        os.write(input, 0, input.length);
	    }

	    int responseCode = connection.getResponseCode();

	    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
	        System.out.println("✅ Pickup method created for location ID: " + locationId);
	    } else {
	        try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	            String errorResponse = errorScanner.useDelimiter("\\A").next();
	            System.err.println("❌ Failed to create pickup method. Code: " + responseCode);
	            System.err.println("Error: " + errorResponse);
	        }
	    }
	}

	
	public void deleteInventoryLocationsOneByOne(Map<String, Integer> locationMap) throws Exception {
	    if (locationMap == null || locationMap.isEmpty()) {
	        System.out.println("⚠️ No locations to delete.");
	        return;
	    }

	    String locale = "en"; // or pass this as a method parameter
	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    for (Map.Entry<String, Integer> entry : locationMap.entrySet()) {
	        String key = entry.getKey();
	        Integer locationId = entry.getValue();

	        if (locationId == null) {
	            System.err.println("⚠️ Skipping null location ID for key: " + key);
	            continue;
	        }

//	        String path = "inventory/locations";
//	        String queryParams = "location_id:in=" + locationId;
	        
	        String path = "inventory/locations?location_id:in=" + locationId;
	        
	        System.out.println("🗑️ Deleting location: " + key + " (ID: " + locationId + ")");

	        HttpURLConnection connection = BigCommerceApiClient.createRequest(
	            storeHash,
	            accessToken,
	            path,
	            "DELETE"
	        );

	        int responseCode = connection.getResponseCode();

	        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
	            System.out.println("✅ Successfully deleted location ID: " + locationId);
	        } else {
	            try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	                String errorResponse = errorScanner.useDelimiter("\\A").next();
	                System.err.println("❌ Failed to delete location ID: " + locationId + " (Code: " + responseCode + ")");
	                System.err.println("Error: " + errorResponse);
	            }
	        }
	    }
	}
	
	public Map<Integer, Integer> getPickupMethodIdByLocationId(String locale) throws Exception {
	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    HttpURLConnection connection = BigCommerceApiClient.createRequest(
	        storeHash,
	        accessToken,
	        "pickup/methods",
	        "GET"
	    );

	    connection.setRequestProperty("Accept", "application/json");

	    int responseCode = connection.getResponseCode();

	    Map<Integer, Integer> locationToPickupIdMap = new HashMap<>();

	    if (responseCode == HttpURLConnection.HTTP_OK) {
	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
	            StringBuilder responseBuilder = new StringBuilder();
	            String line;
	            while ((line = reader.readLine()) != null) {
	                responseBuilder.append(line);
	            }

	            JSONObject responseJson = new JSONObject(responseBuilder.toString());
	            JSONArray dataArray = responseJson.getJSONArray("data");

	            for (int i = 0; i < dataArray.length(); i++) {
	                JSONObject pickupMethod = dataArray.getJSONObject(i);
	                int locationId = pickupMethod.getInt("location_id");
	                int methodId = pickupMethod.getInt("id");

	                locationToPickupIdMap.put(locationId, methodId);
	            }
	        }
	    } else {
	        try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	            String errorResponse = errorScanner.useDelimiter("\\A").next();
	            System.err.println("❌ Failed to fetch pickup methods. Code: " + responseCode);
	            System.err.println("Error: " + errorResponse);
	        }
	    }

	    return locationToPickupIdMap;
	}
	
	public void deletePickupMethodsByIds(List<Integer> pickupMethodIds, String locale) throws Exception {
	    if (pickupMethodIds == null || pickupMethodIds.isEmpty()) {
	        System.out.println("⚠️ No pickup methods to delete.");
	        return;
	    }

	    String storeHash = StoreHash.getStoreHashByLocale(locale);
	    String accessToken = AccessToken.getStoreAccessTokenByLocale(locale);

	    for (Integer methodId : pickupMethodIds) {
	        if (methodId == null) continue;

	        String path = "pickup/methods?id:in=" + methodId;

	        HttpURLConnection connection = BigCommerceApiClient.createRequest(
	            storeHash,
	            accessToken,
	            path,
	            "DELETE"
	        );

	        connection.setRequestProperty("Accept", "application/json");

	        int responseCode = connection.getResponseCode();

	        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
	            System.out.println("✅ Deleted pickup method ID: " + methodId);
	        } else {
	            try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
	                String errorResponse = errorScanner.useDelimiter("\\A").next();
	                System.err.println("❌ Failed to delete pickup method ID: " + methodId + " (Code: " + responseCode + ")");
	                System.err.println("Error: " + errorResponse);
	            }
	        }
	    }
	}

	
	
}


