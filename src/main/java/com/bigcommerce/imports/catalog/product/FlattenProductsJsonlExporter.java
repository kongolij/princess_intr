package com.bigcommerce.imports.catalog.product;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class FlattenProductsJsonlExporter implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("merged_product_v2.json (2).json");
        if (inputStream == null) {
            System.err.println("❌ Could not find 'Sample For EPAM_2.json' in resources.");
            return;
        }

        JsonNode root = mapper.readTree(inputStream);
        if (!root.isArray()) {
            System.err.println("❌ JSON root is not an array.");
            return;
        }

        File outputFile = new File("src/main/resources/Flattened_Products_V3.jsonl");
        outputFile.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (JsonNode product : root) {
                writer.write(mapper.writeValueAsString(product));
                writer.newLine();
            }
        }

        System.out.println("✅ Done! JSONL written to: " + outputFile.getAbsolutePath());
        System.out.println("⏱️ Duration (min): " + (System.currentTimeMillis() - startTime) / 60000.0);
        System.exit(0);
    }
}
