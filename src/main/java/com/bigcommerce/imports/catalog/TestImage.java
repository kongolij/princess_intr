package com.bigcommerce.imports.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class TestImage {

	public static void main(String[] args) {
		String searchTerm = "specific phrase"; // Replace with the term you want to search

		// Initialize Tesseract OCR
		ITesseract tesseract = new Tesseract();

		// Set the Tesseract OCR engine's language data path (update with your own path)
		tesseract.setDatapath("C:\\bigComerce\\catalogImport\\src\\main\\resources");

		// Set the language, if needed (e.g., "eng" for English)
		tesseract.setLanguage("eng");

		// Load the image from the resources folder
		try (InputStream imageStream = TestImage.class.getClassLoader()
				.getResourceAsStream("Ev22_CurrentFlyers-EN.png")) {
			if (imageStream == null) {
				throw new IllegalArgumentException("Image file not found in resources.");
			}

			// Create a temporary file to pass to Tesseract
			Path tempImagePath = Files.createTempFile("tempImage", ".png");
			Files.copy(imageStream, tempImagePath, StandardCopyOption.REPLACE_EXISTING);

			// Perform OCR on the image
			String extractedText = tesseract.doOCR(tempImagePath.toFile());

			// Output the extracted text (optional, for verification)
			System.out.println("Extracted Text:\n" + extractedText);

			// Search for the term within the extracted text
			if (extractedText.toLowerCase().contains(searchTerm.toLowerCase())) {
				System.out.println("Found the search term '" + searchTerm + "' in the image text!");
			} else {
				System.out.println("The search term '" + searchTerm + "' was not found in the image text.");
			}

			// Delete the temporary file
			Files.delete(tempImagePath);

		} catch (TesseractException e) {
			System.err.println("Error during OCR processing: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
