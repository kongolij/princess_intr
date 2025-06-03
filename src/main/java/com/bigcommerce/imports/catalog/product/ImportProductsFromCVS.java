package com.bigcommerce.imports.catalog.product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.product.dto.Category;
import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.Variant;
import com.bigcommerce.imports.catalog.product.service.AttributeLabelService;
import com.bigcommerce.imports.catalog.product.service.BigCommerceProductService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ImportProductsFromCVS implements CommandLineRunner {

//	static int CATEGOU_TREE_ID=7;  // jimmy store

//	static int CATEGORY_TREE_ID = 2; // EPAM-PAL-DE store

	private final BigCommerceProductService bigCommerceProductService;
	private final AttributeLabelService attributeLabelService;

	public ImportProductsFromCVS(BigCommerceProductService bigCommerceProductService,
			AttributeLabelService attributeLabelService) {
		this.bigCommerceProductService = bigCommerceProductService;
		this.attributeLabelService = attributeLabelService;
	}

	@Override
	public void run(String... args) throws Exception {

		long startTime = System.currentTimeMillis(); // Start timing

		ObjectMapper mapper = new ObjectMapper();
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product-with-assets-single-line.txt");
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product-variants-assets-product-level-verified.txt");
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("PAL_Product_20250324_csv2");

//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product-flat-line-PA0009322322.txt");

//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Sample For EPAM (flattened).json");

//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Sample For EPAM (flattened)_1.json");

//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Flattened_Products.jsonl");
		
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("merged_products_flattened.json");
		
		
		if (inputStream == null) {
			System.err.println("❌ CSV file not found in resources!");
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			List<Product> products = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				line = line.trim(); // Remove leading/trailing spaces
				Product product = mapper.readValue(line, Product.class);
				products.add(product);

				System.out.println("✅ Product Number: " + product.productNumber);
				String categoriesCsv = product.getCategories().stream().map(Category::getId)
						.collect(Collectors.joining(", "));

				System.out.println("  📂 Categories: " + categoriesCsv);
				System.out.println("  🔧 Variants:");

			}
			// Get attribute label codes with English and French translations
			long startLabelMapTime = System.currentTimeMillis();
			Map<String, Map<String, String>> attributeLabelMap = attributeLabelService.buildEnglishAndFrenchMaps();
			long endLabelMapTime = System.currentTimeMillis();
			double labelMapDurationInSeconds = (endLabelMapTime - startLabelMapTime) / 1000.0;
			int totalMappedAttributes = attributeLabelMap.values().stream()
				    .mapToInt(Map::size)
				    .sum();

			System.out.printf("✅ Mapped %d attribute codes in %.2f seconds%n", totalMappedAttributes, labelMapDurationInSeconds);
			
			addProductToBigCommerce(products, "en", attributeLabelMap);
		} catch (IOException e) {
			System.err.println("❌ Error reading JSON line: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("✅ Done! runnig time in min ");
		System.exit(0);
	}

	public void addProductToBigCommerce(List<Product> products, String locale,
			Map<String, Map<String, String>> attribtueLabelMap) throws Exception {
		bigCommerceProductService.importProducts(products, locale, attribtueLabelMap);
	}

}
