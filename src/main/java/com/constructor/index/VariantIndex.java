package com.constructor.index;

import java.io.FileWriter;
import java.io.PrintWriter;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
import com.opencsv.CSVWriter;

//@Component
public class VariantIndex implements CommandLineRunner {

	private static final String API_URL = "https://api.bigcommerce.com/stores/%s/v3/catalog/categories/%d/metafields";
	private static final String TREE_API_URL = "https://api.bigcommerce.com/stores/%s/v3/catalog/trees/%d/categories";
	private static final String STORE_HASH = BigCommerceStoreConfig.STORE_HASH;
	private static final String ACCESS_TOKEN = BigCommerceStoreConfig.ACCESS_TOKEN;
	private static final int CATEGORY_TREE_ID = BigCommerceStoreConfig.CATEGORY_TREE_ID;

	private static final String CSV_PATH_EN = "target/output/item_groups_en.csv";
	private static final String CSV_PATH_FR = "target/output/item_groups_fr.csv";

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		long startTime = System.currentTimeMillis(); // Start timing
		List<CategoryNode> allCategories = fetchCategoryTree(STORE_HASH, CATEGORY_TREE_ID);

		Map<Integer, String> idToSlug = new HashMap<>();
		for (CategoryNode node : allCategories) {
			idToSlug.put(node.id, generateSlug(node));
		}

		try (CSVWriter writerEn = new CSVWriter(new FileWriter(CSV_PATH_EN));
				CSVWriter writerFr = new CSVWriter(new FileWriter(CSV_PATH_FR))) {

			writerEn.writeNext(new String[] { "parent_id", "id", "name", "data" });
			writerFr.writeNext(new String[] { "parent_id", "id", "name", "data" });

			for (CategoryNode node : allCategories) {
				Map<String, String> localizedNames = fetchLocalizedNames(STORE_HASH, node.id);
				String enName = localizedNames.getOrDefault("en", node.name);
				String frName = localizedNames.getOrDefault("fr", node.name);
				String url = node.url != null ? node.url : "";
				String parentId = node.parentId != null ? node.parentId : "";
				String data = String.format("{\"url\":\"%s\"}", url);

//                   String idSlug = idToSlug.get(node.id);
//                   String parentSlug = node.parentId != null ? idToSlug.get(Integer.valueOf(node.parentId)) : "all";
//                   String data = String.format("{\"url\":\"%s\"}", url);

//                   writerEn.writeNext(new String[]{parentSlug, idSlug, enName, data});
//                   writerFr.writeNext(new String[]{parentSlug, idSlug, frName, data});

				writerEn.writeNext(new String[] { parentId, String.valueOf(node.id), enName, data });
				writerFr.writeNext(new String[] { parentId, String.valueOf(node.id), frName, data });
			}
		}

//        try (PrintWriter writerEn = new PrintWriter(new FileWriter(CSV_PATH_EN));
//             PrintWriter writerFr = new PrintWriter(new FileWriter(CSV_PATH_FR))) {
//
//            writerEn.println("parent_id,id,name,data");
//            writerFr.println("parent_id,id,name,data");
//
//            for (CategoryNode node : allCategories) {
//                Map<String, String> localizedNames = fetchLocalizedNames(STORE_HASH, node.id);
//                String enName = localizedNames.getOrDefault("en", node.name);
//                String frName = localizedNames.getOrDefault("fr", node.name);
//                String url = node.url != null ? node.url : "";
//                String parentId = node.parentId != null ? node.parentId : "";
//                String data = String.format("{ \"url\": \"%s\" }", url);
//
//                writerEn.printf("%s,%s,%s,%s%n", parentId, node.id, enName, data);
//                writerFr.printf("%s,%s,%s,%s%n", parentId, node.id, frName, data);
//            }
//        }

		System.out.println("✅ English CSV written to " + CSV_PATH_EN);
		System.out.println("✅ French CSV written to " + CSV_PATH_FR);
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.printf("✅ Done! Runtime: %.2f seconds%n", totalTime / 1000.0);

		System.exit(0);
	}

	private List<CategoryNode> fetchCategoryTree(String storeHash, int treeId) throws Exception {
		String urlStr = String.format(TREE_API_URL, storeHash, treeId);
		HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("X-Auth-Token", ACCESS_TOKEN);
		conn.setRequestProperty("Accept", "application/json");

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed to fetch category tree: " + conn.getResponseCode());
		}

		Scanner scanner = new Scanner(conn.getInputStream());
		String response = scanner.useDelimiter("\\A").next();
		scanner.close();

		JSONArray data = new JSONObject(response).getJSONArray("data");
		List<CategoryNode> all = new ArrayList<>();
		parseTree(null, data, all);
		return all;
	}

	private void parseTree(Integer parentId, JSONArray nodes, List<CategoryNode> all) {
		for (int i = 0; i < nodes.length(); i++) {
			JSONObject obj = nodes.getJSONObject(i);
			int id = obj.getInt("id");
			String name = obj.getString("name");
			String url = obj.optString("url", "");

			CategoryNode node = new CategoryNode(id, name, parentId != null ? parentId.toString() : null, url);
			all.add(node);

			if (obj.has("children")) {
				parseTree(id, obj.getJSONArray("children"), all);
			}
		}
	}

	private String generateSlug(CategoryNode node) {
		if (node.url != null && !node.url.isEmpty()) {
			String[] parts = node.url.split("/");
			return parts.length > 0 ? parts[parts.length - 1] : slugify(node.name);
		}
		return slugify(node.name);
	}

	private String slugify(String input) {
		return input.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
	}

	private Map<String, String> fetchLocalizedNames(String storeHash, int categoryId) {
		try {
			String urlStr = String.format(API_URL, storeHash, categoryId);
			HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("X-Auth-Token", ACCESS_TOKEN);
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200)
				return Map.of();

			Scanner scanner = new Scanner(conn.getInputStream());
			String response = scanner.useDelimiter("\\A").next();
			scanner.close();

			JSONArray data = new JSONObject(response).getJSONArray("data");
			return data.toList().stream().map(obj -> new JSONObject((Map<?, ?>) obj))
					.filter(meta -> meta.has("key") && meta.has("value"))
					.collect(Collectors.toMap(m -> m.getString("key"), m -> m.getString("value")));
		} catch (Exception e) {
			System.err.println("⚠️ Failed to fetch metafields for category " + categoryId + ": " + e.getMessage());
			return Map.of();
		}
	}

	private static class CategoryNode {
		int id;
		String name;
		String parentId;
		String url;

		CategoryNode(int id, String name, String parentId, String url) {
			this.id = id;
			this.name = name;
			this.parentId = parentId;
			this.url = url;
		}
	}
}
