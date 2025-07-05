package com.bigcommerce.imports.b2bOrg.mapper;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bigcommerce.imports.b2bOrg.dto.B2BOrg;

public class B2bOrgMapper {

//	{
//		  "companyName": "string5",
//		  "companyPhone": "string5",
//		  "companyEmail": "user7@example.com",
//		  "addressLine1": "string",
//		  "addressLine2": "string",
//		  "city": "string",
//		  "state": "string",
//		  "country": "Canada",
//		  "zipCode": "string",
//		  "adminFirstName": "string",
//		  "adminLastName": "string",
//		  "adminEmail": "user7@example.com",
//		  "adminPhoneNumber": "string",
//		  "catalogId": 0,
//		  "acceptCreationEmail": false,
//		  "extraFields": [
//		    {
//		      "fieldName": "string",
//		      "fieldValue": "string"
//		    }
//		  ],
//		  "uuid": "1234",
//		  "channelIds": [
//		    1,
//		    2
//		  ],
//		  "originChannelId": 1
//		}
	
//  customer_number","status_1","customer_attention",
//	customer_name","address_1","address_2","address_3","
//  city","province","postal","
//  country","phone","credit_limit","credit_available"
	
	private static final String ALPHANUM = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static String randomId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
	
    // Map full country names to ISO2 codes
    private static final Map<String, String> COUNTRY_MAP = new HashMap<>();
    static {
        COUNTRY_MAP.put("Canada", "CA");
        COUNTRY_MAP.put("United States", "US");
        COUNTRY_MAP.put("Mexico", "MX");
    }

    private static String resolveCountryCode(String countryInput) {
        if (countryInput == null) return "CA"; // default fallback
        String trimmed = countryInput.trim();
        return COUNTRY_MAP.getOrDefault(trimmed, trimmed.length() == 2 ? trimmed.toUpperCase() : "CA");
    }
    
	public static JSONObject buildB2BOrgPayload(B2BOrg org) {
		JSONObject orgJson = new JSONObject();
		
		String uniqueId = randomId(6); // shorter unique ID

        String dummyFirstName = "First_" + uniqueId;
        String dummyLastName = "Last_" + uniqueId;
        String dummyCompanyEmail = "co_" + uniqueId + "@demo.com";
        String dummyAdminEmail = "admin_" + uniqueId + "@demo.com";
        

		orgJson.put("companyName", org.getCustomer_name());
		orgJson.put("companyPhone", org.getPhone());
		orgJson.put("companyEmail", dummyCompanyEmail);
		orgJson.put("addressLine1", org.getAddress_1());
		orgJson.put("addressLine2", org.getAddress_2());
		orgJson.put("city", org.getCity());
		orgJson.put("state", org.getProvince());
		orgJson.put("country", resolveCountryCode(org.getCountry()));
		orgJson.put("zipCode", org.getPostal());
		orgJson.put("adminFirstName", dummyFirstName);
		orgJson.put("adminLastName", dummyLastName);
		orgJson.put("adminEmail", dummyCompanyEmail);
		orgJson.put("adminPhoneNumber", org.getPhone());
//		orgJson.put("uuid", org.getCustomer_number());
//		orgJson.put("companyStatus", org.getStatus_1());
		orgJson.put("acceptCreationEmail", false);
		
		// Add extraFields
	    JSONArray extraFields = new JSONArray();

	    if (org.getCustomer_attention() != null) {
	        JSONObject field1 = new JSONObject();
	        field1.put("fieldName", "customer_attention");
	        field1.put("fieldValue", org.getCustomer_attention());
	        extraFields.put(field1);
	    }

	    if (org.getCustomer_number() != null) {
	        JSONObject field2 = new JSONObject();
	        field2.put("fieldName", "organization_number");
	        field2.put("fieldValue",  org.getCustomer_number());
	        extraFields.put(field2);
	    }

	    if (org.getStatus_1() != null) {
	        JSONObject field3 = new JSONObject();
	        field3.put("fieldName", "org_status_code");
	        field3.put("fieldValue", org.getStatus_1());
	        extraFields.put(field3);
	    }

	    orgJson.put("extraFields", extraFields);
		
		

      	return orgJson;
	}
	
	public static JSONObject buildB2BCreditPayload(B2BOrg org) {
	    JSONObject creditJson = new JSONObject();

	    creditJson.put("creditEnabled", true);
	    creditJson.put("creditCurrency", "CAD");

	    // Safe parsing of numeric credit values
	    try {
	        if (org.getCredit_available() != null ) {
	            creditJson.put("availableCredit", org.getCredit_available().doubleValue());
	        } else {
	            creditJson.put("availableCredit", 0); // fallback
	        }
	    } catch (NumberFormatException e) {
	        creditJson.put("availableCredit", 0);
	    }

//	    Whether the customer is allowed to make purchases 
//	    using purchase orders when total price exceeds available credit.
	    creditJson.put("limitPurchases", false);
	    
//	    Prevents all company users from making purchases.
	    creditJson.put("creditHold", false);

	    return creditJson;
	}

}
