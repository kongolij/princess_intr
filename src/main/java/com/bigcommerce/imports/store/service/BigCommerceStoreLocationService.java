package com.bigcommerce.imports.store.service;

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
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.clinet.BigCommerceApiClient;
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

	


}


