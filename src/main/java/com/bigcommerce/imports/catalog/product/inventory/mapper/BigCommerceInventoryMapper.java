package com.bigcommerce.imports.catalog.product.inventory.mapper;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.bigcommerce.imports.catalog.product.inventory.dto.InventoryRecord;
import com.bigcommerce.imports.catalog.product.inventory.dto.SkuVariantInfo;

public class BigCommerceInventoryMapper {

	
	public static JSONArray buildInventoryAdjustmentPayload(
		    List<InventoryRecord> inventoryRecords,
		    Map<String, SkuVariantInfo> skuToIdsMap,
		    Map<String, Integer> storeLocationMap
		) {
		    JSONArray adjustments = new JSONArray();

		    for (InventoryRecord record : inventoryRecords) {
		        String sku = record.getSku();
		        int storeCode = record.getStoreId();  // e.g., "1", "53"
		        long quantity = record.getAvailableQty();

		        SkuVariantInfo productAndVariant = skuToIdsMap.get(sku);
		        Integer locationId = storeLocationMap.get(String.valueOf(storeCode));
//		        Integer locationId = storeLocationMap.get(storeCode);

		        if (productAndVariant == null) {
		            System.err.println("❌ Variant not found for SKU: " + sku);
		            continue;
		        }
		        if (locationId == null) {
		            System.err.println("❌ Location not found for Store Code: " + storeCode);
		            continue;
		        }

		        int variantId = productAndVariant.getVariantId(); // or getSecond()

		        JSONObject inventoryJson = createBigCommerceInventoryAdjustmentJson(variantId, locationId, quantity);
		        adjustments.put(inventoryJson);
		    }

		    return adjustments;
		}

	private static JSONObject createBigCommerceInventoryAdjustmentJson(int variantId, long location_id , long quantity) {
	    JSONObject inventoryJson = new JSONObject();
	    inventoryJson.put("location_id", location_id);
	    inventoryJson.put("variant_id", variantId);
	    inventoryJson.put("quantity", quantity);
	    return inventoryJson;
	}
}
