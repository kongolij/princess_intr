package com.bigcommerce.imports.attribtuelabels;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.azure.cosmos.CosmosContainer;
import com.bigcommerce.imports.catalog.dto.CategoryNode;
import com.bigcommerce.imports.catalog.product.service.BigCommerceProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;

//@Component
public class ImportAttribtueLabels implements CommandLineRunner {

	private final CosmosContainer container;

	public ImportAttribtueLabels(CosmosContainer container) {
		this.container = container;
	}

	@Override
	public void run(String... args) throws Exception {
		long startTime = System.currentTimeMillis(); // Start timing

		//test
		InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream("AttributeLabels_20250330195115.json");

		if (inputStream == null) {
			System.err.println("❌ JSON file not found in resources!");
			return;
		}

		try {
			// Parse the JSON file as a list of objects
			ObjectMapper objectMapper = new ObjectMapper();
			List<Object> attributeLabels = objectMapper.readValue(inputStream, List.class);

			System.out.println("📦 Total records: " + attributeLabels.size());

			for (Object item : attributeLabels) {
				container.createItem(item); // assumes each item has an "id" field
			}

			System.out.println("✅ Finished uploading records to Cosmos DB");
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("❌ Error while processing JSON: " + e.getMessage());
		}

	}

}
