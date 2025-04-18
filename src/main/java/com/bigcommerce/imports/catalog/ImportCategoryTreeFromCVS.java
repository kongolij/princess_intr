package com.bigcommerce.imports.catalog;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.constants.CategoryFeedHeaders;
import com.bigcommerce.imports.catalog.constants.LocaleConstants;
import com.bigcommerce.imports.catalog.dto.CategoryNode;
import com.bigcommerce.imports.catalog.service.BigCommerceService;
import com.opencsv.CSVReader;

//@Component
public class ImportCategoryTreeFromCVS implements CommandLineRunner {

	private final BigCommerceService bigCommerceCategoryService;
	
//	static int CATEGOU_TREE_ID=7;  // jimmy store
//    static int CATEGOU_TREE_ID=2;  // PAL store
    public static int CATEGOU_TREE_ID=2;  // EPAM-PAL-DE store
//    static int CATEGOU_TREE_ID=2;  // PAL store
	

	public ImportCategoryTreeFromCVS(BigCommerceService bigCommerceCategoryService) {

		this.bigCommerceCategoryService = bigCommerceCategoryService;
	}

	@Override
	public void run(String... args) {

		long startTime = System.currentTimeMillis(); // Start timing

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("Collections_20250324194123.csv");

		if (inputStream == null) {
			System.err.println("❌ CSV file not found in resources!");
			return;
		}

		try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
			List<String[]> rows = reader.readAll();

			if (rows.isEmpty()) {
				System.out.println("⚠️ CSV is empty!");
				return;
			}

			String[] headers = rows.get(0); // first row is header
			List<CategoryNode> rootCategories = buildCategoryTree(rows, headers);
			
			

			System.out.println("✅ Root categories: " + rootCategories.size());
			// printCategoryTree(rootCategories, 0);

			int totalCount = countTotalNodes(rootCategories);
			int longEnCount = countLongNames(rootCategories, "en");
			int longFrCount = countLongNames(rootCategories, "fr_CA");

			System.out.println("✅ Done! Total categories imported: " + totalCount);
			System.out.println("✅ Max depth : " + getMaxDepth(rootCategories));
			System.out.println("✅ Categories with 'displayName_en' > 50 chars: " + longEnCount);
			System.out.println("✅ Categories with 'displayName_fr_CA' > 50 chars: " + longFrCount);
			printLongEnglishNames(rootCategories);

			truncateLongEnglishNames(rootCategories, 50);

//			Map<String, Integer> categoriesForTheChannel = bigCommerceCategoryService.getFlattenedCategoryNameToIdMap("en",7);
			Map<String, Integer> categoriesForTheChannel1 = bigCommerceCategoryService
					.getBCExternalToInternalCategoryMap("en");
//			checkDuplicateNames(rootCategories);

//			System.out.println("✅ Existing Category count : " + categoriesForTheChannel.size());
			System.out.println("✅ Existing Category count IDS : " + categoriesForTheChannel1.size());
			
			printUnmatchedCategories( rootCategories, categoriesForTheChannel1, "en");
//			bigCommerceCategoryService.importCategoryTreeInThreads(rootCategories, "en", CATEGOU_TREE_ID, categoriesForTheChannel1);

			int missingImageCount = countMissingImages(rootCategories);
			System.out.println("🖼️ Categories missing imageFileName: " + missingImageCount);

			long endTime = System.currentTimeMillis(); // End timing
			long durationMillis = endTime - startTime;
			double durationMinutes = durationMillis / 1000.0 / 60.0;

			System.out.println("✅ Done! runnig time in min " + durationMinutes); 
			System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("❌ Error while processing CSV: " + e.getMessage());
		}
	}

	public List<CategoryNode> buildCategoryTree(List<String[]> rows, String[] headers) {
		Map<String, CategoryNode> nodeMap = new HashMap<>();
		Set<String> allChildIds = new HashSet<>();

		for (int i = 1; i < rows.size(); i++) {
			String[] row = rows.get(i);

		
			String id = getValue(headers, row, CategoryFeedHeaders.ID);
			String name = getValue(headers, row, CategoryFeedHeaders.DISPLAY_NAME_EN);
			if (name != null && name.length() > 50) {
				System.out.println("⚠️ Long name (" + name.length() + "): " + name);
			}

			String frName = getValue(headers, row, CategoryFeedHeaders.DISPLAY_NAME_FR);
			String description = getValue(headers, row, CategoryFeedHeaders.DESCRIPTION_EN);
			String frDescription = getValue(headers, row, CategoryFeedHeaders.DISPLAY_NAME_FR);
			String slug = getValue(headers, row, CategoryFeedHeaders.SEO_URL_SLUG);
			
			String isActive = getValue(headers, row, CategoryFeedHeaders.ACTIVE);
			boolean isActiveAsBoolean = "TRUE".equalsIgnoreCase(isActive);

	
			String rawImageFileNames = getValue(headers, row, CategoryFeedHeaders.IMAGE_FILE_NAMES);
			// we will pick up one first filename
			String imageFileName = (rawImageFileNames != null && !rawImageFileNames.isBlank())
			        ? rawImageFileNames.split(",")[0].trim()
			        : null;
			
			
			CategoryNode node = new CategoryNode(id, 
					name, Map.of(LocaleConstants.EN, name, LocaleConstants.FR, frName), 
					null, 
					description,Map.of(LocaleConstants.EN, description, LocaleConstants.FR, frDescription), 
					slug, 
					new ArrayList<>()
					);

			node.setActive(isActiveAsBoolean);
			node.setImageFileName(imageFileName);

			String childSks = getValue(headers, row, CategoryFeedHeaders.CHILD_CATEGORIES);
			if (childSks != null && !childSks.isBlank()) {
				List<String> childIds = Arrays.stream(childSks.split(",")).map(String::trim).filter(s -> !s.isEmpty())
						.collect(Collectors.toList());

				node.getChildSkList().addAll(childIds);
				allChildIds.addAll(childIds);
			}

			nodeMap.put(id, node);
		}

		for (CategoryNode parent : nodeMap.values()) {
			for (String childId : parent.getChildSkList()) {
				CategoryNode child = nodeMap.get(childId);
				if (child != null) {
					parent.addChild(child);
					child.setParentId(parent.getId());
				}
			}
		}

		return nodeMap.values().stream().filter(node -> !allChildIds.contains(node.getId()))
				.collect(Collectors.toList());

	}

	private static void printCategoryTree(List<CategoryNode> nodes, int level) {
		for (CategoryNode node : nodes) {
			System.out.println("  ".repeat(level) + node.getId() + " " + node.getSlug());
			printCategoryTree(node.getChildren(), level + 1);
		}
	}

	private static int countTotalNodes(List<CategoryNode> roots) {
		int[] count = { 0 };
		roots.forEach(root -> countNodesRecursive(root, count));
		return count[0];
	}

	private static void countNodesRecursive(CategoryNode node, int[] count) {
		count[0]++;
		for (CategoryNode child : node.getChildren()) {
			countNodesRecursive(child, count);
		}
	}

	private static String getValue(String[] headers, String[] row, String columnName) {
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].equalsIgnoreCase(columnName)) {
				return i < row.length ? row[i] : "";
			}
		}
		return "";
	}

	private static int getMaxDepth(List<CategoryNode> nodes) {
		int max = 0;
		for (CategoryNode node : nodes) {
			max = Math.max(max, getDepthRecursive(node, 1));
		}
		return max;
	}

	private static int getDepthRecursive(CategoryNode node, int currentDepth) {
		if (node.getChildren().isEmpty()) {
			return currentDepth;
		}
		int maxChildDepth = currentDepth;
		for (CategoryNode child : node.getChildren()) {
			maxChildDepth = Math.max(maxChildDepth, getDepthRecursive(child, currentDepth + 1));
		}
		return maxChildDepth;
	}

	private int countLongNames(List<CategoryNode> nodes, String locale) {
		int[] count = { 0 };
		for (CategoryNode node : nodes) {
			countLongNamesRecursive(node, locale, count);
		}
		return count[0];
	}

	private void countLongNamesRecursive(CategoryNode node, String locale, int[] count) {
		String name = node.getLocalizedName().get(locale);
		if (name != null && name.length() > 50) {
			count[0]++;
		}
		for (CategoryNode child : node.getChildren()) {
			countLongNamesRecursive(child, locale, count);
		}
	}

	private void printLongEnglishNames(List<CategoryNode> nodes) {
		for (CategoryNode node : nodes) {
			printLongEnglishNamesRecursive(node);
		}
	}

	private void printLongEnglishNamesRecursive(CategoryNode node) {
		String enName = node.getLocalizedName().get("en");
		if (enName != null && enName.length() > 50) {
			System.out.println("🟡 ID: " + node.getId() + " | Name (" + enName.length() + "): " + enName);
		}
		for (CategoryNode child : node.getChildren()) {
			printLongEnglishNamesRecursive(child);
		}
	}

	private void truncateLongEnglishNames(List<CategoryNode> nodes, int maxLength) {
		for (CategoryNode node : nodes) {
			truncateRecursive(node, maxLength);
		}
	}

	private void truncateRecursive(CategoryNode node, int maxLength) {
		String enName = node.getLocalizedName().get("en");

		if (enName != null && enName.length() > maxLength) {
			System.out.println("✂️ Truncating EN name for ID " + node.getId() + " from " + enName.length() + " → "
					+ maxLength + " chars");

			String truncated = enName.substring(0, maxLength).trim();

			// Clone the original map to make it mutable
			Map<String, String> clonedMap = new HashMap<>(node.getLocalizedName());
			clonedMap.put("en", truncated);

			node.setLocalizedName(clonedMap); // Replace with mutable copy
			node.setName(truncated); // Also update top-level name
		}

		for (CategoryNode child : node.getChildren()) {
			truncateRecursive(child, maxLength);
		}
	}

	private void checkDuplicateNames(List<CategoryNode> categoryTree) {
		Map<String, List<String>> nameToIds = new HashMap<>();

		collectNamesRecursive(categoryTree, nameToIds);

		for (Map.Entry<String, List<String>> entry : nameToIds.entrySet()) {
			if (entry.getValue().size() > 1) {
				System.out.println("⚠️ Duplicate name: '" + entry.getKey() + "' used by IDs: " + entry.getValue());
			}
		}
	}

	private void collectNamesRecursive(List<CategoryNode> nodes, Map<String, List<String>> nameToIds) {
		for (CategoryNode node : nodes) {
			String name = node.getName();
			nameToIds.computeIfAbsent(name, k -> new ArrayList<>()).add(node.getId());
			collectNamesRecursive(node.getChildren(), nameToIds);
		}
	}
	
	
	private void printUnmatchedCategories(List<CategoryNode> rootCategories, Map<String, Integer> existingCategoryMap, String locale) {
	    List<String> unmatchedDetails = new ArrayList<>();
	    collectUnmatchedCategoryExternalIds(rootCategories, existingCategoryMap, locale, unmatchedDetails);

	    if (unmatchedDetails.isEmpty()) {
	        System.out.println("✅ All external IDs from CSV matched existing BigCommerce categories.");
	    } else {
	        System.out.println("❌ Categories with external IDs not found in BigCommerce:");
	        unmatchedDetails.forEach(System.out::println);
	        System.out.println("❌ Total unmatched: " + unmatchedDetails.size());
	    }
	}

	private void collectUnmatchedCategoryExternalIds(List<CategoryNode> nodes, Map<String, Integer> existingCategoryMap, String locale, List<String> unmatchedDetails) {
	    for (CategoryNode node : nodes) {
	        String externalId = node.getId(); // or getId(), depending on your model
	        String displayName = node.getName();

	        if (externalId != null && !existingCategoryMap.containsKey(externalId)) {
	            unmatchedDetails.add("ID: " + externalId + ", Name: " + displayName);
	        }

	        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
	            collectUnmatchedCategoryExternalIds(node.getChildren(), existingCategoryMap, locale, unmatchedDetails);
	        }
	    }
	}

	private int countMissingImages(List<CategoryNode> nodes) {
	    int[] count = {0};
	    for (CategoryNode node : nodes) {
	        countMissingImagesRecursive(node, count);
	    }
	    return count[0];
	}

	private void countMissingImagesRecursive(CategoryNode node, int[] count) {
	    String imageFileName = node.getImageFileName();
	    if (imageFileName == null || imageFileName.isBlank()) {
	        count[0]++;
	    }

	    for (CategoryNode child : node.getChildren()) {
	        countMissingImagesRecursive(child, count);
	    }
	}
	
	

}
