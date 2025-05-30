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

		List<String> skus = inventoryRecords.stream().map(InventoryRecord::getSku).filter(Objects::nonNull).distinct()
				.collect(Collectors.toList());

		// 2. Lookup product_id and variant_id in chunks

		Map<String, SkuVariantInfo> skuToIdsMap = new HashMap<>();
		for (int i = 0; i < skus.size(); i += 50) {
			List<String> chunk = skus.subList(i, Math.min(i + 50, skus.size()));
			Map<String, SkuVariantInfo> chunkMap = bigCommerceRepository.getVariantProductAndIdsBySkusUpdated(chunk);
			skuToIdsMap.putAll(chunkMap);
		}

		// 3 get store code location map
		Map<String, Integer> storeLocationMap = bigCommerceRepository.getStoreCodeToLocationIdMap();

		// 3. Construct inventory adjustment payload
		JSONArray inventoryAdjustmentItems = BigCommerceInventoryMapper
				.buildInventoryAdjustmentPayload(inventoryRecords, skuToIdsMap, storeLocationMap);

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

		// activate all products
		bigCommerceRepository.activateInventoryTrackingFromSkuMap(skuToIdsMap);

	}

}
