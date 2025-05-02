package com.bigcommerce.imports.catalog.product.prices.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import com.bigcommerce.imports.catalog.product.prices.dto.VariantPrice;
import com.bigcommerce.imports.catalog.product.repository.BigCommerceRepository;

@Service
public class BigCommerceProductPriceService {

	private final BigCommerceRepository bigCommerceRepository;

	public BigCommerceProductPriceService(BigCommerceRepository bigCommerceRepository) {
		this.bigCommerceRepository = bigCommerceRepository;

	}

	public void updateVariantPrices(List<VariantPrice> variantPrices) {
		try {
			// 1. Extract SKUs
			List<String> skus = variantPrices.stream().map(VariantPrice::getSkuBr).filter(Objects::nonNull).distinct()
					.collect(Collectors.toList());

			// 2. Lookup variant IDs in chunks of 50
			Map<String, Pair<Integer, Integer>> skuToIdsMap = new HashMap<>();
			for (int i = 0; i < skus.size(); i += 50) {
				List<String> chunk = skus.subList(i, Math.min(i + 50, skus.size()));
				Map<String, Pair<Integer, Integer>> chunkMap = bigCommerceRepository.getVariantProductAndIdsBySkus(chunk);
				skuToIdsMap.putAll(chunkMap);
			}

			// 3. Update prices per variant
			for (VariantPrice vp : variantPrices) {
	            Pair<Integer, Integer> ids = skuToIdsMap.get(vp.getSkuBr());

	            if (ids == null) {
	                System.err.printf("❌ SKU not found in BC: %s%n", vp.getSkuBr());
	                continue;
	            }

	            int productId = ids.getLeft();
	            int variantId = ids.getRight();

	            boolean success = bigCommerceRepository.updateVariantPrice(productId, variantId, vp.getListPrice(), vp.getSalePrice());

	            if (success) {
	                System.out.printf("✅ Updated: SKU=%s | ProductID=%d | VariantID=%d | List=%.2f | Sale=%s%n",
	                        vp.getSkuBr(), productId, variantId, vp.getListPrice(),
	                        vp.getSalePrice() != null ? vp.getSalePrice() : "N/A");
	            } else {
	                System.err.printf("❌ Failed update: SKU=%s | ProductID=%d | VariantID=%d%n",
	                        vp.getSkuBr(), productId, variantId);
	            }
	        }

		} catch (Exception e) {
			System.err.println("💥 Exception while updating variant prices: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
