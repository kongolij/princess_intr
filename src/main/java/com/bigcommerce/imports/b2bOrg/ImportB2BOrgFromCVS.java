package com.bigcommerce.imports.b2bOrg;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.b2bOrg.Repository.BigCommerceB2BOrgRepository;
import com.bigcommerce.imports.b2bOrg.dto.B2BOrg;
import com.bigcommerce.imports.b2bOrg.mapper.B2bOrgMapper;
import com.opencsv.CSVReader;

//@Component
public class ImportB2BOrgFromCVS implements CommandLineRunner {

    private final BigCommerceB2BOrgRepository bigCommerceB2BOrgRepository;

    public ImportB2BOrgFromCVS(BigCommerceB2BOrgRepository bigCommerceB2BOrgRepository) {
        this.bigCommerceB2BOrgRepository = bigCommerceB2BOrgRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();

//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("PAL_OCCACM_20250702220500098643.csv");
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("PAL_BCACM_20250707072502294796_utf8.csv");
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

            List<B2BOrg> batch = new ArrayList<>();
            List<Future<?>> creditTasks = new ArrayList<>();
            ExecutorService creditExecutor = Executors.newFixedThreadPool(5);

            // Skip header
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                if (row.length < 14) {
                    System.err.printf("⚠️ Skipping malformed row %d: Expected 14 columns%n", i);
                    continue;
                }

                B2BOrg b2bOrg = new B2BOrg();
                b2bOrg.setCustomer_number(parseIntSafe(row, 0));
                b2bOrg.setStatus_1(safeGet(row, 1));
                b2bOrg.setCustomer_attention(safeGet(row, 2));
                b2bOrg.setCustomer_name(safeGet(row, 3));
                b2bOrg.setAddress_1(safeGet(row, 4));
                b2bOrg.setAddress_2(safeGet(row, 5));
                b2bOrg.setAddress_3(safeGet(row, 6));
                b2bOrg.setCity(safeGet(row, 7));
                b2bOrg.setProvince(safeGet(row, 8));
                b2bOrg.setPostal(safeGet(row, 9));
                b2bOrg.setCountry(safeGet(row, 10));
                b2bOrg.setPhone(safeGet(row, 11));
               
                
                b2bOrg.setDummyCompanyEmail(safeGet(row, 12));
                b2bOrg.setDummyFirstName(safeGet(row, 13));
                b2bOrg.setDummyLastName(safeGet(row, 14));
                b2bOrg.setDummyAdminEmail(safeGet(row, 15));
               
                
               
                
                b2bOrg.setCredit_limit(parseIntSafe(row, 16));
                b2bOrg.setCredit_available(parseIntSafe(row, 17));
                
                
                batch.add(b2bOrg);

                if (batch.size() == 10 || i == rows.size() - 1) {
                    // Process batch of 10
                    JSONArray payload = new JSONArray();
                    Map<Integer, B2BOrg> companyIdToOrg = new HashMap<>();

                    for (B2BOrg org : batch) {
                        JSONObject payloadItem = B2bOrgMapper.buildB2BOrgPayload(org);
                        payload.put(payloadItem);
                    }

                    List<Integer> orgIds = bigCommerceB2BOrgRepository.bulkCreateB2BOrganizations(payload);

                    for (int j = 0; j < orgIds.size(); j++) {
                        int orgId = orgIds.get(j);
                        B2BOrg org = batch.get(j);

                        creditTasks.add(creditExecutor.submit(() -> {
                            JSONObject creditPayload = B2bOrgMapper.buildB2BCreditPayload(org);
                            try {
								bigCommerceB2BOrgRepository.updateCompanyCredit(orgId, creditPayload);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                        }));
                    }

                    batch.clear();
                }
            }

            creditExecutor.shutdown();
            creditExecutor.awaitTermination(10, TimeUnit.MINUTES);

            long durationMillis = System.currentTimeMillis() - startTime;
            System.out.printf("✅ Done! Imported orgs. Total time: %.2f min%n", durationMillis / 1000.0 / 60.0);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error while processing CSV: " + e.getMessage());
        }
    }

    private String safeGet(String[] row, int index) {
        return (index < row.length) ? row[index].trim() : null;
    }

    private Integer parseIntSafe(String[] row, int index) {
        String value = safeGet(row, index);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
