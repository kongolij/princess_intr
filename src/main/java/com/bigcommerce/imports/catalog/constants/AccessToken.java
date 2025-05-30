package com.bigcommerce.imports.catalog.constants;

public enum AccessToken {

//	EN("en", "o85zg0lb40mrhtgx873a464lqohsobp"), // Store hash for English locale
// PAL-Sandboc	
//	EN("en", "148wypnv70g5uptk97cd5y5bjxeh6b0"), // Store hash for English locale : PAL
//	FR("fr", "prc8tvv9ldrgrllrxq9crplca2jaqwv"); // Store hash for French locale (replace with actual hash)

//EPAM-PAL_DEV	
	EN("en", "211w656tmge6rmsgo0olr8jtky50j38"), // Store hash for English locale : PAL
	FR("fr", "prc8tvv9ldrgrllrxq9crplca2jaqwv"); // Store hash for French locale (replace with actual hash)

	private final String locale;
	private final String hash;

	AccessToken(String locale, String hash) {
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
	public static String getStoreAccessTokenByLocale(String locale) {
		for (AccessToken accessToken : AccessToken.values()) {
			if (accessToken.getLocale().equalsIgnoreCase("en")) {
				return accessToken.getHash();
			}
		}
		throw new IllegalArgumentException("Invalid locale: " + locale);
	}
}
