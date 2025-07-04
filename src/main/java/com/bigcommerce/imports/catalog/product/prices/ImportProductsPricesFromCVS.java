package com.bigcommerce.imports.catalog.product.prices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.product.prices.dto.VariantPrice;
import com.bigcommerce.imports.catalog.product.prices.service.BigCommerceProductPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;

//@Component
public class ImportProductsPricesFromCVS implements CommandLineRunner {

	private final BigCommerceProductPriceService bigCommerceProductPriceService;

	public ImportProductsPricesFromCVS(BigCommerceProductPriceService bigCommerceProductPriceService) {
		this.bigCommerceProductPriceService = bigCommerceProductPriceService;
	}

	@Override
	public void run(String... args) throws Exception {

		long startTime = System.currentTimeMillis(); // Start timing

		ObjectMapper mapper = new ObjectMapper();
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("variant-prices-adjusted.csv");
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Price_20250503220824.csv");

		if (inputStream == null) {
			System.err.println("❌ CSV file not found in resources!");
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			List<VariantPrice> variantPrices = new ArrayList<>();
			// Skip first two lines (e.g., headers or metadata)
			reader.readLine();
			reader.readLine();

			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty())
					continue;

				String[] tokens = line.split(",");
				if (tokens.length < 2)
					continue;

				VariantPrice vp = new VariantPrice();
				vp.setSkuBr(tokens[0]);
				vp.setListPrice(new BigDecimal(tokens[1]));

				if (tokens.length > 2 && !tokens[2].isEmpty()) {
					vp.setSalePrice(new BigDecimal(tokens[2]));
				}

				variantPrices.add(vp);
			}
			 System.out.println("✅ Loaded Variant Prices:");
			bigCommerceProductPriceService.updateVariantPrices(variantPrices);
		} catch (IOException e) {
			System.err.println("❌ Error reading JSON line: " + e.getMessage());
			e.printStackTrace();
		}
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.printf("✅ Done! Runtime: %.2f seconds%n", totalTime / 1000.0);

		System.exit(0);
	}

}
