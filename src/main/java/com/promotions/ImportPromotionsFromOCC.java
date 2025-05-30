package com.promotions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promotions.dto.PromotionWrapper;
import com.promotions.dto.PromotionWrapper.Promotion;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

//@Component
public class ImportPromotionsFromOCC implements CommandLineRunner {

	static class TemplateStats {
		int total = 0;
		int active = 0;

		void add(boolean isActive) {
			total++;
			if (isActive)
				active++;
		}

		int getInactive() {
			return total - active;
		}
	}

	@Override
	public void run(String... args) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Promotions_no_exiry.json");
		if (inputStream == null) {
			System.err.println("❌ promotions.json not found in classpath.");
			return;
		}

		PromotionWrapper wrapper = objectMapper.readValue(inputStream, PromotionWrapper.class);
		List<Promotion> promotionsData = wrapper.getPromotion();

		// Parse JSON root and get "promotion" list
		Map<String, Object> root = objectMapper.readValue(inputStream, new TypeReference<>() {
		});
		Object promoListObj = root.get("promotion");

		if (!(promoListObj instanceof List)) {
			System.err.println("❌ 'promotion' key is not an array.");
			return;
		}

		List<Map<String, Object>> promotions = (List<Map<String, Object>>) promoListObj;

		int totalCount = promotions.size();
		int activeCount = 0;
		Map<String, TemplateStats> templateStatsMap = new TreeMap<>(); // sorted by template

		for (Map<String, Object> promo : promotions) {
			String template = (String) promo.get("template");

			boolean isEnabled = Boolean.TRUE.equals(promo.get("enabled"));
			boolean isNotExpired = true;

			Object endDateObj = promo.get("endDate");
			if (endDateObj instanceof String) {
				try {
					Instant endDate = Instant.parse((String) endDateObj);
					isNotExpired = endDate.isAfter(Instant.now());
				} catch (Exception e) {
					System.err.println("⚠️ Invalid endDate format: " + endDateObj);
					isNotExpired = false;
				}
			}

			boolean isActive = isEnabled && isNotExpired;

			if (isActive) {
				activeCount++;
			}

			if (template != null) {
				templateStatsMap.computeIfAbsent(template, k -> new TemplateStats()).add(isActive);
			}
		}

		// Output unique templates
		System.out.println("📋 Unique Promotion Templates:");
		templateStatsMap.keySet().forEach(System.out::println);

		// Output overall summary
		System.out.println("\n📊 Promotion Summary:");
		System.out.println("Total promotions: " + totalCount);
		System.out.println("Active promotions: " + activeCount);

		// Output per-template breakdown
		System.out.println("\n📈 Breakdown by Template Type:");
		System.out.printf("%-50s %-10s %-10s %-10s%n", "Template Type", "Total", "Active", "Inactive");
		System.out.println("=".repeat(85));
		for (Map.Entry<String, TemplateStats> entry : templateStatsMap.entrySet()) {
			TemplateStats stats = entry.getValue();
			System.out.printf("%-50s %-10d %-10d %-10d%n", entry.getKey(), stats.total, stats.active,
					stats.getInactive());
		}

		System.exit(0);
	}
}
