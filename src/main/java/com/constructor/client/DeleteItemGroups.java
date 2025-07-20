package com.constructor.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.service.BigCommerceGraphQlService;
import com.bigcommerce.imports.catalog.service.BigCommerceService;
import com.constructor.index.dto.ProductGraphQLResponse;
import com.constructor.index.mapper.ConstructorJsonlProductMapper;
import com.opencsv.CSVWriter;

//@Component
public class DeleteItemGroups implements CommandLineRunner {

//		final String dev-key = "key_879PE9ZDeOIU8rND";

	@Override
	public void run(String... args) throws Exception {

		String apiToken = "tok_5NCEybey4GqC52oL";
		String apiKey = "key_on1j1t2BjFymbXpC"; // public API key

		String section = "Products";
		String clientId = "springboot-app-1.0";
		
		String urlString = String.format(
			    "https://ac.cnstrc.com/v1/item_groups?key=%s&section=%s&c=%s",
			    URLEncoder.encode(apiKey, "UTF-8"),
			    URLEncoder.encode(section, "UTF-8"),
			    URLEncoder.encode(clientId, "UTF-8")
			);
		
		
//		String urlString = "https://ac.cnstrc.com/v1/item_groups";

		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("DELETE");
		connection.setRequestProperty("Accept", "application/json");

		// Set Basic Auth (apiToken as username, blank password)
		String basicAuth = Base64.getEncoder().encodeToString((apiToken + ":").getBytes(StandardCharsets.UTF_8));
		connection.setRequestProperty("Authorization", "Basic " + basicAuth);

		int responseCode = connection.getResponseCode();
		System.out.println("🔁 Response Code: " + responseCode);

		BufferedReader reader = new BufferedReader(
				new InputStreamReader((responseCode >= 200 && responseCode < 300) ? connection.getInputStream()
						: connection.getErrorStream()));

		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}

		reader.close();
		connection.disconnect();

		System.out.println("✅ Delete request complete.");
		System.exit(0);
	}

}
