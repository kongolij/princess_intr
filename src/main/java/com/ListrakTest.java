package com;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ListrakTest {
    public static void main(String[] args) throws Exception {
        String token = getAuthToken();
        if (token != null) {
        	sendTransactionalEmailTest(token);
        }
        System.exit(0);
    }

    private static String getAuthToken() throws IOException {
        URL url = new URL("https://auth.listrak.com/OAuth2/Token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
       
        
        String clientId = "bpi5eb3uavm1rhf839ju";
        String clientSecret = "QZzejPkNAfmy2jl4lN7yXSQe0EPc3EQLumpTpWPFpuk";
        String scope = "Message";
        

        
        String body = "grant_type=client_credentials"
        	    + "&client_id=" + URLEncoder.encode(clientId, "UTF-8")
        	    + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
        	    + "&scope=" + URLEncoder.encode("Message", "UTF-8");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            String response = readResponse(conn.getInputStream());
            String accessToken = extractToken(response);
            System.out.println("Access token: " + accessToken);
            return accessToken;
        } else {
            System.err.println("Failed to get token. HTTP " + responseCode);
            System.err.println(readResponse(conn.getErrorStream()));
            return null;
        }
    }

    private static void sendTransactionalEmailTest(String token) throws IOException {
        URL url = new URL("https://api.listrak.com/email/v1/List/345025/TransactionalMessage/11560579/Message");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Prevent auto-following redirects (e.g., POST → GET on 302)
        conn.setInstanceFollowRedirects(false);

        // Setup request
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");

        // JSON body to send
        String jsonBody = """
        {
          "emailAddress": "jimmy_kongoli@epam.com",
          "segmentationFieldValues": [
            {
              "segmentationFieldId": 37453,
              "value": "https://your-site.com/reset-password?token=abc123"
            }
          ]
        }
        """;

        // Send request body
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Log method & redirect
        System.out.println("Request Method: " + conn.getRequestMethod());
        int responseCode = conn.getResponseCode();
        System.out.println("HTTP Response Code: " + responseCode);
        String location = conn.getHeaderField("Location");
        if (location != null) {
            System.out.println("Redirect Location: " + location);
        }

        // Read and print response body (success or error)
        InputStream is = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                System.out.println("Response Body:");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }

        // Print result
        if (responseCode == 200 || responseCode == 201) {
            System.out.println("✅ Message sent successfully!");
        } else {
            System.err.println("❌ Failed to send message. HTTP " + responseCode);
        }
    }
    
    private static void sendTransactionalEmail(String token) throws IOException {
        URL url = new URL("https://api.listrak.com/email/v1/List/345025/TransactionalMessage/11560579/Message");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");

        String jsonBody = """
        {
          "emailAddress": "jimmy_kongoli@epam.com",
          "segmentationFieldValues": [
            {
              "fieldId": 37453,
              "value": "https://your-site.com/reset-password?token=abc123"
            }
          ]
        }
        """;

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            System.out.println("Message sent successfully!");
        } else {
            System.err.println("Failed to send message. HTTP " + responseCode);
           
        }
    }

    private static String readResponse(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        return result.toString();
    }

    private static String extractToken(String json) {
        int index = json.indexOf("\"access_token\":\"");
        if (index != -1) {
            int start = index + 16;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        }
        return null;
    }
}
