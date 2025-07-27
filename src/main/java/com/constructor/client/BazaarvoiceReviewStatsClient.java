package com.constructor.client;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BazaarvoiceReviewStatsClient {

    private static final String API_KEY = "caus1wSRX1yg9tWgpFziu9rXCjYixY8n73ydrFOmk3BMg"; // Replace with your actual key
    private static final String BAZAARVOICE_STATS_URL = "https://stg.api.bazaarvoice.com/data/statistics.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static class ReviewStats {
        public final double averageRating;
        public final int totalReviews;

        public ReviewStats(double averageRating, int totalReviews) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
        }
    }

    public ReviewStats fetchStatsForProduct(String productId) {
        try {
            String requestUrl = BAZAARVOICE_STATS_URL +
                "?passkey=" + API_KEY +
                "&apiversion=5.4" +
                "&stats=reviews" +
                "&filter=productid:" + productId +
                "&format=json";

            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }

                JsonNode root = objectMapper.readTree(response.toString());
                JsonNode statsNode = root
                    .path("Results").get(0)
                    .path("ProductStatistics")
                    .path("ReviewStatistics");

                double avg = statsNode.path("AverageOverallRating").asDouble(0.0);
                int count = statsNode.path("TotalReviewCount").asInt(0);

                if (count > 0) {
                    System.out.println("✅ Product " + productId + " has " + count + " reviews, average rating: " + avg);
                }

                
                return new ReviewStats(avg, count);
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("Failed to fetch review stats for productId=" + productId + ": " + e.getMessage());
            return new ReviewStats(0.0, 0);
        }
    }
}
