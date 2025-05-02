package com.bigcommerce.imports.catalog.product.inventory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.product.inventory.dto.InventoryRecord;
import com.bigcommerce.imports.catalog.product.inventory.service.BigCommerceProductInventoryService;
import com.bigcommerce.imports.catalog.product.prices.dto.VariantPrice;
import com.bigcommerce.imports.catalog.product.prices.service.BigCommerceProductPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;

//@Component
public class ImportProductsInventoryFromCVS implements CommandLineRunner {

	private final BigCommerceProductInventoryService bigCommerceProductInventoryService;

	public ImportProductsInventoryFromCVS(BigCommerceProductInventoryService bigCommerceProductInventoryService) {
		this.bigCommerceProductInventoryService = bigCommerceProductInventoryService;
	}

	@Override
	public void run(String... args) throws Exception {

		long startTime = System.currentTimeMillis(); // Start timing

		ObjectMapper mapper = new ObjectMapper();
//		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("final_sku_inventory_by_store.csv");
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("final_sku_inventory_single_sku.csv");

		if (inputStream == null) {
			System.err.println("❌ CSV file not found in resources!");
			return;
		}

		 try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
	            String line;
	            List<InventoryRecord> inventoryRecords = new ArrayList<>();

	            // Skip header
	            reader.readLine();

	            while ((line = reader.readLine()) != null) {
	                line = line.trim();
	                if (line.isEmpty()) continue;

	                String[] tokens = line.split(",");
	                if (tokens.length != 3) continue;

	                String sku = tokens[0];
	                int storeId = Integer.parseInt(tokens[1]);
	                int qty = Integer.parseInt(tokens[2]);

	                InventoryRecord record = new InventoryRecord(sku, storeId, qty);
	                inventoryRecords.add(record);
	            }
	           
	            bigCommerceProductInventoryService.updateVariantInventory(inventoryRecords);
	            System.out.printf("✅ Parsed %d inventory records.%n", inventoryRecords.size());
	            for (InventoryRecord record : inventoryRecords) {
	                System.out.printf("SKU: %s | Store: %d | Qty: %d%n", record.getSku(), record.getStoreId(), record.getAvailableQty());
	            }
	        } catch (IOException e) {
	            System.err.println("❌ Error reading inventory CSV: " + e.getMessage());
	            e.printStackTrace();
	        }

		 long totalTime = System.currentTimeMillis() - startTime;
	        System.out.println("✅ Done! Run time in seconds: " + (System.currentTimeMillis() - startTime) / 1000.0);
	        System.exit(0);
	    }
	
	

}
