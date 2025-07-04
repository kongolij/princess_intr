package com.bigcommerce.imports.catalog.product;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
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

//@Component
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
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Flattened_Products_V6.jsonl");
		
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("merged_products_flattened.json");
		
		
		if (inputStream == null) {
			System.err.println("❌ CSV file not found in resources!");
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			List<Product> products = new ArrayList<>();
			Map<String, Product> productMap = new HashMap<>();
			
			// Collect category IDs, brand IDs, and product-to-SKU mapping
	        List<String> categoryIds = new ArrayList<>();
	        List<String> brandIds = new ArrayList<>();
	        Map<String, List<String>> productToSkusMap = new HashMap<>();
	        
			while ((line = reader.readLine()) != null) {
				line = line.trim(); // Remove leading/trailing spaces
				Product product = mapper.readValue(line, Product.class);
				products.add(product);

				// Check if the product has only one variant and no options
			    boolean isSimpleProduct = false;
			    if (product.getVariants() != null && product.getVariants().size() == 1) {
			        Variant singleVariant = product.getVariants().get(0);
			        isSimpleProduct = (singleVariant.getOption_values() == null || singleVariant.getOption_values().isEmpty());
			    }

			    String key;
			    if (isSimpleProduct && product.getVariants().get(0).getSkuNumber() != null) {
			        // Use the SKU as the key for simple product
			        key = product.getVariants().get(0).getSkuNumber();
			    } else if (product.getProductNumber() != null) {
			        // Otherwise, use the product number
			        key = product.getProductNumber();
			    } else {
			        // Fallback – skip if no suitable key
			    	 System.err.printf("⚠️ Skipping product: no suitable key "
			    	 		+ "(productNumber: %s, firstVariantSku: %s)%n",
			                 product.getProductNumber(),
			                 (product.getVariants() != null && !product.getVariants().isEmpty()) ? product.getVariants().get(0).getSkuNumber() : "null");
			        continue;
			    }
			    
			    // Put the product in the map
			    productMap.put(key, product);
				
				// ✅ Collect category IDs
	            if (product.getCategories() != null) {
	                for (Category category : product.getCategories()) {
	                    if (category.getId() != null) {
	                        categoryIds.add(category.getId());
	                    }
	                }
	            }

	            // ✅ Collect brand IDs
	            if (product.getBrand() != null && product.getBrand() != null) {
	                brandIds.add(product.getBrand());
	            }

	            // ✅ Collect productNumber -> list of SKUs
	            List<String> skus = product.getVariants().stream()
	                    .map(Variant::getSkuNumber)
	                    .distinct()
	                    .collect(Collectors.toList());
	            productToSkusMap.put(product.getProductNumber(), skus);
				System.out.println("✅ Product Number: " + product.productNumber);

			}
			
			// 🟢 Print categories list as a CSV
			List<String> uniqueCategoryIds = categoryIds.stream().distinct().collect(Collectors.toList());
			String categoryCsv = String.join(", ", uniqueCategoryIds);
			System.out.println("✅ Categories (CSV):");
			System.out.println(categoryCsv);

			// 🟢 Print brands list as a CSV
			List<String> uniqueBrandIds = brandIds.stream().distinct().collect(Collectors.toList());
			String brandCsv = String.join(", ", uniqueBrandIds);
			System.out.println("✅ Brands (CSV):");
			System.out.println(brandCsv);

			// 🟢 Print products and their SKUs in CSV-like style
			System.out.println("✅ Products and their SKUs:");
			productToSkusMap.forEach((productNumber, skus) -> {
			    String skuList = skus.stream().collect(Collectors.joining(", "));
			    System.out.printf("%s, [%s]%n", productNumber, skuList);
			});
	        
			// Get attribute label codes with English and French translations
			long startLabelMapTime = System.currentTimeMillis();
			Map<String, Map<String, String>> attributeLabelMap = attributeLabelService.buildEnglishAndFrenchMaps();
			long endLabelMapTime = System.currentTimeMillis();
			double labelMapDurationInSeconds = (endLabelMapTime - startLabelMapTime) / 1000.0;
			int totalMappedAttributes = attributeLabelMap.values().stream()
				    .mapToInt(Map::size)
				    .sum();

			System.out.printf("✅ Mapped %d attribute codes in %.2f seconds%n", totalMappedAttributes, labelMapDurationInSeconds);
			
//			addProductToBigCommerce(products, "en", attributeLabelMap);
			addProductToBigCommerce(productMap, "en", attributeLabelMap);
		} catch (IOException e) {
			System.err.println("❌ Error reading JSON line: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("✅ Done! runnig time in min ");
		System.exit(0);
	}

	public void addProductToBigCommerce(
			Map<String, Product> productMap, 
			String locale,
			Map<String, Map<String, String>> attribtueLabelMap) throws Exception {
		bigCommerceProductService.importProducts(productMap, locale, attribtueLabelMap);
	}
	
	public void addProductToBigCommerce(List<Product> products, String locale,
			Map<String, Map<String, String>> attribtueLabelMap) throws Exception {
//		bigCommerceProductService.importProducts(products, locale, attribtueLabelMap);
	}

}
