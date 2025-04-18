package com.bigcommerce.imports.catalog.clinet;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BigCommerceApiClient {

	//DEV
	private static final String STORE_HASH = "u2rpux9vkx";
	private static final String ACCESS_TOKEN = "211w656tmge6rmsgo0olr8jtky50j38";
	private static final int NEXT_PUBLIC_BC_CHANNEL_ID = 1730140;
	
//	//QA
//	private static final String STORE_HASH = "kpz3wrpdrb";
//	private static final String ACCESS_TOKEN = "tt37h0cc8h7u7mqxi1kyra4qf2jg3wx";
//	private static final int NEXT_PUBLIC_BC_CHANNEL_ID = 1730142;
//	
    //	
//	private static final String STORE_HASH = "nkqg1lsole";
//	private static final String ACCESS_TOKEN = "o85zg0lb40mrhtgx873a464lqohsobp";
//	private static final int NEXT_PUBLIC_BC_CHANNEL_ID = 1604412;
	
//	private static final String STORE_HASH = "w1jeucyusb";
//	private static final String ACCESS_TOKEN = "e69e436a4cfe7f21b58f33c324b708df6fb186af85a66f27c2f3ceec5efeb4d2";
//	private static final int NEXT_PUBLIC_BC_CHANNEL_ID = 1604412;
	
	
	// used for api 
//	ACCESS TOKEN: 148wypnv70g5uptk97cd5y5bjxeh6b0
	
//	CLIENT NAME: dev-admin-token-v1
//	CLIENT ID: 72w0ns4c140sqiwfsevaoeof2vy4vcn
//	CLIENT SECRET: e69e436a4cfe7f21b58f33c324b708df6fb186af85a66f27c2f3ceec5efeb4d2
//	NAME: dev-admin-token-v1
//	API PATH: https://api.bigcommerce.com/stores/w1jeucyusb/v3/
		

	public static HttpURLConnection createRequest(String storeHash, String accessToken, String endpoint, String method) throws Exception {

		URL url = new URL("https://api.bigcommerce.com/stores/" + storeHash + "/v3/" + endpoint);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(method);
		connection.setRequestProperty("X-Auth-Token", accessToken);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);
		return connection;
	}

	public static HttpURLConnection createRequest(String storeHash, String endpoint, String method,
			Map<String, Object> queryParams) throws Exception {

		String baseUrl = "https://api.bigcommerce.com/stores/" + storeHash + "/v3/" + endpoint;

        // Append query parameters if provided
        String urlWithParams = queryParams != null && !queryParams.isEmpty() 
                ? baseUrl + "?" + formatParams(queryParams)
                : baseUrl;
        

		URL url = new URL(urlWithParams);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(method);
		connection.setRequestProperty("X-Auth-Token", ACCESS_TOKEN);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);
		return connection;
	}

	// Helper method for formatting query parameters
    private static String formatParams(Map<String, Object> queryParams) {
        return queryParams.entrySet().stream()
            .map(entry -> {
                String key = encode(entry.getKey());
                String value;

                if (entry.getValue() instanceof List) {
                    // Join list values with commas
                    value = ((List<?>) entry.getValue()).stream()
                        .map(item -> encode(item.toString()))
                        .collect(Collectors.joining(","));
                } else {
                    // Handle single values
                    value = encode(entry.getValue().toString());
                }

                return key + "=" + value;
            })
            .collect(Collectors.joining("&"));
    }

    // Helper method to encode query parameter values
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }
    }

}
