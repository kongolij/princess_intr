package com.constructor.index.mapper;

import com.constructor.index.dto.ProductGraphQLResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstructorJsonlProductMapper {

	private static final ObjectMapper mapper = new ObjectMapper();

//	{
//	"id": "cotton-t-shirt",
//	"name": "Cotton T-Shirt",
//	"data": 
//	{
//		"url": "https://constructor.com/",
//		"image_url": "https://constructorio-integrations.s3.amazonaws.com/tikus-threads/2022-06-29/WOVEN-CASUAL-SHIRT_BUTTON-DOWN-WOVEN-SHIRT_BSH01757SB1918_3_category.jpg",
//		"product_type": ["Shirts","T-Shirts"],
//		"group_ids": ["tops-athletic","tops-casual"],
//		"material": "Cotton",
//		"keywords": ["gym","casual","athletic","workout","comfort","simple"],
//		"description": "Treat yourself to a comfy upgrade with this Short Sleeve Shirt from Etchell's Emporium. This short-sleeve T-shirt comes with a classic crew-neck, giving you style and comfort that can easily be paired with a variety of bottoms.",
//		"active": true,
//		"price": 18}}
	public static String mapToJsonlLine(ProductGraphQLResponse.Product product,
			Map<Integer, List<Integer>> categoriesPath, String locale) {
		ObjectNode node = mapper.createObjectNode();
		ObjectMapper localMapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();
		 

		node.put("id", product.getSku());
		
		data.set("url", new TextNode(product.getPath()));
//		node.put("url", product.getPath());
//		node.put("active", "true");
		data.set("active", BooleanNode.TRUE);

		// Image URL
		String imageUrl = product.getImages() != null && product.getImages().getEdges() != null
				&& !product.getImages().getEdges().isEmpty() ? product.getImages().getEdges().get(0).getNode().getUrl()
						: "";
//		node.put("image_url", imageUrl);
		data.set("image_url", new TextNode(imageUrl));

		// Custom Fields
		Map<String, String> customFieldMap = new HashMap<>();
		if (product.getCustomFields() != null && product.getCustomFields().getEdges() != null) {
			for (var edge : product.getCustomFields().getEdges()) {
				customFieldMap.put(edge.getNode().getName(), edge.getNode().getValue());
			}
		}
		
		Map<String, String> metafieldMap = new HashMap<>();
		if (product.getMetafields() != null && product.getMetafields().getEdges() != null) {
			for (var edge : product.getMetafields().getEdges()) {
				var metaNode = edge.getNode();
				if (node != null && metaNode.getKey() != null) {
					metafieldMap.put(metaNode.getKey(), metaNode.getValue());
				}
			}
		}

		// Get localized display name from "displayName" custom field
//        String itemName = product.getName(); // fallback
		String itemName = ""; // fallback
		try {
		    // Use locale to pick the right metafield
		    String metaKey = "displayName_" + locale; // e.g., "en" or "fr"
		    String attributesJson = metafieldMap.get(metaKey);
		    if (attributesJson != null) {
		            itemName = attributesJson;
		    }else {
		    	
		    	    itemName = product.getName(); // or product.getName() depending on your model
		    	
		    }
		} catch (Exception e) {
		    System.err.println("❌ Failed to parse localized product_attributes_" + locale + ": " + e.getMessage());
		}
//		node.put("item_name", itemName);
		node.put("name", itemName);

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
//		node.set("group_ids", groupIdsArray);
		data.set("group_ids", groupIdsArray);

		// Facet Category Name (localized)
//		String targetLocale = "en";
		String facetCategoryName = null;
		if (product.getCategories() != null && product.getCategories().getEdges() != null
				&& !product.getCategories().getEdges().isEmpty()) {
			ProductGraphQLResponse.Category category = product.getCategories().getEdges().get(0).getNode();
			facetCategoryName = category.getName(); // fallback
			if (category.getMetafields() != null && category.getMetafields().getEdges() != null) {
				for (ProductGraphQLResponse.MetafieldEdge metaEdge : category.getMetafields().getEdges()) {
					ProductGraphQLResponse.Metafield mf = metaEdge.getNode();
					if (mf != null && locale.equalsIgnoreCase(mf.getKey())) {
						facetCategoryName = mf.getValue();
						break;
					}
				}
			}
		}
		if (facetCategoryName != null) {
//			node.put("metadata:category_name", facetCategoryName);
			data.set("metadata:category_name", TextNode.valueOf(facetCategoryName));
		}

		String brandName = product.getBrand() != null ? product.getBrand().getName() : "";
//		node.put("metadata:brand", brandName);
//		node.put("metadata:productType", customFieldMap.getOrDefault("productType", ""));
//		node.put("metadata:productStatus", customFieldMap.getOrDefault("productStatus", ""));
//		node.put("metadata:productClearance", customFieldMap.getOrDefault("productClearance", ""));
//		node.put("metadata:availabilityCode", customFieldMap.getOrDefault("availabilityCode", ""));
//		node.put("metadata:creationDate", customFieldMap.getOrDefault("occCreationDate", ""));
//		node.put("metadata:Reviews", 0);
//		node.put("metadata:Average Rating", 0);
		
		data.set("metadata:brand", TextNode.valueOf(brandName));
		data.set("metadata:productType", TextNode.valueOf(customFieldMap.getOrDefault("productType", "")));
		data.set("metadata:productStatus", TextNode.valueOf(customFieldMap.getOrDefault("productStatus", "")));
		data.set("metadata:productClearance", TextNode.valueOf(customFieldMap.getOrDefault("productClearance", "")));
		data.set("metadata:availabilityCode", TextNode.valueOf(customFieldMap.getOrDefault("availabilityCode", "")));
		data.set("metadata:creationDate", TextNode.valueOf(customFieldMap.getOrDefault("occCreationDate", "")));
		data.set("metadata:Reviews", IntNode.valueOf(0));
		data.set("metadata:Average Rating", IntNode.valueOf(0));

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

//		node.put("metadata:availability", String.valueOf(availabilityArray.size()));
		data.set("metadata:availability", availabilityArray);

		node.set("data", data);
		return node.toString();
	}

	public static String mapToJsonlLine(ProductGraphQLResponse.Product product, String langauge) {
		return mapToJsonlLine(product, new HashMap<>(), langauge);
	}

	public static void writeJsonl(List<ProductGraphQLResponse.Product> products, String outputFilePath,
			Map<Integer, List<Integer>> categoriesPath,String langauge ) throws Exception {
		try (var writer = new java.io.BufferedWriter(new java.io.FileWriter(outputFilePath))) {
			for (ProductGraphQLResponse.Product product : products) {
				String jsonLine = mapToJsonlLine(product, categoriesPath,  langauge);
				writer.write(jsonLine);
				writer.newLine();
			}
		}
	}

	public static void writeJsonl(List<ProductGraphQLResponse.Product> products, String outputFilePath, String langauge)
			throws Exception {
		writeJsonl(products, outputFilePath, new HashMap<>(), langauge);
	}
}