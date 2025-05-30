package com.bigcommerce.imports.brand;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.brand.dto.Brand;
import com.bigcommerce.imports.brand.mapper.BigCommerceBrandMapper;
import com.bigcommerce.imports.catalog.service.BigCommerceService;
import com.opencsv.CSVReader;

//@Component
public class DeleteBrands implements CommandLineRunner {

	private final BigCommerceService bigCommerceCategoryService;

	public DeleteBrands(BigCommerceService bigCommerceCategoryService) {

		this.bigCommerceCategoryService = bigCommerceCategoryService;
	}

	@Override
	public void run(String... args) throws Exception {

		long startTime = System.currentTimeMillis(); // Start timing

		List<String> brandNames = bigCommerceCategoryService.getAllBrandNames();

		int deletedCount = 0;

		for (String brandName : brandNames) {
			if (brandName != null && !brandName.isBlank()) {
				boolean deleted = bigCommerceCategoryService.deleteBrandByName(brandName);
				if (deleted) {
					System.out.printf("🗑️ Deleted brand: %s%n", brandName);
					deletedCount++;
				} else {
					System.err.printf("❌ Failed to delete brand: %s%n", brandName);
				}
			}
		}

		long duration = System.currentTimeMillis() - startTime;
		System.out.printf("✅ Finished. Total brands deleted: %d | Time: %.2f min%n", deletedCount,
				duration / 1000.0 / 60.0);
		System.exit(0);
	}

}
