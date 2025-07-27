package com.constructor.client;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class ConstructorApis {

    private static final String API_TOKEN = "tok_5NCEybey4GqC52oL"; // private token
    private static final String API_KEY = "key_on1j1t2BjFymbXpC";   // public key

    public void createOrReplaceVariations(String jsonPayload) throws Exception {
        String endpoint = "https://ac.cnstrc.com/v2/variations?key=" + API_KEY + "&force=true";
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Basic auth header
        String basicAuth = Base64.getEncoder().encodeToString((API_TOKEN + ":").getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);

        // Configure connection
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Write JSON payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Handle response
        int statusCode = connection.getResponseCode();
        if (statusCode >= 200 && statusCode < 300) {
            System.out.println("✅ Variations updated successfully.");
        } else {
            throw new RuntimeException("❌ Failed: HTTP error code: " + statusCode);
        }
    }
}

