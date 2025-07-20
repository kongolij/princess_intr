package com.bigcommerce.imports.catalog.product.inventory.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.bigcommerce.imports.catalog.product.inventory.dto.InventoryRecord;
import com.bigcommerce.imports.catalog.product.inventory.dto.SkuVariantInfo;
import com.bigcommerce.imports.catalog.product.inventory.mapper.BigCommerceInventoryMapper;
import com.bigcommerce.imports.catalog.product.repository.BigCommerceRepository;

@Service
public class BigCommerceProductInventoryService {

	private final BigCommerceRepository bigCommerceRepository;

	public BigCommerceProductInventoryService(BigCommerceRepository bigCommerceRepository) {
		this.bigCommerceRepository = bigCommerceRepository;
	}

	public void updateVariantInventory(List<InventoryRecord> inventoryRecords) throws Exception {

		// 1. Extract all distinct SKUs from the incoming inventory records
		List<String> skus = inventoryRecords.stream()
				.map(InventoryRecord::getSku)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		
		// 2. For each chunk of up to 50 SKUs, retrieve the corresponding variant and product IDs
		Map<String, SkuVariantInfo> skuToIdsMap = new HashMap<>();
		for (int i = 0; i < skus.size(); i += 50) {
			List<String> chunk = skus.subList(i, Math.min(i + 50, skus.size()));
			Map<String, SkuVariantInfo> chunkMap = bigCommerceRepository.getVariantProductAndIdsBySkusUpdated(chunk);
			skuToIdsMap.putAll(chunkMap);
		}

		// 3. Get a mapping from store code to BigCommerce location ID
		Map<String, Integer> storeLocationMap = bigCommerceRepository.getStoreCodeToLocationIdMap();
		
		// 4. Attach locationId to each InventoryRecord (based on storeId)
		for (InventoryRecord record : inventoryRecords) {
		    Integer locationId = storeLocationMap.get(String.valueOf(record.getStoreId()));
		    if (locationId != null) {
		        record.setLocationId(locationId);
		    } else {
		        System.err.printf("⚠️ No location found for storeId %d (sku: %s)%n", record.getStoreId(), record.getSku());
		    }
		}

		// 5. Build the inventory adjustment payload (quantity changes per SKU per location)
		JSONArray inventoryAdjustmentItems = BigCommerceInventoryMapper
				.buildInventoryAdjustmentPayload(inventoryRecords, skuToIdsMap, storeLocationMap);

		// 6. Send inventory adjustments in batches of 100 items to the inventoryOverrides endpoint
		final int BATCH_SIZE = 100;
		List<JSONObject> items = new ArrayList<>();
		for (int i = 0; i < inventoryAdjustmentItems.length(); i++) {
			items.add(inventoryAdjustmentItems.getJSONObject(i));
		}

		for (int i = 0; i < items.size(); i += BATCH_SIZE) {
			List<JSONObject> batch = items.subList(i, Math.min(i + BATCH_SIZE, items.size()));
			JSONArray batchArray = new JSONArray(batch);

			JSONObject payload = new JSONObject();
			payload.put("reason", "Bulk absolute inventory adjustment");
			payload.put("items", batchArray);

			try {
				bigCommerceRepository.inventoryOverrides(payload);
				System.out.printf("✅ Inventory batch [%d - %d) updated successfully.%n", i,
						Math.min(i + BATCH_SIZE, items.size()));
			} catch (Exception e) {
				System.err.printf("❌ Failed inventory update for batch [%d - %d): %s%n", i,
						Math.min(i + BATCH_SIZE, items.size()), e.getMessage());
				e.printStackTrace();
			}
		}

        //		Update Inventory Settings for a Location
        //		https://api.bigcommerce.com/stores/{store_hash}/v3/inventory/locations/{location_id}/items
	    // activate all products
		//{
		//  "settings": [
		//		               {
		//		                 "identity": {
		//		                   "sku": "RE-130"
		//		                 },
		//		                 "safety_stock": 0,
		//		                 "is_in_stock": true,
		//		                 "warning_level": 0,
		//		                 "bin_picking_number": "{"en":"","fr":""}"
		//		               }
		//		             ]
		//		           }
		
//	
		// 7. Prepare inventory settings updates (e.g., is_in_stock, safety stock, bin_picking_number)
				// Build a map of locationId → List<settings objects> for each SKU at each location
		Map<Integer, List<JSONObject>> locationToSettingsMap = new HashMap<>();

		for (InventoryRecord record : inventoryRecords) {
			SkuVariantInfo info = skuToIdsMap.get(record.getSku());
			if (info == null || record.getLocationId() == null) {
				continue; // skip invalid entries
			}

			// Create a settings object for this SKU
			JSONObject setting = new JSONObject();
			JSONObject identity = new JSONObject();
			identity.put("sku", record.getSku());
			setting.put("identity", identity);
			setting.put("safety_stock", 0);
			setting.put("is_in_stock", true);
			setting.put("warning_level", 0);

			// ✅ Hardcoded bin_picking_number JSON string with "en" and "fr"
			JSONObject binPicking = new JSONObject();
			binPicking.put("en", "aisle 1");
			binPicking.put("fr", "allée 1");
			setting.put("bin_picking_number", binPicking.toString());

			// Add to map grouped by location
			locationToSettingsMap
				.computeIfAbsent(record.getLocationId(), x -> new ArrayList<>())
				.add(setting);
		}
		
		// 8. Send inventory settings updates in smaller batches (e.g., 10 SKUs per request) per location
		
		final int MAX_SKUS_PER_BATCH = 10;

		for (Map.Entry<Integer, List<JSONObject>> entry : locationToSettingsMap.entrySet()) {
		    int locationId = entry.getKey();
		    List<JSONObject> allSettingsList = entry.getValue();

		    for (int i = 0; i < allSettingsList.size(); i += MAX_SKUS_PER_BATCH) {
		        JSONArray batch = new JSONArray();
		        for (int j = i; j < Math.min(i + MAX_SKUS_PER_BATCH, allSettingsList.size()); j++) {
		            batch.put(allSettingsList.get(j));
		        }

		        JSONObject payload = new JSONObject();
		        payload.put("settings", batch);

		        try {
		            bigCommerceRepository.updateInventorySettingsForLocation(locationId, payload);
		            System.out.printf("✅ Settings updated for location %d, SKUs [%d - %d)%n", locationId, i, i + batch.length());
		        } catch (Exception e) {
		            System.err.printf("❌ Failed to update settings for location %d, batch [%d - %d): %s%n",
		                    locationId, i, i + batch.length(), e.getMessage());
		            e.printStackTrace();
		        }
		    }
		}
		
		// 9.  Ensure that inventory tracking is enabled for all affected SKUs
		bigCommerceRepository.activateInventoryTrackingFromSkuMap(skuToIdsMap);

	}

}
