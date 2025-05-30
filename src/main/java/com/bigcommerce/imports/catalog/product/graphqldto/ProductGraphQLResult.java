package com.bigcommerce.imports.catalog.product.graphqldto;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProductGraphQLResult {
	private Data data;

	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public static ProductGraphQLResult fromJson(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, ProductGraphQLResult.class);
	}

	public static class Data {
		private Site site;

		public Site getSite() {
			return site;
		}

		public void setSite(Site site) {
			this.site = site;
		}
	}

	public static class Site {
		private Product product;

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}
	}

	public static class Product {
		private int entityId;
		private String name;
		private Variants variants;
		private ProductOptions productOptions;
		private Inventory inventory;
		private Prices prices;
		private Metafields metafields;

		public int getEntityId() {
			return entityId;
		}

		public void setEntityId(int entityId) {
			this.entityId = entityId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Variants getVariants() {
			return variants;
		}

		public void setVariants(Variants variants) {
			this.variants = variants;
		}

		public ProductOptions getProductOptions() {
			return productOptions;
		}

		public void setProductOptions(ProductOptions productOptions) {
			this.productOptions = productOptions;
		}

		public Inventory getInventory() {
			return inventory;
		}

		public void setInventory(Inventory inventory) {
			this.inventory = inventory;
		}

		public Prices getPrices() {
			return prices;
		}

		public void setPrices(Prices prices) {
			this.prices = prices;
		}

		public Metafields getMetafields() {
			return metafields;
		}

		public void setMetafields(Metafields metafields) {
			this.metafields = metafields;
		}
	}

	public static class Variants {
		private List<VariantEdge> edges;

		public List<VariantEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<VariantEdge> edges) {
			this.edges = edges;
		}
	}

	public static class VariantEdge {
		private Variant node;

		public Variant getNode() {
			return node;
		}

		public void setNode(Variant node) {
			this.node = node;
		}
	}

	public static class Variant {
		private String sku;
		private boolean isPurchasable;
		private Inventory inventory;
		private Metafields metafields;

		public String getSku() {
			return sku;
		}

		public void setSku(String sku) {
			this.sku = sku;
		}

		public boolean isPurchasable() {
			return isPurchasable;
		}

		public void setPurchasable(boolean isPurchasable) {
			this.isPurchasable = isPurchasable;
		}

		public Inventory getInventory() {
			return inventory;
		}

		public void setInventory(Inventory inventory) {
			this.inventory = inventory;
		}

		public Metafields getMetafields() {
			return metafields;
		}

		public void setMetafields(Metafields metafields) {
			this.metafields = metafields;
		}
	}

	public static class Inventory {
		private boolean hasVariantInventory;
		private boolean isInStock;
		private Aggregated aggregated;
		private ByLocation byLocation;

		public boolean isHasVariantInventory() {
			return hasVariantInventory;
		}

		public void setHasVariantInventory(boolean hasVariantInventory) {
			this.hasVariantInventory = hasVariantInventory;
		}

		public boolean isInStock() {
			return isInStock;
		}

		public void setInStock(boolean inStock) {
			isInStock = inStock;
		}

		public Aggregated getAggregated() {
			return aggregated;
		}

		public void setAggregated(Aggregated aggregated) {
			this.aggregated = aggregated;
		}

		public ByLocation getByLocation() {
			return byLocation;
		}

		public void setByLocation(ByLocation byLocation) {
			this.byLocation = byLocation;
		}
	}

	public static class Aggregated {
		private int availableToSell;
		private int warningLevel;

		public int getAvailableToSell() {
			return availableToSell;
		}

		public void setAvailableToSell(int availableToSell) {
			this.availableToSell = availableToSell;
		}

		public int getWarningLevel() {
			return warningLevel;
		}

		public void setWarningLevel(int warningLevel) {
			this.warningLevel = warningLevel;
		}
	}

	public static class ByLocation {
		private List<LocationEdge> edges;

		public List<LocationEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<LocationEdge> edges) {
			this.edges = edges;
		}
	}

	public static class LocationEdge {
		private LocationInventory node;

		public LocationInventory getNode() {
			return node;
		}

		public void setNode(LocationInventory node) {
			this.node = node;
		}
	}

	public static class LocationInventory {
		private int locationEntityId;
		private boolean isInStock;
		private int locationEntityTypeId;
		private int availableToSell;

		public int getLocationEntityId() {
			return locationEntityId;
		}

		public void setLocationEntityId(int locationEntityId) {
			this.locationEntityId = locationEntityId;
		}

		public boolean isInStock() {
			return isInStock;
		}

		public void setInStock(boolean inStock) {
			isInStock = inStock;
		}

		public int getLocationEntityTypeId() {
			return locationEntityTypeId;
		}

		public void setLocationEntityTypeId(int locationEntityTypeId) {
			this.locationEntityTypeId = locationEntityTypeId;
		}

		public int getAvailableToSell() {
			return availableToSell;
		}

		public void setAvailableToSell(int availableToSell) {
			this.availableToSell = availableToSell;
		}
	}

	public static class ProductOptions {
		private List<ProductOptionEdge> edges;

		public List<ProductOptionEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<ProductOptionEdge> edges) {
			this.edges = edges;
		}
	}

	public static class ProductOptionEdge {
		private ProductOption node;

		public ProductOption getNode() {
			return node;
		}

		public void setNode(ProductOption node) {
			this.node = node;
		}
	}

	public static class ProductOption {
		private int entityId;
		private String displayName;
		private boolean isRequired;
		private boolean isVariantOption;
		private String __typename;
		private String earliest;
		private String latest;
		private String limitDateBy;

		public int getEntityId() {
			return entityId;
		}

		public void setEntityId(int entityId) {
			this.entityId = entityId;
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public boolean isRequired() {
			return isRequired;
		}

		public void setRequired(boolean required) {
			isRequired = required;
		}

		public boolean isVariantOption() {
			return isVariantOption;
		}

		public void setVariantOption(boolean variantOption) {
			isVariantOption = variantOption;
		}

		public String get__typename() {
			return __typename;
		}

		public void set__typename(String __typename) {
			this.__typename = __typename;
		}

		public String getEarliest() {
			return earliest;
		}

		public void setEarliest(String earliest) {
			this.earliest = earliest;
		}

		public String getLatest() {
			return latest;
		}

		public void setLatest(String latest) {
			this.latest = latest;
		}

		public String getLimitDateBy() {
			return limitDateBy;
		}

		public void setLimitDateBy(String limitDateBy) {
			this.limitDateBy = limitDateBy;
		}
	}

	public static class Prices {
		private PriceRange priceRange;
		private PriceRange retailPriceRange;

		public PriceRange getPriceRange() {
			return priceRange;
		}

		public void setPriceRange(PriceRange priceRange) {
			this.priceRange = priceRange;
		}

		public PriceRange getRetailPriceRange() {
			return retailPriceRange;
		}

		public void setRetailPriceRange(PriceRange retailPriceRange) {
			this.retailPriceRange = retailPriceRange;
		}
	}

	public static class PriceRange {
		private Price min;
		private Price max;

		public Price getMin() {
			return min;
		}

		public void setMin(Price min) {
			this.min = min;
		}

		public Price getMax() {
			return max;
		}

		public void setMax(Price max) {
			this.max = max;
		}
	}

	public static class Price {
		private double value;
		private String currencyCode;

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}

		public String getCurrencyCode() {
			return currencyCode;
		}

		public void setCurrencyCode(String currencyCode) {
			this.currencyCode = currencyCode;
		}
	}

	public static class Metafields {
		private List<MetafieldEdge> edges;

		public List<MetafieldEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<MetafieldEdge> edges) {
			this.edges = edges;
		}
	}

	public static class MetafieldEdge {
		private Metafield node;

		public Metafield getNode() {
			return node;
		}

		public void setNode(Metafield node) {
			this.node = node;
		}
	}

	public static class Metafield {
		private int entityId;
		private String key;
		private String value;

		public int getEntityId() {
			return entityId;
		}

		public void setEntityId(int entityId) {
			this.entityId = entityId;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
