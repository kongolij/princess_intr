package com.bigcommerce.imports.b2bOrg;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        
        
      

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("PAL_OCCACM_20250702220500098643.csv");

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

            List<B2BOrg> b2bOrgList = new ArrayList<>();

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                if (row.length < 10) {
                    System.err.println("⚠️ Skipped malformed row at index " + i + ": Expected at least 10 columns, got " + row.length);
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
                b2bOrg.setCredit_limit(parseIntSafe(row, 12));
                b2bOrg.setCredit_available(parseIntSafe(row, 13));

                JSONObject b2bOrgPayload =  B2bOrgMapper.buildB2BOrgPayload(b2bOrg);
                
                long orgId= bigCommerceB2BOrgRepository.createB2BOrganization(b2bOrgPayload);
                
                JSONObject creditPayload = B2bOrgMapper.buildB2BCreditPayload(b2bOrg);
                bigCommerceB2BOrgRepository.updateCompanyCredit(orgId, creditPayload);
                
                b2bOrgList.add(b2bOrg);
            }

            // TODO: Call a service to process the b2bOrgList if needed

            long durationMillis = System.currentTimeMillis() - startTime;
            System.out.printf("✅ Done! Imported %d B2B Orgs. Running time: %.2f min%n", b2bOrgList.size(), durationMillis / 1000.0 / 60.0);

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

    private Boolean parseBooleanSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
