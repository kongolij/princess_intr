package com.bigcommerce.imports.catalog.product.service;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.ProductCreationResult;
import com.bigcommerce.imports.catalog.product.mapper.BigCommerceProductMapper;
import com.bigcommerce.imports.catalog.product.repository.BigCommerceRepository;
import com.bigcommerce.imports.catalog.service.BigCommerceService;

@Component
public class BigCommerceProductService {

	private final BigCommerceRepository bigCommerceRepository;
	private final BigCommerceProductMapper bigCommerceProductMapper;
	
	private final BigCommerceService bigCommerceCategoryService;
			
	
	public BigCommerceProductService(BigCommerceRepository bigCommerceRepository,
			BigCommerceProductMapper bigCommerceProductMapper,
			BigCommerceService bigCommerceCategoryService
			) {
		this.bigCommerceRepository=bigCommerceRepository;
		this.bigCommerceProductMapper=bigCommerceProductMapper;
		this.bigCommerceCategoryService=bigCommerceCategoryService;
		
	}
	
	public  void importProducts(List<Product> products,  String locale) throws Exception {
			 
		 Map<String, Integer> categoriesMAp = bigCommerceCategoryService
					.getBCExternalToInternalCategoryMap("en");
		 
		// Process and create each product individually
		 if (!(products== null || products.isEmpty()) ) {
		    for (Product product : products) {
		        
		            // Map product to BigCommerce JSON format
		            JSONObject productJson = bigCommerceProductMapper.mapProductToBigCommerce(product, categoriesMAp, locale);
		            JSONArray  productCustomAttributeJson = bigCommerceProductMapper.mapProductToBigCommerceCustomAttr(product);
		            Map<String, Map<String, JSONObject>>  varaintCustomAttributeJson = bigCommerceProductMapper.mapVaraintAttribtuesToBigCommerceCustomAttr(product);
		            // Wrap the product JSON in a JSONArray to match the expected parameter in `createProductWithVariants`
		            JSONArray productsJsonArray = new JSONArray();
		            productsJsonArray.put(productJson);

		            ProductCreationResult pr = bigCommerceRepository.createProductWithVariants(locale, productJson);

		            System.out.println("Created product with category ID: " + pr.getProductId());
		            if (pr.getProductId()!=0) {
		            	
//		            	 Map<String, Map<String, JSONObject>> localeSkuAttributeMap,
//		     	        Map<String, Integer> skuToVariantIdMap
		            	Map<String,JSONArray> varaintMetafileds = bigCommerceProductMapper.buildLocaleMetafields(varaintCustomAttributeJson,pr.getSkuToVariantIdMap() );
//		            	JSONObject productImageJson = bigCommerceProductMapper.mapProductToBigCommerceImage(product,pr.getProductId());
//		            	bigCommerceRepository.createProductImages(locale, productImageJson, productId);
                    	bigCommerceRepository.assignProductToChannel(locale, pr.getProductId(), BigCommerceStoreConfig.NEXT_PUBLIC_BC_CHANNEL_ID);
		            	bigCommerceRepository.setProductCustomFields(locale, productCustomAttributeJson, pr.getProductId());
		            	
		            	bigCommerceRepository.setVariantMetafieldsInBatches(locale, varaintMetafileds);
		            	
		            }
		        
//		    }
		    
		 }
//		 products.stream()
//	        .filter(product -> product.getProductType().equalsIgnoreCase(type)) // Filter by product type
//	        .map(product -> bigCommerceProductMapper.mapProductToBigCommerce(product, categoryNames, type, locale)) 
//	        .forEach(json -> {
//	            // Here you can handle each JSONObject, e.g., printing or further processing
//	            System.out.println(json.toString(2)); // Pretty-print each JSON object
//	        });
	}
		 }
}
