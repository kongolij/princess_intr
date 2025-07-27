package com.constructor.index;

import java.io.BufferedWriter;
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
import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.bigcommerce.imports.catalog.service.BigCommerceService;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.mapper.ConstructorJsonlProductMapper;
import com.opencsv.CSVWriter;

//@Component
public class VariantIndex implements CommandLineRunner {

//	private static final String PATH_EN = "target/output/variant_en/variations.jsonl";
	private static final String PATH_EN = "target/output/index_en/variations.jsonl";
	private static final String PATH_FR = "target/output/index_fr/variations.jsonl";

	private final BigCommerceService bigCommerceCategoryService;
	private final BigCommerceGraphQlService bigCommerceGraphQlService;
	private final ConstructorJsonlProductMapper constructorJsonlProductMapper;

	public VariantIndex(BigCommerceService bigCommerceCategoryService,
			BigCommerceGraphQlService bigCommerceGraphQlService,
			ConstructorJsonlProductMapper constructorJsonlProductMapper) {
		this.bigCommerceCategoryService = bigCommerceCategoryService;
		this.bigCommerceGraphQlService = bigCommerceGraphQlService;
		this.constructorJsonlProductMapper=constructorJsonlProductMapper;

	}

	@Override
	public void run(String... args) throws Exception {

		List<ProductGraphQLResponse.Product> products = bigCommerceGraphQlService.getAllProducts();
		System.out.println("Fetched " + products.size() + " products.");
		try (BufferedWriter jsonlWriter = new BufferedWriter(new FileWriter(PATH_EN))) {

			for (ProductGraphQLResponse.Product product : products) {
				List<String> jsonLines = constructorJsonlProductMapper.mapVariantsToJsonlLine(product, "en");

				for (String jsonLine : jsonLines) {
					jsonlWriter.write(jsonLine);
					jsonlWriter.newLine();
					System.out.println("📝 " + jsonLine);
				}

			}
			System.exit(0);
		}
	}

}
