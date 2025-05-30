package com.bigcommerce.imports.customer;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

//@Component
public class ImportCustomeAddressFromJson implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		long startTime = System.currentTimeMillis();
		System.out.println("🚀 Starting JSON structure inspection...");

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Accounts.json");
		if (inputStream == null) {
			System.err.println("❌ JSON file not found in resources!");
			return;
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(inputStream);

		JsonNode resultsNode = root.path("organization");
		if (!resultsNode.isArray()) {
			System.err.println("❌ organization 'user' to be an array, but found: " + resultsNode.getNodeType());
			System.exit(1);
		}

		// Print one full record (e.g., first)
		if (resultsNode.size() > 0) {
			System.out.println("\n🔍 Sample record (first organization):\n");
			String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultsNode.get(0));
			System.out.println(pretty);
		} else {
			System.out.println("⚠️ No records found under 'organization'");
		}

		Map<String, Set<String>> structureMap = new LinkedHashMap<>();
		int count = 0;

		for (JsonNode node : resultsNode) {
			collectStructure(node, "", structureMap);
			count++;
			if (count % 10_000 == 0) {
				System.out.println("📦 Processed " + count + " records...");
			}
		}

		System.out.println("\n✅ Total entries processed: " + count);

		System.out.println("\n📄 Merged JSON structure:");
		for (Map.Entry<String, Set<String>> entry : structureMap.entrySet()) {
			System.out.printf("%s: %s%n", entry.getKey(), entry.getValue());
		}

		long durationMs = System.currentTimeMillis() - startTime;
		System.out.printf("\n⏱️ Completed in %.2f seconds%n", durationMs / 1000.0);

		System.exit(0);
	}

	private void collectStructure(JsonNode node, String path, Map<String, Set<String>> structureMap) {
		if (node.isObject()) {
			node.fieldNames().forEachRemaining(field -> {
				JsonNode value = node.get(field);
				String fullPath = path.isEmpty() ? field : path + "." + field;
				structureMap.computeIfAbsent(fullPath, k -> new LinkedHashSet<>()).add(getType(value));
				collectStructure(value, fullPath, structureMap);
			});
		} else if (node.isArray()) {
			for (JsonNode item : node) {
				collectStructure(item, path + "[]", structureMap);
			}
		}
	}

	private String getType(JsonNode node) {
		if (node.isTextual())
			return "String";
		if (node.isInt())
			return "Integer";
		if (node.isLong())
			return "Long";
		if (node.isDouble() || node.isFloat() || node.isNumber())
			return "Double";
		if (node.isBoolean())
			return "Boolean";
		if (node.isArray())
			return "Array";
		if (node.isObject())
			return "Object";
		if (node.isNull())
			return "Null";
		return "Unknown";
	}
}
