package com.bigcommerce.imports.catalog.product.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

import com.bigcommerce.imports.catalog.constants.CommonConstants;
import com.bigcommerce.imports.catalog.product.SlugGenerator;
import com.bigcommerce.imports.catalog.product.constant.AttributeLabels;
import com.bigcommerce.imports.catalog.product.dto.Asset;
import com.bigcommerce.imports.catalog.product.dto.Attribute;
import com.bigcommerce.imports.catalog.product.dto.Category;
import com.bigcommerce.imports.catalog.product.dto.DisplayName;
import com.bigcommerce.imports.catalog.product.dto.OptionValue;
import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.ProductCreationResult;
import com.bigcommerce.imports.catalog.product.dto.ProductRefernce;
import com.bigcommerce.imports.catalog.product.dto.Replace;
import com.bigcommerce.imports.catalog.product.dto.Variant;
import com.bigcommerce.imports.catalog.product.repository.BigCommerceRepository;
import com.bigcommerce.imports.catalog.product.service.AttributeLabelService;
import com.bigcommerce.imports.catalog.product.service.BigCommerceProductSyncService.ProductSyncContext;

@SuppressWarnings("unused")
@Component
public class BigCommerceProductMapper {
	
	private final BigCommerceRepository  bigCommerceRepository;
	
	public BigCommerceProductMapper(BigCommerceRepository  bigCommerceRepository) {
		this.bigCommerceRepository=bigCommerceRepository;
	}

	public JSONObject mapProductToBigCommerce(Product product, Map<String, Integer> categoryIds,
			Map<String, Integer> brandIds, String locale) {
		JSONObject productJson = new JSONObject();

		// 1. Set categories
		List<Integer> catIds = product.categories.stream().map(Category::getId) // extract the String id
				.map(categoryIds::get) // map to Integer using the lookup map
				.filter(Objects::nonNull) // filter out any nulls (not found in map)
				.collect(Collectors.toList()); // collect to list
		productJson.put("categories", catIds);

		// 2. Set brand
		if (product.brand != null && brandIds.containsKey(product.brand)) {
			productJson.put("brand_id", brandIds.get(product.brand));
		}

		// 3. Set type: default to physical
		productJson.put("type", "physical");
		productJson.put("is_visible", product.isActive());

		Variant firstVariant = product.variants != null && !product.variants.isEmpty() ? product.variants.get(0) : null;

		if (firstVariant == null) {
			throw new IllegalArgumentException("Product must have at least one variant");
		}

		// weight fallback
		Double weight = firstVariant.getPaWeight();
		if (weight == null || weight <= 0.0) {
			weight = 1.0;
			System.out.println(
					"⚠️ Missing or invalid weight for SKU: " + firstVariant.getSkuNumber() + ". Defaulting to 1.0");
		}

		String name = getLocalizedAttribute(product.attributes, "displayName", locale);
		if (name == null || name.trim().isEmpty()) {
			name = product.getProductNumber(); // fallback
			System.out.println("⚠️ Missing display name in locale '" + locale + "'. Using product number: " + name);
		}

		if (product.variants.size() == 1
				&& (firstVariant.getOption_values() == null || firstVariant.getOption_values().isEmpty())) {
			// Simple product
			productJson.put("name", name);
			productJson.put("is_visible", firstVariant.isActive());
			productJson.put("sku", firstVariant.getSkuNumber());
			productJson.put("weight", weight);
			productJson.put("height", firstVariant.getPaHeight());
			productJson.put("depth", firstVariant.getPaLength());
			productJson.put("width", firstVariant.getPaWidth());
			productJson.put("upc", firstVariant.getPaUPC());
			productJson.put("gtin", firstVariant.getPaVendorNumber());
			productJson.put("mpn", firstVariant.getPaVendorPartNumber());
			productJson.put("price", 0.00); // Required
		} else if (product.variants.size() >= 1) {
			// Configurable product
			productJson.put("name", name);
			productJson.put("is_visible", product.isActive());
			productJson.put("sku", product.getProductNumber());
			productJson.put("weight", weight); // Required
			productJson.put("price", 0.00); // Required

//	        productJson.put("availability",product.getPaAvailabilityCode()  );
//	        Supported values: 
//	        	    available - the product is available for purchase; 
//	        		disabled - the product is listed on the storefront, but cannot be purchased; 
//	        	    preorder - the product is listed for pre-orders.

			productJson.put("variants", mapVariantsToBigCommerce(product.variants));
		}

		System.out.println("📦 Final product JSON:\n" + productJson.toString(2));

		return productJson;
	}

	public JSONObject mapProductToBigCommerce(
	        Product product,
	        Map<String, Integer> categoryIds,
	        Map<String, Integer> brandIds,
	        ProductSyncContext context,
	        Map<Integer, List<ProductRefernce>> unresolvedReferencesMap) {

	    JSONObject productJson = new JSONObject();
	    
	   
	    // 1. Set categories
	    List<Integer> catIds = product.getCategories().stream()
	        .map(Category::getId)
	        .map(categoryIds::get)
	        .filter(Objects::nonNull)
	        .collect(Collectors.toList());
	    
	    productJson.put("categories", catIds);

	    // 2. Set brand if available
	    if (product.getBrand() != null && brandIds.containsKey(product.getBrand())) {
	        productJson.put("brand_id", brandIds.get(product.getBrand()));
	    }

	    // 3️ Handle Related Products (References)
	    // However, BC's field does NOT capture the type of the reference (e.g., "accessory", etc).
	    // Therefore, we also store this data as a custom field (with type info) to maintain a full picture.
	    // ⚠️ This does create some redundancy and should be evaluated for consistency across systems
	    if (product.getReferences() != null && !product.getReferences().isEmpty()) {
	        JSONArray referenceArray = new JSONArray();
	        JSONArray relatedProductIds = new JSONArray();
	        
	        for (ProductRefernce ref : product.getReferences()) {
	            String refProductNumber = ref.getProductNumber();
	            Integer internalId = context.productNumberToProductId.get(refProductNumber);

	            if (internalId != null) {
	            	
	                // ✅ Resolved reference – include in product JSON
	                JSONObject refJson = new JSONObject();
	                refJson.put("product_id", internalId);
	                refJson.put("type", ref.getType());
	                referenceArray.put(refJson);
	                relatedProductIds.put(internalId);
	            } else {
	                // ❌ Unresolved reference – track for retry after all products are created
	                System.out.printf(
	                        "⚠️ Unresolved reference: product '%s' references '%s' (type: %s) which is not yet created.%n",
	                        product.getProductNumber(),
	                        refProductNumber,
	                        ref.getType()
	                );
	            }
	        }

	        if (!referenceArray.isEmpty()) {
	            productJson.put("related_products", relatedProductIds);
	        }
	    }
	    
	    // 4. Handle replacements (delete if previously mapped)
	    // Typically, "replacements" are only introduced when reorganizing existing products 
	    // into new products. 
	    // We do not expect to handle replacements for existing BigCommerce products during an update;
	    // this logic is primarily for new products in a reorganization scenario.
	    if (product.getReplaces() != null && !product.getReplaces().isEmpty()) {
	        List<Integer> idsToDelete = new ArrayList<>();

	        for (Replace rep : product.getReplaces()) {
	            if (rep == null || rep.getProductNumber() == null) {
	                continue;
	            }

	            String oldProductNumber = rep.getProductNumber();
	            Integer internalId = context.productNumberToProductId.get(oldProductNumber);

	            if (internalId != null) {
	                idsToDelete.add(internalId);
	                System.out.printf("🗑️ Product %s is marked to replace %s (ID: %d)%n",
	                    product.getProductNumber(), oldProductNumber, internalId);
	            } else {
	                System.out.printf("⚠️ Could not resolve replacement product %s for deletion%n", oldProductNumber);
	            }
	        }

	        if (!idsToDelete.isEmpty()) {
	            System.out.printf("🚨 Attempting to delete %d product(s) replaced by product %s: %s%n",
	                idsToDelete.size(),
	                product.getProductNumber(),
	                idsToDelete);

	            try {
	                bigCommerceRepository.deleteProductsInBatch(idsToDelete);
	                System.out.printf("✅ Successfully deleted %d replaced product(s) for product %s%n",
	                    idsToDelete.size(), product.getProductNumber());
	            } catch (Exception e) {
	                System.err.printf("❌ Failed to delete replaced products for %s. Error: %s%n",
	                    product.getProductNumber(), e.getMessage());
	                e.printStackTrace();
	            }
	        } else {
	            System.out.printf("ℹ️ No deletions needed for product %s — no resolved replacements.%n",
	                product.getProductNumber());
	        }
	    }

	    // 5. Core fields
	    productJson.put("type", "physical");
	    productJson.put("is_visible", product.isActive());

	    String name = Optional.ofNullable(product.getDisplayName())
                .map(DisplayName::getEn)
                .filter(StringUtils::hasText)
                .orElse(product.getProductNumber());
	    
//	    String name = getLocalizedAttribute(product.getAttributes(), "displayName", "en");
	    if (name == null || name.trim().isEmpty()) {
	        name = product.getProductNumber();
	    }
	    productJson.put("name", name);

	    Variant firstVariant = product.getVariants() != null && !product.getVariants().isEmpty()
	        ? product.getVariants().get(0)
	        : null;

	    if (firstVariant == null) {
	        throw new IllegalArgumentException("Product must have at least one variant");
	    }

	    Double weight = Optional.ofNullable(firstVariant.getPaWeight()).filter(w -> w > 0).orElse(1.0);

	    if (product.getVariants().size() == 1 &&
	        (firstVariant.getOption_values() == null || firstVariant.getOption_values().isEmpty())) {

	        // Simple product
	        productJson.put("is_visible", firstVariant.isActive());
	        productJson.put("sku", firstVariant.getSkuNumber());
	        productJson.put("weight", weight);
	        productJson.put("height", firstVariant.getPaHeight());
	        productJson.put("depth", firstVariant.getPaLength());
	        productJson.put("width", firstVariant.getPaWidth());
	        productJson.put("upc", firstVariant.getPaUPC());
	        productJson.put("gtin", firstVariant.getPaVendorNumber());
	        productJson.put("mpn", firstVariant.getPaVendorPartNumber());
	        productJson.put("price", 0.00);

	    } else {
	        // Configurable product
	        productJson.put("is_visible", product.isActive());
	        productJson.put("sku", product.getProductNumber());
	        productJson.put("weight", weight);
	        productJson.put("price", 0.00);

	        productJson.put("variants", mapVariantsToBigCommerce(product.getVariants()));
	    }

	    System.out.println("📦 Final product JSON for new product:\n" + productJson.toString(2));
	    return productJson;
	}

	/**
	 * Maps a Product object to a BigCommerce-compatible JSON object for product updates.
	 * This includes basic fields, brand and category IDs, and related product references.
	 * Note: "related_products" Carefully review whether these should be included as we will have them peristed as custom filed as well.
	 */
	public JSONObject mapProductToUpdateJson(
		    Map<Integer, String> productIdToName,
		    Product product,
		    Map<String, Integer> categoryIds,
		    Map<String, Integer> brandIds,
		    ProductSyncContext context,
		    int currentProductId
		) {
		    JSONObject productJson = new JSONObject();

		    // 1️ Assign Categories
		    List<Integer> catIds = product.getCategories().stream()
		        .map(Category::getId)
		        .map(categoryIds::get)
		        .filter(Objects::nonNull)
		        .collect(Collectors.toList());

		    if (!catIds.isEmpty()) {
		        productJson.put("categories", catIds);
		    }

		    // 2️ Assign Brand
		    if (product.getBrand() != null && brandIds.containsKey(product.getBrand())) {
		        productJson.put("brand_id", brandIds.get(product.getBrand()));
		    }else {
		    	System.out.printf(
		    	        "⚠️ Brand not found in brandIds map for product '%s'. Brand name: '%s'%n",
		    	        product.getProductNumber(),
		    	        product.getBrand()
		    	    );
		    }

		    // 3️ Handle Related Products (References)
		    // However, BC's field does NOT capture the type of the reference (e.g., "accessory", etc).
		    // Therefore, we also store this data as a custom field (with type info) to maintain a full picture.
		    // ⚠️ This does create some redundancy and should be evaluated for consistency across systems
		    if (product.getReferences() != null && !product.getReferences().isEmpty()) {
		        JSONArray referenceArray = new JSONArray();
		        JSONArray relatedProductIds = new JSONArray();
		        
		        for (ProductRefernce ref : product.getReferences()) {
		            String refProductNumber = ref.getProductNumber();
		            Integer internalId = context.productNumberToProductId.get(refProductNumber);

		            if (internalId != null) {
		            	
		                // ✅ Resolved reference – include in product JSON
		                JSONObject refJson = new JSONObject();
		                refJson.put("product_id", internalId);
		                refJson.put("type", ref.getType());
		                referenceArray.put(refJson);
		                relatedProductIds.put(internalId);
		            } else {
		                // ❌ Unresolved reference – track for retry after all products are created
		                System.out.printf(
		                        "⚠️ Unresolved reference: product '%s' references '%s' (type: %s) which is not yet created.%n",
		                        product.getProductNumber(),
		                        refProductNumber,
		                        ref.getType()
		                );
		            }
		        }

		        if (!referenceArray.isEmpty()) {
		            productJson.put("related_products", relatedProductIds);
		        }
		    }


		    //  4️ Core Product Fields
		    productJson.put("type", "physical");
		    productJson.put("is_visible", product.isActive());

		    String name = Optional.ofNullable(product.getDisplayName())
                    .map(DisplayName::getEn)
                    .filter(StringUtils::hasText)
                    .orElse(product.getProductNumber());
		    
		    // Only set name if it's not already present in the BC system
//		    if (!productIdToName.containsValue(name)) {
		        productJson.put("name", name);
//		    }

		   
		    // 5️ Variant Defaults
		    // If the product has a single simple variant (no options), promote variant fields to product level.   
		    Variant firstVariant = product.getVariants() != null && !product.getVariants().isEmpty()
		        ? product.getVariants().get(0)
		        : null;

		    if (firstVariant != null) {
		    	// Use variant weight if valid, else default to 1.0
		        Double weight = Optional.ofNullable(firstVariant.getPaWeight()).filter(w -> w > 0).orElse(1.0);
		        productJson.put("weight", weight);
		        productJson.put("price", 0.00); // required
		        
		       
		        
		        boolean incomingIsSimple = product.getVariants().size() == 1 &&
		                (firstVariant.getOption_values() == null || firstVariant.getOption_values().isEmpty());

		        // 🚀 Check if existing product has variants (using context)
		        boolean existingHasVariants = context.skuToVariantId.entrySet().stream()
		                .anyMatch(e -> e.getValue() != null && e.getValue() > 0);
		            
		        if (incomingIsSimple) {
		        	// Promote SKU-level fields to product level
		            productJson.put("sku", firstVariant.getSkuNumber());
		            putIfNotNull(productJson, "upc", firstVariant.getPaUPC());
		            putIfNotNull(productJson, "gtin", firstVariant.getPaVendorNumber());
		            putIfNotNull(productJson, "mpn", firstVariant.getPaVendorPartNumber());
		            putIfNotNull(productJson, "width", firstVariant.getPaWidth());
		            putIfNotNull(productJson, "height", firstVariant.getPaHeight());
		            putIfNotNull(productJson, "depth", firstVariant.getPaLength());
		        } else {
		            productJson.put("sku", product.getProductNumber());
		        }
		    }

		    // 6️ Final Logging
		    System.out.println("🔄 Product update payload:\n" + productJson.toString(2));
		    return productJson;
		}

	
	public JSONObject mapProductToUpdateJson(Map<Integer, String> productIdToName, Product product,
			Map<String, Integer> categoryIds, Map<String, Integer> brandIds) {
		JSONObject productJson = new JSONObject();

        // 1. Set categories (filter out any unresolved ones)
		List<Integer> catIds = product.getCategories().stream()
				.map(Category::getId)
				.map(categoryIds::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (!catIds.isEmpty()) {
			productJson.put("categories", catIds);
		}

        // 2. Set brand if available
		if (product.getBrand() != null && brandIds.containsKey(product.getBrand())) {
			productJson.put("brand_id", brandIds.get(product.getBrand()));
		}

		

		
        // 3. Set core fields
		productJson.put("type", "physical");
		productJson.put("is_visible", product.isActive());

		String name = getLocalizedAttribute(product.getAttributes(), "displayName", "en");
		if (name == null || name.trim().isEmpty()) {
			name = product.getProductNumber(); // fallback
		}

        // Avoid setting name if it's identical to current name
		// ✅ Avoid setting name if it's already used by any product
		if (!productIdToName.containsValue(name)) {
		    productJson.put("name", name);
		}
		

		Variant firstVariant = product.getVariants() != null && !product.getVariants().isEmpty()
				? product.getVariants().get(0)
				: null;

		if (firstVariant != null) {
			Double weight = Optional.ofNullable(firstVariant.getPaWeight()).filter(w -> w > 0).orElse(1.0);
			productJson.put("weight", weight);
			productJson.put("price", 0.00); // required

			boolean isSimple = product.getVariants().size() == 1
					&& (firstVariant.getOption_values() == null || firstVariant.getOption_values().isEmpty());

			if (isSimple) {
				productJson.put("sku", firstVariant.getSkuNumber());
				putIfNotNull(productJson, "upc", firstVariant.getPaUPC());
				putIfNotNull(productJson, "gtin", firstVariant.getPaVendorNumber());
				putIfNotNull(productJson, "mpn", firstVariant.getPaVendorPartNumber());
				putIfNotNull(productJson, "width", firstVariant.getPaWidth());
				putIfNotNull(productJson, "height", firstVariant.getPaHeight());
				putIfNotNull(productJson, "depth", firstVariant.getPaLength());
			} else {
				productJson.put("sku", product.getProductNumber());
			}
		}

		System.out.println("🔄 Product update payload:\n" + productJson.toString(2));
		return productJson;
	}

	public JSONArray mapProductToBigCommerceCustomAttr(Product product,
			Map<String, Map<String, String>> attribtueLabelMap) {

		Variant firstVariant = product.variants != null && !product.variants.isEmpty() ? product.variants.get(0) : null;
		if (product.variants.size() == 1
				&& (firstVariant.getOption_values() == null || firstVariant.getOption_values().isEmpty())) {
			List<Attribute> allAttrs = firstVariant.getAttributes();
			List<Attribute> filteredAttrs = product.getAttributes();
			List<Attribute> mergedAttrs = Stream.concat(allAttrs.stream(), filteredAttrs.stream())
					.collect(Collectors.toList());
			return mapProductAttributesToCustomFieldsRich(mergedAttrs, attribtueLabelMap);

		} else if (product.variants.size() >= 1) {

			JSONArray customFields = mapProductAttributesToCustomFieldsRich(product.attributes, attribtueLabelMap);

			// Add flat fields (Levy, Clearance, etc.)

			Map<String, Object> flatFields = Map.of("paLevy", product.isPaLevy(), "paAvailabilityCode",
					product.getPaAvailabilityCode(), "paProductClearance", product.isPaProductClearance(),
					"paProductStatus", product.getPaProductStatus(), "externalProductNumber",
					product.getProductNumber());

			for (Map.Entry<String, Object> entry : flatFields.entrySet()) {
				if (entry.getValue() != null) {
					JSONObject field = new JSONObject();
					field.put("name", entry.getKey());
					field.put("value", entry.getValue());
					customFields.put(field);
				}
			}

			return customFields;

		}
		return null;

	}

	  
	/**
	 * Maps predefined product attributes (like levy, availability, clearance, status, etc.)
	 * and **resolved references** to custom fields.
	 *
	 * If some references are not yet resolved (not created), they will be tracked in unresolvedReferencesMap.
	 *
	 * @param product The product to map.
	 * @param productNumberToProductId Mapping of product numbers to their BigCommerce product IDs.
	 * @param unresolvedReferencesMap  Where to track unresolved references for later retry.
	 * @param currentProductId         The current product's BigCommerce ID.
	 * @return JSONArray of custom fields for this product.
	 */
	public static JSONArray mapPredefinedProductAttributesToCustomFields(
	        Product product,
	        Map<String, Integer> productNumberToProductId, 
	        int currentProductId) {

	    JSONArray customFields = new JSONArray();

  	    
	    // 🔎  Build the flatFields map — skipping empty or null values
	    Map<String, Object> flatFields = new HashMap<>();

	    // Always include boolean fields (since they’re primitive)
	    flatFields.put("levy", String.valueOf(product.isPaLevy()));
	    flatFields.put("productClearance", String.valueOf(product.isPaProductClearance()));
	   
	    flatFields.put("productClearance", String.valueOf(product.isPaProductClearance()));
	     
	     // Conditionally add string fields if they’re not empty
	     if (StringUtils.hasText(product.getPaAvailabilityCode())) {
	         flatFields.put("availabilityCode", product.getPaAvailabilityCode());
	     }

	     if (StringUtils.hasText(product.getPaProductStatus())) {
	        flatFields.put("productStatus", product.getPaProductStatus());
	     }

	     if ( product.getVariants().size()==1 && 
	    		 ( product.getVariants().get(0).getOption_values() == null || product.getVariants().get(0).getOption_values().isEmpty())) {
	    	 flatFields.put("shippable", product.getVariants().get(0).getPaShippable());
	    	 System.out.println(" cccccc found a single variant shippabel " +  product.getVariants().get(0).getPaShippable() );
	    	
	     }
	    
	     
	     
	     if (StringUtils.hasText(product.getProductNumber())) {
	         flatFields.put(CommonConstants.NEW_External_Product_Number, product.getProductNumber());
	     }
	     
	     String frenchName = product.getDisplayName().getFr_CA();
	     if (StringUtils.hasText(frenchName)) {
	    	    String slugFr = SlugGenerator.generateSlug(frenchName);
	    	    flatFields.put("slug_fr", slugFr);
	     }
	   


	    for (Map.Entry<String, Object> entry : flatFields.entrySet()) {
	        Object value = entry.getValue();
	        if (value != null) {
	            String stringValue = String.valueOf(value).trim();
	            if (!stringValue.isEmpty()) { // Skip empty string values
	                JSONObject field = new JSONObject();
	                field.put("name", entry.getKey());
	                field.put("value", stringValue);
	                customFields.put(field);
	            }
	        }
	    }

	    return customFields;
	}


	
	public static JSONArray mapPredefinedProductAttributesToCustomFields (Product product) {
		JSONArray customFields = new JSONArray();

		Map<String, Object> flatFields = Map.of(
				"levy", String.valueOf(product.isPaLevy()), 
				"availabilityCode",product.getPaAvailabilityCode(), 
				"productClearance", String.valueOf(product.isPaProductClearance()),
				"productStatus", product.getPaProductStatus(), 
				CommonConstants.NEW_External_Product_Number,product.getProductNumber());

		for (Map.Entry<String, Object> entry : flatFields.entrySet()) {
			Object value = entry.getValue();
			if (value != null) {
				String stringValue = String.valueOf(value).trim();
				if (!stringValue.isEmpty()) { // Skip empty string values
					JSONObject field = new JSONObject();
					field.put("name", entry.getKey());
					field.put("value", stringValue);
					customFields.put(field);
				}
			}
		}

		return customFields;
	}

	/**
	 * Maps a Product object to a set of JSON metafields to be stored to BC.
	 * Each metafield captures either:
	 *  - Explicit localized product attributes (flattened as individual fields)
	 *  - Dynamically discovered product attributes grouped by locale
	 *  - Related product references (if resolved)
	 *
	 * @param product The product to map
	 * @param productId The internal ID of the product
	 * @param productNumberToProductId A lookup for resolving product references to internal IDs
	 * @param attributeLabelMap Localized attribute labels (en/fr)
	 * @param unresolvedReferencesMap To track references that couldn’t be resolved yet
	 * @return A JSONArray of metafields
	 */
	public static JSONArray mapProductToMetafields(
	    Product product,
	    int productId,
	    Map<String, Integer> productNumberToProductId,
	    Map<String, Map<String, String>> attributeLabelMap,
	    Map<Integer, List<ProductRefernce>> unresolvedReferencesMap) {

	    JSONArray metafields = new JSONArray();

	    
	    // 🟩 1️ Explicit localized fields (flattened individually for easier querying in downstream systems)
	    Map<String, String> flatFields = new HashMap<>();
	    
//	    Map<String, JSONObject> flatFields = new HashMap<>();
////	    
//	    flatFields.put("displayName",
//	    	    createLocalizedValue.apply(
//	    	        product.getDisplayName() != null && StringUtils.hasText(product.getDisplayName().getEn()) 
//	    	            ? product.getDisplayName().getEn() : "",
//	    	        product.getDisplayName() != null && StringUtils.hasText(product.getDisplayName().getFr_CA()) 
//	    	            ? product.getDisplayName().getFr_CA() : ""
//	    	    )
//	    	);
////
//	    	flatFields.put("longDescription",
//	    	    createLocalizedValue.apply(
//	    	        product.getLongDescription() != null && StringUtils.hasText(product.getLongDescription().getEn()) 
//	    	            ? product.getLongDescription().getEn() : "",
//	    	        product.getLongDescription() != null && StringUtils.hasText(product.getLongDescription().getFr_CA()) 
//	    	            ? product.getLongDescription().getFr_CA() : ""
//	    	    )
//	    	);
////
//	    	flatFields.put("sharedApplications",
//	    	    createLocalizedValue.apply(
//	    	        product.getPaSharedApplications() != null && StringUtils.hasText(product.getPaSharedApplications().getEn()) 
//	    	            ? product.getPaSharedApplications().getEn() : "",
//	    	        product.getPaSharedApplications() != null && StringUtils.hasText(product.getPaSharedApplications().getFr_CA()) 
//	    	            ? product.getPaSharedApplications().getFr_CA() : ""
//	    	    )
//	    	);
////
//	    	flatFields.put("sharedFeatures",
//	    	    createLocalizedValue.apply(
//	    	        product.getPaSharedFeatures() != null && StringUtils.hasText(product.getPaSharedFeatures().getEn()) 
//	    	            ? product.getPaSharedFeatures().getEn() : "",
//	    	        product.getPaSharedFeatures() != null && StringUtils.hasText(product.getPaSharedFeatures().getFr_CA()) 
//	    	            ? product.getPaSharedFeatures().getFr_CA() : ""
//	    	    )
//	    	);
	    	
	    if (product.getDisplayName() != null && StringUtils.hasText(product.getDisplayName().getEn())) {
	        flatFields.put("displayName_en", product.getDisplayName().getEn());
	    }
	    if (product.getDisplayName() != null && StringUtils.hasText(product.getDisplayName().getFr_CA())) {
	        flatFields.put("displayName_fr", product.getDisplayName().getFr_CA());
	    }

	    if (product.getLongDescription() != null && StringUtils.hasText(product.getLongDescription().getEn())) {
	        flatFields.put("longDescription_en", product.getLongDescription().getEn());
	    }
	    if (product.getLongDescription() != null && StringUtils.hasText(product.getLongDescription().getFr_CA())) {
	        flatFields.put("longDescription_fr", product.getLongDescription().getFr_CA());
	    }

	    if (product.getPaSharedApplications() != null && StringUtils.hasText(product.getPaSharedApplications().getEn())) {
	        flatFields.put("sharedApplications_en", product.getPaSharedApplications().getEn());
	    }
	    if (product.getPaSharedApplications() != null && StringUtils.hasText(product.getPaSharedApplications().getFr_CA())) {
	        flatFields.put("sharedApplications_fr", product.getPaSharedApplications().getFr_CA());
	    }

	    if (product.getPaSharedFeatures() != null && StringUtils.hasText(product.getPaSharedFeatures().getEn())) {
	        flatFields.put("sharedFeatures_en", product.getPaSharedFeatures().getEn());
	    }
	    if (product.getPaSharedFeatures() != null && StringUtils.hasText(product.getPaSharedFeatures().getFr_CA())) {
	        flatFields.put("sharedFeatures_fr", product.getPaSharedFeatures().getFr_CA());
	    }


	    // 🔄 2️ Add explicit fields as individual metafields (one per locale + attribute)
	    	
	    	// 🔥 Create metafields from the map (each value is JSON!)
//	    	for (Map.Entry<String, JSONObject> entry : flatFields.entrySet()) {
//	    	    JSONObject field = new JSONObject();
//	    	    field.put("namespace", "product_attributes");
//	    	    field.put("key", entry.getKey());
//	    	    field.put("value", entry.getValue().toString());  // store as JSON string
//	    	    field.put("permission_set", "read_and_sf_access");
//	    	    field.put("description", "Localized product attribute: " + entry.getKey());
//	    	    field.put("resource_id", productId);
//
//	    	    metafields.put(field);
//	    	} 	
	    for (Map.Entry<String, String> entry : flatFields.entrySet()) {
	        JSONObject field = new JSONObject();
	        field.put("namespace", "product_attributes");
	        field.put("key", entry.getKey());
	        field.put("value", entry.getValue());
	        field.put("permission_set", "read_and_sf_access");
	        field.put("description", "Flattened localized product attribute: " + entry.getKey());
	        field.put("resource_id", productId);
	        metafields.put(field);
	    }

	    // 🌍 3️ Dynamic attributes (attributes array) grouped by locale (en/fr)
	    JSONArray attributesEn = new JSONArray();
	    JSONArray attributesFr = new JSONArray();

	    // ✅ Sort attributes by sequence (if present)
	    List<Attribute> sortedAttributes = product.getAttributes().stream()
	        .filter(attr -> attr.getId() != null && !attr.getId().isEmpty())
	        .sorted(Comparator.comparingInt(attr -> {
	            try {
	                return Integer.parseInt(attr.getSeq() != null ? attr.getSeq() : "9999");
	            } catch (NumberFormatException e) {
	                return 9999; // fallback if no sequence
	            }
	        }))
	        .toList();

	    for (Attribute attr : sortedAttributes) {
	        String id = attr.getId();
	        String enValue = attr.en != null ? attr.en.trim() : "";
	        String frValue = attr.fr_CA != null ? attr.fr_CA.trim() : "";

	        boolean hasEn = !enValue.isEmpty();
	        boolean hasFr = !frValue.isEmpty();

	        // Skip attributes with no localized data
	        if (!hasEn && !hasFr) continue;

	        // ⚙️ Remove "pa" prefix for keys (for cleaner downstream use)
	        String keyBase = id.startsWith("pa") ? id.substring(2) : id;

	        // 🏷️ Localized label resolution
	        if (hasEn) {
	            String label = attributeLabelMap.getOrDefault("en", Map.of()).getOrDefault(id, id);
	            JSONObject entry = new JSONObject();
	            entry.put("label", label);
	            entry.put("value", enValue);
	            entry.put("seq", parseSeq(attr.getSeq()));
	            attributesEn.put(entry);
	        }
	        if (hasFr) {
	            String label = attributeLabelMap.getOrDefault("fr", Map.of()).getOrDefault(id, id);
	            JSONObject entry = new JSONObject();
	            entry.put("label", label);
	            entry.put("value", frValue);
	            entry.put("seq", parseSeq(attr.getSeq()));
	            attributesFr.put(entry);
	        }
	    }

	    // 📦 4️ Add dynamic English attributes as a single metafield (structured JSON array as value)
	    if (!attributesEn.isEmpty()) {
	        JSONObject field = new JSONObject();
	        field.put("namespace", "product_attributes");
	        field.put("key", "product_attributes_en");
	        field.put("value", attributesEn.toString());
	        field.put("permission_set", "read_and_sf_access");
	        field.put("description", "Dynamic English product attributes");
	        field.put("resource_id", productId);
	        metafields.put(field);
	    }

	    // 📦 5️ Add dynamic French attributes
	    if (!attributesFr.isEmpty()) {
	        JSONObject field = new JSONObject();
	        field.put("namespace", "product_attributes");
	        field.put("key", "product_attributes_fr");
	        field.put("value", attributesFr.toString());
	        field.put("permission_set", "read_and_sf_access");
	        field.put("description", "Dynamic French product attributes");
	        field.put("resource_id", productId);
	        metafields.put(field);
	    }

	    // 🔗 6️ Add related product references (if resolved)
	    if (product.getReferences() != null && !product.getReferences().isEmpty()) {
	        JSONArray referenceArray = new JSONArray();

	        for (ProductRefernce ref : product.getReferences()) {
	            String refProductNumber = ref.getProductNumber();
	            Integer internalId = productNumberToProductId.get(refProductNumber);

	            if (internalId != null) {
	                // ✅ Reference resolved – add as a JSON object
	                JSONObject refJson = new JSONObject();
	                refJson.put("product_id", internalId);
	                refJson.put("type", ref.getType());
	                referenceArray.put(refJson);
	            } else {
	                // ❌ Unresolved – track it for later retry
	                unresolvedReferencesMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(ref);
	                System.out.printf(
	                    "⚠️ Unresolved reference: product '%s' references '%s' (type: %s) which is not yet created.%n",
	                    product.getProductNumber(), refProductNumber, ref.getType());
	            }
	        }

	        // 🟢 Add resolved references as a separate metafield
	        if (!referenceArray.isEmpty()) {
	            JSONObject relatedProductsField = new JSONObject();
	            relatedProductsField.put("namespace", "product_attributes");
	            relatedProductsField.put("key", "related_products");
	            relatedProductsField.put("value", referenceArray.toString());
	            relatedProductsField.put("permission_set", "read_and_sf_access");
	            relatedProductsField.put("description", "Related product references");
	            relatedProductsField.put("resource_id", productId);
	            metafields.put(relatedProductsField);
	        }
	    }

	    return metafields;
	}


	public Map<String, Map<String, JSONObject>> mapVariantAttribtuesToBigCommerceCustomAttr(Product product,
			Map<String, Map<String, String>> attributeLabelMap) {

		Map<String, Map<String, JSONObject>> localeSkuMap = new HashMap<>();

		Map<String, JSONObject> enMap = new HashMap<>();
		Map<String, JSONObject> frMap = new HashMap<>();

		for (Variant variant : product.getVariants()) {
			String sku = variant.getSkuNumber();
			if (sku == null || sku.isEmpty())
				continue;

			JSONObject enJson = new JSONObject();
			JSONObject frJson = new JSONObject();

			// Step 1: Capture specific known fields
			if (variant.getPaCountryOfOrigin() != null && !variant.getPaCountryOfOrigin().isEmpty()) {
				enJson.put("paCountryOfOrigin", variant.getPaCountryOfOrigin());
				frJson.put("paCountryOfOrigin", variant.getPaCountryOfOrigin());
			}

			if (variant.getPaUPC() != null && !variant.getPaUPC().isEmpty()) {
				enJson.put("paUPC", variant.getPaUPC());
				frJson.put("paUPC", variant.getPaUPC());
			}

			// Step 2: Add dynamic attributes
			if (variant.getAttributes() != null) {
				for (Attribute attr : variant.getAttributes()) {
					String attrId = attr.getId();
					String enVal = attr.getEn();
					String frVal = attr.getFr_CA();

					if (enVal != null && !enVal.trim().isEmpty()) {
						enJson.put(attrId, enVal.trim());
					}
					if (frVal != null && !frVal.trim().isEmpty()) {
						frJson.put(attrId, frVal.trim());
					}
				}
			}

			enMap.put(sku, enJson);
			frMap.put(sku, frJson);
		}

		localeSkuMap.put("en", enMap);
		localeSkuMap.put("fr_CA", frMap);

		return localeSkuMap;
	}

	public Map<String, Map<String, JSONObject>> buildLocalizedVariantAttributeMap(Product product,
			Map<String, Map<String, String>> attribtueLabelMap) {

		Map<String, Map<String, JSONObject>> localeSkuMap = new HashMap<>();
		Map<String, Map<String, String>> localeLabelMap = new HashMap<>();

		Map<String, JSONObject> enMap = mapAttributesToLocale(product.getVariants(), "en");
		Map<String, JSONObject> frMap = mapAttributesToLocale(product.getVariants(), "fr_CA");

		localeSkuMap.put("en", enMap);
		localeSkuMap.put("fr_CA", frMap);

		return localeSkuMap;
	}
	
	
	/**
	 * Maps variant-level attributes to JSON metafields, ready for further processing.
	 */
	public Map<String, JSONObject> mapPredefinedVariantAttributesToMetafields(Product product) {
	    Map<String, JSONObject> result = new HashMap<>();

	    if (product == null || product.getVariants() == null) {
	        return result;
	    }

	    for (Variant variant : product.getVariants()) {
	        String sku = variant.getSkuNumber();
	        if (sku == null || sku.isEmpty()) continue;

	        JSONObject predefinedAttributes = new JSONObject();

	        // Basic variant fields
	        predefinedAttributes.put("skuSeq", variant.getSkuSeq());
	        predefinedAttributes.put("clearance", String.valueOf(variant.isPaSkuClearance()));
	        predefinedAttributes.put("availabilityCode", variant.getPaAvailabilityCode() != null ? variant.getPaAvailabilityCode() : "");
	        predefinedAttributes.put("shippable", variant.getPaShippable());
	        predefinedAttributes.put("skuStatus", variant.getPaSkuStatus());
	        predefinedAttributes.put("arrivalDate", variant.getPaArrivalDate());
	        predefinedAttributes.put("country_of_origin", variant.getPaCountryOfOrigin());

	        // ✅ Videos (only if at least one has a valid URL)
	        if (variant.getVideos() != null) {
	            JSONObject videosJson = new JSONObject();
	            boolean hasValidVideo = false;

	            if (variant.getVideos().getVideos_en() != null && !variant.getVideos().getVideos_en().isEmpty()) {
	                JSONArray enVideos = new JSONArray(variant.getVideos().getVideos_en());
	                videosJson.put("videos_en", enVideos);
	                hasValidVideo |= enVideos.toList().stream()
	                        .anyMatch(v -> {
	                            Object url = ((Map<?, ?>) v).get("url");
	                            return url != null && !((String) url).isBlank();
	                        });
	            }

	            if (variant.getVideos().getVideos_fr() != null && !variant.getVideos().getVideos_fr().isEmpty()) {
	                JSONArray frVideos = new JSONArray(variant.getVideos().getVideos_fr());
	                videosJson.put("videos_fr", frVideos);
	                hasValidVideo |= frVideos.toList().stream()
	                        .anyMatch(v -> {
	                            Object url = ((Map<?, ?>) v).get("url");
	                            return url != null && !((String) url).isBlank();
	                        });
	            }

	            if (hasValidVideo) {
	                predefinedAttributes.put("videos", videosJson);
	            }
	        }

	        // ✅ Web links (only if at least one URL is not blank)
	        if (variant.getWebLinks() != null) {
	            String urlEn = variant.getWebLinks().getUrl_en();
	            String urlFr = variant.getWebLinks().getUrl_fr();
	            if ((urlEn != null && !urlEn.isBlank()) || (urlFr != null && !urlFr.isBlank())) {
	                JSONObject linksJson = new JSONObject();
	                linksJson.put("url_en", urlEn != null ? urlEn : "");
	                linksJson.put("url_fr", urlFr != null ? urlFr : "");
	                predefinedAttributes.put("webLinks", linksJson);
	            }
	        }

	        // ✅ Manual PDF assets
	        if (variant.getAssets() != null) {
	            JSONArray manuals = new JSONArray();
	            variant.getAssets().stream()
	                    .filter(asset -> "document".equalsIgnoreCase(asset.getType()))
	                    .forEach(asset -> {
	                        JSONObject manualJson = new JSONObject();
	                        manualJson.put("filename", asset.getPaDocumentsFileName());
	                        manualJson.put("label", asset.getDocumentLabel() != null ? asset.getDocumentLabel() : "Manual PDF");
	                        manualJson.put("description", asset.getDescription() != null ? asset.getDescription() : "PDF Document");
	                        manuals.put(manualJson);
	                    });

	            if (!manuals.isEmpty()) {
	                predefinedAttributes.put("manual_pdfs", manuals); // 🔑 using plural
	            }
	        }

	        // ✅ Direct localized fields (English and French)
	        if (variant.getDisplayName() != null) {
	            String enValue = variant.getDisplayName().getEn();
	            String frValue = variant.getDisplayName().getFr_CA();
	            if (StringUtils.hasText(enValue)) {
	                predefinedAttributes.put("displayName_en", enValue);
	            }
	            if (StringUtils.hasText(frValue)) {
	                predefinedAttributes.put("displayName_fr", frValue);
	            }
	        }

	        if (variant.getLongDescription() != null) {
	            String enValue = variant.getLongDescription().getEn();
	            String frValue = variant.getLongDescription().getFr_CA();
	            if (StringUtils.hasText(enValue)) {
	                predefinedAttributes.put("longDescription_en", enValue);
	            }
	            if (StringUtils.hasText(frValue)) {
	                predefinedAttributes.put("longDescription_fr", frValue);
	            }
	        }

	        if (variant.getPaSharedApplications() != null) {
	            String enValue = variant.getPaSharedApplications().getEn();
	            String frValue = variant.getPaSharedApplications().getFr_CA();
	            if (StringUtils.hasText(enValue)) {
	                predefinedAttributes.put("sharedApplications_en", enValue);
	            }
	            if (StringUtils.hasText(frValue)) {
	                predefinedAttributes.put("sharedApplications_fr", frValue);
	            }
	        }

	        if (variant.getPaSharedFeatures() != null) {
	            String enValue = variant.getPaSharedFeatures().getEn();
	            String frValue = variant.getPaSharedFeatures().getFr_CA();
	            if (StringUtils.hasText(enValue)) {
	                predefinedAttributes.put("sharedFeatures_en", enValue);
	            }
	            if (StringUtils.hasText(frValue)) {
	                predefinedAttributes.put("sharedFeatures_fr", frValue);
	            }
	        }

	        if (variant.getPaSaleDates() != null) {
	            String enValue = variant.getPaSaleDates().getEn();
	            String frValue = variant.getPaSaleDates().getFr_CA();
	            if (StringUtils.hasText(enValue)) {
	                predefinedAttributes.put("saleDates_en", enValue);
	            }
	            if (StringUtils.hasText(frValue)) {
	                predefinedAttributes.put("saleDates_fr", frValue);
	            }
	        }
	        
//	        if (variant.getDisplayName() != null) {
//	            String enValue = variant.getDisplayName().getEn();
//	            String frValue = variant.getDisplayName().getFr_CA();
//	            Map<String, String> localizedValues = new HashMap<>();
//	            if (StringUtils.hasText(enValue)) {
//	                localizedValues.put("en", enValue);
//	            }
//	            if (StringUtils.hasText(frValue)) {
//	                localizedValues.put("fr", frValue);
//	            }
//	            if (!localizedValues.isEmpty()) {
//	                predefinedAttributes.put("displayName", localizedValues);
//	            }
//	        }
//
//	        if (variant.getLongDescription() != null) {
//	            String enValue = variant.getLongDescription().getEn();
//	            String frValue = variant.getLongDescription().getFr_CA();
//	            Map<String, String> localizedValues = new HashMap<>();
//	            if (StringUtils.hasText(enValue)) {
//	                localizedValues.put("en", enValue);
//	            }
//	            if (StringUtils.hasText(frValue)) {
//	                localizedValues.put("fr", frValue);
//	            }
//	            if (!localizedValues.isEmpty()) {
//	                predefinedAttributes.put("longDescription", localizedValues);
//	            }
//	        }
//
//	        if (variant.getPaSharedApplications() != null) {
//	            String enValue = variant.getPaSharedApplications().getEn();
//	            String frValue = variant.getPaSharedApplications().getFr_CA();
//	            Map<String, String> localizedValues = new HashMap<>();
//	            if (StringUtils.hasText(enValue)) {
//	                localizedValues.put("en", enValue);
//	            }
//	            if (StringUtils.hasText(frValue)) {
//	                localizedValues.put("fr", frValue);
//	            }
//	            if (!localizedValues.isEmpty()) {
//	                predefinedAttributes.put("sharedApplications", localizedValues);
//	            }
//	        }
//
//	        if (variant.getPaSharedFeatures() != null) {
//	            String enValue = variant.getPaSharedFeatures().getEn();
//	            String frValue = variant.getPaSharedFeatures().getFr_CA();
//	            Map<String, String> localizedValues = new HashMap<>();
//	            if (StringUtils.hasText(enValue)) {
//	                localizedValues.put("en", enValue);
//	            }
//	            if (StringUtils.hasText(frValue)) {
//	                localizedValues.put("fr", frValue);
//	            }
//	            if (!localizedValues.isEmpty()) {
//	                predefinedAttributes.put("sharedFeatures", localizedValues);
//	            }
//	        }
//	       
//	        if (variant.getPaSaleDates() != null) {
//	            String enValue = variant.getPaSaleDates().getEn();
//	            String frValue = variant.getPaSaleDates().getFr_CA();
//	            Map<String, String> localizedValues = new HashMap<>();
//	            if (StringUtils.hasText(enValue)) {
//	                localizedValues.put("en", enValue);
//	            }
//	            if (StringUtils.hasText(frValue)) {
//	                localizedValues.put("fr", frValue);
//	            }
//	            if (!localizedValues.isEmpty()) {
//	                predefinedAttributes.put("saleDates", localizedValues);
//	            }
//	        }
	        // Final mapping by SKU
	        result.put(sku, predefinedAttributes);
	    }

	    return result;
	}


	
	public static JSONArray mapProductAttributesToCustomFieldsRich(List<Attribute> attributes,
			Map<String, Map<String, String>> attribtueLabelMap) {

		JSONArray customFields = new JSONArray();

		Set<String> localizedAttributes = Set.of("displayName", "longDescription", "paSharedFeatures",
				"paSharedApplications");

		Map<String, String> labelsEn = attribtueLabelMap.get("en");
		Map<String, String> labelsFr = attribtueLabelMap.get("fr");

		JSONObject attributesEn = new JSONObject();
		JSONObject attributesFr = new JSONObject();

		for (Attribute attr : attributes) {
			String id = attr.getId();
			if (id == null || id.isEmpty())
				continue;

			boolean hasEn = attr.en != null && !attr.en.trim().isEmpty();
			boolean hasFr = attr.fr_CA != null && !attr.fr_CA.trim().isEmpty();

			if (!hasEn && !hasFr)
				continue;

			// Handle the localized core fields
			if (localizedAttributes.contains(id)) {
				JSONObject localizedObject = new JSONObject();
				if (hasEn)
					localizedObject.put("en", attr.en.trim());
				if (hasFr)
					localizedObject.put("fr_CA", attr.fr_CA.trim());

				JSONObject field = new JSONObject();
				field.put("name", id);
				field.put("value", localizedObject.toString());
				customFields.put(field);
			} else {
				// Accumulate dynamic attributes
				if (hasEn)
					attributesEn.put(id, attr.en.trim());
				if (hasFr)
					attributesFr.put(id, attr.fr_CA.trim());
			}
		}

		if (!attributesEn.isEmpty()) {
			JSONObject field = new JSONObject();
			field.put("name", "product_attributes_en");
			field.put("value", attributesEn.toString());
			customFields.put(field);
		}

		if (!attributesFr.isEmpty()) {
			JSONObject field = new JSONObject();
			field.put("name", "product_attributes_fr");
			field.put("value", attributesFr.toString());
			customFields.put(field);
		}

		return customFields;
	}

	public static JSONArray mapProductAttributesToCustomFields(List<Attribute> attributes,
			Map<String, Map<String, String>> attribtueLabelMap) {
		JSONArray customFields = new JSONArray();

		Map<String, String> labelsEn = attribtueLabelMap.get("en");
		Map<String, String> labelsFr = attribtueLabelMap.get("fr");

		for (Attribute attr : attributes) {
			String id = attr.getId();
			if (id == null || id.isEmpty())
				continue;

			String labelEn = labelsEn != null ? labelsEn.getOrDefault(id, id) : id;
			String labelFr = labelsFr != null ? labelsFr.getOrDefault(id, id) : id;

			// Add English field only if value is set
			if (attr.en != null && !attr.en.trim().isEmpty()) {
				JSONObject enField = new JSONObject();
				enField.put("name", labelEn + "_en");
				enField.put("value", attr.en.trim());
				customFields.put(enField);
			}

			// Add French field only if value is set
			if (attr.fr_CA != null && !attr.fr_CA.trim().isEmpty()) {
				JSONObject frField = new JSONObject();
				frField.put("name", labelFr + "_fr");
				frField.put("value", attr.fr_CA.trim());
				customFields.put(frField);
			}
		}

		return customFields;
	}

	public static JSONArray mapProductAttributesToCustomFields(List<Attribute> attributes) {
		JSONArray customFields = new JSONArray();

		for (Attribute attr : attributes) {
			// English field
			JSONObject enField = new JSONObject();
			enField.put("name", attr.getId() + "_en");
			enField.put("value", attr.en != null ? attr.en : "");
			customFields.put(enField);

			// French field
			JSONObject frField = new JSONObject();
			frField.put("name", attr.getId() + "_fr");
			frField.put("value", attr.fr_CA != null ? attr.fr_CA : "");
			customFields.put(frField);
		}

		return customFields;
	}

	public JSONArray buildMergedLocaleMetafieldsAsArray(Map<String, Map<String, JSONObject>> localeSkuAttributeMap,
			Map<String, Integer> skuToVariantIdMap) {

		JSONArray metafields = new JSONArray();

		for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
			String sku = skuEntry.getKey();
			Integer variantId = skuEntry.getValue();
			if (variantId == null)
				continue;

			List<String> presentLocales = new ArrayList<>();
			List<String> valueParts = new ArrayList<>();

			// Collect ordered attributes and locale codes
			for (Map.Entry<String, Map<String, JSONObject>> localeEntry : localeSkuAttributeMap.entrySet()) {
				String locale = localeEntry.getKey();
				Map<String, JSONObject> localeMap = localeEntry.getValue();

				JSONObject attrs = localeMap.get(sku);
				if (attrs != null && attrs.length() > 0) {
					presentLocales.add(locale);
					valueParts.add(":".concat(attrs.toString())); // prepend ":" before JSON
				}
			}

			if (valueParts.isEmpty())
				continue;

			// Create array-style string: [:{...},:{...}]
			String valueString = "[" + String.join(",", valueParts) + "]";

			// Construct key suffix like custom_attributes_en_fr_CA
			String keySuffix = String.join("_", presentLocales);

			JSONObject metafield = new JSONObject();
			metafield.put("key", "custom_attributes_" + keySuffix);
			metafield.put("value", valueString);
			metafield.put("namespace", "variant_attributes");
			// Allows anyone to read the metafield (e.g. via API), but only the creator can
			// update/delete it.
			metafield.put("permission_set", "read_and_sf_access");
			metafield.put("description", "Variant attributes ordered by: " + keySuffix);
			metafield.put("resource_id", variantId);

			metafields.put(metafield);
		}

		return metafields;
	}

	public Map<String, JSONArray> buildLocalizedVariantMetafields(
	        Map<String, Map<String, JSONObject>> localeSkuAttributeMap,
	        Map<String, Integer> skuToVariantIdMap,
	        Map<String, Map<String, String>> attribtueLabelMap) {

	    Map<String, JSONArray> metafieldMap = new HashMap<>();

	    for (Map.Entry<String, Map<String, JSONObject>> localeEntry : localeSkuAttributeMap.entrySet()) {
	        String locale = localeEntry.getKey();
	        Map<String, JSONObject> skuToAttrs = localeEntry.getValue();
	        Map<String, String> localizedLabels = attribtueLabelMap.get(locale.equals("fr_CA") ? "fr" : "en");
	        String localeKey = locale.equalsIgnoreCase("fr_CA") ? "fr"
	                : (locale.equalsIgnoreCase("en_CA") || locale.equalsIgnoreCase("en_US") ? "en" : locale);

	        JSONArray metafieldsForLocale = new JSONArray();

	        for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
	            String sku = skuEntry.getKey();
	            Integer variantId = skuEntry.getValue();
	            if (variantId == null) continue;

	            JSONObject rawAttributes = skuToAttrs.get(sku);
	            if (rawAttributes == null) continue;

	            JSONArray localizedAttrArray = new JSONArray();

	            Iterator<String> keys = rawAttributes.keys();
	            while (keys.hasNext()) {
	                String attrId = keys.next();

	                // ❌ Skip specific fields that are handled separately
//	                if ("displayName".equalsIgnoreCase(attrId) || "paSaleDates".equalsIgnoreCase(attrId)) continue;

	                String value = rawAttributes.optString(attrId, "").trim();
	                if (value.isEmpty()) continue;

	                String label = localizedLabels != null ? localizedLabels.getOrDefault(attrId, attrId) : attrId;

	                JSONObject attrEntry = new JSONObject();
	                attrEntry.put("label", label);
	                attrEntry.put("value", value);              
	                localizedAttrArray.put(attrEntry);
	            }

	            if (localizedAttrArray.isEmpty()) continue;

	            JSONObject metafield = new JSONObject();
	            metafield.put("key", "variant_attributes_" + localeKey);
	            metafield.put("value", localizedAttrArray.toString());
	            metafield.put("namespace", "variant_attributes");
	            metafield.put("permission_set", "read_and_sf_access");
	            metafield.put("description", "All variant attributes in " + locale);
	            metafield.put("resource_id", variantId);

	            metafieldsForLocale.put(metafield);
	        }

	        metafieldMap.put(locale, metafieldsForLocale);
	    }

	    return metafieldMap;
	}


	private boolean isEmptyJsonContent(JSONObject json) {
	    for (String key : json.keySet()) {
	        Object value = json.get(key);
	        if (value instanceof String && !((String) value).isBlank()) {
	            return false;
	        } else if (value instanceof JSONObject && !((JSONObject) value).isEmpty()) {
	            return false;
	        } else if (value instanceof JSONArray && !((JSONArray) value).isEmpty()) {
	            return false;
	        }
	    }
	    return true;
	}


	public Map<String, JSONArray> buildProductLocaleMetafields(
			Map<String, Map<String, JSONObject>> localeSkuAttributeMap, ProductCreationResult pr) {

		Map<String, JSONArray> metafieldMap = new HashMap<>();

		for (Map.Entry<String, Map<String, JSONObject>> localeEntry : localeSkuAttributeMap.entrySet()) {
			String locale = localeEntry.getKey();
			Map<String, JSONObject> skuToAttrs = localeEntry.getValue();

			JSONArray metafieldsForLocale = new JSONArray();
			Map<String, Integer> skuToVariantIdMap = pr.getSkuToVariantIdMap();
			for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
				String sku = skuEntry.getKey();
				Integer variantId = skuEntry.getValue();
				if (variantId == null)
					continue;

				JSONObject attributes = skuToAttrs.get(sku);
				if (attributes == null)
					continue;

				JSONObject metafield = new JSONObject();
				metafield.put("key", "product_attributes_" + locale);
				metafield.put("value", attributes.toString()); // serialized JSON object
				metafield.put("namespace", "product_attributes");
				// Allows anyone to read the metafield (e.g. via API and grph ql), but only the
				// creator can update/delete it.
				metafield.put("permission_set", "read_and_sf_access");
				metafield.put("description", "All variant attributes in " + locale);
				metafield.put("resource_id", pr.getProductId());

				metafieldsForLocale.put(metafield);
			}

			metafieldMap.put(locale, metafieldsForLocale);
		}

		return metafieldMap;
	}

	public JSONArray buildStaticVariantMetafields(Map<String, JSONObject> staticSkuAttributeMap,
			Map<String, Integer> skuToVariantIdMap) {
		JSONArray staticMetafields = new JSONArray();

		for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
			String sku = skuEntry.getKey();
			Integer variantId = skuEntry.getValue();
			if (variantId == null)
				continue;

			JSONObject staticAttrs = staticSkuAttributeMap.get(sku);
			if (staticAttrs == null)
				continue;

			for (String fieldName : staticAttrs.keySet()) {
				String value = staticAttrs.optString(fieldName, "").trim();
				if (value.isEmpty())
					continue; // ✅ Skip empty or whitespace-only values

				JSONObject field = new JSONObject();
				field.put("key", fieldName);
				field.put("value", value);
				field.put("namespace", "variant_attributes");
				field.put("permission_set", "read_and_sf_access");
				field.put("description", "Variant flags: " + fieldName);
				field.put("resource_id", variantId);

				staticMetafields.put(field);
			}
			
		}

		return staticMetafields;
	}

	/**
	 * 🔁 Maps a list of  {@link Variant} objects to the BigCommerce-compatible JSON format.
	 *
	 * This method prepares variant-level data to be included in a BigCommerce product creation or update payload.
	 * Each variant is transformed into a structured {@link JSONObject} with the following standard fields:
	 *
	 * - 🔢 {@code sku}: Stock Keeping Unit, uniquely identifies the variant
	 * - 📏 {@code height}, {@code width}, {@code depth}: Physical dimensions of the variant
	 * - ⚖️ {@code weight}: Weight of the variant
	 * - 📦 {@code upc}: Universal Product Code
	 * - 🆔 {@code gtin}: Vendor number (treated as Global Trade Item Number)
	 * - 🔧 {@code mpn}: Manufacturer Part Number (vendor part number)
	 * - 🏷️ {@code option_values}: (Optional) Mapped product option values such as size, color, etc., using {@link #mapVariantsOptionValues(Variant)}
	 *
	 * The resulting JSON array can be directly used in BigCommerce's product API under the `variants` field.
	 *
	 * @param variants A list of {@link Variant} objects representing different configurations of a product
	 * @return A {@link JSONArray} where each item is a JSON object representing a BigCommerce variant
	 */
	private JSONArray mapVariantsToBigCommerce(List<Variant> variants) {
		JSONArray variantsArray = new JSONArray();

		for (Variant variant : variants) {

			JSONObject variantJson = new JSONObject();

			variantJson.put("sku", variant.getSkuNumber());
			variantJson.put("height", variant.getPaHeight());
			variantJson.put("depth", variant.getPaLength());
			variantJson.put("width", variant.getPaWidth());
			variantJson.put("weight", variant.getPaWeight());
			variantJson.put("upc", variant.getPaUPC());
			variantJson.put("gtin", variant.getPaVendorNumber());
			variantJson.put("mpn", variant.getPaVendorPartNumber());

			if (variant.getOption_values() != null && !variant.getOption_values().isEmpty()) {
				variantJson.put("option_values", mapVariantsOptionValues(variant));
			}

			variantsArray.put(variantJson);
		}

		return variantsArray;
	}
	
	/**
	 * Creates a metafield JSON array for variants, checking both productNumber and skuNumber
	 * in references. Each object includes:
	 * - "namespace": "variant_attributes"
	 * - "key": "related_variants"
	 * - "value": JSON string of related variant references
	 * Skips creating entries for variants with no resolved references.
	 */
	public JSONArray buildRelatedVariantMetafields(
	        List<Variant> variants,
	        ProductSyncContext context,
	        Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap) {

	    JSONArray metafields = new JSONArray();

	    for (Variant variant : variants) {
	        if (variant.getReferences() == null || variant.getReferences().isEmpty()) {
	            continue; // No references for this variant
	        }

	        JSONArray referenceArray = new JSONArray();

	        for (ProductRefernce ref : variant.getReferences()) {
	            String refProductNumber = ref.getParentProduct();
	            String refSkuNumber = ref.getSkuNumber();

	            // First try to resolve by productNumber
	            Integer internalProductId = context.productNumberToProductId.get(refProductNumber);
	            // Also check if skuNumber can resolve to a variantId
	            Integer internalVariantId = context.skuToVariantId.get(refSkuNumber);

	            if (internalProductId != null || internalVariantId != null) {
	                // ✅ Create JSON object for resolved reference
	                JSONObject refJson = new JSONObject();
	                if (refProductNumber != null) {
	                    refJson.put("ref_product_id", refProductNumber);
	                }
	                if (internalVariantId != null) {
	                    refJson.put("variant_id", internalVariantId);
	                }
	                refJson.put("sku_number", refSkuNumber);
	                refJson.put("type", ref.getType());
	                referenceArray.put(refJson);
	            } else {
	                // ❌ Track for retry later
	                variantsUnresolvedReferencesMap
	                        .computeIfAbsent(Integer.valueOf(variant.getSkuNumber()), k -> new ArrayList<>())
	                        .add(ref);
	                System.out.printf(
	                        "⚠️ Unresolved reference: variant '%s' references product '%s' / sku '%s' (type: %s)%n",
	                        variant.getSkuNumber(),
	                        refProductNumber,
	                        refSkuNumber,
	                        ref.getType()
	                );
	            }
	        }

	        if (!referenceArray.isEmpty()) {
	            // ✅ Create a metafield JSON object for this variant
	            JSONObject metafield = new JSONObject();
	            metafield.put("namespace", "variant_attributes");  // Changed from variant_related_data
	            metafield.put("key", "related_variants");          // Changed to static key
	            metafield.put("value", referenceArray.toString());
	            metafield.put("permission_set", "read_and_sf_access");
	            metafield.put("resource_id", context.skuToVariantId.get(variant.getSkuNumber())); // optional

	            metafields.put(metafield);
	        }
	    }

	    return metafields;
	}



	/**
	 * Helper method to build variant JSONs including related variants if resolved.
	 * Returns a JSONArray with all variant JSON payloads.
	 */
	public JSONArray addRelatedVariants(
	        List<Variant> variants,
	        ProductSyncContext context,
	        Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap) {

	    JSONArray updatedVariantsArray = new JSONArray();

	    for (Variant variant : variants) {
	        JSONObject variantJson = new JSONObject();
//	        variantJson.put("sku", variant.getSkuNumber());
	        // You might want to add other basic variant details here as needed.

	        if (variant.getReferences() != null && !variant.getReferences().isEmpty()) {
	            JSONArray referenceArray = new JSONArray();

	            for (ProductRefernce ref : variant.getReferences()) {
	                String refProductNumber = ref.getParentProduct();
	                Integer internalId = context.productNumberToProductId.get(refProductNumber);

	                if (internalId != null) {
	                    // ✅ Resolved reference – include in variant JSON
	                    JSONObject refJson = new JSONObject();
	                    refJson.put("product_id", internalId);
	                    refJson.put("sku_number", ref.getSkuNumber());
	                    refJson.put("type", ref.getType());
	                    referenceArray.put(refJson);
	                } else {
	                	  // ❌ Unresolved reference – track for retry later using SKU as key
	                    variantsUnresolvedReferencesMap
	                            .computeIfAbsent(Integer.valueOf(variant.getSkuNumber()), k -> new ArrayList<>())
	                            .add(ref);
	                    System.out.printf(
	                            "⚠️ Unresolved reference: variant '%s' references '%s' (type: %s) which is not yet created.%n",
	                            variant.getSkuNumber(),
	                            refProductNumber,
	                            ref.getType()
	                    );
	                }
	            }

	            if (!referenceArray.isEmpty()) {
	                variantJson.put("related_variants", referenceArray);
	            }
	        }

	        updatedVariantsArray.put(variantJson);
	    }

	    return updatedVariantsArray;
	}

	

	
	private String getLocalizedNameAttribute(List<Attribute> attributes, String attributeId, String locale) {
		for (Attribute attr : attributes) {
			if (attr.id.equalsIgnoreCase(attributeId)) {
				if ("fr_CA".equalsIgnoreCase(locale)) {
					return attr.fr_CA != null ? attr.fr_CA : attr.en;
				} else {
					return attr.en;
				}
			}
		}
		return ""; // fallback if not found
	}

	private String getLocalizedAttribute(List<Attribute> attributes, String attributeId, String locale) {
		for (Attribute attr : attributes) {
			if (attr.id.equalsIgnoreCase(attributeId)) {
				if ("fr_CA".equalsIgnoreCase(locale)) {
					return attr.fr_CA != null ? attr.fr_CA : attr.en;
				} else {
					return attr.en;
				}
			}
		}
		return ""; // fallback if not found
	}

	
	/**
	 * 🏷️ Maps the option values for a given variant into BigCommerce-compatible JSON format.
	 *
	 * This method processes the {@code option_values} of a {@link Variant} and converts each
	 * {@link OptionValue} into a JSON object with the following fields:
	 *
	 * - {@code option_display_name}: The name of the product option (e.g., Size, Color) in English
	 * - {@code label}: The selected value for that option (e.g., Small, Red) in English
	 *
	 * These values are expected to align with the option definitions already created in BigCommerce.
	 *
	 * @param variant The {@link Variant} containing option values to map
	 * @return A {@link JSONArray} containing BigCommerce-formatted option value entries
	 */
	public JSONArray mapVariantsOptionValues(Variant variant) {
		JSONArray optionValuesArray = new JSONArray();

		if (variant.option_values != null) {
			for (OptionValue option : variant.option_values) {
				JSONObject optionValueJson = new JSONObject();
				optionValueJson.put("option_display_name", option.getOption_name_en());
				optionValueJson.put("label", option.getOption_value_en());
				optionValuesArray.put(optionValueJson);
			}
		}

		return optionValuesArray;
	}
	
	public JSONArray mapVariantsOptionValuesForProd(Variant variant) {
	    JSONArray optionValuesArray = new JSONArray();

	    if (variant.option_values != null) {
	        int index = 0;
	        for (OptionValue option : variant.option_values) {
	            JSONObject optionValueJson = new JSONObject();
	            optionValueJson.put("label", option.getOption_value_en()); // ✅ required
	            optionValueJson.put("sort_order", index++);                // ✅ required
	            optionValuesArray.put(optionValueJson);
	        }
	    }

	    return optionValuesArray;
	}

	public JSONArray buildFrenchOptionLabelMetafields(List<Variant> variants, Map<String, Integer> skuToVariantIdMap) {
		JSONArray metafields = new JSONArray();

		for (Variant variant : variants) {
			String sku = variant.getSkuNumber();
			Integer variantId = skuToVariantIdMap.get(sku);
			if (variantId == null)
				continue;

			JSONArray optionValuesArray = new JSONArray();

			if (variant.option_values != null) {
				for (OptionValue option : variant.option_values) {
					String nameFr = option.getOption_name_fr_CA();
					String valueFr = option.getOption_value_fr_CA();

					if (nameFr != null && !nameFr.trim().isEmpty() && valueFr != null && !valueFr.trim().isEmpty()) {

						JSONObject optionJson = new JSONObject();
						optionJson.put("option_display_name", nameFr.trim());
						optionJson.put("label", valueFr.trim());
						optionValuesArray.put(optionJson);
					}
				}
			}

			if (!optionValuesArray.isEmpty()) {
				JSONObject metafield = new JSONObject();
				metafield.put("key", "option_labels_fr");
				metafield.put("value", optionValuesArray.toString());
				metafield.put("namespace", "variant_attributes");
				metafield.put("permission_set", "read_and_sf_access");
				metafield.put("description", "French option labels/values");
				metafield.put("resource_id", variantId);

				metafields.put(metafield);
			}
		}

		return metafields;
	}

	public JSONArray buildVariantPdfMetafields(List<Variant> variants, Map<String, Integer> skuToVariantIdMap) {
		JSONArray pdfMetafields = new JSONArray();

		for (Variant variant : variants) {
			String sku = variant.getSkuNumber();
			Integer variantId = skuToVariantIdMap.get(sku);
			if (variantId == null)
				continue;

			List<Asset> assets = variant.getAssets();
			if (assets == null)
				continue;

			for (Asset asset : assets) {
				if (!"document".equalsIgnoreCase(asset.getType()))
					continue;

				String filename = asset.getPaDocumentsFileName();
				if (filename == null || filename.trim().isEmpty())
					continue;

				String label = asset.getDocumentLabel() != null ? asset.getDocumentLabel() : "Manual PDF";
				String description = asset.getDescription() != null ? asset.getDescription() : "PDF Document";

				JSONObject valueJson = new JSONObject();
				valueJson.put("filename", filename);
				valueJson.put("label", label);
				valueJson.put("description", description);

				JSONObject metafield = new JSONObject();
				metafield.put("key", "manual_pdf_name");
				metafield.put("value", valueJson.toString());
				metafield.put("namespace", "variant_attributes");
				metafield.put("permission_set", "read_and_sf_access");
				metafield.put("description", description);
				metafield.put("resource_id", variantId);

				pdfMetafields.put(metafield);
			}
		}

		return pdfMetafields;
	}

	private static Map<String, JSONObject> mapAttributesAndAttLabelsToLocale(List<Variant> variants, String locale,
			Map<String, Map<String, String>> attribtueLabelMap) {
		Map<String, JSONObject> result = new HashMap<>();

		// Pick the correct label map
//	    Map<String, String> labelMap = "fr_CA".equals(locale)
//	            ? AttributeLabels.LABELS_FR
//	            : AttributeLabels.LABELS_EN;

		Map<String, String> labelMap = "fr_CA".equals(locale) ? attribtueLabelMap.get("fr")
				: attribtueLabelMap.get("en");

		for (Variant variant : variants) {
			JSONObject localizedAttributes = new JSONObject();

			if (variant.attributes != null) {
				for (Attribute attr : variant.attributes) {
					String value = "";
					switch (locale) {
					case "fr_CA":
						value = attr.fr_CA != null ? attr.fr_CA : "";
						break;
					case "en":
					default:
						value = attr.en != null ? attr.en : "";
						break;
					}

					
					if (attr.id != null && !attr.id.isEmpty()) {
						// Get label if available, else fallback to ID
						String label = labelMap.getOrDefault(attr.id, attr.id);
						
						
						
						localizedAttributes.put(label, value);
					}
				}
			}

			if (variant.skuNumber != null && !variant.skuNumber.isEmpty()) {
				result.put(variant.skuNumber, localizedAttributes);
			}
		}

		return result;
	}

	public static Map<String, JSONObject> mapAttributesToLocale(List<Variant> variants, String locale) {
		Map<String, JSONObject> result = new HashMap<>();

		for (Variant variant : variants) {
			JSONObject localizedAttributes = new JSONObject();

			if (variant.attributes != null) {
				for (Attribute attr : variant.attributes) {
					String value = "";
					switch (locale) {
					case "fr_CA":
						value = attr.fr_CA != null ? attr.fr_CA : "";
						break;
					case "en":
					default:
						value = attr.en != null ? attr.en : "";
						break;
					}
					
					if (attr.id != null && !attr.id.isEmpty()) {
	                    // 🌟 Include the attribute value
	                    localizedAttributes.put(attr.id, value);
	                }
				}
			}
			
			

			if (variant.skuNumber != null && !variant.skuNumber.isEmpty()) {
				result.put(variant.skuNumber, localizedAttributes);
			}
		}

		return result;
	}

	private void putIfNotNull(JSONObject obj, String key, Object value) {
		if (value != null) {
			obj.put(key, value);
		}
	}
	
	// Utility method for safe sequence parsing
	private static int parseSeq(String seqStr) {
	    try {
	        return Integer.parseInt(seqStr != null ? seqStr : "9999");
	    } catch (NumberFormatException e) {
	        return 9999;
	    }
	}
	
	// Utility function to create a JSON object for { "en": "", "fr": "" }
	private static BiFunction<String, String, JSONObject> createLocalizedValue = (en, fr) -> {
	    JSONObject json = new JSONObject();
	    json.put("en", en != null ? en : "");
	    json.put("fr", fr != null ? fr : "");
	    return json;
	};

}
