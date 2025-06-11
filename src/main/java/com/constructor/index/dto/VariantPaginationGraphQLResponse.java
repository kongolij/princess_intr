package com.constructor.index.dto;

import java.util.List;

public class VariantPaginationGraphQLResponse {

	public class VariantPaginationResponse {
		  public Site site;

		  public static class Site {
		    public Product product;
		  }

		  public static class Product {
		    public VariantConnection variants;
		  }

		  public static class VariantConnection {
		    public PageInfo pageInfo;
		    public List<VariantEdge> edges;
		  }

		  public static class PageInfo {
		    public boolean hasNextPage;
		    public String endCursor;
		  }

		  public static class VariantEdge {
		    public Variant node;
		  }

		  public static class Variant {
		    public int entityId;
		    public String sku;
		    // and other fields you need
		  }
		}

}
