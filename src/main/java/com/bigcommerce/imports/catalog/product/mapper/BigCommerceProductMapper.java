package com.bigcommerce.imports.catalog.product.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.product.constant.AttributeLabels;
import com.bigcommerce.imports.catalog.product.dto.Attribute;
import com.bigcommerce.imports.catalog.product.dto.OptionValue;
import com.bigcommerce.imports.catalog.product.dto.Product;
import com.bigcommerce.imports.catalog.product.dto.ProductCreationResult;
import com.bigcommerce.imports.catalog.product.dto.Variant;

@Component
public class BigCommerceProductMapper {

	public JSONObject mapProductToBigCommerce(Product product, Map<String, Integer> categoryNames, String locale) {

		JSONObject productJson = new JSONObject();

		List<Integer> catIds = product.categories.stream().map(categoryNames::get).filter(Objects::nonNull)
				.collect(Collectors.toList());
		productJson.put("categories", catIds);

		productJson.put("type", "physical");
		Variant firstVariant = product.variants != null && !product.variants.isEmpty() ? product.variants.get(0) : null;
		if (product.variants.size() == 1 && (firstVariant.getOption_values()==null || firstVariant.getOption_values().isEmpty())) {
			productJson.put("name", getLocalizedAttribute(product.attributes, "displayName", locale));
			productJson.put("sku", firstVariant.getSkuNumber());
			productJson.put("weight", firstVariant.paWeight);
			productJson.put("height", firstVariant.getPaHeight());
			productJson.put("depth", firstVariant.getPaLength());
			productJson.put("width", firstVariant.getPaWidth());
			productJson.put("weight", firstVariant.getPaWeight());
			productJson.put("upc", firstVariant.getPaUPC());
			productJson.put("gtin", firstVariant.getPaVendorNumber());
			productJson.put("mpn", firstVariant.getPaVendorPartNumber());
			productJson.put("price", 0.00);
		} else if (product.variants.size() > 1) {
			productJson.put("name", getLocalizedAttribute(product.attributes, "displayName", locale));
			productJson.put("sku", product.getProductNumber());
			productJson.put("weight", firstVariant.paWeight); // required
			productJson.put("price", 0.00); // required
			productJson.put("variants", mapVariantsToBigCommerce(product.variants));
		}

		return productJson;
	}

	public JSONArray mapProductToBigCommerceCustomAttr(Product product) {

		Variant firstVariant = product.variants != null && !product.variants.isEmpty() ? product.variants.get(0) : null;
		if (product.variants.size() == 1 && (firstVariant.getOption_values()==null || firstVariant.getOption_values().isEmpty())) {
			List<Attribute> allAttrs = firstVariant.getAttributes();

			// Step 1: Find the label code (if any)
			Optional<Attribute> labelAttr = allAttrs.stream()
			    .filter(attr -> attr.getId() != null && attr.getId().matches("^[A-Z]\\d{4,6}$"))
			    .filter(attr -> (attr.getEn() == null || attr.getEn().trim().isEmpty()) &&
			                    (attr.getFr_CA() == null || attr.getFr_CA().trim().isEmpty()))
			    .findFirst();

			// Step 2: Exclude it
			List<Attribute> filteredAttrs = product.getAttributes();
			return mapProductAttributesToCustomFields(filteredAttrs);
		} else 
		if (product.variants.size() >= 1) {
			JSONArray customFields=  mapProductAttributesToCustomFields(product.attributes);
			String countryOfOrigin = product.getPaCountryOfOrigin();
			if (countryOfOrigin != null && !countryOfOrigin.isEmpty()) {
			    JSONObject countryField = new JSONObject();
			    countryField.put("name", "countryOfOrigin");
			    countryField.put("value", countryOfOrigin);
			    customFields.put(countryField);
			}
			return customFields;
			
		}
		return null;

	}

	public Map<String, Map<String, JSONObject>> mapVaraintAttribtuesToBigCommerceCustomAttr(Product product) {

		Map<String, Map<String, JSONObject>> localeSkuMap = new HashMap<>();
		
//		Map<String, JSONObject> enMap = mapAttributesToLocale(product.getVariants(), "en");
//		Map<String, JSONObject> frMap = mapAttributesToLocale(product.getVariants(), "fr_CA");
		
		Map<String, JSONObject> enMap = mapAttributesAndAttLabelsToLocale(product.getVariants(), "en");
		Map<String, JSONObject> frMap = mapAttributesAndAttLabelsToLocale(product.getVariants(), "fr_CA");
		localeSkuMap.put("en", enMap);
		localeSkuMap.put("fr_CA", frMap);
		
		
		

		return localeSkuMap;
	}
	
	
	public Map<String, JSONObject> mapToVariantAttrToMetadata(Product product) {
	    Map<String, JSONObject> result = new HashMap<>();

	    for (Variant variant : product.getVariants()) {
	        String sku = variant.getSkuNumber();
	        if (sku == null || sku.isEmpty()) continue;

	        JSONObject staticAttributes = new JSONObject();
	        staticAttributes.put("clearance", String.valueOf(variant.isPaSkuClearance()));
	        staticAttributes.put("availabilityCode", variant.getPaAvailabilityCode() != null ? variant.getPaAvailabilityCode() : "");
	        staticAttributes.put("levy", String.valueOf(variant.isPaLevy()));
	        staticAttributes.put("shippable", "Y");
//	        staticAttributes.put("productStatus", variant.getPaProductStatus() != null ? variant.getPaProductStatus() : "");
	        staticAttributes.put("productStatus", "A");

	        // ✅ Add first "manual" asset if available
	        if (variant.getAssets() != null) {
	            variant.getAssets().stream()
	                .filter(asset -> "manual".equalsIgnoreCase(asset.getType()))
	                .findFirst()
	                .ifPresent(manualAsset -> staticAttributes.put("manual", manualAsset.getPaDocumentsFileName()));
	        }
	        
	        result.put(sku, staticAttributes);
	    }

	    return result;
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

	public JSONArray buildMergedLocaleMetafieldsAsArray(
	        Map<String, Map<String, JSONObject>> localeSkuAttributeMap,
	        Map<String, Integer> skuToVariantIdMap) {

	    JSONArray metafields = new JSONArray();

	    for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
	        String sku = skuEntry.getKey();
	        Integer variantId = skuEntry.getValue();
	        if (variantId == null) continue;

	        List<String> presentLocales = new ArrayList<>();
	        List<String> valueParts = new ArrayList<>();

	        // Collect ordered attributes and locale codes
	        for (Map.Entry<String, Map<String, JSONObject>> localeEntry : localeSkuAttributeMap.entrySet()) {
	            String locale = localeEntry.getKey();
	            Map<String, JSONObject> localeMap = localeEntry.getValue();

	            JSONObject attrs = localeMap.get(sku);
	            if (attrs != null && attrs.length() > 0) {
	                presentLocales.add(locale);
	                valueParts.add(":".concat(attrs.toString()));  // prepend ":" before JSON
	            }
	        }

	        if (valueParts.isEmpty()) continue;

	        // Create array-style string: [:{...},:{...}]
	        String valueString = "[" + String.join(",", valueParts) + "]";

	        // Construct key suffix like custom_attributes_en_fr_CA
	        String keySuffix = String.join("_", presentLocales);

	        JSONObject metafield = new JSONObject();
	        metafield.put("key", "custom_attributes_" + keySuffix);
	        metafield.put("value", valueString);
	        metafield.put("namespace", "variant_attributes");
	        //Allows anyone to read the metafield (e.g. via API), but only the creator can update/delete it.
	        metafield.put("permission_set", "read");
	        metafield.put("description", "Variant attributes ordered by: " + keySuffix);
	        metafield.put("resource_id", variantId);

	        metafields.put(metafield);
	    }

	    return metafields;
	}
	
	

	
	public Map<String, JSONArray> buildLocaleMetafields(
	        Map<String, Map<String, JSONObject>> localeSkuAttributeMap,
	        Map<String, Integer> skuToVariantIdMap) {

	    Map<String, JSONArray> metafieldMap = new HashMap<>();

	    for (Map.Entry<String, Map<String, JSONObject>> localeEntry : localeSkuAttributeMap.entrySet()) {
	        String locale = localeEntry.getKey();
	        Map<String, JSONObject> skuToAttrs = localeEntry.getValue();

	        JSONArray metafieldsForLocale = new JSONArray();

	        for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
	            String sku = skuEntry.getKey();
	            Integer variantId = skuEntry.getValue();
	            if (variantId == null) continue;

	            JSONObject attributes = skuToAttrs.get(sku);
	            if (attributes == null) continue;

	            JSONObject metafield = new JSONObject();
	            metafield.put("key", "variant_attributes_" + locale);
	            metafield.put("value", attributes.toString());  // serialized JSON object
	            metafield.put("namespace", "variant_attributes");
	            //Allows anyone to read the metafield (e.g. via API), but only the creator can update/delete it.
	            metafield.put("permission_set", "read");
	            metafield.put("description", "All variant attributes in " + locale);
	            metafield.put("resource_id", variantId);

	            metafieldsForLocale.put(metafield);
	        }

	        metafieldMap.put(locale, metafieldsForLocale);
	    }

	    return metafieldMap;
	}
	
	public Map<String, JSONArray> buildProductLocaleMetafields(
	        Map<String, Map<String, JSONObject>> localeSkuAttributeMap,
	        ProductCreationResult pr) {

	    Map<String, JSONArray> metafieldMap = new HashMap<>();

	    for (Map.Entry<String, Map<String, JSONObject>> localeEntry : localeSkuAttributeMap.entrySet()) {
	        String locale = localeEntry.getKey();
	        Map<String, JSONObject> skuToAttrs = localeEntry.getValue();

	        JSONArray metafieldsForLocale = new JSONArray();
	        Map<String, Integer> skuToVariantIdMap = pr.getSkuToVariantIdMap();
	        for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
	            String sku = skuEntry.getKey();
	            Integer variantId = skuEntry.getValue();
	            if (variantId == null) continue;

	            JSONObject attributes = skuToAttrs.get(sku);
	            if (attributes == null) continue;

	            JSONObject metafield = new JSONObject();
	            metafield.put("key", "product_attributes_" + locale);
	            metafield.put("value", attributes.toString());  // serialized JSON object
	            metafield.put("namespace", "product_attributes");
	            //Allows anyone to read the metafield (e.g. via API), but only the creator can update/delete it.
	            metafield.put("permission_set", "read");
	            metafield.put("description", "All variant attributes in " + locale);
	            metafield.put("resource_id", pr.getProductId());

	            metafieldsForLocale.put(metafield);
	        }

	        metafieldMap.put(locale, metafieldsForLocale);
	    }

	    return metafieldMap;
	}
	
	public JSONArray buildStaticVariantMetafields(
		    Map<String, JSONObject> staticSkuAttributeMap,
		    Map<String, Integer> skuToVariantIdMap
		) {
		    JSONArray staticMetafields = new JSONArray();

		    for (Map.Entry<String, Integer> skuEntry : skuToVariantIdMap.entrySet()) {
		        String sku = skuEntry.getKey();
		        Integer variantId = skuEntry.getValue();
		        if (variantId == null) continue;

		        JSONObject staticAttrs = staticSkuAttributeMap.get(sku);
		        if (staticAttrs == null) continue;

		        for (String fieldName : staticAttrs.keySet()) {
		            JSONObject field = new JSONObject();
		            field.put("key", fieldName);
		            field.put("value", staticAttrs.get(fieldName).toString());
		            field.put("namespace", "variant_attributes");
		            //Allows anyone to read the metafield (e.g. via API), but only the creator can update/delete it.
		            field.put("permission_set", "read");
		            field.put("description", "Variant flags: " + fieldName);
		            field.put("resource_id", variantId);

		            staticMetafields.put(field);
		        }
		    }

		    return staticMetafields;
		}


	private JSONArray mapVariantsToBigCommerce(List<Variant> variants) {
		JSONArray variantsArray = new JSONArray();

		for (Variant variant : variants) {

			JSONObject variantJson = new JSONObject();

			Optional<String> nameAttr = variant.attributes.stream().filter(attr -> "A01550".equals(attr.id))
					.map(attr -> attr.en).findFirst();

			nameAttr.ifPresent(name -> variantJson.put("name", name));

			variantJson.put("sku", variant.getSkuNumber());
			variantJson.put("height", variant.getPaHeight());
			variantJson.put("depth", variant.getPaLength());
			variantJson.put("width", variant.getPaWidth());
			variantJson.put("weight", variant.getPaWeight());
			variantJson.put("upc", variant.getPaUPC());
			variantJson.put("gtin", variant.getPaVendorNumber());
			variantJson.put("mpn", variant.getPaVendorPartNumber());

			if (variant.getOption_values() != null && !variant.getOption_values().isEmpty() ) {
				variantJson.put("option_values", mapVariantsOptionValues(variant));
			}

			variantsArray.put(variantJson);
		}

		return variantsArray;
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

	private JSONArray mapVariantsOptionValues(Variant variant) {
		JSONArray optionValuesArray = new JSONArray();

		if (variant.option_values != null) {
			for (OptionValue option : variant.option_values) {
				JSONObject optionValueJson = new JSONObject();
				optionValueJson.put("option_display_name", option.option_name);
				optionValueJson.put("label", option.value);
				optionValuesArray.put(optionValueJson);
			}
		}

		return optionValuesArray;
	}
	
	public static Map<String, JSONObject> mapAttributesAndAttLabelsToLocale(List<Variant> variants, String locale) {
	    Map<String, JSONObject> result = new HashMap<>();

	    // Pick the correct label map
	    Map<String, String> labelMap = "fr_CA".equals(locale)
	            ? AttributeLabels.LABELS_FR
	            : AttributeLabels.LABELS_EN;

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

}
