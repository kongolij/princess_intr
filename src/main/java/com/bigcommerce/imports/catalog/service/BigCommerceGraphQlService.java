package com.bigcommerce.imports.catalog.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.dto.ProductListGraphQLResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BigCommerceGraphQlService {

	public ProductGraphQLResponse.Product getProductById(int productId) throws Exception {
		String query = """
				    query GetProduct {
				      site {
				        product(entityId: %d) {
				          entityId
				          name
				          prices {
				            price { currencyCode value }
				            salePrice { currencyCode value }
				          }
				          images {
				            edges { node { urlOriginal } }
				          }
				          customFields {
				            edges { node { entityId name value } }
				          }
				          metafields(namespace: "product_attributes") { edges { node { entityId key value } } }
				          variants {
				            edges {
				              node {
				                sku
				                isPurchasable
				                height { value unit }
				                width { value unit }
				                weight { value unit }
				                prices {
				                  price { currencyCode value }
				                  salePrice { currencyCode value }
				                }
				                defaultImage { urlOriginal }
				                inventory {
				                  isInStock
				                  aggregated { availableToSell warningLevel }
				                  byLocation {
				                    edges {
				                      node {
				                        locationEntityId
				                        locationEntityTypeId
				                        isInStock
				                        availableToSell
				                      }
				                    }
				                  }
				                }
				                options {
				                  edges {
				                    node {
				                      entityId
				                      displayName
				                      values {
				                        edges { node { entityId label } }
				                      }
				                    }
				                  }
				                }
				                metafields(namespace: "variant_attributes") {
				                  edges { node { entityId key value } }
				                }
				              }
				            }
				          }
				          inventory {
				            hasVariantInventory
				          }
				        }
				      }
				    }
				""".formatted(productId);

		HttpURLConnection conn = createGraphQLRequest(BigCommerceStoreConfig.STORE_HASH,
				BigCommerceStoreConfig.BC_CHANNEL_ID, BigCommerceStoreConfig.STOREFRONT_TOKEN, query);

		int status = conn.getResponseCode();
		if (status != 200) {
			throw new RuntimeException("Failed: HTTP " + status);
		}

		String response;
		try (Scanner scanner = new Scanner(conn.getInputStream())) {
			response = scanner.useDelimiter("\\A").next();
		}

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JsonNode root = mapper.readTree(response).path("data");

		return mapper.treeToValue(root.get("site").get("product"), ProductGraphQLResponse.Product.class);
	}

	private String buildProductListQuery(String afterCursor) {
		String afterClause = (afterCursor != null) ? ", after: \"" + afterCursor + "\"" : "";
		return """
				    query {
				      site {
				        products(first: 50%s) {
				          pageInfo {
				            hasNextPage
				            endCursor
				          }
				          edges {
				            node {
				              entityId
				              sku
				              path
				              name
				              type
				              description
				              brand{name}
				              categories{edges{node{name entityId}}}
				              images { edges { node { url(width: 400) } } }
				              customFields { edges { node { entityId name value } } }
				              metafields(namespace: "product_attributes", first: 50) { 
				                      pageInfo { hasNextPage endCursor }
				                      edges { node { entityId key value } 
				                    } 
				              }
				              prices {
				                      basePrice { currencyCode value }
				                      price     { currencyCode value }
				                      salePrice { currencyCode value }
				              }
				              variants (first: 100){
				                pageInfo { 
				                   hasNextPage 
				                   endCursor 
				                }
				                edges {
				                  node {
				                    entityId
				                    sku
				                    isPurchasable
				                    height { value unit }
				                    width { value unit }
				                    weight { value unit }
				                    prices {
				                      basePrice  { currencyCode value }
				                      price      { currencyCode value }
				                      salePrice  { currencyCode value }
				                    }
				                    defaultImage { url(width: 400) }
				                    inventory {
				                      isInStock
				                      aggregated { availableToSell warningLevel }
				                      byLocation {
				                        edges {
				                          node {
				                            locationEntityId
				                            locationEntityTypeId
				                            isInStock
				                            availableToSell
				                          }
				                        }
				                      }
				                    }
				                    options {
				                      edges {
				                        node {
				                          entityId
				                          displayName
				                          values {
				                            edges { node { entityId label } }
				                          }
				                        }
				                      }
				                    }
				                    metafields(namespace: "variant_attributes", first: 50) {
				                      pageInfo { hasNextPage endCursor }
				                      edges { node { entityId key value } }
				                    }
				                  }
				                }
				              }
				            }
				          }
				        }
				      }
				    }
				""".formatted(afterClause);
	}

	/**
	 * Builds a  GraphQL query for paginating variants of a given product.
	 */
	private String buildVariantPaginationQuery(int productId, String afterCursor) {
	    String afterClause = (afterCursor != null) ? ", after: \"" + afterCursor + "\"" : "";
	    return """
	        query {
	          site {
	            product(entityId: %d) {
	              variants(first: 50%s) {
	                pageInfo {
	                  hasNextPage
	                  endCursor
	                }
	                edges {
	                  node {
	                    entityId
	                    sku
	                    isPurchasable
	                    height { value unit }
	                    width { value unit }
	                    weight { value unit }
	                    prices {
	                      price { currencyCode value }
	                      salePrice { currencyCode value }
	                    }
	                    defaultImage { url(width: 400) }
	                    inventory {
	                      isInStock
	                      aggregated { availableToSell warningLevel }
	                      byLocation {
	                        edges {
	                          node {
	                            locationEntityId
	                            locationEntityTypeId
	                            isInStock
	                            availableToSell
	                          }
	                        }
	                      }
	                    }
	                    options {
	                      edges {
	                        node {
	                          entityId
	                          displayName
	                          values {
	                            edges { node { entityId label } }
	                          }
	                        }
	                      }
	                    }
	                    metafields(namespace: "variant_attributes") {
	                      edges { node { entityId key value } }
	                    }
	                  }
	                }
	              }
	            }
	          }
	        }
	        """.formatted(productId, afterClause);
	}
	
	
	  
	public List<ProductGraphQLResponse.Product> getAllProducts() throws Exception {
	    List<ProductGraphQLResponse.Product> products = new ArrayList<>();
	    String afterCursor = null;
	    boolean hasNextPage;

	    ObjectMapper mapper = new ObjectMapper();
	    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	    do {
	        String query = buildProductListQuery(afterCursor);
	        HttpURLConnection conn = createGraphQLRequest(
	                BigCommerceStoreConfig.STORE_HASH,
	                BigCommerceStoreConfig.BC_CHANNEL_ID,
	                BigCommerceStoreConfig.STOREFRONT_TOKEN, query);

	        if (conn.getResponseCode() != 200) {
	            throw new RuntimeException("Failed: HTTP " + conn.getResponseCode());
	        }

	        String response;
	        try (Scanner scanner = new Scanner(conn.getInputStream())) {
	            response = scanner.useDelimiter("\\A").next();
	        }

	        JsonNode root = mapper.readTree(response).path("data");
	        ProductListGraphQLResponse result = mapper.treeToValue(root, ProductListGraphQLResponse.class);

	        for (ProductListGraphQLResponse.ProductEdge edge : result.site.products.edges) {
	            ProductGraphQLResponse.Product product = edge.node;

	            // ✅ Handle product-level metafield pagination
	            if (product.metafields != null &&
	                    product.metafields.pageInfo != null &&
	                    product.metafields.pageInfo.hasNextPage) {
	            
	                String productMetaAfterCursor = product.metafields.pageInfo.endCursor;
	                boolean productMetaHasNextPage = product.metafields.pageInfo.hasNextPage;

	                while (productMetaHasNextPage) {
	                    String metaQuery = buildProductMetafieldPaginationQuery(product.entityId, productMetaAfterCursor);
	                    HttpURLConnection metaConn = createGraphQLRequest(
	                            BigCommerceStoreConfig.STORE_HASH,
	                            BigCommerceStoreConfig.BC_CHANNEL_ID,
	                            BigCommerceStoreConfig.STOREFRONT_TOKEN, metaQuery);

	                    if (metaConn.getResponseCode() != 200) {
	                        throw new RuntimeException("Failed: HTTP " + metaConn.getResponseCode());
	                    }

	                    String metaResponse;
	                    try (Scanner metaScanner = new Scanner(metaConn.getInputStream())) {
	                        metaResponse = metaScanner.useDelimiter("\\A").next();
	                    }

	                    JsonNode metaRoot = mapper.readTree(metaResponse)
	                            .path("data")
	                            .path("site")
	                            .path("product")
	                            .path("metafields");

	                    ProductGraphQLResponse.MetafieldConnection metaResult =
	                            mapper.treeToValue(metaRoot, ProductGraphQLResponse.MetafieldConnection.class);

	                    product.metafields.edges.addAll(metaResult.edges);

	                    productMetaHasNextPage = metaResult.pageInfo.hasNextPage;
	                    productMetaAfterCursor = metaResult.pageInfo.endCursor;
	                }
	            }
	            
//	            if (product.variants != null && product.variants.edges != null) {
//	                for (ProductGraphQLResponse.VariantEdge variantEdge : product.variants.edges) {
//	                	
//	                	ProductGraphQLResponse.Variant variant = variantEdge.node;   	 
//	                    // 🔍 Only if variant metafields HAVE next pages
//	                    if (variant.metafields != null &&
//	                        variant.metafields.pageInfo != null &&
//	                        variant.metafields.pageInfo.hasNextPage) {
//	                        processVariantMetafieldPagination(variant, mapper);
//	                    }
//	                }
//	            }

	            // ✅ Handle variant-level pagination
	            if (product.variants != null && product.variants.pageInfo.hasNextPage) {
	                String variantAfterCursor = product.variants.pageInfo.endCursor;
	                boolean variantHasNextPage = product.variants.pageInfo.hasNextPage;
//	                boolean variantHasNextPage = true;	               
	                while (variantHasNextPage) {
	                    String variantQuery = buildVariantPaginationQuery(product.entityId, variantAfterCursor);
	                    HttpURLConnection variantConn = createGraphQLRequest(
	                            BigCommerceStoreConfig.STORE_HASH,
	                            BigCommerceStoreConfig.BC_CHANNEL_ID,
	                            BigCommerceStoreConfig.STOREFRONT_TOKEN, variantQuery);

	                    if (variantConn.getResponseCode() != 200) {
	                        throw new RuntimeException("Failed: HTTP " + variantConn.getResponseCode());
	                    }

	                    String variantResponse;
	                    try (Scanner variantScanner = new Scanner(variantConn.getInputStream())) {
	                        variantResponse = variantScanner.useDelimiter("\\A").next();
	                    }

	                    JsonNode variantRoot = mapper.readTree(variantResponse)
	                            .path("data")
	                            .path("site")
	                            .path("product")
	                            .path("variants");

	                    ProductGraphQLResponse.VariantConnection variantResult =
	                            mapper.treeToValue(variantRoot, ProductGraphQLResponse.VariantConnection.class);

	                    product.variants.edges.addAll(variantResult.edges);

	                    // ✅ Variant-level metafield pagination
	                    if (product.getEntityId()==613) {
	                    	int a=0;
	                    }
	                    for (ProductGraphQLResponse.VariantEdge variantEdge : variantResult.edges) {
	                        ProductGraphQLResponse.Variant variant = variantEdge.node;
	                        if (variant.metafields != null &&
	                            variant.metafields.pageInfo != null &&
	                            variant.metafields.pageInfo.hasNextPage) {
	                    
	                            String variantMetaAfterCursor = variant.metafields.pageInfo.endCursor;
	                            boolean variantMetaHasNextPage = variant.metafields.pageInfo.hasNextPage;

	                            while (variantMetaHasNextPage) {
	                            	String variantNodeId = "gid://bigcommerce/ProductVariant/" + variant.entityId;
	                            	String metaQuery = buildVariantMetafieldPaginationQuery(
	                            			variantNodeId, variantMetaAfterCursor);
//	                                String metaQuery = buildVariantMetafieldPaginationQuery(
//	                                        variant.entityId, variantMetaAfterCursor);
	                                HttpURLConnection metaConn = createGraphQLRequest(
	                                        BigCommerceStoreConfig.STORE_HASH,
	                                        BigCommerceStoreConfig.BC_CHANNEL_ID,
	                                        BigCommerceStoreConfig.STOREFRONT_TOKEN, metaQuery);

	                                if (metaConn.getResponseCode() != 200) {
	                                    throw new RuntimeException("Failed: HTTP " + metaConn.getResponseCode());
	                                }

	                                String metaResponse;
	                                try (Scanner metaScanner = new Scanner(metaConn.getInputStream())) {
	                                    metaResponse = metaScanner.useDelimiter("\\A").next();
	                                }

	                                JsonNode metaRoot = mapper.readTree(metaResponse)
	                                        .path("data")
	                                        .path("site")
	                                        .path("node")
	                                        .path("metafields");

	                                ProductGraphQLResponse.MetafieldConnection metaResult =
	                                        mapper.treeToValue(metaRoot, ProductGraphQLResponse.MetafieldConnection.class);

	                                variant.metafields.edges.addAll(metaResult.edges);

	                                variantMetaHasNextPage = metaResult.pageInfo.hasNextPage;
	                                variantMetaAfterCursor = metaResult.pageInfo.endCursor;
	                            }
	                        }
	                    }

	                    variantHasNextPage = variantResult.pageInfo.hasNextPage;
	                    variantAfterCursor = variantResult.pageInfo.endCursor;
	                }
	            }
	            
	            products.add(product);
	        }

	        hasNextPage = result.site.products.pageInfo.hasNextPage;
	        afterCursor = result.site.products.pageInfo.endCursor;

	    } while (hasNextPage);

	    return products;
	}

	public List<ProductGraphQLResponse.Product> getAllProductsaa() throws Exception {

		List<ProductGraphQLResponse.Product> products = new ArrayList<>();
        String afterCursor = null;
        boolean hasNextPage;

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        do {
            String query = buildProductListQuery(afterCursor);
            HttpURLConnection conn = createGraphQLRequest(
                    BigCommerceStoreConfig.STORE_HASH,
                    BigCommerceStoreConfig.BC_CHANNEL_ID,
                    BigCommerceStoreConfig.STOREFRONT_TOKEN, query);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed: HTTP " + conn.getResponseCode());
            }

            String response;
            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                response = scanner.useDelimiter("\\A").next();
            }

            JsonNode root = mapper.readTree(response).path("data");
            ProductListGraphQLResponse result = mapper.treeToValue(root, ProductListGraphQLResponse.class);

            for (ProductListGraphQLResponse.ProductEdge edge : result.site.products.edges) {
                ProductGraphQLResponse.Product product = edge.node;

                // Handle variant pagination if needed
                if (product.variants != null &&
                        product.variants.pageInfo != null &&
                        product.variants.pageInfo.hasNextPage) {

                    String variantAfterCursor = product.variants.pageInfo.endCursor;
                    boolean variantHasNextPage = true;

                    while (variantHasNextPage) {
                        String variantQuery = buildVariantPaginationQuery(product.entityId, variantAfterCursor);
                        HttpURLConnection variantConn = createGraphQLRequest(
                                BigCommerceStoreConfig.STORE_HASH,
                                BigCommerceStoreConfig.BC_CHANNEL_ID,
                                BigCommerceStoreConfig.STOREFRONT_TOKEN, variantQuery);

                        if (variantConn.getResponseCode() != 200) {
                            throw new RuntimeException("Failed: HTTP " + variantConn.getResponseCode());
                        }

                        String variantResponse;
                        try (Scanner variantScanner = new Scanner(variantConn.getInputStream())) {
                            variantResponse = variantScanner.useDelimiter("\\A").next();
                        }

                        JsonNode variantRoot = mapper.readTree(variantResponse)
                                .path("data")
                                .path("site")
                                .path("product")
                                .path("variants");

                        ProductGraphQLResponse.VariantConnection variantResult =
                                mapper.treeToValue(variantRoot, ProductGraphQLResponse.VariantConnection.class);

                        product.variants.edges.addAll(variantResult.edges);

                        variantHasNextPage = variantResult.pageInfo.hasNextPage;
                        variantAfterCursor = variantResult.pageInfo.endCursor;
                    }
                }

                products.add(product);
            }

            hasNextPage = result.site.products.pageInfo.hasNextPage;
            afterCursor = result.site.products.pageInfo.endCursor;

        } while (hasNextPage);

        return products;
    }

	public List<ProductGraphQLResponse.Product> getAllProductsInit() throws Exception {
		List<ProductGraphQLResponse.Product> products = new ArrayList<>();
		String afterCursor = null;
		boolean hasNextPage;

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		do {
			String query = buildProductListQuery(afterCursor);
			HttpURLConnection conn = createGraphQLRequest(
					BigCommerceStoreConfig.STORE_HASH,
					BigCommerceStoreConfig.BC_CHANNEL_ID, 
					BigCommerceStoreConfig.STOREFRONT_TOKEN, query);

			int status = conn.getResponseCode();
			if (status != 200) {
				throw new RuntimeException("Failed: HTTP " + status);
			}

			String response;
			try (Scanner scanner = new Scanner(conn.getInputStream())) {
				response = scanner.useDelimiter("\\A").next();
			}

			JsonNode root = mapper.readTree(response).path("data");
			ProductListGraphQLResponse result = mapper.treeToValue(root, ProductListGraphQLResponse.class);

			for (ProductListGraphQLResponse.ProductEdge edge : result.site.products.edges) {
				products.add(edge.node);
			}

			hasNextPage = result.site.products.pageInfo.hasNextPage;
			afterCursor = result.site.products.pageInfo.endCursor;

		} while (hasNextPage);

		return products;
	}

	public static HttpURLConnection createGraphQLRequest(String storeHash, int storefrontChannelId, String accessToken,
			String graphqlQuery) throws IOException {

		URL url = new URL("https://store-" + storeHash + "-" + storefrontChannelId + ".mybigcommerce.com/graphql");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken); // ✅ corrected header
		conn.setDoOutput(true);

		String body = String.format("{\"query\": %s}", JSONObject.quote(graphqlQuery));
		try (OutputStream os = conn.getOutputStream()) {
			os.write(body.getBytes(StandardCharsets.UTF_8));
		}

		return conn;
	}

	private String buildProductMetafieldPaginationQuery(int productId, String afterCursor) {
	    String afterClause = (afterCursor != null) ? ", after: \"" + afterCursor + "\"" : "";
	    return """
	        query {
	          site {
	            product(entityId: %d) {
	              metafields(namespace: "product_attributes", first: 50%s) {
	                pageInfo { hasNextPage endCursor }
	                edges { node { entityId key value } }
	              }
	            }
	          }
	        }
	    """.formatted(productId, afterClause);
	}

	private String buildVariantMetafieldPaginationQuery(int variantId, String afterCursor) {
	    String afterClause = (afterCursor != null) ? ", after: \"" + afterCursor + "\"" : "";
	    return """
	        query {
	          site {
	            node(id: "%d") {
	              ... on Variant {
	                metafields(namespace: "variant_attributes", first: 50%s) {
	                  pageInfo { hasNextPage endCursor }
	                  edges { node { entityId key value } }
	                }
	              }
	            }
	          }
	        }
	    """.formatted(variantId, afterClause);
	}
	
	private String buildVariantMetafieldPaginationQuery(String variantNodeId, String afterCursor) {
	    String afterClause = (afterCursor != null) ? ", after: \"" + afterCursor + "\"" : "";
	    return """
	        query {
	          site {
	            node(id: "%s") {
	              ... on Variant {
	                metafields(namespace: "variant_attributes", first: 50%s) {
	                  pageInfo { hasNextPage endCursor }
	                  edges { node { entityId key value } }
	                }
	              }
	            }
	          }
	        }
	    """.formatted(variantNodeId, afterClause);
	}
	
	

}
