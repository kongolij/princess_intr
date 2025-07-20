package com.bigcommerce.imports.b2bOrg.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;

@Component
public class BigCommerceB2BOrgRepository {

	private static final String ACCESS_TOKEN = BigCommerceStoreConfig.B2B_ACCESS_TOKEN;
	private static final String API_URL = "https://api-b2b.bigcommerce.com/api/v3/io/companies";

//	Creates multiple Company accounts at once. 
//	You can create up to 10 Companies in a single request.
	
	public List<Integer> bulkCreateB2BOrganizations(JSONArray companiesPayload) throws Exception {
	    String bulkEndpoint = "https://api-b2b.bigcommerce.com/api/v3/io/companies/bulk";

	    URL url = new URL(bulkEndpoint);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("authToken", ACCESS_TOKEN);
		connection.setDoOutput(true);

	    // Send the JSON payload
	    try (OutputStream os = connection.getOutputStream()) {
	        byte[] input = companiesPayload.toString().getBytes("utf-8");
	        os.write(input, 0, input.length);
	    }

	    int responseCode = connection.getResponseCode();
	    List<Integer> companyIds = new ArrayList<>();

	    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
	            StringBuilder responseBuilder = new StringBuilder();
	            String line;
	            while ((line = reader.readLine()) != null) {
	                responseBuilder.append(line);
	            }

	            JSONObject jsonResponse = new JSONObject(responseBuilder.toString());
	            JSONArray data = jsonResponse.getJSONArray("data");

	            for (int i = 0; i < data.length(); i++) {
	                JSONObject companyResult = data.getJSONObject(i);
	                int companyId = companyResult.getInt("companyId");
	                companyIds.add(companyId);
	            }

	            System.out.println("✅ Created " + companyIds.size() + " B2B organizations: " + companyIds);
	        }
	    } else {
	        System.err.println("❌ Failed to create bulk B2B organizations. HTTP code: " + responseCode);
	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
	            StringBuilder errorBuilder = new StringBuilder();
	            String line;
	            while ((line = reader.readLine()) != null) {
	                errorBuilder.append(line);
	            }
	            System.err.println("Error message: " + errorBuilder.toString());
	        } catch (Exception e) {
	            System.err.println("⚠️ Could not read error stream: " + e.getMessage());
	        }
	    }

	    return companyIds;
	}
	
	
	public long createB2BOrganization(JSONObject companyPayload) throws Exception {
		URL url = new URL(API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("authToken", ACCESS_TOKEN);
		connection.setDoOutput(true);

		// Write request body
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = companyPayload.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
			System.out.println("✅ B2B Organization created successfully.");
			try (Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8")) {
	            String responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
	            JSONObject responseJson = new JSONObject(responseBody);

	            // Extract from nested "data" object
	            JSONObject data = responseJson.getJSONObject("data");
	            long companyId = data.getLong("companyId");

	            System.out.println("✅ B2B Organization created with companyId: " + companyId);
	            return companyId;
	        }
		} else {
			System.err.println("❌ Failed to create B2B organization. HTTP code: " + responseCode);
			try (Scanner scanner = new Scanner(connection.getErrorStream())) {
				String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
				System.err.println("Error message: " + errorResponse);
			} catch (Exception e) {
				System.err.println("⚠️ Could not read error stream: " + e.getMessage());
			}
			throw new RuntimeException("Failed to create B2B organization");
		}
	}
	
	public void updateCompanyCredit(long companyId, JSONObject creditPayload) throws Exception {
		String endpoint = String.format("https://api-b2b.bigcommerce.com/api/v3/io/companies/%d/credit", companyId);

	    URL url = new URL(endpoint); // ✅ Use the correct endpoint, not API_URL
	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

	    connection.setRequestMethod("PUT");
	    connection.setRequestProperty("Content-Type", "application/json");
	    connection.setRequestProperty("authToken", ACCESS_TOKEN);
	    connection.setDoOutput(true);

	    // Send the credit JSON payload
	    try (OutputStream os = connection.getOutputStream()) {
	        byte[] input = creditPayload.toString().getBytes("utf-8");
	        os.write(input, 0, input.length);
	    }

	    int responseCode = connection.getResponseCode();
	    
//	    InputStream responseStream = (responseCode >= 200 && responseCode < 300)
//	        ? connection.getInputStream()
//	        : connection.getErrorStream();
//
//	    StringBuilder responseBody = new StringBuilder();
//	    try (Scanner scanner = new Scanner(responseStream)) {
//	        while (scanner.hasNextLine()) {
//	            responseBody.append(scanner.nextLine());
//	        }
//	    }
	    if (responseCode == HttpURLConnection.HTTP_OK) {
	    	
//	    	System.out.printf("✅ Company credit updated for org ID %d. Response: %s%n", companyId, responseBody);
	        System.out.println("✅ Company credit updated successfully.");
	    } else {
	        System.err.println("❌ Failed to update company credit. HTTP code: " + responseCode);
	        try (Scanner scanner = new Scanner(connection.getErrorStream())) {
	            String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
	            System.err.println("Error message: " + errorResponse);
	        } catch (Exception e) {
	            System.err.println("⚠️ Could not read error stream: " + e.getMessage());
	        }
	    }
	}

	
	public Map<String, Long> getOrgNumberToCompanyIdMap() throws Exception {
	    int offset = 0;
	    int limit = 100;
	    boolean morePages = true;

	    Map<String, Long> orgMap = new HashMap<>();

	    while (morePages) {
	        String endpoint = String.format(
	            "https://api-b2b.bigcommerce.com/api/v3/io/companies?offset=%d&limit=%d&sortBy=companyName&orderBy=ASC&isIncludeExtraFields=1",
	            offset, limit
	        );

	        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
	        connection.setRequestMethod("GET");
	        connection.setRequestProperty("Content-Type", "application/json");
	        connection.setRequestProperty("authToken", ACCESS_TOKEN);
	        connection.setRequestProperty("Accept", "application/json");

	        int responseCode = connection.getResponseCode();

	        if (responseCode == 429) {
	            String retryAfter = connection.getHeaderField("Retry-After");
	            int waitSeconds = retryAfter != null ? Integer.parseInt(retryAfter) : 5;
	            System.out.println("⚠️ Rate limit hit. Waiting " + waitSeconds + " seconds...");
	            Thread.sleep(waitSeconds * 1000L);
	            continue;
	        }

	        if (responseCode == 200) {
	            String response = new BufferedReader(new InputStreamReader(connection.getInputStream()))
	                .lines().collect(Collectors.joining("\n"));

	            JSONObject json = new JSONObject(response);
	            JSONArray data = json.getJSONArray("data");
	            JSONObject pagination = json.getJSONObject("meta").getJSONObject("pagination");

	            for (int i = 0; i < data.length(); i++) {
	                JSONObject company = data.getJSONObject(i);
	                long companyId = company.getLong("companyId");

	                JSONArray extraFields = company.optJSONArray("extraFields");
	                if (extraFields != null) {
	                    for (int j = 0; j < extraFields.length(); j++) {
	                        JSONObject field = extraFields.getJSONObject(j);
	                        if ("organization_number".equals(field.optString("fieldName"))) {
	                            String orgNum = field.optString("fieldValue");
	                            if (!orgNum.isEmpty()) {
	                                orgMap.put(orgNum, companyId);
	                            }
	                        }
	                    }
	                }
	            }

	            int totalCount = pagination.getInt("totalCount");
	            offset += limit;
	            morePages = offset < totalCount;

	            System.out.printf("📦 Total so far: %d / %d%n", orgMap.size(), totalCount);
	            Thread.sleep(250); // optional delay to avoid 429
	        } else {
	            throw new IOException("❌ Failed to fetch companies. HTTP " + responseCode);
	        }
	    }

	    System.out.printf("🎉 Finished mapping %d organization numbers to company IDs.%n", orgMap.size());
	    return orgMap;
	}
	





}