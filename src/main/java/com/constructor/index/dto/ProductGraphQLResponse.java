package com.constructor.index.dto;

import java.util.List;

public class ProductGraphQLResponse {

	public Site site;

	public static class Site {
		public Product product;

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}
	}

	public static class Product {
		public int entityId;
		public String name;
		public String path;
		public String sku;
		public CategoryConnection categories;
		public Prices prices;
		public ImageConnection images;
		public CustomFieldConnection customFields;
		public MetafieldConnection metafields;
		public VariantConnection variants;
		public Inventory inventory;

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

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getSku() {
			return sku;
		}

		public void setSku(String sku) {
			this.sku = sku;
		}

		public CategoryConnection getCategories() {
			return categories;
		}

		public void setCategories(CategoryConnection categories) {
			this.categories = categories;
		}

		public Prices getPrices() {
			return prices;
		}

		public void setPrices(Prices prices) {
			this.prices = prices;
		}

		public ImageConnection getImages() {
			return images;
		}

		public void setImages(ImageConnection images) {
			this.images = images;
		}

		public CustomFieldConnection getCustomFields() {
			return customFields;
		}

		public void setCustomFields(CustomFieldConnection customFields) {
			this.customFields = customFields;
		}

		public MetafieldConnection getMetafields() {
			return metafields;
		}

		public void setMetafields(MetafieldConnection metafields) {
			this.metafields = metafields;
		}

		public VariantConnection getVariants() {
			return variants;
		}

		public void setVariants(VariantConnection variants) {
			this.variants = variants;
		}

		public Inventory getInventory() {
			return inventory;
		}

		public void setInventory(Inventory inventory) {
			this.inventory = inventory;
		}
	}

	public static class Prices {
		public Money price;
		public Money salePrice;

		public Money getPrice() {
			return price;
		}

		public void setPrice(Money price) {
			this.price = price;
		}

		public Money getSalePrice() {
			return salePrice;
		}

		public void setSalePrice(Money salePrice) {
			this.salePrice = salePrice;
		}
	}

	public static class Money {
		public String currencyCode;
		public double value;

		public String getCurrencyCode() {
			return currencyCode;
		}

		public void setCurrencyCode(String currencyCode) {
			this.currencyCode = currencyCode;
		}

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}
	}

	public static class ImageConnection {
		public List<ImageEdge> edges;

		public List<ImageEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<ImageEdge> edges) {
			this.edges = edges;
		}
	}

	public static class ImageEdge {
		public Image node;

		public Image getNode() {
			return node;
		}

		public void setNode(Image node) {
			this.node = node;
		}
	}

	public static class Image {
		public String url;
		public String urlOriginal;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getUrlOriginal() {
			return urlOriginal;
		}

		public void setUrlOriginal(String urlOriginal) {
			this.urlOriginal = urlOriginal;
		}
	}

	public static class CategoryConnection {
		public List<Edge<Category>> edges;

		public List<Edge<Category>> getEdges() {
			return edges;
		}

		public void setEdges(List<Edge<Category>> edges) {
			this.edges = edges;
		}
	}

	public static class Edge<T> {
		public T node;

		public T getNode() {
			return node;
		}

		public void setNode(T node) {
			this.node = node;
		}
	}

	public static class Category {
		public int entityId;
		public String name;
		public String path;
		public MetafieldConnection metafields;

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

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public MetafieldConnection getMetafields() {
			return metafields;
		}

		public void setMetafields(MetafieldConnection metafields) {
			this.metafields = metafields;
		}
	}

	public static class MetafieldConnection {
		
		public PageInfo pageInfo;
		
		public List<MetafieldEdge> edges;

		public List<MetafieldEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<MetafieldEdge> edges) {
			this.edges = edges;
		}
	}

	public static class MetafieldEdge {
		public Metafield node;

		public Metafield getNode() {
			return node;
		}

		public void setNode(Metafield node) {
			this.node = node;
		}
	}

	public static class Metafield {
		public int entityId;
		public String key;
		public String value;

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

	public static class CustomFieldConnection {
		public List<CustomFieldEdge> edges;

		public List<CustomFieldEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<CustomFieldEdge> edges) {
			this.edges = edges;
		}
	}

	public static class CustomFieldEdge {
		public CustomField node;

		public CustomField getNode() {
			return node;
		}

		public void setNode(CustomField node) {
			this.node = node;
		}

	}

	public static class CustomField {
		public int entityId;
		public String name;
		public String value;

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

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class VariantConnection {
		
		public PageInfo pageInfo;
		
		public List<VariantEdge> edges;

		public List<VariantEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<VariantEdge> edges) {
			this.edges = edges;
		}

		public PageInfo getPageInfo() {
			return pageInfo;
		}

		public void setPageInfo(PageInfo pageInfo) {
			this.pageInfo = pageInfo;
		}
		
	}

	public static class VariantEdge {
		public Variant node;

		public Variant getNode() {
			return node;
		}

		public void setNode(Variant node) {
			this.node = node;
		}
	}

	public static class Variant {
		public int entityId;
		public String sku;
		public boolean isPurchasable;
		public Dimension height;
		public Dimension width;
		public Dimension weight;
		public Prices prices;
		public Image defaultImage;
		public VariantInventory inventory;
		public OptionConnection options;
		public MetafieldConnection metafields;

		public int getEntityId() {
			return entityId;
		}

		public void setEntityId(int entityId) {
			this.entityId = entityId;
		}

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

		public Dimension getHeight() {
			return height;
		}

		public void setHeight(Dimension height) {
			this.height = height;
		}

		public Dimension getWidth() {
			return width;
		}

		public void setWidth(Dimension width) {
			this.width = width;
		}

		public Dimension getWeight() {
			return weight;
		}

		public void setWeight(Dimension weight) {
			this.weight = weight;
		}

		public Prices getPrices() {
			return prices;
		}

		public void setPrices(Prices prices) {
			this.prices = prices;
		}

		public Image getDefaultImage() {
			return defaultImage;
		}

		public void setDefaultImage(Image defaultImage) {
			this.defaultImage = defaultImage;
		}

		public VariantInventory getInventory() {
			return inventory;
		}

		public void setInventory(VariantInventory inventory) {
			this.inventory = inventory;
		}

		public OptionConnection getOptions() {
			return options;
		}

		public void setOptions(OptionConnection options) {
			this.options = options;
		}

		public MetafieldConnection getMetafields() {
			return metafields;
		}

		public void setMetafields(MetafieldConnection metafields) {
			this.metafields = metafields;
		}
	}

	public static class Dimension {
		public double value;
		public String unit;

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}
	}

	public static class Inventory {
		public boolean hasVariantInventory;

		public boolean isHasVariantInventory() {
			return hasVariantInventory;
		}

		public void setHasVariantInventory(boolean hasVariantInventory) {
			this.hasVariantInventory = hasVariantInventory;
		}
	}

	public static class VariantInventory {
		public boolean isInStock;
		public AggregatedInventory aggregated;
		public LocationInventoryConnection byLocation;

		public boolean isInStock() {
			return isInStock;
		}

		public void setInStock(boolean inStock) {
			isInStock = inStock;
		}

		public AggregatedInventory getAggregated() {
			return aggregated;
		}

		public void setAggregated(AggregatedInventory aggregated) {
			this.aggregated = aggregated;
		}

		public LocationInventoryConnection getByLocation() {
			return byLocation;
		}

		public void setByLocation(LocationInventoryConnection byLocation) {
			this.byLocation = byLocation;
		}
	}

	public static class AggregatedInventory {
		public int availableToSell;
		public int warningLevel;

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

	public static class LocationInventoryConnection {
		public List<LocationInventoryEdge> edges;

		public List<LocationInventoryEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<LocationInventoryEdge> edges) {
			this.edges = edges;
		}
	}

	public static class LocationInventoryEdge {
		public LocationInventory node;

		public LocationInventory getNode() {
			return node;
		}

		public void setNode(LocationInventory node) {
			this.node = node;
		}
	}

	public static class LocationInventory {
		public int locationEntityId;
		public boolean isInStock;
		public String locationEntityTypeId;
		public int availableToSell;

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

		public String getLocationEntityTypeId() {
			return locationEntityTypeId;
		}

		public void setLocationEntityTypeId(String locationEntityTypeId) {
			this.locationEntityTypeId = locationEntityTypeId;
		}

		public int getAvailableToSell() {
			return availableToSell;
		}

		public void setAvailableToSell(int availableToSell) {
			this.availableToSell = availableToSell;
		}
	}

	public static class OptionConnection {
		public List<OptionEdge> edges;

		public List<OptionEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<OptionEdge> edges) {
			this.edges = edges;
		}
	}

	public static class OptionEdge {
		public Option node;

		public Option getNode() {
			return node;
		}

		public void setNode(Option node) {
			this.node = node;
		}
	}

	public static class Option {
		public int entityId;
		public String displayName;
		public OptionValueConnection values;

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

		public OptionValueConnection getValues() {
			return values;
		}

		public void setValues(OptionValueConnection values) {
			this.values = values;
		}
	}

	public static class OptionValueConnection {
		public List<OptionValueEdge> edges;

		public List<OptionValueEdge> getEdges() {
			return edges;
		}

		public void setEdges(List<OptionValueEdge> edges) {
			this.edges = edges;
		}
	}

	public static class OptionValueEdge {
		public OptionValue node;

		public OptionValue getNode() {
			return node;
		}

		public void setNode(OptionValue node) {
			this.node = node;
		}
	}

	public static class OptionValue {
		public int entityId;
		public String label;

		public int getEntityId() {
			return entityId;
		}

		public void setEntityId(int entityId) {
			this.entityId = entityId;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}
	}
	
	public static class PageInfo {
		
		public boolean hasNextPage;
		public String endCursor;
		
		public boolean isHasNextPage() {
			return hasNextPage;
		}
		public void setHasNextPage(boolean hasNextPage) {
			this.hasNextPage = hasNextPage;
		}
		public String getEndCursor() {
			return endCursor;
		}
		public void setEndCursor(String endCursor) {
			this.endCursor = endCursor;
		}
		  
		  
		} 
}