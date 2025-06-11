package com.bigcommerce.imports.catalog.product.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.bigcommerce.imports.catalog.product.inventory.dto.SkuVariantInfo;
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

	public void importProducts(Map<String, Product> productMap, String locale,
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
		System.out.printf("✅ Fetched %d brands in %.2f seconds%n", brandMap.size(), durationInSeconds);

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
		if (!(productMap == null || productMap.isEmpty())) {
			
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
			Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap = new HashMap<>();
			
			for (Map.Entry<String, Product> entry : productMap.entrySet()) {
//			for (Product product : products) {

				
		        Product product = entry.getValue();
				String productNumber = product.getProductNumber();
				
//				//has replaces
//				if (!"PA1000001804".equals(productNumber)) {
//				//variant has no options
//				//if (!"PA0003461449".equals(productNumber)) {
     		    if (!"PA0008677999".equals(productNumber)) {
     		    	continue;
			    }
//    		    if (!"PA1000001804".equals(productNumber)) {
// 	       	    	continue;
//     		    }
	
				

				Integer existingProductId = productSyncContext.productNumberToProductId.get(productNumber);

				Variant firstVariant = product.getVariants() != null && !product.getVariants().isEmpty()
						? product.getVariants().get(0)
						: null;

				// 🚀 Only consider if incoming product is a single variant with NO options
				boolean incomingIsSimple = product.getVariants().size() == 1 &&
				    (firstVariant.getOption_values() == null || firstVariant.getOption_values().isEmpty());

				// 🚀 Check if this product already has variants in BigCommerce (not just in our local object)
				boolean existingHasVariants = false;
				boolean existingVariantHasOptions = false;
				if (firstVariant != null) {
				    Integer variantId = productSyncContext.skuToVariantId.get(firstVariant.getSkuNumber());
				    existingHasVariants = (variantId != null && variantId > 0);

				    // ✅ Check if existing variant has options in BC (using productSyncContext’s map)
				    Boolean hasOptions = productSyncContext.skuHasOptionsMap.get(firstVariant.getSkuNumber());
				    existingVariantHasOptions = (hasOptions != null && hasOptions);
				}

				// 🔄 If incoming is simple, but existing BC variant has options, delete and recreate
				if (incomingIsSimple && existingHasVariants && existingVariantHasOptions) {
				    System.out.printf(
				        "🛑 Converting product '%s' (ID: %d) from variant-based (with options) to simple. Deleting to recreate as simple.%n",
				        productNumber, existingProductId);

				    try {
				        bigCommerceRepository.deleteProductsInBatch(Collections.singletonList(existingProductId));
				        System.out.printf("✅ Deleted product '%s' (ID: %d).%n", productNumber, existingProductId);

				        // Recreate as a simple product
				        createNewProduct(
				            product,
				            productSyncContext,
				            attribtueLabelMap,
				            categoriesMap,
				            brandMap,
				            unresolvedReferencesMap,
				            variantsUnresolvedReferencesMap
				        );

				        continue; // Skip update flow
				    } catch (Exception e) {
				        System.err.printf("❌ Failed to delete product '%s' (ID: %d): %s%n",
				            productNumber, existingProductId, e.getMessage());
				        e.printStackTrace();
				    }
				}
		     
		            
		        // ✅ Product already exists – update flow
		        if (existingProductId != null) {

					updateExistingProduct(
					        product,
					        existingProductId,
					        productSyncContext,
					        attribtueLabelMap,
					        categoriesMap,
					        brandMap,
					        unresolvedReferencesMap,
					        variantsUnresolvedReferencesMap);
				}else {
					 // 🆕 Create flow for new products	
		            createNewProduct(
		            		 product,
						     productSyncContext,
						     attribtueLabelMap,
						     categoriesMap,
						     brandMap,
						     unresolvedReferencesMap,
						     variantsUnresolvedReferencesMap
		            );
				}


			}

			// try to set unresilved referendes 
			postProcessUnresolvedReferences(
			        productMap,
			        unresolvedReferencesMap,
			        variantsUnresolvedReferencesMap,
			        productSyncContext,
			        attribtueLabelMap,
			        categoriesMap,
			        brandMap);
			
	       

		}
	}
	
	private void updateExistingProduct(
			Product product,
		    int existingProductId,
		    ProductSyncContext context,
		    Map<String, Map<String, String>> attribtueLabelMap,
		    Map<String, Integer> categoriesMap,
		    Map<String, Integer> brandMap,
		    Map<Integer, List<ProductRefernce>> unresolvedReferencesMap,
		    Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap) throws Exception {
		
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
				existingProductId);

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
			bigCommerceRepository.deleteProductMetafieldsInBatches(existingMetafieldIds);

		}

		// 🧩 Update product custom fields and localized/dynamic metafields
		updateProductFields(product, existingProductId, attribtueLabelMap, context.productNumberToProductId, unresolvedReferencesMap);
        
		// 🔄 Update all variant-level data (custom fields, metafields, images, etc.)
		updateVariantDetails(product, context, attribtueLabelMap, existingProductId, variantsUnresolvedReferencesMap);
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
		     Map<Integer, List<ProductRefernce>> unresolvedReferencesMap,
		     Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap) throws Exception {

		// 🛠️ Map the full product definition (basic info, brand, categories) to BigCommerce JSON
		JSONObject productJson = bigCommerceProductMapper.mapProductToBigCommerce(
			    product,
			    categoriesMap,
			    brandMap,
			    productSyncContext,
			    unresolvedReferencesMap
			);
	   
	    // 🏷️ Build product-level custom fields (e.g., vendor code, MPN, clearance)
		ProductCreationResult creationResult = null;
		try {
			// 🆕  Attempt to create the product
			 creationResult = bigCommerceRepository.createProductWithVariants(productJson);
		} catch (Exception e) {
			// ⚠️  If duplicate error (409), attempt to find and update existing product
			if (e.getMessage() != null && e.getMessage().contains("409")) {
				String productSku = productJson.optString("sku", "UNKNOWN");
				if (!"UNKNOWN".equals(productSku)) {
					// 🔍 Try to get existing product by SKU
					JSONObject existingProduct = bigCommerceRepository.getProductBySKU(productSku);
					if (existingProduct != null) {
						int existingProductId = existingProduct.optInt("id", -1);
						try {

							// 1 🟢 Populate missing cleanup info for disabled product
							List<Integer> customFieldIds = bigCommerceRepository
									.getCustomFieldIdsForProduct(existingProductId);
							if (!customFieldIds.isEmpty()) {
								productSyncContext.productIdToCustomFieldIds.put(existingProductId, customFieldIds);
							}

							List<Integer> productMetafieldIds = bigCommerceRepository
									.getProductMetafieldIds(existingProductId);
							if (!productMetafieldIds.isEmpty()) {
								productSyncContext.productIdToMetafieldIds.put(existingProductId, productMetafieldIds);
							}

							// 2️ Update variant-level variant IDs and variant metafield IDs
							JSONArray variants = existingProduct.optJSONArray("variants");
							if (variants != null) {
								for (int i = 0; i < variants.length(); i++) {
									JSONObject variantJson = variants.getJSONObject(i);
									int variantId = variantJson.optInt("id", -1);
									String sku = variantJson.optString("sku", null);

									if (sku != null && variantId > 0) {
										// Update variant ID map
										productSyncContext.skuToVariantId.put(sku, variantId);

										// Fetch and update variant metafield IDs

										List<Integer> variantMetafields = bigCommerceRepository
												.getMetafieldIdsForVariant(existingProductId,variantId);
										if (!variantMetafields.isEmpty()) {
											productSyncContext.variantSkuToMetafieldIds.put(sku, variantMetafields);
										}
									}
								}
							}

						} catch (Exception ex) {
							System.err.printf("⚠️ Failed to fetch ");
							ex.printStackTrace();
						}
					    // 🔄 Proceed with update flow
						System.out.printf("🔄 Switching to update flow for product '%s' (ID: %d)%n", product.getProductNumber(), existingProductId);
						updateExistingProduct(
								product,
								existingProductId,
								productSyncContext,
								attributeLabelMap,
								categoriesMap,
								brandMap,
								unresolvedReferencesMap,
								variantsUnresolvedReferencesMap);
						return; // ➡️ Exit creation flow after update
					}
				}
			}
			 // 🚫 4Re-throw any other unexpected errors
			throw e;
		}

		// 🆕  New product created – handle post-creation tasks
	    int productId = creationResult.getProductId();
	    Map<String, Integer> skuToVariantIdMap = creationResult.getSkuToVariantIdMap();
	    System.out.printf("✅ Created product '%s' with ID: %d%n", product.getProductNumber(), productId);
	    
	    if (productId == 0) {
	    	System.err.printf("❌ Failed to create product '%s' (no product ID returned).%n", product.getProductNumber());
	        return;
	    }

	     // 🌐 Assign to channel
	    bigCommerceRepository.assignProductToChannel( productId, BigCommerceStoreConfig.BC_CHANNEL_ID);

	     // 📦  Upload product-level assets
	    Map<String, String> uploadedProductAssets = uploadProductsAssets(productId, product);
	    
	     // 📝  Update product fields (like custom fields, references, metafields)
        updateProductFields(
        		product, 
        		productId, 
        		attributeLabelMap, 
        		productSyncContext.productNumberToProductId, 
        		unresolvedReferencesMap);
        
         // 📝  Create variant-level metafields (localized, static, and label data)
        createVariantMetafieldsOnly(
        		product, 
        		productSyncContext, 
        		attributeLabelMap, 
        		skuToVariantIdMap,
        		variantsUnresolvedReferencesMap);
       
	
        // 📸 Upload variant-level assets
	    uploadVariantAssets(product, skuToVariantIdMap, productId, uploadedProductAssets);
	}


	public void updateProductFields(
			Product product, 
			int productId, 
			Map<String, Map<String, String>> attribtueLabelMap,
			Map<String, Integer> productNumberToProductId,
			Map<Integer, List<ProductRefernce>> unresolvedReferencesMap) throws Exception {
		
		// 🏷️ Map predefined (non-localized) product-level attributes (e.g., MPN, UPC, vendor code) to custom fields	                                           
		JSONArray customFields = BigCommerceProductMapper.mapPredefinedProductAttributesToCustomFields(
				product,
				productNumberToProductId,
				productId);
		bigCommerceRepository.setProductCustomFields(customFields, productId);

		 // 🌍 Map localized product attributes (e.g., name, description in EN/FR) to metafields
		JSONArray metafields = BigCommerceProductMapper.mapProductToMetafields(
				product, 
				productId,
				productNumberToProductId,
				attribtueLabelMap,
				unresolvedReferencesMap);
		
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
		
	 
	   
	   if (product.getVariants()==null || product.getVariants().size() == 1) {
	        Variant singleVariant = product.getVariants().get(0);
	        if (singleVariant.getOption_values() == null || singleVariant.getOption_values().isEmpty()) {
	            System.out.printf("⚠️ Skipping image upload for single-variant simple product (SKU: %s).%n", singleVariant.getSkuNumber());
	            return;
	        }
	    }
		 
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
			int existingProductId,
			Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap) throws Exception {
		
		// 🔍 Look up variant ID and existing metafields from context
		Map<String, Integer> variantIdMap = context.skuToVariantId;
		Map<String, List<Integer>> metafieldIds = context.variantSkuToMetafieldIds;
		
		
		// 🔄 Sort variants by numeric skuSeq (lowest to highest)
		List<Variant> sortedVariants = product.getVariants().stream()
				.sorted(Comparator.comparingInt(variant -> {
			try {
				return Integer.parseInt(variant.getSkuSeq() != null ? variant.getSkuSeq() : "9999");
			} catch (NumberFormatException e) {
				return 9999; // fallback if skuSeq is invalid
			}
		})).toList();


		// 🧩 Check if the product is a simple product (single variant, no options)
		boolean isSimpleProduct = 
			    product.getVariants().size() == 1 &&
			    (product.getVariants().get(0).getOption_values() == null ||
			     product.getVariants().get(0).getOption_values().isEmpty());
		
		if (!isSimpleProduct) {
			// 🔄 Update variant details only if the product has multiple options.
			// ⚠️ For simple products (1 variant, no options), we skip this step to avoid overwriting the single variant.
			// Any updates (e.g. dimensions or SKU-level attributes) are handled via metafields instead.
			
			List<JSONObject> variantUpdatePayloads = prepareVariantUpdatePayloads(
					sortedVariants, 
					variantIdMap,
					existingProductId, 
					context, 
					variantsUnresolvedReferencesMap);
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
		
		//check this out
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
		
		JSONArray relatedVariants = bigCommerceProductMapper.buildRelatedVariantMetafields(
				product.getVariants(),
		        context,
		        variantsUnresolvedReferencesMap
		);
		

		// 💾 Upload all variant metafields to BigCommerce in batch
		bigCommerceRepository.setVariantMetafieldsInBatches(
				localizedVariantMetafields, 
				staticMetafields,
				frenchOptionLabelMetafield, 
				relatedVariants);
		
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
	        Map<String, Integer> skuToVariantId,
	        Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap) throws Exception {

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

	    JSONArray relatedVariants = bigCommerceProductMapper.buildRelatedVariantMetafields(
				product.getVariants(),
		        context,
		        variantsUnresolvedReferencesMap
		);
	    
	    // 💾 Upload all variant metafields
	    bigCommerceRepository.setVariantMetafieldsInBatches(
	            localizedVariantMetafields,
	            staticMetafields,
	            frenchOptionLabelMetafields,
	            relatedVariants
	            
	    );
	}


	// Helper method to prepare variant batch update payloads
	private List<JSONObject> prepareVariantUpdatePayloads(
			List<Variant> sortedVariants, 
			Map<String, Integer> variantIdMap, 
			int productId,
			ProductSyncContext context,
			Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap) throws Exception {

	    List<JSONObject> variantUpdatePayloads = new ArrayList<>();


	 		for (Variant variant : sortedVariants) {
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
		                    
		                    String seqString = ov.getSeq(); // assuming this is your string sequence
		                    int sortOrder = 9999; // default fallback value
		                    try {
		                        sortOrder = Integer.parseInt(seqString);
		                    } catch (NumberFormatException e) {
		                        
		                    }
		                    payload.put("sort_order", sortOrder);
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
	
	@SuppressWarnings("unused")
	private void postProcessUnresolvedReferences(
	        Map<String, Product> productMap,
	        Map<Integer, List<ProductRefernce>> unresolvedReferencesMap,
	        Map<Integer, List<ProductRefernce>> variantsUnresolvedReferencesMap,
	        ProductSyncContext productSyncContext,
	        Map<String, Map<String, String>> attributeLabelMap,
	        Map<String, Integer> categoriesMap,
	        Map<String, Integer> brandMap) throws Exception {

//	    System.out.println("\n🟢 --- Unresolved Product References ---");
//	    for (Map.Entry<Integer, List<ProductRefernce>> entry : unresolvedReferencesMap.entrySet()) {
//	        System.out.printf("Unresolved Product References : %d\n", entry.getKey());
//	        for (ProductRefernce ref : entry.getValue()) {
//	            System.out.printf(" cccccczxxx  -> Reference to product '%s' (type: %s, sku: %s)\n",
//	                    ref.getParentProduct(), ref.getType(), ref.getSkuNumber());
//	        }
//	    }
		
		

	    System.out.println("\n🟡 --- Unresolved Variant References ---");
	    List<String> unresolvedSkus = new ArrayList<>();
	    for (Map.Entry<Integer, List<ProductRefernce>> entry : variantsUnresolvedReferencesMap.entrySet()) {
	        Integer productId = entry.getKey();
	        List<ProductRefernce> references = entry.getValue();
	        System.out.printf("Unresolved Variant : %d\n", productId);

	        for (ProductRefernce ref : references) {
	            List<String> possibleSkus = new ArrayList<>();
	            if (ref.getProductNumber() != null && !ref.getProductNumber().isEmpty()) {
	                possibleSkus.add(ref.getProductNumber());
	            } else if (ref.getParentProduct() != null && !ref.getParentProduct().isEmpty()) {
	                possibleSkus.add(ref.getParentProduct());
	            }
	            if (ref.getSkuNumber() != null && !ref.getSkuNumber().isEmpty()) {
	                possibleSkus.add(ref.getSkuNumber());
	            }

	            // Query BC once for all these SKUs
	            Map<String, SkuVariantInfo> resolvedSkuMap =
	                    bigCommerceRepository.getVariantProductAndIdsBySkusUpdated(possibleSkus);

	            // Find the first resolved SKU ➜ get productId
	            Integer existingProductId = null;
	            for (String sku : possibleSkus) {
	                SkuVariantInfo info = resolvedSkuMap.get(sku);
	                if (info != null) {
	                    existingProductId = info.getProductId();
	                    System.out.printf("✅ SKU: %s resolved to ProductId: %d (variantId: %d)\n",
	                            sku, info.getProductId(), info.getVariantId());
	                    break;
	                }
	            }

	            if (existingProductId != null) {
	                Product productForUpdate = productMap.get(productId.toString());
	                // Log the update intention
	                System.out.printf("  XXXXXXXXX   🔧 Updating product in BC with ID: %d%n", existingProductId);

	                if (productForUpdate != null) {
	                    updateExistingProduct(
	                            productForUpdate,
	                            existingProductId,
	                            productSyncContext,
	                            attributeLabelMap,
	                            categoriesMap,
	                            brandMap,
	                            unresolvedReferencesMap,
	                            variantsUnresolvedReferencesMap);
	                } else {
	                    System.out.printf("⚠️ No product found for productId: %d\n", productId);
	                }
	            } else {
	                System.out.println("⚠️ None of the possible SKUs resolved in BC for this reference.");
	            }

	            System.out.printf("  ddd ddd -> Reference to product '%s' (type: %s, sku: %s)\n",
	                    ref.getParentProduct(), ref.getType(), ref.getSkuNumber());

	            if (ref.getSkuNumber() != null && !ref.getSkuNumber().isEmpty()) {
	                unresolvedSkus.add(ref.getSkuNumber());
	            }
	        }
	    }
	}

}
