package com.bigcommerce.imports.brand.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.dto.ProductGraphQLResponse.CustomFieldConnection;
import com.constructor.index.dto.ProductGraphQLResponse.CustomFieldEdge;
import com.constructor.index.dto.ProductGraphQLResponse.VariantConnection;
import com.constructor.index.dto.ProductGraphQLResponse.VariantEdge;
import com.constructor.index.dto.ProductGraphQLResponse.Variant;

public class BigCommerceProductSyncService {

	private final BigCommerceGraphQlService graphQLService;

	public BigCommerceProductSyncService(BigCommerceGraphQlService graphQLService) {
		this.graphQLService = graphQLService;
	}

	/**
	 * Fetch all existing products from BigCommerce and build lookup maps: -
	 * productNumber -> productId - skuNumber -> variantId - skuNumber ->
	 * variantMetadata (custom fields)
	 */
	public ProductSyncContext buildProductSyncContext() throws Exception {
		List<ProductGraphQLResponse.Product> allProducts = graphQLService.getAllProducts();
		System.out.println("✅ Fetched " + allProducts.size() + " products.");

		Map<String, Integer> productNumberToIdMap = new HashMap<>();
		Map<String, Integer> skuToVariantIdMap = new HashMap<>();
		Map<String, Map<String, String>> skuToVariantCustomFieldsMap = new HashMap<>();

//        for (ProductGraphQLResponse.Product product : allProducts) {
//            String productNumber = getProductNumberFromCustomFields(product.customFields);
//            if (productNumber != null) {
//                productNumberToIdMap.put(product.sku != null ? product.sku : productNumber, product.entityId);
//            }
//
//            if (product.variants != null && product.variants.getEdges() != null) {
//                for (VariantEdge variantEdge : product.variants.getEdges()) {
//                    Variant variant = variantEdge.getNode();
//                    String sku = variant.getSku();
//                    if (sku != null) {
//                        skuToVariantIdMap.put(sku, variant.getEntityId());
//
//                        Map<String, String> customFieldMap = new HashMap<>();
//                        if (variant.getCustomFields() != null && variant.getCustomFields().getEdges() != null) {
//                            for (CustomFieldEdge edge : variant.getCustomFields().getEdges()) {
//                                ProductGraphQLResponse.Product.CustomField cf = edge.getNode();
//                                customFieldMap.put(cf.getName(), cf.getValue());
//                            }
//                        }
//                        skuToVariantCustomFieldsMap.put(sku, customFieldMap);
//                    }
//                }
//            }
//        }

		return new ProductSyncContext(productNumberToIdMap, skuToVariantIdMap, skuToVariantCustomFieldsMap);
	}

	private String getProductNumberFromCustomFields(CustomFieldConnection customFields) {
		if (customFields == null || customFields.getEdges() == null)
			return null;
//        for (CustomFieldEdge edge : customFields.getEdges()) {
//            ProductGraphQLResponse.Product.CustomField cf = edge.getNode();
//            if ("productNumber".equalsIgnoreCase(cf.getName())) {
//                return cf.getValue();
//            }
//        }
		return null;
	}

	public static class ProductSyncContext {
		public final Map<String, Integer> productNumberToProductId;
		public final Map<String, Integer> skuToVariantId;
		public final Map<String, Map<String, String>> skuToVariantCustomFields;

		public ProductSyncContext(Map<String, Integer> productNumberToProductId, Map<String, Integer> skuToVariantId,
				Map<String, Map<String, String>> skuToVariantCustomFields) {
			this.productNumberToProductId = productNumberToProductId;
			this.skuToVariantId = skuToVariantId;
			this.skuToVariantCustomFields = skuToVariantCustomFields;
		}
	}
}
