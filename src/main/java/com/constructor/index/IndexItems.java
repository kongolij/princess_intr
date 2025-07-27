package com.constructor.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.bigcommerce.imports.catalog.service.BigCommerceService;
import com.constructor.client.BazaarvoiceReviewStatsClient;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.mapper.ConstructorJsonlProductMapper;
import com.opencsv.CSVWriter;

//@Component
public class IndexItems implements CommandLineRunner {

	
	private static final String PATH_EN = "target/output/index_en/item.jsonl";
	private static final String PATH_FR = "target/output/index_fr/item.jsonl";

	private final BigCommerceService bigCommerceCategoryService;
	private final BigCommerceGraphQlService bigCommerceGraphQlService;
	private final ConstructorJsonlProductMapper constructorJsonlProductMapper;

	public IndexItems(BigCommerceService bigCommerceCategoryService,
			BigCommerceGraphQlService bigCommerceGraphQlService, 
			ConstructorJsonlProductMapper constructorJsonlProductMapper) {
		this.bigCommerceCategoryService = bigCommerceCategoryService;
		this.bigCommerceGraphQlService = bigCommerceGraphQlService;
		this.constructorJsonlProductMapper=constructorJsonlProductMapper;

	}

//	{
//		"id": "cotton-t-shirt",
//		"name": "Cotton T-Shirt",
//		"data": 
//		{
//			"url": "https://constructor.com/",
//			"image_url": "https://constructorio-integrations.s3.amazonaws.com/tikus-threads/2022-06-29/WOVEN-CASUAL-SHIRT_BUTTON-DOWN-WOVEN-SHIRT_BSH01757SB1918_3_category.jpg",
//			"product_type": ["Shirts","T-Shirts"],
//			"group_ids": ["tops-athletic","tops-casual"],
//			"material": "Cotton",
//			"keywords": ["gym","casual","athletic","workout","comfort","simple"],
//			"description": "Treat yourself to a comfy upgrade with this Short Sleeve Shirt from Etchell's Emporium. This short-sleeve T-shirt comes with a classic crew-neck, giving you style and comfort that can easily be paired with a variety of bottoms.",
//			"active": true,
//			"price": 18}}
	
	@Override
	public void run(String... args) throws Exception {

		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		long startTime = System.currentTimeMillis(); // Start timing

//    	ProductGraphQLResponse.Product p= bigCommerceGraphQlService.getProductById(274);

		Map<Integer, List<Integer>> categoriesPath  = bigCommerceCategoryService.getCategoryPathMapFromTree();
		int a = categoriesPath.size();

		List<ProductGraphQLResponse.Product> products = bigCommerceGraphQlService.getAllProducts();
		System.out.println("Fetched " + products.size() + " products.");
		try (BufferedWriter jsonlWriter = new BufferedWriter(new FileWriter(PATH_EN))) {

			for (ProductGraphQLResponse.Product product : products) {
				String jsonLine = constructorJsonlProductMapper.mapToJsonlLine(product, categoriesPath , "en");
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
