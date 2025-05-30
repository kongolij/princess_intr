package com.constructor.index.dto;

import java.util.List;

public class ProductListGraphQLResponse {

	public Site site;

	public static class Site {
		public ProductConnection products;
	}

	public static class ProductConnection {
		public PageInfo pageInfo;
		public List<ProductEdge> edges;
	}

	public static class PageInfo {
		public boolean hasNextPage;
		public String endCursor;
	}

	public static class ProductEdge {
		public ProductGraphQLResponse.Product node;
	}

}
