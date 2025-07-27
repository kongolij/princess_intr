package com.constructor.index.mapper;

import com.bigcommerce.imports.catalog.product.graphqldto.MetafieldConnection;
import com.constructor.client.BazaarvoiceReviewStatsClient;
import com.constructor.client.BazaarvoiceReviewStatsClient.ReviewStats;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.dto.ProductGraphQLResponse.CustomField;
import com.constructor.index.dto.ProductGraphQLResponse.CustomFieldConnection;
import com.constructor.index.dto.ProductGraphQLResponse.CustomFieldEdge;
import com.constructor.index.dto.ProductGraphQLResponse.ImageEdge;
import com.constructor.index.dto.ProductGraphQLResponse.Option;
import com.constructor.index.dto.ProductGraphQLResponse.OptionEdge;
import com.constructor.index.dto.ProductGraphQLResponse.OptionValueEdge;
import com.constructor.index.dto.ProductGraphQLResponse.Variant;
import com.constructor.index.dto.ProductGraphQLResponse.VariantConnection;
import com.constructor.index.dto.ProductGraphQLResponse.VariantEdge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class ConstructorJsonlProductMapper {

	private final BazaarvoiceReviewStatsClient reviewStatsClient;

	public ConstructorJsonlProductMapper(BazaarvoiceReviewStatsClient reviewStatsClient) {
		this.reviewStatsClient = reviewStatsClient;
	}

	private static final ObjectMapper mapper = new ObjectMapper();

//	{
//	"id": "cotton-t-shirt",
//	"name": "Cotton T-Shirt",
//	"data": 
//	{
//		"url": "https://constructor.com/",
//		"image_url": "https://constructorio-integrations.s3.amazonaws.com/tikus-threads/2022-06-29/WOVEN-CASUAL-SHIRT_BUTTON-DOWN-WOVEN-SHIRT_BSH01757SB1918_3_category.jpg",
//		"product_type": ["Shirts","T-Shirts"],
//		"group_ids": ["tops-athletic","tops-casual"],
//		"material": "Cotton",
//		"keywords": ["gym","casual","athletic","workout","comfort","simple"],
//		"description": "Treat yourself to a comfy upgrade with this Short Sleeve Shirt from Etchell's Emporium. This short-sleeve T-shirt comes with a classic crew-neck, giving you style and comfort that can easily be paired with a variety of bottoms.",
//		"active": true,
//		"price": 18}}
	public String mapToJsonlLine(ProductGraphQLResponse.Product product, Map<Integer, List<Integer>> categoriesPath,
			String locale) {
		ObjectNode node = mapper.createObjectNode();
		ObjectMapper localMapper = new ObjectMapper();
		ObjectNode data = mapper.createObjectNode();

		String externalId = null;
		CustomFieldConnection customFieldConnection = product.getCustomFields();

		if (customFieldConnection != null && customFieldConnection.getEdges() != null) {
			for (CustomFieldEdge edge : customFieldConnection.getEdges()) {
				if (edge != null && edge.getNode() != null) {
					CustomField field = edge.getNode();
					if ("product_external_id".equalsIgnoreCase(field.getName())) {
						externalId = field.getValue();
						break;
					}
				}
			}
		}

		node.put("id", externalId != null ? externalId : product.getSku());

		data.set("url", new TextNode("https://www-dev.princessauto.com/en/product" + product.getPath()));
		data.set("active", BooleanNode.TRUE);

		// Image URL
		String imageUrl = product.getImages() != null && product.getImages().getEdges() != null
				&& !product.getImages().getEdges().isEmpty() ? product.getImages().getEdges().get(0).getNode().getUrl()
						: "";
//		node.put("image_url", imageUrl);
		data.set("image_url", new TextNode(imageUrl));

		// Custom Fields
		Map<String, String> customFieldMap = new HashMap<>();
		if (product.getCustomFields() != null && product.getCustomFields().getEdges() != null) {
			for (var edge : product.getCustomFields().getEdges()) {
				customFieldMap.put(edge.getNode().getName(), edge.getNode().getValue());
			}
		}

		Map<String, String> metafieldMap = new HashMap<>();
		if (product.getMetafields() != null && product.getMetafields().getEdges() != null) {
			for (var edge : product.getMetafields().getEdges()) {
				var metaNode = edge.getNode();
				if (node != null && metaNode.getKey() != null) {
					metafieldMap.put(metaNode.getKey(), metaNode.getValue());
				}
			}
		}

		com.constructor.index.dto.ProductGraphQLResponse.MetafieldConnection mertafileds = product.getMetafields();

		// Get localized display name from "displayName" custom field
//        String itemName = product.getName(); // fallback
		String itemName = ""; // fallback
		String description = ""; // fallback
		try {
			// Use locale to pick the right metafield
			String metaKey = "displayName_" + locale; // e.g., "en" or "fr"
			String descriptionMetaKey = "longDescription_" + locale; // e.g., "en" or "fr"
			String attributesJson = metafieldMap.get(metaKey);
			String descriptionJson = metafieldMap.get(descriptionMetaKey);
			if (attributesJson != null) {
				itemName = attributesJson;
			} else {

				itemName = product.getName(); // or product.getName() depending on your model

			}
			description = Objects.requireNonNullElse(descriptionJson, "");

		} catch (Exception e) {
			System.err.println("❌ Failed to parse localized product_attributes_" + locale + ": " + e.getMessage());
		}

		node.put("name", itemName);
//		node.put("description", description);

		// Group IDs
		ArrayNode groupIdsArray = mapper.createArrayNode();
		if (product.getCategories() != null && product.getCategories().getEdges() != null) {
			for (ProductGraphQLResponse.Edge<ProductGraphQLResponse.Category> edge : product.getCategories()
					.getEdges()) {
				ProductGraphQLResponse.Category category = edge.getNode();
				int categoryId = category.getEntityId();
				groupIdsArray.add(String.valueOf(categoryId));
			}
		}
		data.set("group_ids", groupIdsArray);
		data.set("description", TextNode.valueOf(description));

		// Facet Category Name (localized)
//		String targetLocale = "en";
		String facetCategoryName = null;
		if (product.getCategories() != null && product.getCategories().getEdges() != null
				&& !product.getCategories().getEdges().isEmpty()) {
			ProductGraphQLResponse.Category category = product.getCategories().getEdges().get(0).getNode();
			facetCategoryName = category.getName(); // fallback
			if (category.getMetafields() != null && category.getMetafields().getEdges() != null) {
				for (ProductGraphQLResponse.MetafieldEdge metaEdge : category.getMetafields().getEdges()) {
					ProductGraphQLResponse.Metafield mf = metaEdge.getNode();
					if (mf != null && locale.equalsIgnoreCase(mf.getKey())) {
						facetCategoryName = mf.getValue();
						break;
					}
				}
			}
		}
		if (facetCategoryName != null) {
//			node.put("metadata:category_name", facetCategoryName);
			data.set("metadata:catalogTaxonomy", TextNode.valueOf(facetCategoryName));
		}

		String brandName = product.getBrand() != null ? product.getBrand().getName() : "";
//		node.put("metadata:brand", brandName);
//		node.put("metadata:productType", customFieldMap.getOrDefault("productType", ""));
//		node.put("metadata:productStatus", customFieldMap.getOrDefault("productStatus", ""));
//		node.put("metadata:productClearance", customFieldMap.getOrDefault("productClearance", ""));
//		node.put("metadata:availabilityCode", customFieldMap.getOrDefault("availabilityCode", ""));
//		node.put("metadata:creationDate", customFieldMap.getOrDefault("occCreationDate", ""));
//		node.put("metadata:Reviews", 0);
//		node.put("metadata:Average Rating", 0);

		ReviewStats reviewStats = reviewStatsClient.fetchStatsForProduct(externalId);

		data.set("metadata:brand", TextNode.valueOf(brandName));
		data.set("metadata:productType", TextNode.valueOf(product.getType() != null ? product.getType() : ""));
		data.set("metadata:productStatus", TextNode.valueOf(customFieldMap.getOrDefault("productStatus", "")));
		data.set("metadata:productClearance", TextNode.valueOf(customFieldMap.getOrDefault("productClearance", "")));
		data.set("metadata:availabilityCode", TextNode.valueOf(customFieldMap.getOrDefault("availabilityCode", "")));
		data.set("metadata:creationDate", TextNode.valueOf(customFieldMap.getOrDefault("occCreationDate", "")));
		data.set("metadata:Reviews", IntNode.valueOf(reviewStats.totalReviews));
		data.set("metadata:Average Rating", DoubleNode.valueOf(reviewStats.averageRating));

		// Metadata availability
		ArrayNode availabilityArray = mapper.createArrayNode();
		if (product.getVariants() != null && product.getVariants().getEdges() != null) {
			for (var variantEdge : product.getVariants().getEdges()) {
				var inventory = variantEdge.getNode().getInventory();
				var byLocation = inventory != null ? inventory.getByLocation() : null;
				if (byLocation != null && byLocation.getEdges() != null) {
					for (var locEdge : byLocation.getEdges()) {
						availabilityArray.add(String.valueOf(locEdge.getNode().getLocationEntityId()));
					}
				}
			}
		}
//        ObjectNode metadataNode = mapper.createObjectNode();
//        metadataNode.set("availability", availabilityArray);
//        node.set("metadata", metadataNode);

//		node.put("metadata:availability", String.valueOf(availabilityArray.size()));
		data.set("metadata:availability", availabilityArray);

		node.set("data", data);
		return node.toString();
	}

	public List<String> mapVariantsToJsonlLine(ProductGraphQLResponse.Product product, String locale) {
		List<String> jsonlLines = new ArrayList<>();

		String externalId = null;
		CustomFieldConnection customFieldConnection = product.getCustomFields();
		if (customFieldConnection != null && customFieldConnection.getEdges() != null) {
			for (CustomFieldEdge edge : customFieldConnection.getEdges()) {
				if (edge != null && edge.getNode() != null) {
					CustomField field = edge.getNode();
					if ("product_external_id".equalsIgnoreCase(field.getName())) {
						externalId = field.getValue();
						break;
					}
				}
			}
		}

		String productId = externalId != null ? externalId : product.getSku();
		VariantConnection variantConnection = product.getVariants();

		if (variantConnection == null || variantConnection.getEdges() == null
				|| variantConnection.getEdges().isEmpty()) {
			System.out.println("❌ yyyyyy  No variants present for product: " + productId);
			return jsonlLines;
		} else if (variantConnection.getEdges().size() == 1) {
			System.out.println("❌ yyyyyy  single sku product: " + productId);
			Variant variant = variantConnection.getEdges().get(0).getNode();

			ObjectNode root = mapper.createObjectNode();
			root.put("id", variant.getSku());
			root.put("item_id", productId);
			root.put("name", product.getName());

			ObjectNode data = mapper.createObjectNode();
			data.put("active", variant.isPurchasable());

			// ✅ Added null checks for images list and its contents
			if (product.images != null && product.images.edges != null && !product.images.edges.isEmpty()) {
				ImageEdge imageEdge = product.images.edges.get(0);
				if (imageEdge != null && imageEdge.node != null && imageEdge.node.url != null) {
					data.put("image_url", imageEdge.node.url);
				}
			}

			// ✅ Added fallback for pricing when variant.prices is null — use product.prices
			Double listPrice = null, salePrice = null;
			if (product.prices != null) {
				listPrice = product.prices.getBasePrice() != null ? product.prices.getBasePrice().getValue() : null;
				salePrice = product.prices.getSalePrice() != null ? product.prices.getSalePrice().getValue() : null;
			}

			if (listPrice != null) {
				data.put("list_price", listPrice);
			}

			if (salePrice != null && !salePrice.equals(listPrice)) {
				data.put("sale_price", salePrice);
				data.put("is_on_sale", true);
				System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxx ");
			} else {
				data.put("is_on_sale", false);
			}

			// ✅ Calculated fallback facet_price
			Double facetPrice = (salePrice != null && !salePrice.equals(listPrice)) ? salePrice : listPrice;
			if (facetPrice != null) {
				data.put("facet_price", facetPrice);
			}

			root.set("data", data);
			jsonlLines.add(root.toString());
			return jsonlLines;

		}

		for (VariantEdge edge : variantConnection.getEdges()) {
			if (edge == null || edge.getNode() == null)
				continue;

			Variant variant = edge.getNode(); // your Variant class

			ObjectNode root = mapper.createObjectNode();
			root.put("id", variant.getSku());
			root.put("item_id", productId);
			root.put("name", product.getName()); // or variant-specific name if you have it

			ObjectNode data = mapper.createObjectNode();
			data.put("active", variant.isPurchasable());

//	        if (product.getPath() != null) {
//	            data.put("url", "https://yourdomain.com" + product.getPath());
//	        }

			if (variant.defaultImage != null && variant.defaultImage.getUrl() != null) {
				data.put("image_url", variant.defaultImage.getUrl());
			}

			// Pricing
			if (variant.prices != null) {
				Double listPrice = variant.prices.getBasePrice() != null ? variant.prices.getBasePrice().getValue() : null;
				Double salePrice = variant.prices.getSalePrice() != null ? variant.prices.getSalePrice().getValue()
						: null;

				if (listPrice != null) {
					data.put("list_price", listPrice);
				}
				System.out.println("aaaa  " + listPrice + "   " + salePrice);
				if (salePrice != null && !salePrice.equals(listPrice)) {
					data.put("sale_price", salePrice);
					data.put("is_on_sale", true);
					System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxx ");
				} else {
					data.put("is_on_sale", false);
				}

				// Determine price to use for faceting
				Double facetPrice = (salePrice != null && !salePrice.equals(listPrice)) ? salePrice : listPrice;
				if (facetPrice != null) {
					data.put("facet_price", facetPrice);
				}
			}

			// Dimensions
//	        if (variant.height != null) data.put("height", variant.height.getValue());
//	        if (variant.width != null) data.put("width", variant.width.getValue());
//	        if (variant.weight != null) data.put("weight", variant.weight.getValue());

			// Options (e.g., color, size)
			if (variant.options != null && variant.options.getEdges() != null) {
				for (OptionEdge optionEdge : variant.options.getEdges()) {
					if (optionEdge != null && optionEdge.getNode() != null) {
						Option option = optionEdge.getNode();
						if (option.getDisplayName() != null && option.getValues() != null
								&& option.getValues().getEdges() != null) {
							List<OptionValueEdge> valueEdges = option.getValues().getEdges();
							if (!valueEdges.isEmpty() && valueEdges.get(0) != null
									&& valueEdges.get(0).getNode() != null) {
								String label = valueEdges.get(0).getNode().getLabel();
								if (label != null) {
									data.put(option.getDisplayName().toLowerCase(), label);
								}
							}
						}
					}
				}
			}

			// Custom metafields (if any logic is needed)
//	        if (variant.metafields != null && variant.metafields.getEdges() != null) {
//	            for (MetafieldEdge edgeMeta : variant.metafields.getEdges()) {
//	                if (edgeMeta != null && edgeMeta.getNode() != null) {
//	                    Metafield mf = edgeMeta.getNode();
//	                    data.put(mf.getKey(), mf.getValue());
//	                }
//	            }
//	        }

			root.set("data", data);
			jsonlLines.add(root.toString());
		}

		return jsonlLines;
	}

	public String mapVariantToJsonlLine(ProductGraphQLResponse.Product product, String locale) {
	    List<ObjectNode> variationNodes = new ArrayList<>();

	    String externalId = null;
	    CustomFieldConnection customFieldConnection = product.getCustomFields();
	    if (customFieldConnection != null && customFieldConnection.getEdges() != null) {
	        for (CustomFieldEdge edge : customFieldConnection.getEdges()) {
	            if (edge != null && edge.getNode() != null) {
	                CustomField field = edge.getNode();
	                if ("product_external_id".equalsIgnoreCase(field.getName())) {
	                    externalId = field.getValue();
	                    break;
	                }
	            }
	        }
	    }

	    String productId = externalId != null ? externalId : product.getSku();
	    VariantConnection variantConnection = product.getVariants();

	    if (variantConnection == null || variantConnection.getEdges() == null || variantConnection.getEdges().isEmpty()) {
	        System.out.println("❌ No variants present for product: " + productId);
	        return null;
	    }

	    // Handle all variants (even if only 1 exists)
	    for (VariantEdge edge : variantConnection.getEdges()) {
	        if (edge == null || edge.getNode() == null) continue;

	        Variant variant = edge.getNode();

	        ObjectNode root = mapper.createObjectNode();
	        root.put("id", variant.getSku());
	        root.put("item_id", productId);
	        root.put("name", product.getName());

	        ObjectNode data = mapper.createObjectNode();
	        data.put("active", variant.isPurchasable());

	        // ✅ Use product image if no variant image is present
	        if (variantConnection.getEdges().size() == 1) {
	        	System.out.println(" got one-to-one " + productId);
	        }
	        		
	        if (variant.defaultImage != null && variant.defaultImage.getUrl() != null) {
	            data.put("image_url", variant.defaultImage.getUrl());
	        } else if (product.images != null && product.images.edges != null && !product.images.edges.isEmpty()) {
	            ImageEdge imageEdge = product.images.edges.get(0);
	            if (imageEdge != null && imageEdge.node != null && imageEdge.node.url != null) {
	                data.put("image_url", imageEdge.node.url);
	            }
	        }

	        // ✅ Pricing logic
	        Double listPrice = null, salePrice = null;
	        if (variant.prices != null) {
	            listPrice = variant.prices.getBasePrice() != null ? variant.prices.getBasePrice().getValue() : null;
	            salePrice = variant.prices.getSalePrice() != null ? variant.prices.getSalePrice().getValue() : null;
	        } else if (product.prices != null) {
	        	System.out.println(" single prod ");
	            listPrice = product.prices.getBasePrice() != null ? product.prices.getBasePrice().getValue() : null;
	            salePrice = product.prices.getSalePrice() != null ? product.prices.getSalePrice().getValue() : null;
	        }

	        if (listPrice != null) {
	            data.put("list_price", listPrice);
	        }

	        if (salePrice != null && !salePrice.equals(listPrice)) {
	            data.put("sale_price", salePrice);
	            data.put("is_on_sale", true);
	            System.out.println(" got one xxxxxxxxxxxxxxxxxxxxxxxxxxxxx ");
	        } else {
	            data.put("is_on_sale", false);
	        }

	        Double facetPrice = (salePrice != null && !salePrice.equals(listPrice)) ? salePrice : listPrice;
	        if (facetPrice != null) {
	            data.put("facet_price", facetPrice);
	        }

	        // ✅ Option handling (e.g. color/size)
	        if (variant.options != null && variant.options.getEdges() != null) {
	            for (OptionEdge optionEdge : variant.options.getEdges()) {
	                if (optionEdge != null && optionEdge.getNode() != null) {
	                    Option option = optionEdge.getNode();
	                    if (option.getDisplayName() != null && option.getValues() != null
	                            && option.getValues().getEdges() != null) {
	                        List<OptionValueEdge> valueEdges = option.getValues().getEdges();
	                        if (!valueEdges.isEmpty() && valueEdges.get(0) != null && valueEdges.get(0).getNode() != null) {
	                            String label = valueEdges.get(0).getNode().getLabel();
	                            if (label != null) {
	                                data.put(option.getDisplayName().toLowerCase(), label);
	                            }
	                        }
	                    }
	                }
	            }
	        }

	        root.set("data", data);
	        variationNodes.add(root);
	    }

	    // ✅ Wrap all variants in a single JSON object with key: variations
	    ObjectNode wrapper = mapper.createObjectNode();
	    wrapper.set("variations", mapper.valueToTree(variationNodes));

	    try {
	        return mapper.writeValueAsString(wrapper);
	    } catch (Exception e) {
	        System.err.println("❌ Failed to convert variations to JSON: " + e.getMessage());
	        return null;
	    }
	}

	public String mapToJsonlLine(ProductGraphQLResponse.Product product, String langauge) {
		return mapToJsonlLine(product, new HashMap<>(), langauge);
	}

	public void writeJsonl(List<ProductGraphQLResponse.Product> products, String outputFilePath,
			Map<Integer, List<Integer>> categoriesPath, String langauge) throws Exception {
		try (var writer = new java.io.BufferedWriter(new java.io.FileWriter(outputFilePath))) {
			for (ProductGraphQLResponse.Product product : products) {
				String jsonLine = mapToJsonlLine(product, categoriesPath, langauge);
				writer.write(jsonLine);
				writer.newLine();
			}
		}
	}

	public void writeJsonl(List<ProductGraphQLResponse.Product> products, String outputFilePath, String langauge)
			throws Exception {
		writeJsonl(products, outputFilePath, new HashMap<>(), langauge);
	}
}