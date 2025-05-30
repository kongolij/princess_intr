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
public class ImportBrandFromCVS implements CommandLineRunner {

	private final BigCommerceService bigCommerceCategoryService;

	public ImportBrandFromCVS(BigCommerceService bigCommerceCategoryService) {

		this.bigCommerceCategoryService = bigCommerceCategoryService;
	}

	@Override
	public void run(String... args) {
		long startTime = System.currentTimeMillis(); // Start timing

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Brands_20250507194053.csv");

		if (inputStream == null) {
			System.err.println("❌ CSV file not found in resources!");
			return;
		}

		try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
			List<String[]> rows = reader.readAll();

			if (rows.isEmpty()) {
				System.out.println("⚠️ CSV is empty!");
				return;
			}

			List<Brand> brandList = new ArrayList<>();

			for (int i = 1; i < rows.size(); i++) {
				String[] row = rows.get(i);

				if (row.length < 10) {
					System.err.println("⚠️ Skipped malformed row at index " + i + ": Expected at least 10 columns, got "
							+ row.length);
					continue;
				}

				Brand brand = new Brand();

				brand.setId(safeGet(row, 0));
				brand.setDisplayName(safeGet(row, 1));
				brand.setDocumentsFileName(safeGet(row, 2));
				brand.setAltText(safeGet(row, 3));
				brand.setTitleText(safeGet(row, 4));
				brand.setActive(parseBooleanSafe(safeGet(row, 5)));
				brand.setSeoDescription(safeGet(row, 6));
				brand.setSeoKeywords(safeGet(row, 7));
				brand.setSeoTitle(safeGet(row, 8));
				brand.setSeoURLSlug(safeGet(row, 9));

				// brand.setFixedChildProducts(...) // IGNORED

				if (brand.getActive() == null || !brand.getActive()) {
					System.out.printf("⏩ Skipped inactive brand at row %d%n", i);
					continue;
				}

				if (brand.getDisplayName() == null || brand.getDisplayName().trim().equalsIgnoreCase("not available")) {
					System.out.printf("⏩ Skipped unavailable brand name at row %d%n", i);
					continue;
				}

				brandList.add(brand);
			}
			processBrandsToBigCommerce(brandList);
			long durationMillis = System.currentTimeMillis() - startTime;
			System.out.printf("✅ Done! Running time: %.2f min%n", durationMillis / 1000.0 / 60.0);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("❌ Error while processing CSV: " + e.getMessage());
		}
	}

	private void processBrandsToBigCommerce(List<Brand> brandList) {
		int createdCount = 0;

		for (Brand brand : brandList) {
			try {
				JSONObject brandJson = BigCommerceBrandMapper.mapBrandToBigCommerceJson(brand);
				Integer brandID = bigCommerceCategoryService.createBigCommerceBrand(brandJson);

				if (brandID != null) {
					createdCount++;
					System.out.printf("✅ Created brand: %s (ID: %d)%n", brand.getDisplayName(), brandID);

					JSONArray metafieldBatch = new JSONArray();
					JSONObject metafieldJson = BigCommerceBrandMapper.buildBrandMetafieldJson(brand, brandID);
					metafieldBatch.put(metafieldJson);

					bigCommerceCategoryService.createBrandMetafields(brandID, metafieldBatch);
					// Optional: create metafields now
					// JSONArray metafields = buildBrandMetafields(brand);
					// bigCommerceCategoryService.createBrandMetafields(brandID, metafields,
					// locale);
				} else {
					System.err.printf("❌ Failed to create brand: %s%n", brand.getDisplayName());
				}
			} catch (Exception ex) {
				System.err.printf("❌ Exception while creating brand %s: %s%n", brand.getDisplayName(), ex.getMessage());
			}
		}

		System.out.printf("🎯 Total brands processed: %d | Successfully created: %d%n", brandList.size(), createdCount);
	}

	private String safeGet(String[] row, int index) {
		return (index < row.length) ? row[index].trim() : null;
	}

	private Boolean parseBooleanSafe(String value) {
		if (value == null || value.isBlank())
			return null;
		return Boolean.parseBoolean(value.trim());
	}
}
