package com.bigcommerce.imports.catalog.constants;

public enum StoreHash {

//	EN("en", "nkqg1lsole"), // Store hash for English locale: jimmy store
//PAl-dev
//	EN("en", "w1jeucyusb"), // Store hash for English locale: PAL store
//EPAM_DEV	
	EN("en", "u2rpux9vkx"), // Store hash for English locale: PAL store
// EAPM QA	
//	EN("fr", "kpz3wrpdrb"); // Store hash for French locale (replace with actual hash)
	
	FR("fr", "kpz3wrpdrb");

	private final String locale;
	private final String hash;

	StoreHash(String locale, String hash) {
		this.locale = locale;
		this.hash = hash;
	}

	public String getLocale() {
		return locale;
	}

	public String getHash() {
		return hash;
	}

	// Static method to get store hash by locale
	public static String getStoreHashByLocale(String locale) {
		for (StoreHash storeHash : StoreHash.values()) {
			if (storeHash.getLocale().equalsIgnoreCase("en")) {
				return storeHash.getHash();
			}
		}
		throw new IllegalArgumentException("Invalid locale: " + locale);
	}
}
