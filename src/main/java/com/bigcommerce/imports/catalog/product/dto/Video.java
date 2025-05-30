package com.bigcommerce.imports.catalog.product.dto;

import java.util.List;

public class Video {

	private List<VideoEntry> videos_en;
	private List<VideoEntry> videos_fr;

	// Getters and setters
	public List<VideoEntry> getVideos_en() {
		return videos_en;
	}

	public void setVideos_en(List<VideoEntry> videos_en) {
		this.videos_en = videos_en;
	}

	public List<VideoEntry> getVideos_fr() {
		return videos_fr;
	}

	public void setVideos_fr(List<VideoEntry> videos_fr) {
		this.videos_fr = videos_fr;
	}

	// Inner class
	public static class VideoEntry {
		private String url;
		private int order;

		// Getters and setters
		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public int getOrder() {
			return order;
		}

		public void setOrder(int order) {
			this.order = order;
		}
	}
}
