package com.constructor.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.bigcommerce.imports.catalog.service.BigCommerceService;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.mapper.ConstructorJsonlProductMapper;
import com.opencsv.CSVWriter;

//@Component
public class IndexCatalog implements CommandLineRunner {

	private static final String PATH_EN = "target/output/item_en.csv";
	private static final String PATH_FR = "target/output/item_fr.csv";

	private final BigCommerceService bigCommerceCategoryService;
	private final BigCommerceGraphQlService bigCommerceGraphQlService;

	public IndexCatalog(BigCommerceService bigCommerceCategoryService,
			BigCommerceGraphQlService bigCommerceGraphQlService) {
		this.bigCommerceCategoryService = bigCommerceCategoryService;
		this.bigCommerceGraphQlService = bigCommerceGraphQlService;

	}

	@Override
	public void run(String... args) throws Exception {

		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		long startTime = System.currentTimeMillis(); // Start timing

//    	ProductGraphQLResponse.Product p= bigCommerceGraphQlService.getProductById(274);

		Map<Integer, List<Integer>> categoriiesPath = bigCommerceCategoryService.getCategoryPathMapFromTree();
		int a = categoriiesPath.size();

		List<ProductGraphQLResponse.Product> products = bigCommerceGraphQlService.getAllProducts();
		System.out.println("Fetched " + products.size() + " products.");
		try (BufferedWriter jsonlWriter = new BufferedWriter(new FileWriter(PATH_EN))) {

			for (ProductGraphQLResponse.Product product : products) {
				String jsonLine = ConstructorJsonlProductMapper.mapToJsonlLine(product, categoriiesPath);
				jsonlWriter.write(jsonLine);
				jsonlWriter.newLine();
				System.out.println("[" + (++a) + "] " + jsonLine); // Optional preview
			}

		}

//    	String outputFilePath = "constructor_items.jsonl";
//        ConstructorJsonlProductMapper.writeJsonl(products, outputFilePath);

		long totalTime = System.currentTimeMillis() - startTime;
		System.out.printf("✅ Done! Runtime: %.2f seconds%n", totalTime / 1000.0);

		System.exit(0);
	}
}
