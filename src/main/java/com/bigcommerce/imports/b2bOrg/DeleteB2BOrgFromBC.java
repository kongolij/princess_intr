package com.bigcommerce.imports.b2bOrg;

import com.bigcommerce.imports.b2bOrg.Repository.BigCommerceB2BOrgRepository;
import com.bigcommerce.imports.catalog.constants.BigCommerceStoreConfig;
import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//@Component
public class DeleteB2BOrgFromBC implements CommandLineRunner {

    private final BigCommerceB2BOrgRepository bigCommerceB2BOrgRepository;
   
    private static final String ACCESS_TOKEN = BigCommerceStoreConfig.B2B_ACCESS_TOKEN;

    // Use a rate limiter (e.g. 2 deletions per second max)
    private static final RateLimiter rateLimiter = RateLimiter.create(4.0);

    public DeleteB2BOrgFromBC(BigCommerceB2BOrgRepository bigCommerceB2BOrgRepository) {
        this.bigCommerceB2BOrgRepository = bigCommerceB2BOrgRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();

        Map<String, Long> orgMap = bigCommerceB2BOrgRepository.getOrgNumberToCompanyIdMap();
        ExecutorService executor = Executors.newFixedThreadPool(5); // Use 5 threads to avoid too much throttling

        for (Long companyId : orgMap.values()) {
            executor.submit(() -> deleteCompanyByIdWithRetry(companyId));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("✅ Finished deleting companies in " + duration / 1000 + " seconds");
    }

    public void deleteCompanyByIdWithRetry(long companyId) {
        int maxRetries = 5;
        int attempt = 0;
        Random random = new Random();

        while (attempt < maxRetries) {
            try {
                rateLimiter.acquire(); // ensure rate limiting

                String endpoint = String.format("https://api-b2b.bigcommerce.com/api/v3/io/companies/%d", companyId);
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();

                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("authToken", ACCESS_TOKEN);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();

                if (responseCode == 204 || responseCode == 200 ) {
                    System.out.printf("✅ Successfully deleted company ID %d%n", companyId);
                    return;
                } else if (responseCode == 429) {
                	String retryAfter = connection.getHeaderField("Retry-After");
                    int waitSeconds = retryAfter != null ? Integer.parseInt(retryAfter) : 10;
                    int jitter = random.nextInt(1000); // add up to 1s jitter
                    System.out.printf("⏳ Rate limit hit while deleting company ID %d. Retrying in %d seconds...%n", companyId, waitSeconds);
                    Thread.sleep(waitSeconds * 1000L + jitter);
                    
//                    String retryAfter = connection.getHeaderField("Retry-After");
//                    int waitSeconds = retryAfter != null ? Integer.parseInt(retryAfter) : 10;
//                    System.out.printf("⏳ Rate limit hit while deleting company ID %d. Retrying in %d seconds...%n", companyId, waitSeconds);
//                    Thread.sleep(waitSeconds * 1000L);
                } else {
                    InputStream errorStream = connection.getErrorStream();
                    String error = (errorStream != null)
                            ? new BufferedReader(new InputStreamReader(errorStream)).lines().collect(Collectors.joining("\n"))
                            : "(no response body)";
                    System.out.printf("❌ Failed to delete company ID %d: HTTP %d: %s%n", companyId, responseCode, error);
                    return;
                }
            } catch (Exception e) {
                System.out.printf("❌ Error deleting company ID %d: %s%n", companyId, e.getMessage());
            }

            attempt++;
        }

        System.out.printf("❌ Failed to delete company ID %d after %d retries.%n", companyId, maxRetries);
    }
}
