package com.constructor.index.deleterecords;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.bigcommerce.imports.catalog.service.BigCommerceService;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.mapper.ConstructorJsonlProductMapper;
import com.opencsv.CSVWriter;

//@Component
public class DeleteVariants implements CommandLineRunner {


	private static final String API_KEY = "key_on1j1t2BjFymbXpC"; // Public API key
	private static final String API_TOKEN = "tok_5NCEybey4GqC52oL"; // Secret token for Basic Auth
	private static final String SECTION = "Products";
	private static final String CLIENT_ID = "springboot-app-1.0";
	private static final int BATCH_SIZE = 20;

	@Override
	public void run(String... args) throws Exception {

		// Step 1: Fetch all item IDs from Constructor.io
		List<String> allItemIds = fetchAllItemIds();
		System.out.printf("🔍 Found %d items to delete%n", allItemIds.size());

		// Step 2: Process in batches of 20
		for (int i = 0; i < allItemIds.size(); i += BATCH_SIZE) {
			List<String> batchItemIds = allItemIds.subList(i, Math.min(i + BATCH_SIZE, allItemIds.size()));
			List<String> variantIds = fetchVariants(batchItemIds);

			if (variantIds.isEmpty()) {
				System.out.printf("ℹ️  No variants found for item batch [%d - %d)%n", i, i + batchItemIds.size());
				continue;
			}

			// Step 3: Delete variants
			sendVariantDeleteRequest(variantIds, i);
		}

	
		System.out.println("✅ All delete batches completed.");
		System.exit(0);
	}

	// Step 1: Fetch all item IDs from /v1/items
	private List<String> fetchAllItemIds() throws Exception {
		String url = String.format("https://ac.cnstrc.com/v2/items?key=%s&section=%s&c=%s",
				URLEncoder.encode(API_KEY, "UTF-8"), URLEncoder.encode(SECTION, "UTF-8"),
				URLEncoder.encode(CLIENT_ID, "UTF-8"));

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("GET");
		String auth = Base64.getEncoder().encodeToString((API_TOKEN + ":").getBytes(StandardCharsets.UTF_8));
		conn.setRequestProperty("Authorization", "Basic " + auth);

		int status = conn.getResponseCode();
		if (status != 200) {
			throw new RuntimeException("Failed to fetch items: HTTP " + status);
		}

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
		StringBuilder jsonStr = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			jsonStr.append(line);
		}

		JSONObject json = new JSONObject(jsonStr.toString());
		JSONArray items = json.getJSONArray("items");

		List<String> ids = new ArrayList<>();
		for (int i = 0; i < items.length(); i++) {
			ids.add(items.getJSONObject(i).getString("id"));
		}

		return ids;
	}

	// 🔹 Fetch variation IDs using item IDs
	private List<String> fetchVariants(List<String> itemIds) throws Exception {
		StringBuilder urlBuilder = new StringBuilder("https://ac.cnstrc.com/v2/variations");
		urlBuilder.append("?key=").append(URLEncoder.encode(API_KEY, StandardCharsets.UTF_8));
		urlBuilder.append("&section=Products");

		for (String itemId : itemIds) {
			urlBuilder.append("&item_id=").append(URLEncoder.encode(itemId, StandardCharsets.UTF_8));
		}

		HttpURLConnection conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
		conn.setRequestMethod("GET");
		String auth = Base64.getEncoder().encodeToString((API_TOKEN + ":").getBytes(StandardCharsets.UTF_8));
		conn.setRequestProperty("Authorization", "Basic " + auth);

		int status = conn.getResponseCode();
		if (status != 200)
			throw new RuntimeException("Failed to fetch variants: HTTP " + status);

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
		StringBuilder jsonStr = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null)
			jsonStr.append(line);

		JSONArray variations = new JSONObject(jsonStr.toString()).getJSONArray("variations");
		List<String> variantIds = new ArrayList<>();
		for (int i = 0; i < variations.length(); i++) {
			variantIds.add(variations.getJSONObject(i).getString("id"));
		}
		return variantIds;
	}

	// 🔹 DELETE /v2/variations
	private void sendVariantDeleteRequest(List<String> variantIds, int startIndex) throws Exception {
		String url = String.format("https://ac.cnstrc.com/v2/variations?key=%s&section=%s&force=true",
				URLEncoder.encode(API_KEY, "UTF-8"), URLEncoder.encode(SECTION, "UTF-8"));

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("DELETE");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");

		String encodedAuth = Base64.getEncoder().encodeToString((API_TOKEN + ":").getBytes(StandardCharsets.UTF_8));
		conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

		JSONObject payload = new JSONObject();
		JSONArray arr = new JSONArray();
		for (String variantId : variantIds) {
			arr.put(new JSONObject().put("id", variantId));
		}
		payload.put("variations", arr);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
		}

		int responseCode = conn.getResponseCode();
		System.out.printf(" DELETE /variations [%d - %d): HTTP %d%n", startIndex, startIndex + variantIds.size(),
				responseCode);

		if (responseCode >= 200 && responseCode < 300) {
		    System.out.printf("✅ DELETE /variations [%d - %d): HTTP %d%n", startIndex, startIndex + variantIds.size(), responseCode);
		} else {
		    throw new RuntimeException("❌ Failed DELETE /variations: HTTP " + responseCode);
		}
		conn.disconnect();
	}

}
