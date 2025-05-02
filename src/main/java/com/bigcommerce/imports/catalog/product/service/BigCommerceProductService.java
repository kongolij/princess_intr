package com.bigcommerce.imports.catalog.product.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
import com.bigcommerce.imports.catalog.constants.CommonConstants;
import com.bigcommerce.imports.catalog.product.dto.Asset;
import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.ProductCreationResult;
import com.bigcommerce.imports.catalog.product.dto.Variant;
import com.bigcommerce.imports.catalog.product.mapper.BigCommerceProductMapper;
import com.bigcommerce.imports.catalog.product.repository.BigCommerceRepository;
import com.bigcommerce.imports.catalog.service.BigCommerceService;

import io.micrometer.common.util.StringUtils;

@Component
public class BigCommerceProductService {

	private final BigCommerceRepository bigCommerceRepository;
	private final BigCommerceProductMapper bigCommerceProductMapper;

	private final BigCommerceService bigCommerceCategoryService;

	public BigCommerceProductService(BigCommerceRepository bigCommerceRepository,
			BigCommerceProductMapper bigCommerceProductMapper, BigCommerceService bigCommerceCategoryService) {
		this.bigCommerceRepository = bigCommerceRepository;
		this.bigCommerceProductMapper = bigCommerceProductMapper;
		this.bigCommerceCategoryService = bigCommerceCategoryService;

	}

	public void importProducts(List<Product> products, String locale) throws Exception {

		Map<String, Integer> categoriesMAp = bigCommerceCategoryService.getBCExternalToInternalCategoryMap("en");

		// Process and create each product individually
		if (!(products == null || products.isEmpty())) {
			for (Product product : products) {

				// Map product to BigCommerce JSON format
				JSONObject productJson = bigCommerceProductMapper.mapProductToBigCommerce(product, categoriesMAp,
						locale);
				
				
				JSONArray productCustomAttributeJson = bigCommerceProductMapper
						.mapProductToBigCommerceCustomAttr(product);
				
				JSONObject externalIdField = new JSONObject();
				externalIdField.put("name", "externalProductNumber");
				externalIdField.put("value", product.getProductNumber());
				productCustomAttributeJson.put(externalIdField);
				
				
				
				Map<String, Map<String, JSONObject>> varaintCustomAttributeJson = bigCommerceProductMapper
						.mapVaraintAttribtuesToBigCommerceCustomAttr(product);
				
				Map<String, JSONObject> varaintStaticAttributeJson = bigCommerceProductMapper.mapToVariantAttrToMetadata(product);
				
				// Wrap the product JSON in a JSONArray to match the expected parameter in
				// `createProductWithVariants`
				JSONArray productsJsonArray = new JSONArray();
				productsJsonArray.put(productJson);

				ProductCreationResult pr = bigCommerceRepository.createProductWithVariants(locale, productJson);

				System.out.println("Created product with category ID: " + pr.getProductId());
				if (pr.getProductId() != 0) {

					Map<String, JSONArray> varaintMetafileds = bigCommerceProductMapper
							.buildLocaleMetafields(varaintCustomAttributeJson, pr.getSkuToVariantIdMap());
					
					Map<String, JSONArray> productMetafileds = bigCommerceProductMapper
							.buildProductLocaleMetafields(varaintCustomAttributeJson, pr);
					
					JSONArray varaintStMetafileds= bigCommerceProductMapper.buildStaticVariantMetafields(varaintStaticAttributeJson,pr.getSkuToVariantIdMap());
				
					//
					bigCommerceRepository.assignProductToChannel(locale, pr.getProductId(),
							BigCommerceStoreConfig.BC_CHANNEL_ID);
					
					
					bigCommerceRepository.setProductCustomFields(locale, productCustomAttributeJson, pr.getProductId());

					if (product.getVariants().size()==1 && (product.getVariants().get(0).getOption_values()==null)) {
						bigCommerceRepository.setProductMetafieldsInBatches( productMetafileds);
					}else {
					    bigCommerceRepository.setVariantMetafieldsInBatches(locale, varaintMetafileds, varaintStMetafileds);
					}
					uploadProductAssets(pr.getProductId(), product );
					// PDF not supported 
                    //uploadVariantAssets(product, pr.getSkuToVariantIdMap(), pr.getProductId() );

				}
			}

		}
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
	
	private void uploadVariantAssets(Product product, Map<String, Integer> skuToVariantIdMap, int productId) {
	    if (product.getVariants() == null) return;

	    for (Variant variant : product.getVariants()) {
	        List<Asset> assets = variant.getAssets();
	        if (assets == null || assets.isEmpty()) continue;

	        String sku = variant.getSkuNumber();
	        Integer variantId = skuToVariantIdMap.get(sku);
	        if (variantId == null) continue;

	        for (Asset asset : assets) {
	            if (!"manual".equalsIgnoreCase(asset.getType())) continue;

	            String fileName = asset.getPaDocumentsFileName();
	            if (StringUtils.isEmpty(fileName) || "NULL".equalsIgnoreCase(fileName)) continue;

	            String imageUrl = CommonConstants.VARIANT_MANUAL_URL + fileName;

	            try {
	                byte[] imageBytes = downloadImageBytes(imageUrl);
	                if (imageBytes != null) {
	                    bigCommerceRepository.uploadVariantImageToBigCommerce(productId, variantId, fileName, imageBytes);
	                    System.out.println("✅ Uploaded variant image for SKU " + sku + ": " + fileName);
	                } else {
	                    System.err.println("⚠️ Failed to download variant image: " + imageUrl);
	                }
	            } catch (Exception e) {
	                System.err.println("❌ Error uploading image for variant SKU " + sku + ": " + e.getMessage());
	                e.printStackTrace();
	            }
	        }
	    }
	}
	
	private byte[] downloadImageBytes(String imageFileName) {
	    try {
	        String imageUrl =  imageFileName;
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
	
}
