package com.constructor.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class ConstructorApiClient implements CommandLineRunner {

//	final String dev-key = "key_879PE9ZDeOIU8rND";

	@Override
	public void run(String... args) throws Exception {
		
		String apiToken = "tok_5NCEybey4GqC52oL";
		String apiKey = "key_on1j1t2BjFymbXpC"; // public API key

//		File itemsCsv = null;
		File variationsCsv = null;
		File itemGroupsCsv = null;

//		File itemGroupsCsv = new File("item_groups.csv");
		
		
//		File itemGroupsCsv = new File("C:\\bigComerce\\catalogImport\\princess_intr\\target\\output\\index_en\\item_groups.csv");
		File itemsCsv = new File("C:\\bigComerce\\catalogImport\\princess_intr\\target\\output\\index_en\\item.jsonl");
//		File variationsCsv = new File("C:\\bigComerce\\catalogImport\\princess_intr\\target\\output\\variant_en\\variations.jsonl");

		uploadFullCatalogToConstructor(apiToken, apiKey, itemsCsv, variationsCsv, itemGroupsCsv);
//		uploadPartialCatalogToConstructor(apiToken, apiKey, itemsCsv, variationsCsv, itemGroupsCsv);
		System.out.printf("✅ Done!");

		System.exit(0);

	}

	public static void uploadFullCatalogToConstructor(String apiToken, String apiKey, File itemsCsv, File variationsCsv,
			File itemGroupsCsv) throws Exception {
		String urlString = null;
		if (itemGroupsCsv!=null ) {
			 urlString = "https://ac.cnstrc.com/v1/catalog?key=" + URLEncoder.encode(apiKey, "UTF-8") + "&format=csv";
		}else {
		      urlString = "https://ac.cnstrc.com/v1/catalog?key=" + URLEncoder.encode(apiKey, "UTF-8") + "&format=jsonl"; // Required
																															// //																													// files
		}
		String boundary = "----Boundary" + System.currentTimeMillis();
		String lineFeed = "\r\n";

		HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
		connection.setRequestMethod("PUT");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

//		dG9rXzVOQ0V5YmV5NEdxQzUyb0w6
		// Basic Auth with apiToken as username, no password
		String basicAuth = Base64.getEncoder().encodeToString((apiToken + ":").getBytes(StandardCharsets.UTF_8));
		connection.setRequestProperty("Authorization", "Basic " + basicAuth);

		try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
			if (itemsCsv != null) {
				writeFilePart(writer, "items", itemsCsv, boundary, lineFeed);
			}
			if (variationsCsv != null) {
				writeFilePart(writer, "variations", variationsCsv, boundary, lineFeed);
			}
			if (itemGroupsCsv != null) {
				writeFilePart(writer, "item_groups", itemGroupsCsv, boundary, lineFeed);
			}

			// End of multipart
			writer.writeBytes("--" + boundary + "--" + lineFeed);
			writer.flush();
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
			System.out.println("✅ Catalog uploaded successfully to Constructor.io");
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorBody = errorScanner.useDelimiter("\\A").hasNext() ? errorScanner.next() : "";
				throw new RuntimeException("❌ Upload failed. Status: " + responseCode + ". Error: " + errorBody);
			}
		}
	}
	
	public static void uploadPartialCatalogToConstructor(String apiToken, String apiKey, File itemsCsv, File variationsCsv,
			File itemGroupsCsv) throws Exception {
		String urlString = "https://ac.cnstrc.com/v1/catalog?key=" 
			+ URLEncoder.encode(apiKey, "UTF-8") 
			+ "&format=jsonl"; // Required
																															// //
																															// files
		String boundary = "----Boundary" + System.currentTimeMillis();
		String lineFeed = "\r\n";

		HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
		connection.setRequestMethod("PUT");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

//		dG9rXzVOQ0V5YmV5NEdxQzUyb0w6
		// Basic Auth with apiToken as username, no password
		String basicAuth = Base64.getEncoder().encodeToString((apiToken + ":").getBytes(StandardCharsets.UTF_8));
		connection.setRequestProperty("Authorization", "Basic " + basicAuth);

		try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
			if (itemsCsv != null) {
				writeFilePart(writer, "items", itemsCsv, boundary, lineFeed);
			}
			if (variationsCsv != null) {
				writeFilePart(writer, "variations", variationsCsv, boundary, lineFeed);
			}
			if (itemGroupsCsv != null) {
				writeFilePart(writer, "item_groups", itemGroupsCsv, boundary, lineFeed);
			}

			// End of multipart
			writer.writeBytes("--" + boundary + "--" + lineFeed);
			writer.flush();
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
			System.out.println("✅ Catalog uploaded successfully to Constructor.io");
		} else {
			try (Scanner errorScanner = new Scanner(connection.getErrorStream())) {
				String errorBody = errorScanner.useDelimiter("\\A").hasNext() ? errorScanner.next() : "";
				throw new RuntimeException("❌ Upload failed. Status: " + responseCode + ". Error: " + errorBody);
			}
		}
	}


	private static void writeFilePart(DataOutputStream writer, String fieldName, File file, String boundary,
			String lineFeed) throws IOException {
		writer.writeBytes("--" + boundary + lineFeed);
		writer.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName()
				+ "\"" + lineFeed);
		writer.writeBytes("Content-Type: text/csv" + lineFeed);
		writer.writeBytes(lineFeed);

		try (FileInputStream fileInput = new FileInputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = fileInput.read(buffer)) != -1) {
				writer.write(buffer, 0, bytesRead);
			}
		}

		writer.writeBytes(lineFeed);
	}

}
