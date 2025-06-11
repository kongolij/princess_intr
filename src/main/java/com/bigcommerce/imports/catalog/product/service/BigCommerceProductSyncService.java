package com.bigcommerce.imports.catalog.product.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.CommonConstants;
import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.dto.ProductGraphQLResponse.CustomFieldConnection;
import com.constructor.index.dto.ProductGraphQLResponse.CustomFieldEdge;
import com.constructor.index.dto.ProductGraphQLResponse.MetafieldEdge;
import com.constructor.index.dto.ProductGraphQLResponse.Variant;
import com.constructor.index.dto.ProductGraphQLResponse.VariantEdge;

@Component
public class BigCommerceProductSyncService {

    private final BigCommerceGraphQlService graphQLService;

    public BigCommerceProductSyncService(BigCommerceGraphQlService graphQLService) {
        this.graphQLService = graphQLService;
    }

    /**
     * Fetch all existing products from BigCommerce and build lookup maps:
     * - productNumber -> productId
     * - skuNumber -> variantId
     * - skuNumber -> variantMetadata (custom fields)
     * - productId -> customFieldIds (for later deletion)
     * - productId -> metafieldIds (product metafield deletion)
     * - skuNumber -> metafieldIds (variant metafield deletion)
     * - productId -> productName (for validation/logging)
     * - skuNumber -> hasOptions (true/false)
     */
    public ProductSyncContext buildProductSyncContext() throws Exception {
        List<ProductGraphQLResponse.Product> allProducts = graphQLService.getAllProducts();
        System.out.println("✅ Fetched " + allProducts.size() + " products.");

        Map<String, Integer> productNumberToIdMap = new HashMap<>();
        Map<String, Integer> skuToVariantIdMap = new HashMap<>();
        Map<String, Map<String, String>> skuToVariantCustomFieldsMap = new HashMap<>();
        Map<Integer, List<Integer>> productIdToCustomFieldIds = new HashMap<>();
        Map<Integer, List<Integer>> productIdToMetafieldIds = new HashMap<>();
        Map<String, List<Integer>> variantSkuToMetafieldIds = new HashMap<>();
        Map<Integer, String> productIdToNameMap = new HashMap<>();
        Map<String, Boolean> skuHasOptionsMap = new HashMap<>();

        for (ProductGraphQLResponse.Product product : allProducts) {
            String productNumber = getProductNumberFromCustomFields(product.getCustomFields());
            if (productNumber != null && !productNumber.isBlank()) {
                productNumberToIdMap.put(productNumber, product.getEntityId());
            }

            if (product.getName() != null) {
                productIdToNameMap.put(product.getEntityId(), product.getName());
            }

            // Track custom field IDs for deletion
            if (product.getCustomFields() != null && product.getCustomFields().getEdges() != null) {
                List<Integer> customFieldIds = product.getCustomFields().getEdges().stream()
                        .map(edge -> edge.getNode().getEntityId()).collect(Collectors.toList());
                if (!customFieldIds.isEmpty()) {
                    productIdToCustomFieldIds.put(product.getEntityId(), customFieldIds);
                }
            }

            // Track product-level metafields for deletion
            if (product.getMetafields() != null && product.getMetafields().getEdges() != null) {
                List<Integer> productMetafieldIds = product.getMetafields().getEdges().stream()
                        .map(edge -> edge.getNode().getEntityId()).collect(Collectors.toList());
                if (!productMetafieldIds.isEmpty()) {
                    productIdToMetafieldIds.put(product.getEntityId(), productMetafieldIds);
                }
            }

            // Process variants
            if (product.getVariants() != null && product.getVariants().getEdges() != null) {
                for (VariantEdge variantEdge : product.getVariants().getEdges()) {
                    Variant variant = variantEdge.getNode();
                    String sku = variant.getSku();
                    if (sku != null) {
                        skuToVariantIdMap.put(sku, variant.getEntityId());

                        // ✅ Track whether this variant has options
                        boolean hasOptions = variant.getOptions() != null && 
                                              variant.getOptions().getEdges() != null && 
                                              !variant.getOptions().getEdges().isEmpty();
                        skuHasOptionsMap.put(sku, hasOptions);

                        Map<String, String> customFieldMap = new HashMap<>();
                        List<Integer> metafieldIds = new java.util.ArrayList<>();

                        if (variant.getMetafields() != null && variant.getMetafields().getEdges() != null) {
                            for (MetafieldEdge edge : variant.getMetafields().getEdges()) {
                                ProductGraphQLResponse.Metafield mf = edge.getNode();
                                customFieldMap.put(mf.getKey(), mf.getValue());
                                metafieldIds.add(mf.getEntityId());
                            }
                        }

                        skuToVariantCustomFieldsMap.put(sku, customFieldMap);
                        if (!metafieldIds.isEmpty()) {
                            variantSkuToMetafieldIds.put(sku, metafieldIds);
                        }
                    }
                }
            }
        }

        return new ProductSyncContext(
                productNumberToIdMap,
                skuToVariantIdMap,
                skuToVariantCustomFieldsMap,
                productIdToCustomFieldIds,
                productIdToMetafieldIds,
                variantSkuToMetafieldIds,
                productIdToNameMap,
                skuHasOptionsMap  // ✅ New map!
        );
    }

    private String getProductNumberFromCustomFields(CustomFieldConnection customFields) {
        if (customFields == null || customFields.getEdges() == null)
            return null;
        for (CustomFieldEdge edge : customFields.getEdges()) {
            ProductGraphQLResponse.CustomField cf = edge.getNode();
            if (CommonConstants.External_Product_Number.equalsIgnoreCase(cf.getName())
                    || CommonConstants.NEW_External_Product_Number.equalsIgnoreCase(cf.getName())) {
                return cf.getValue();
            }
        }
        return null;
    }

    public class ProductSyncContext {
        public final Map<String, Integer> productNumberToProductId;
        public final Map<String, Integer> skuToVariantId;
        public final Map<String, Map<String, String>> skuToVariantCustomFields;
        public final Map<Integer, List<Integer>> productIdToCustomFieldIds;
        public final Map<Integer, List<Integer>> productIdToMetafieldIds;
        public final Map<String, List<Integer>> variantSkuToMetafieldIds;
        public final Map<Integer, String> productIdToName;
        public final Map<String, Boolean> skuHasOptionsMap; // ✅ New!

        public ProductSyncContext(
                Map<String, Integer> productNumberToProductId,
                Map<String, Integer> skuToVariantId,
                Map<String, Map<String, String>> skuToVariantCustomFields,
                Map<Integer, List<Integer>> productIdToCustomFieldIds,
                Map<Integer, List<Integer>> productIdToMetafieldIds,
                Map<String, List<Integer>> variantSkuToMetafieldIds,
                Map<Integer, String> productIdToName,
                Map<String, Boolean> skuHasOptionsMap) {
            this.productNumberToProductId = productNumberToProductId;
            this.skuToVariantId = skuToVariantId;
            this.skuToVariantCustomFields = skuToVariantCustomFields;
            this.productIdToCustomFieldIds = productIdToCustomFieldIds;
            this.productIdToMetafieldIds = productIdToMetafieldIds;
            this.variantSkuToMetafieldIds = variantSkuToMetafieldIds;
            this.productIdToName = productIdToName;
            this.skuHasOptionsMap = skuHasOptionsMap;
        }
    }
}
