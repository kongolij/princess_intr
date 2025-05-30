package com.bigcommerce.imports.catalog.product.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class AttributeLabelService {

	private final CosmosContainer container;

	public AttributeLabelService(CosmosContainer container) {
		this.container = container;
	}

	public Map<String, String> buildLabelMap(String languageCode) {
		Map<String, String> map = new HashMap<>();
		String query = String.format("SELECT c.id, c.label.%s FROM c", languageCode);

		CosmosPagedIterable<JsonNode> results = container.queryItems(query, new CosmosQueryRequestOptions(),
				JsonNode.class);

		for (JsonNode node : results) {
			String id = node.path("id").asText(null);
			String label = node.path(languageCode).asText(null);
			if (id != null && label != null) {
				map.put(id, label);
			}
		}

		return map;
	}

	public Map<String, Map<String, String>> buildEnglishAndFrenchMaps() {
		Map<String, String> labelsEn = buildLabelMap("en");
		Map<String, String> labelsFr = buildLabelMap("fr_CA");

		Map<String, Map<String, String>> combined = new HashMap<>();
		combined.put("en", labelsEn);
		combined.put("fr", labelsFr);
		return combined;
	}

	public Map<String, String> buildAttributeCodeToLabelMap() {
		Map<String, String> attributeMap = new HashMap<>();

		String query = "SELECT c.id, c.label.en FROM c";
		CosmosPagedIterable<JsonNode> results = container.queryItems(query, new CosmosQueryRequestOptions(),
				JsonNode.class);

		for (JsonNode node : results) {
			String id = node.path("id").asText(null);
			String enLabel = node.path("label").path("en").asText(null);

			if (id != null && enLabel != null) {
				attributeMap.put(id, enLabel);
			}
		}

		return attributeMap;
	}
}
