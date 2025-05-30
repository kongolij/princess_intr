package com.constructor.index.mapper;

import com.constructor.index.dto.ProductGraphQLResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstructorJsonlProductMapper {

	private static final ObjectMapper mapper = new ObjectMapper();

	public static String mapToJsonlLine(ProductGraphQLResponse.Product product,
			Map<Integer, List<Integer>> categoriesPath) {
		ObjectNode node = mapper.createObjectNode();
		ObjectMapper localMapper = new ObjectMapper();

		node.put("id", product.getSku());
		node.put("url", product.getPath());
		node.put("active", "true");

		// Image URL
		String imageUrl = product.getImages() != null && product.getImages().getEdges() != null
				&& !product.getImages().getEdges().isEmpty() ? product.getImages().getEdges().get(0).getNode().getUrl()
						: "";
		node.put("image_url", imageUrl);

		// Custom Fields
		Map<String, String> customFieldMap = new HashMap<>();
		if (product.getCustomFields() != null && product.getCustomFields().getEdges() != null) {
			for (var edge : product.getCustomFields().getEdges()) {
				customFieldMap.put(edge.getNode().getName(), edge.getNode().getValue());
			}
		}

		// Get localized display name from "displayName" custom field
//        String itemName = product.getName(); // fallback
		String itemName = ""; // fallback
		try {
			String displayNameJson = customFieldMap.get("displayName");
			if (displayNameJson != null) {
				JsonNode displayNameNode = localMapper.readTree(displayNameJson);
				JsonNode valueNode = displayNameNode.path("value").path("en");
				if (!valueNode.isMissingNode()) {
					itemName = valueNode.asText();
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to parse displayName: " + e.getMessage());
		}
		node.put("item_name", itemName);

		// Group IDs
		ArrayNode groupIdsArray = mapper.createArrayNode();
		if (product.getCategories() != null && product.getCategories().getEdges() != null) {
			for (ProductGraphQLResponse.Edge<ProductGraphQLResponse.Category> edge : product.getCategories()
					.getEdges()) {
				ProductGraphQLResponse.Category category = edge.getNode();
				int categoryId = category.getEntityId();
				groupIdsArray.add(String.valueOf(categoryId));
			}
		}
		node.set("group_ids", groupIdsArray);

		// Facet Category Name (localized)
		String targetLocale = "en";
		String facetCategoryName = null;
		if (product.getCategories() != null && product.getCategories().getEdges() != null
				&& !product.getCategories().getEdges().isEmpty()) {
			ProductGraphQLResponse.Category category = product.getCategories().getEdges().get(0).getNode();
			facetCategoryName = category.getName(); // fallback
			if (category.getMetafields() != null && category.getMetafields().getEdges() != null) {
				for (ProductGraphQLResponse.MetafieldEdge metaEdge : category.getMetafields().getEdges()) {
					ProductGraphQLResponse.Metafield mf = metaEdge.getNode();
					if (mf != null && targetLocale.equalsIgnoreCase(mf.getKey())) {
						facetCategoryName = mf.getValue();
						break;
					}
				}
			}
		}
		if (facetCategoryName != null) {
			node.put("facet:category_name", facetCategoryName);
		}

		node.put("metadata:brand", customFieldMap.getOrDefault("brand", ""));
		node.put("metadata:productType", customFieldMap.getOrDefault("productType", ""));
		node.put("metadata:productStatus", customFieldMap.getOrDefault("paProductStatus", ""));
		node.put("metadata:productClearance", customFieldMap.getOrDefault("paProductClearance", ""));
		node.put("metadata:availabilityCode", customFieldMap.getOrDefault("paAvailabilityCode", ""));
		node.put("metadata:creationDate", customFieldMap.getOrDefault("occCreationDate", ""));
		node.put("metadata:Reviews", 0);
		node.put("metadata:Average Rating", 0);

		// Metadata availability
		ArrayNode availabilityArray = mapper.createArrayNode();
		if (product.getVariants() != null && product.getVariants().getEdges() != null) {
			for (var variantEdge : product.getVariants().getEdges()) {
				var inventory = variantEdge.getNode().getInventory();
				var byLocation = inventory != null ? inventory.getByLocation() : null;
				if (byLocation != null && byLocation.getEdges() != null) {
					for (var locEdge : byLocation.getEdges()) {
						availabilityArray.add(String.valueOf(locEdge.getNode().getLocationEntityId()));
					}
				}
			}
		}
//        ObjectNode metadataNode = mapper.createObjectNode();
//        metadataNode.set("availability", availabilityArray);
//        node.set("metadata", metadataNode);

		node.put("facet:availability", String.valueOf(availabilityArray.size()));

		return node.toString();
	}

	public static String mapToJsonlLine(ProductGraphQLResponse.Product product) {
		return mapToJsonlLine(product, new HashMap<>());
	}

	public static void writeJsonl(List<ProductGraphQLResponse.Product> products, String outputFilePath,
			Map<Integer, List<Integer>> categoriesPath) throws Exception {
		try (var writer = new java.io.BufferedWriter(new java.io.FileWriter(outputFilePath))) {
			for (ProductGraphQLResponse.Product product : products) {
				String jsonLine = mapToJsonlLine(product, categoriesPath);
				writer.write(jsonLine);
				writer.newLine();
			}
		}
	}

	public static void writeJsonl(List<ProductGraphQLResponse.Product> products, String outputFilePath)
			throws Exception {
		writeJsonl(products, outputFilePath, new HashMap<>());
	}
}