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
				        products(first: 10%s) {
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
				              description
				              images { edges { node { url(width: 400) } } }
				              customFields { edges { node { entityId name value } } }
				              metafields(namespace: "product_attributes") { edges { node { entityId key value } } }
				              variants {
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
				      }
				    }
				""".formatted(afterClause);
	}

	public List<ProductGraphQLResponse.Product> getAllProducts() throws Exception {
		List<ProductGraphQLResponse.Product> products = new ArrayList<>();
		String afterCursor = null;
		boolean hasNextPage;

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		do {
			String query = buildProductListQuery(afterCursor);
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

}
