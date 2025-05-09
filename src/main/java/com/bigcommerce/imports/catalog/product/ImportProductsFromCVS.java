package com.bigcommerce.imports.catalog.product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.Variant;
import com.bigcommerce.imports.catalog.product.service.BigCommerceProductService;
import com.fasterxml.jackson.databind.ObjectMapper;

//@Component
public class ImportProductsFromCVS implements CommandLineRunner {

//	static int CATEGOU_TREE_ID=7;  // jimmy store

//	static int CATEGORY_TREE_ID = 2; // EPAM-PAL-DE store

	
	private final BigCommerceProductService bigCommerceProductService;

	public ImportProductsFromCVS(BigCommerceProductService bigCommerceProductService) {
       this.bigCommerceProductService = bigCommerceProductService;
	}

	@Override
	public void run(String... args) throws Exception {

		long startTime = System.currentTimeMillis(); // Start timing

		ObjectMapper mapper = new ObjectMapper();
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product-with-assets-single-line.txt");
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product-variants-assets-product-level-verified.txt");
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("PAL_Product_20250324_csv2");
		
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product-flat-line-PA0009322322.txt");
		

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
				System.out.println("  📂 Category: " + String.join(", ", product.categories));
				System.out.println("  🔧 Variants:");

				
			}
			
			addProductToBigCommerce(products,"en");
		} catch (IOException e) {
			System.err.println("❌ Error reading JSON line: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("✅ Done! runnig time in min " ); 
		System.exit(0);
	}

	public void addProductToBigCommerce(List<Product> products, String locale) throws Exception {
		bigCommerceProductService.importProducts(products, locale);
	}

	

}
