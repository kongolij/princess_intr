package com.bigcommerce.imports.catalog.product.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
import com.bigcommerce.imports.catalog.constants.CommonConstants;
import com.bigcommerce.imports.catalog.product.dto.Asset;
import com.bigcommerce.imports.catalog.product.dto.OptionValue;
import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.ProductCreationResult;
import com.bigcommerce.imports.catalog.product.dto.ProductOptionResolution;
import com.bigcommerce.imports.catalog.product.dto.ProductRefernce;
import com.bigcommerce.imports.catalog.product.dto.Variant;
import com.bigcommerce.imports.catalog.product.mapper.BigCommerceProductMapper;
import com.bigcommerce.imports.catalog.product.repository.BigCommerceRepository;
import com.bigcommerce.imports.catalog.product.service.BigCommerceProductSyncService.ProductSyncContext;
import com.bigcommerce.imports.catalog.service.BigCommerceService;

import io.micrometer.common.util.StringUtils;

@Component
public class BigCommerceProductService {

	private final BigCommerceRepository bigCommerceRepository;
	private final BigCommerceProductMapper bigCommerceProductMapper;
	private final BigCommerceService bigCommerceCategoryService;
	private final BigCommerceProductSyncService bigCommerceProductSyncService;
	

	public BigCommerceProductService(BigCommerceRepository bigCommerceRepository,
			BigCommerceProductMapper bigCommerceProductMapper, BigCommerceService bigCommerceCategoryService,
			BigCommerceProductSyncService bigCommerceProductSyncService
			) {
		this.bigCommerceRepository = bigCommerceRepository;
		this.bigCommerceProductMapper = bigCommerceProductMapper;
		this.bigCommerceCategoryService = bigCommerceCategoryService;
		this.bigCommerceProductSyncService = bigCommerceProductSyncService;
		

	}

	public void importProducts(List<Product> products, String locale,
			Map<String, Map<String, String>> attribtueLabelMap) throws Exception {

		// ⏱️ 1. Retrieve category mappings from external ID to BigCommerce internal ID (for EN locale)
		long startTime = System.currentTimeMillis();
		Map<String, Integer> categoriesMap = bigCommerceCategoryService.getBCExternalToInternalCategoryMap("en");
		long endTime = System.currentTimeMillis();
		double durationInSeconds = (endTime - startTime) / 1000.0;
		System.out.printf("✅ Fetched %d categories in %.2f seconds%n", categoriesMap.size(), durationInSeconds);

		// ⏱️ 2. Retrieve brand mappings from external brand name to BigCommerce internal brand ID
		startTime = System.currentTimeMillis();
		Map<String, Integer> brandMap = bigCommerceCategoryService.getBCExternalToInternalBrandMap("");
		endTime = System.currentTimeMillis();
		durationInSeconds = (endTime - startTime) / 1000.0;
		System.out.printf("✅ Fetched %d brands in %.2f seconds%n", categoriesMap.size(), durationInSeconds);

		// ⏱️ 3. Build the ProductSyncContext — captures current product state in BC (IDs, names, metafields)
		startTime = System.currentTimeMillis();
		ProductSyncContext productSyncContext = bigCommerceProductSyncService.buildProductSyncContext();
		endTime = System.currentTimeMillis();
		durationInSeconds = (endTime - startTime) / 1000.0;
		System.out.printf("✅ Fetched %d ProductSyncContext in %.2f seconds%n",
				productSyncContext.productNumberToProductId.size(), durationInSeconds);

		// 🔁 Process each product individually.
		// ------------------------------------------------------------------------------------------------------
		// Current approach: Products are processed one by one (create/update) in a single thread.
		//
		// Optimization target: Implement batched processing using multi-threading or parallel streams.
		// - Split products into chunks (e.g., List<List<Product>> batches)
		// - Submit each batch to a thread pool (e.g., using ExecutorService)
		// - Ensure thread safety for shared resources (e.g., sync context updates, API rate limits)
		//
		// This is essential for scaling up to larger product feeds (e.g., >10,000 SKUs).
		// ------------------------------------------------------------------------------------------------------
		if (!(products == null || products.isEmpty())) {
			
			// 🔄 unresolvedReferencesMap Tracks unresolved product references during import.
			// -------------------------------------------------------------------------------------
			// Some products may reference other products (e.g., spare parts) that 
			// have not yet been created at the time of processing. These references cannot be 
			// linked immediately because the internal BigCommerce product ID is unknown.
			//
			// We collect all such "forward references" in this map so that after the full import 
			// pass is complete, we can retry and set them correctly.
			//
			// Key:    Target product's internal BC product ID
			// Value:  List of ProductRefernce entries that couldn't be resolved initially
			// -------------------------------------------------------------------------------------
			Map<Integer, List<ProductRefernce>> unresolvedReferencesMap = new HashMap<>();
			
			for (Product product : products) {

				String productNumber = product.getProductNumber();
				
//				//has replaces
//				if (!"PA1000001804".equals(productNumber)) {
//				//variant has no options
//				//if (!"PA0003461449".equals(productNumber)) {
//  				//if (!"PA1000000880".equals(productNumber)) {
//					continue;
//				}

		        Integer existingProductId = productSyncContext.productNumberToProductId.get(productNumber);			
		         // ✅ Product already exists – update flow
				if (existingProductId != null) {

					updateExistingProduct(
					        product,
					        existingProductId,
					        productSyncContext,
					        attribtueLabelMap,
					        categoriesMap,
					        brandMap,
					        unresolvedReferencesMap);
				}else {
					 // 🆕 Create flow for new products	
		            createNewProduct(
		            		 product,
						     productSyncContext,
						     attribtueLabelMap,
						     categoriesMap,
						     brandMap,
						     unresolvedReferencesMap
		            );
				}

//				// Map product to BigCommerce JSON format
//				@SuppressWarnings("unused")
//				JSONObject productJson = bigCommerceProductMapper.mapProductToBigCommerce(product, categoriesMap,
//						brandMap, locale);
//
//				// Map product attr to BigCommerce product custom attributes
//				JSONArray productCustomAttributeJson = bigCommerceProductMapper
//						.mapProductToBigCommerceCustomAttr(product, attribtueLabelMap);
//
//				// sku to localized attributes
				Map<String, Map<String, JSONObject>> skuVaraintCustomAttributesMap = bigCommerceProductMapper
						.buildLocalizedVariantAttributeMap(product, attribtueLabelMap);
//
//				Map<String, JSONObject> varaintStaticAttributeJson = bigCommerceProductMapper
//						.mapPredefinedVariantAttributesToMetafields(product);
//
//				// Wrap the product JSON in a JSONArray to match the expected parameter in
//				// `createProductWithVariants`
//				JSONArray productsJsonArray = new JSONArray();
//				productsJsonArray.put(productJson);
//
//				ProductCreationResult pr = bigCommerceRepository.createProductWithVariants(locale, productJson);
//
//				System.out.println("Created product with category ID: " + pr.getProductId());
//				if (pr.getProductId() != 0) {
//
//					//
//					bigCommerceRepository.assignProductToChannel(locale, pr.getProductId(),
//							BigCommerceStoreConfig.BC_CHANNEL_ID);
//					bigCommerceRepository.setProductCustomFields(productCustomAttributeJson, pr.getProductId());
//
//					Map<String, String> uploadProductsAssets = uploadProductsAssets(pr.getProductId(), product);
//
//					if (product.getVariants().size() == 1
//							&& (product.getVariants().get(0).getOption_values() == null)) {
//
//						continue;
////						Map<String, JSONArray> productMetafileds = bigCommerceProductMapper
////								.buildProductLocaleMetafields(skuVaraintCustomAttributesMap, pr);
////						bigCommerceRepository.setProductMetafieldsInBatches(productMetafileds);
//
//					} else {
//
//						JSONArray optionLabelsFrMetafields = bigCommerceProductMapper
//								.buildFrenchOptionLabelMetafields(product.getVariants(), pr.getSkuToVariantIdMap());
//
//						JSONArray manualMetafields = bigCommerceProductMapper
//								.buildVariantPdfMetafields(product.getVariants(), pr.getSkuToVariantIdMap());
//
//						Map<String, JSONArray> varaintMetafileds = bigCommerceProductMapper
//								.buildLocalizedVariantMetafields(skuVaraintCustomAttributesMap, pr.getSkuToVariantIdMap(), attribtueLabelMap);
//
//						JSONArray varaintStMetafileds = bigCommerceProductMapper
//								.buildStaticVariantMetafields(varaintStaticAttributeJson, pr.getSkuToVariantIdMap());
//
//						bigCommerceRepository.setVariantMetafieldsInBatches(varaintMetafileds, varaintStMetafileds,
//								optionLabelsFrMetafields);
//
//						uploadVariantAssets(product, pr.getSkuToVariantIdMap(), pr.getProductId(),
//								uploadProductsAssets);
//					}
//
//					// PDF not supported
//
//				}
			}

		}
	}
	
	private void updateExistingProduct(
			Product product,
		    int existingProductId,
		    ProductSyncContext context,
		    Map<String, Map<String, String>> attribtueLabelMap,
		    Map<String, Integer> categoriesMap,
		    Map<String, Integer> brandMap,
		    Map<Integer, List<ProductRefernce>> unresolvedReferencesMap) throws Exception {
		
		Map<Integer, List<Integer>> productIdToCustomFieldIds = context.productIdToCustomFieldIds;
		Map<Integer, List<Integer>> productIdToMetafieldIds = context.productIdToMetafieldIds;
		Map<Integer, String> productIdToName = context.productIdToName;

	
		
		// 🛠️ Build the updated JSON payload for the product (categories, brand, etc.)
		JSONObject updatedProductJson = bigCommerceProductMapper.mapProductToUpdateJson(
				productIdToName,
				product, 
				categoriesMap, 
				brandMap,
				context,
				existingProductId,
				unresolvedReferencesMap);

		// 📦 Submit the update request to BigCommerce
		bigCommerceRepository.updateSingleProduct(existingProductId, updatedProductJson);

		// 🧽 Clean up old product-level metafields
		List<Integer> existingCustomFieldIds = productIdToCustomFieldIds.get(existingProductId);
		if (existingCustomFieldIds != null) {
			for (Integer cfId : existingCustomFieldIds) {
				bigCommerceRepository.deleteProductCustomField(existingProductId, cfId);
			}
		}

		// ✅ Delete old product metafields
		List<Integer> existingMetafieldIds = productIdToMetafieldIds.get(existingProductId);
		if (existingMetafieldIds != null) {
			bigCommerceRepository.deleteProductMetafields(existingMetafieldIds);

		}

		// 🧩 Update product custom fields and localized/dynamic metafields
		updateProductFields(product, existingProductId, attribtueLabelMap);
        
		// 🔄 Update all variant-level data (custom fields, metafields, images, etc.)
		updateVariantDetails(product, context, attribtueLabelMap, existingProductId);
	}
	
	/**
	 * 🚀 Creates a new product in BigCommerce along with all its variants and related data.
	 *
	 * This method is invoked for products that do not yet exist in BigCommerce
	 * (i.e., they are not present in the {@link ProductSyncContext}).
	 *
	 * It handles the complete lifecycle of product creation, including:
	 *
	 * - 🛠️ Mapping product attributes, categories, and brand into the BigCommerce product payload
	 * - 📦 Creating the product and its variants via the BigCommerce API
	 * - 🗂️ Assigning the product to the appropriate sales channel
	 * - 🏷️ Mapping and uploading predefined product-level attributes as custom fields
	 *       (e.g., MPN, clearance status, vendor code)
	 * - 🌍 Mapping and batching localized (EN/FR) product attributes into structured metafields
	 * - 🧩 Mapping variant-level attributes, including:
	 *     - Localized variant fields (as JSON metafields per locale)
	 *     - Predefined variant attributes (e.g., weight, status)
	 *     - Option label translations (e.g., for French storefronts)
	 *     - PDF manuals or document-type assets
	 * - 🖼️ Uploading product-level and variant-level images
	 *
	 * @param product The product to be created
	 * @param existingProductId ID of the product if already known (null if creating new)
	 * @param productSyncContext Context for tracking existing product/variant mappings
	 * @param attributeLabelMap Mapping of localized attribute labels per locale
	 * @param categoriesMap Mapping of category slugs/keys to BigCommerce category IDs
	 * @param brandMap Mapping of brand names to BigCommerce brand IDs
	 * @param unresolvedReferencesMap Map of product ID to unresolved cross-product references
	 * @throws Exception if any step of the product creation process fails
	 */
	private void createNewProduct(
			 Product product,
		     ProductSyncContext productSyncContext,
		     Map<String, Map<String, String>> attributeLabelMap,
		     Map<String, Integer> categoriesMap,
		     Map<String, Integer> brandMap,
		     Map<Integer, List<ProductRefernce>> unresolvedReferencesMap) throws Exception {

		// 🛠️ Map the full product definition (basic info, brand, categories) to BigCommerce JSON
		JSONObject productJson = bigCommerceProductMapper.mapProductToBigCommerce(
			    product,
			    categoriesMap,
			    brandMap,
			    productSyncContext,
			    unresolvedReferencesMap
			);
	   
	    // 🏷️ Build product-level custom fields (e.g., vendor code, MPN, clearance)
	    ProductCreationResult creationResult = bigCommerceRepository.createProductWithVariants(productJson);
	   
	    int productId = creationResult.getProductId();
	    Map<String, Integer> skuToVariantIdMap = creationResult.getSkuToVariantIdMap();
	    System.out.println("🆕 Created product with ID: " + productId);

	    if (productId == 0) {
	        System.err.println("❌ Failed to create product: " + product.getProductNumber());
	        return;
	    }

	    // Assign to channel
	    bigCommerceRepository.assignProductToChannel( productId, BigCommerceStoreConfig.BC_CHANNEL_ID);

	    // Upload product-level assets
	    Map<String, String> uploadedProductAssets = uploadProductsAssets(productId, product);
	    
        updateProductFields(product, productId, attributeLabelMap);
        
        //🔁 Create variant-level metafields (localized, static, and labels)
        createVariantMetafieldsOnly(product, productSyncContext, attributeLabelMap, skuToVariantIdMap);
       
		

//	    // Create product-level custom fields
//	    JSONArray productCustomFields = bigCommerceProductMapper.mapProductToBigCommerceCustomAttr(product, attributeLabelMap);
//	    bigCommerceRepository.setProductCustomFields(productCustomFields, productId);

//	    // Handle single-variant product without options
//	    if (product.getVariants().size() == 1 && product.getVariants().get(0).getOption_values() == null) {
//	        System.out.println("ℹ️ Skipping variant metafields for single-variant product: " + product.getProductNumber());
//	        return;
//	    }
//
//	    // Prepare variant-level mappings
//	    Map<String, Map<String, JSONObject>> localizedAttrMap = bigCommerceProductMapper
//	            .buildLocalizedVariantAttributeMap(product, attributeLabelMap);
//	    Map<String, JSONObject> predefinedAttrMap = bigCommerceProductMapper
//	            .mapPredefinedVariantAttributesToMetafields(product);
//
//	    // Build all variant-level metafields
//	    JSONArray frenchOptionLabelMetafields = bigCommerceProductMapper
//	            .buildFrenchOptionLabelMetafields(product.getVariants(), skuToVariantIdMap);
//	    JSONArray manualPdfMetafields = bigCommerceProductMapper
//	            .buildVariantPdfMetafields(product.getVariants(), skuToVariantIdMap);
//	    Map<String, JSONArray> localizedMetafields = bigCommerceProductMapper
//	            .buildLocalizedVariantMetafields(localizedAttrMap, skuToVariantIdMap, attributeLabelMap);
//	    JSONArray predefinedMetafields = bigCommerceProductMapper
//	            .buildStaticVariantMetafields(predefinedAttrMap, skuToVariantIdMap);
//
//	    // Push variant metafields
//	    bigCommerceRepository.setVariantMetafieldsInBatches(localizedMetafields, predefinedMetafields, frenchOptionLabelMetafields);

	    // Upload variant assets
	    uploadVariantAssets(product, skuToVariantIdMap, productId, uploadedProductAssets);
	}


	public void updateProductFields(Product product, int productId, Map<String, Map<String, String>> attribtueLabelMap) throws Exception {
		
		// 🏷️ Map predefined (non-localized) product-level attributes (e.g., MPN, UPC, vendor code) to custom fields
		JSONArray customFields = BigCommerceProductMapper.mapPredefinedProductAttributesToCustomFields(product);
		bigCommerceRepository.setProductCustomFields(customFields, productId);

		 // 🌍 Map localized product attributes (e.g., name, description in EN/FR) to metafields
		JSONArray metafields = BigCommerceProductMapper.mapProductToMetafields(product.getAttributes(), productId, attribtueLabelMap);
		
		// 📥 Upload all product-level metafields in batch
		bigCommerceRepository.setProductMetafieldsInBatches(metafields);

		System.out.println("✅ Product " + product.getProductNumber() + " updated with " + customFields.length()
				+ " custom fields and " + metafields.length() + " metafields.");
	}

	private Map<String, String> uploadProductsAssets(int productId, Product pr) {
		Map<String, String> uploadedAssets = new HashMap<>();

		if (pr.getAssets() == null || pr.getAssets().isEmpty()) {
			return uploadedAssets; // No assets to upload
		}

		for (Asset asset : pr.getAssets()) {
			String fileName = asset.getPaDocumentsFileName();

			if (StringUtils.isEmpty(fileName) || "NULL".equalsIgnoreCase(fileName)
					|| "document".equalsIgnoreCase(asset.getType())) {
				System.err.println(".... pdf at product level: " + fileName + " productId " + productId);
				continue; // Skip invalid or null filenames
			}

			String imageUrl = CommonConstants.PRODUCT_IMAGE_URL + fileName;

			try {
				byte[] imageBytes = downloadImageBytes(imageUrl);
				if (imageBytes != null) {
					String uploadedImageUrl = bigCommerceRepository.uploadProductImageToBigCommerce(productId, fileName,
							imageBytes);

					if (uploadedImageUrl != null) {
						uploadedAssets.put(fileName, uploadedImageUrl);
					} else {
						System.err.println("⚠️ Image upload returned null URL for: " + fileName);
					}
				} else {
					System.err.println("⚠️ Failed to download image bytes from: " + imageUrl);
				}
			} catch (Exception e) {
				System.err.println("❌ Error uploading asset for file " + fileName + " (product ID " + productId + "): "
						+ e.getMessage());
				e.printStackTrace();
			}
		}

		return uploadedAssets;
	}

	private void uploadProductAssets(int productId, Product pr) {
		if (pr.getAssets() == null || pr.getAssets().isEmpty()) {
			return; // No assets to upload
		}

		String fileName = pr.getAssets().get(0).getPaDocumentsFileName();
		if (StringUtils.isEmpty(fileName) || "NULL".equalsIgnoreCase(fileName)) {
			return; // Invalid or missing file name
		}

		String imageUrl = CommonConstants.PRODUCT_IMAGE_URL + fileName;

		try {
			byte[] imageBytes = downloadImageBytes(imageUrl);
			if (imageBytes != null) {
				bigCommerceRepository.uploadProductImageToBigCommerce(productId, fileName, imageBytes);
			} else {
				System.err.println("⚠️ Failed to download image bytes from: " + imageUrl);
			}
		} catch (Exception e) {
			System.err.println("❌ Error uploading asset for product ID " + productId + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void uploadVariantAssets(Product product, Map<String, Integer> skuToVariantIdMap, int productId,
			Map<String, String> uploadProductsAssets) {
		if (product.getVariants() == null)
			return;

		for (Variant variant : product.getVariants()) {
			List<Asset> assets = variant.getAssets();
			if (assets == null || assets.isEmpty())
				continue;

			String sku = variant.getSkuNumber();
			Integer variantId = skuToVariantIdMap.get(sku);
			if (variantId == null)
				continue;

			for (Asset asset : assets) {
				if ("document".equalsIgnoreCase(asset.getType()))
					continue;

				String fileName = asset.getPaDocumentsFileName();
				if (StringUtils.isEmpty(fileName) || "NULL".equalsIgnoreCase(fileName))
					continue;
				String imageUrl = uploadProductsAssets.get(fileName);

//	            String imageUrl = CommonConstants.VARIANT_MANUAL_URL + fileName;
				// image_url

				try {
//	                byte[] imageBytes = downloadImageBytes(imageUrl);
//	                if (imageBytes != null) {
					bigCommerceRepository.uploadVariantImageToBigCommerce(productId, variantId, imageUrl);
					System.out.println("✅ Uploaded variant image for SKU " + sku + ": " + imageUrl);
//	                } else {
//	                    System.err.println("⚠️ Failed to download variant image: " + imageUrl);
//	                }
				} catch (Exception e) {
					System.err.println("❌ Error uploading image for variant SKU " + sku + ": " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	private byte[] downloadImageBytes(String imageFileName) {
		try {
			String imageUrl = imageFileName;
			URL url = new URL(imageUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");

			try (InputStream inputStream = connection.getInputStream()) {
				return inputStream.readAllBytes();
			}
		} catch (Exception e) {
			System.err.println("❌ Failed to download image: " + imageFileName + " → " + e.getMessage());
			return null;
		}
	}

	/**
	 * Updates variant-level details including attribute data and metafields.
	 * Handles both multi-option products and simple products with a single variant.
	 */
	private void updateVariantDetails(
			Product product, 
			ProductSyncContext context,
			Map<String, Map<String, String>> attribtueLabelMap, 
			int existingProductId) throws Exception {
		
		// 🔍 Look up variant ID and existing metafields from context
		Map<String, Integer> variantIdMap = context.skuToVariantId;
		Map<String, List<Integer>> metafieldIds = context.variantSkuToMetafieldIds;

		// 🧩 Check if the product is a simple product (single variant, no options)
		boolean isSimpleProduct = 
			    product.getVariants().size() == 1 &&
			    (product.getVariants().get(0).getOption_values() == null ||
			     product.getVariants().get(0).getOption_values().isEmpty());
		
		if (!isSimpleProduct) {
			// 🔄 Update variant details only if the product has multiple options.
			// ⚠️ For simple products (1 variant, no options), we skip this step to avoid overwriting the single variant.
			// Any updates (e.g. dimensions or SKU-level attributes) are handled via metafields instead.
			
			List<JSONObject> variantUpdatePayloads = prepareVariantUpdatePayloads(product, variantIdMap,
					existingProductId);
			bigCommerceRepository.updateVariantsInBatches(variantUpdatePayloads);
		}
		
		// 🧽 Collect existing variant metafields to be deleted
		List<Integer> allMetafieldsToDelete = new ArrayList<>();
		for (Variant variant : product.getVariants()) {
			String sku = variant.getSkuNumber();
			List<Integer> metaIds = metafieldIds.get(sku);
			if (metaIds != null) {
				allMetafieldsToDelete.addAll(metaIds);
			}
		}
		
		// 🗑️ Clean up all existing variant-level metafields before rewriting
		if (!allMetafieldsToDelete.isEmpty()) {
			bigCommerceRepository.deleteVariantMetafields(allMetafieldsToDelete);
		}
		
		// 🧱 Build variant-level custom attribute maps by locale and static fields
		Map<String, Map<String, JSONObject>> skuVariantCustomAttributesMap = bigCommerceProductMapper
				.buildLocalizedVariantAttributeMap(product, attribtueLabelMap);
		
		
		Map<String, JSONObject> predefinedvariantAttrMap = bigCommerceProductMapper
				.mapPredefinedVariantAttributesToMetafields (product);

		
		// 🇫🇷 Build localized option label metafields for French
		JSONArray frenchOptionLabelMetafield = bigCommerceProductMapper
				.buildFrenchOptionLabelMetafields (product.getVariants(), variantIdMap);
	
		// 🌍 Build localized variant metafields (e.g., "displayName" per language)
		Map<String, JSONArray> localizedVariantMetafields = bigCommerceProductMapper
				.buildLocalizedVariantMetafields(skuVariantCustomAttributesMap, variantIdMap, attribtueLabelMap);
		
		// 🧾 Build static variant metafields like weight, dimensions, etc.
		JSONArray staticMetafields = bigCommerceProductMapper.buildStaticVariantMetafields(predefinedvariantAttrMap,
				variantIdMap);

		// 💾 Upload all variant metafields to BigCommerce in batch
		bigCommerceRepository.setVariantMetafieldsInBatches(localizedVariantMetafields, staticMetafields,frenchOptionLabelMetafield);
		
	}

	/**
	 * Creates variant-level metafields for a newly created product.
	 * Assumes variants have already been created, and their IDs are known.
	 *
	 * Handles:
	 * - Localized attribute metafields
	 * - Predefined/static variant attribute metafields
	 * - French option label metafields
	 *
	 * @param product the product definition
	 * @param context the shared sync context
	 * @param attributeLabelMap localized label map by attribute and locale
	 * @param skuToVariantId variant ID map from product creation (SKU → variantId)
	 */
	public void createVariantMetafieldsOnly(
	        Product product,
	        ProductSyncContext context,
	        Map<String, Map<String, String>> attributeLabelMap,
	        Map<String, Integer> skuToVariantId) throws Exception {

	    // ✅ Update context with new variant IDs
	    context.skuToVariantId.putAll(skuToVariantId);

	    // 🌍 Build localized attributes for each SKU/variant
	    Map<String, Map<String, JSONObject>> skuVariantCustomAttributesMap =
	            bigCommerceProductMapper.buildLocalizedVariantAttributeMap(product, attributeLabelMap);

	    Map<String, JSONObject> predefinedVariantAttrMap =
	            bigCommerceProductMapper.mapPredefinedVariantAttributesToMetafields(product);

	    JSONArray frenchOptionLabelMetafields =
	            bigCommerceProductMapper.buildFrenchOptionLabelMetafields(product.getVariants(), skuToVariantId);

	    Map<String, JSONArray> localizedVariantMetafields =
	            bigCommerceProductMapper.buildLocalizedVariantMetafields(
	                    skuVariantCustomAttributesMap, skuToVariantId, attributeLabelMap);

	    JSONArray staticMetafields =
	            bigCommerceProductMapper.buildStaticVariantMetafields(predefinedVariantAttrMap, skuToVariantId);

	    // 💾 Upload all variant metafields
	    bigCommerceRepository.setVariantMetafieldsInBatches(
	            localizedVariantMetafields,
	            staticMetafields,
	            frenchOptionLabelMetafields
	    );
	}


	// Helper method to prepare variant batch update payloads
	private List<JSONObject> prepareVariantUpdatePayloads(
			Product product, 
			Map<String, Integer> variantIdMap, 
			int productId) throws Exception {

	    List<JSONObject> variantUpdatePayloads = new ArrayList<>();

	   

	    for (Variant variant : product.getVariants()) {
	        String sku = variant.getSkuNumber();
	        Integer variantId = variantIdMap.get(sku);

	        JSONObject variantJson = new JSONObject();

	        if (variantId != null) {
	        	 // ✅ Variant exists in BigCommerce — reference it directly using its ID
	            variantJson.put("id", variantId);
			} else {
				
				  // 🆕 Variant is new — provide product ID and SKU so BC can link it
				variantJson.put("product_id", productId);
				variantJson.put("sku", sku);
				
				// 🎯 Resolve product options (e.g., Color, Size) for the product
				ProductOptionResolution productOption = bigCommerceRepository.getResolvedProductOptions(productId);
				JSONArray resolvedOptionValuesArray = new JSONArray();
				
				 for (OptionValue ov : variant.getOption_values()) {
		                String displayName = ov.getOption_name_en();
		                String label = ov.getOption_value_en();

		                // 🔍 Retrieve option ID by name
		                Integer optionId = productOption.optionNameToId.get(displayName);
		                if (optionId == null) {
		                    throw new IllegalStateException("❌ Missing option_id for: " + displayName);
		                }

		                // 🔁 Check if option value already exists
		                Map<String, Integer> labelMap = productOption.labelToValueIdMap.get(displayName);
		                Integer valueId = (labelMap != null) ? labelMap.get(label) : null;

		                if (valueId == null) {
		                	// ➕ Option value does not exist — create it
		                    JSONObject payload = new JSONObject();
		                    payload.put("label", label);
		                    payload.put("sort_order", 0);
		                    payload.put("is_default", false);
		                    payload.put("value_data", new JSONObject());

		                    valueId = bigCommerceRepository.createProductVariantOptionValue(payload, productId, optionId);

		                    if (labelMap == null) {
		                        labelMap = new HashMap<>();
		                        productOption.labelToValueIdMap.put(displayName, labelMap);
		                    }
		                    labelMap.put(label, valueId);
		                }
		                // 🧩 Add resolved option value to variant payload
		                JSONObject optionValue = new JSONObject();
		                optionValue.put("option_id", optionId);
		                optionValue.put("id", valueId);
		                resolvedOptionValuesArray.put(optionValue);
		            }

		            variantJson.put("option_values", resolvedOptionValuesArray);
		        
	        
				

				
			}

	        // 📦 Set variant-level physical attributes
	        if (variant.getPaWeight() > 0) variantJson.put("weight", variant.getPaWeight());
	        if (variant.getPaWidth() > 0) variantJson.put("width", variant.getPaWidth());
	        if (variant.getPaHeight() > 0) variantJson.put("height", variant.getPaHeight());
	        if (variant.getPaLength() > 0) variantJson.put("depth", variant.getPaLength());

	        // 🏷️ Add standard identifiers (optional if present)
	        if (variant.getPaUPC() != null && !variant.getPaUPC().isEmpty()) variantJson.put("upc", variant.getPaUPC());
	        if (variant.getPaVendorNumber() != null && !variant.getPaVendorNumber().isEmpty()) variantJson.put("gtin", variant.getPaVendorNumber());
	        if (variant.getPaVendorPartNumber() != null && !variant.getPaVendorPartNumber().isEmpty()) variantJson.put("mpn", variant.getPaVendorPartNumber());

	        // ✅ Add to final list of variant updates
	        variantUpdatePayloads.add(variantJson);
	    }

	    return variantUpdatePayloads;
	}


	public int countNewProductsInFeed(List<Product> products, ProductSyncContext context) {
		if (products == null || products.isEmpty())
			return 0;

		int newProductCount = 0;
		for (Product product : products) {
			String productNumber = product.getProductNumber();
			if (!context.productNumberToProductId.containsKey(productNumber)) {
				newProductCount++;
			}
		}
		return newProductCount;
	}
}
