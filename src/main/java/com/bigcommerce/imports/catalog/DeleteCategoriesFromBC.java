package com.bigcommerce.imports.catalog;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.service.BigCommerceService;

//@Component
public class DeleteCategoriesFromBC implements CommandLineRunner {

	private final BigCommerceService bigCommerceCategoryService;

	public DeleteCategoriesFromBC(BigCommerceService bigCommerceCategoryService) {

		this.bigCommerceCategoryService = bigCommerceCategoryService;
	}

	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		List<Integer> categoryIds = bigCommerceCategoryService.fetchAndFlattenCategories(7, "en");
		System.out.println(" size  " + categoryIds.size());
//		bigCommerceCategoryService.deleteCategories(categoryIds, "en");
		
		System.out.println("✅ Done!");
		System.exit(0);

	}

}
