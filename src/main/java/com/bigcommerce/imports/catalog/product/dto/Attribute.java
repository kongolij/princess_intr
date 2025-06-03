package com.bigcommerce.imports.catalog.product.dto;

public class Attribute {

	public String id;
	public String en;
	public String fr_CA;
	public String seq;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEn() {
		return en;
	}

	public void setEn(String en) {
		this.en = en;
	}

	public String getFr_CA() {
		return fr_CA;
	}

	public void setFr_CA(String fr_CA) {
		this.fr_CA = fr_CA;
	}
	
	

	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public boolean isAttributeLabel() {
		// Matches something like A01550, A1234, etc.
		boolean looksLikeCode = id != null && id.matches("^[A-Z]\\d{4,6}$");

		// Doesn't have meaningful content
		boolean noTranslation = (en == null || en.trim().isEmpty()) && (fr_CA == null || fr_CA.trim().isEmpty());

		return looksLikeCode && noTranslation;
	}
}
