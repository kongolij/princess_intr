package com.bigcommerce.imports.catalog.product;

import java.text.Normalizer;
import java.util.Locale;

public class SlugGenerator {

	public static String generateSlug(String input) {
        if (input == null) return null;

        String lowercase = input.toLowerCase(Locale.FRENCH);
        String normalized = Normalizer.normalize(lowercase, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("(^-|-$)", "");
    }
}