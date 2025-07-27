package com.constructor.index;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.constructor.client.ConstructorApis;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.mapper.ConstructorJsonlProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class IndexVariantsApi implements CommandLineRunner {

	
	private final BigCommerceGraphQlService bigCommerceGraphQlService;
	private final ConstructorJsonlProductMapper constructorJsonlProductMapper;
	private final  ConstructorApis constructorApis;

	public IndexVariantsApi(ConstructorApis constructorApis,
			BigCommerceGraphQlService bigCommerceGraphQlService,
			ConstructorJsonlProductMapper constructorJsonlProductMapper) {
		this.constructorApis = constructorApis;
		this.bigCommerceGraphQlService = bigCommerceGraphQlService;
		this.constructorJsonlProductMapper = constructorJsonlProductMapper;

	}

	@Override
	public void run(String... args) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		List<ProductGraphQLResponse.Product> products = bigCommerceGraphQlService.getAllProducts();
		System.out.println("Fetched " + products.size() + " products.");
		
	    ObjectMapper mapper = new ObjectMapper();
	    List<ObjectNode> batchVariations = new ArrayList<>();
	    int batchSize = 20;
	    int batchCount = 0;
	    int totalVariants = 0;
	   
	    
	    for (int i = 0; i < products.size(); i++) {
	    	ProductGraphQLResponse.Product product = products.get(i);
	    
	    	
			String variantPayLoad = constructorJsonlProductMapper.mapVariantToJsonlLine(product, "en");
		
	        if (variantPayLoad == null) continue;

	        ObjectNode payloadNode = (ObjectNode) mapper.readTree(variantPayLoad);
	        if (payloadNode.has("variations")) {
	            for (var variationNode : payloadNode.withArray("variations")) {
	                batchVariations.add((ObjectNode) variationNode);
	            }
	        }

	        boolean isLast = (i == products.size() - 1);
	        if (batchVariations.size() > 0 && (batchVariations.size() >= batchSize || isLast)) {
	        	batchCount++;
	            ObjectNode wrapper = mapper.createObjectNode();
	            wrapper.set("variations", mapper.valueToTree(batchVariations));
	            String batchedPayload = mapper.writeValueAsString(wrapper);

	            constructorApis.createOrReplaceVariations(batchedPayload);  // API call
	            System.out.println("Batch #" + batchCount + " pushed:");
	            System.out.println("  - Variants in batch: " + batchVariations.size());

	            totalVariants += batchVariations.size();
	            batchVariations.clear(); // Reset for next batch
	            
	            
	        }
	         
		}
	    
	    long durationMs = System.currentTimeMillis() - startTime;
	    double durationSeconds = durationMs / 1000.0;

	    System.out.println(" All batches completed.");
	    System.out.println(" Total products processed: " + products.size());
	    System.out.println(" Total variants pushed: " + totalVariants);
	    System.out.println(" Total time: " + durationSeconds + " seconds");

	    System.exit(0);

	}

}
