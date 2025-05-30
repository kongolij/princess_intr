package com.bigcommerce.imports.store.mapper;

import com.bigcommerce.imports.store.dto.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class BigCommerceLocationMapper {

	private static final Map<String, String> DAY_MAPPING = Map.of("sun", "sunday", "mon", "monday", "tue", "tuesday",
			"wed", "wednesday", "thu", "thursday", "fri", "friday", "sat", "saturday");

	public static JSONArray mapLocationToBigCommerce(List<Location> locations) {
		JSONArray result = new JSONArray();

		for (Location loc : locations) {
			JSONObject locationJson = new JSONObject();

			locationJson.put("code", loc.getCode());
			locationJson.put("label", loc.getLabel());
			locationJson.put("description", Optional.ofNullable(loc.getDescription()).orElse(""));
			locationJson.put("type_id", loc.getType_id());
			locationJson.put("enabled", loc.isEnabled());
			locationJson.put("time_zone", loc.getTime_zone());
			locationJson.put("storefront_visibility", loc.isStorefront_visibility());
			locationJson.put("managed_by_external_source", loc.isManaged_by_external_source());

			// Address
			JSONObject addressJson = new JSONObject();
			Address addr = loc.getAddress();
			if (addr != null) {
				addressJson.put("address1", addr.getAddress1());
				addressJson.put("address2", Optional.ofNullable(addr.getAddress2()).orElse(""));
				addressJson.put("city", addr.getCity());
				addressJson.put("state", addr.getState());
				addressJson.put("zip", addr.getZip());
				addressJson.put("email", addr.getEmail());
				addressJson.put("phone", addr.getPhone());
				addressJson.put("country_code", addr.getCountry_code());

				if (addr.getGeo_coordinates() != null) {
					JSONObject geoJson = new JSONObject();
					geoJson.put("latitude", addr.getGeo_coordinates().getLatitude());
					geoJson.put("longitude", addr.getGeo_coordinates().getLongitude());
					addressJson.put("geo_coordinates", geoJson);
				}
			}
			locationJson.put("address", addressJson);

			// Operating Hours
			if (loc.getOperating_hours() != null && !loc.getOperating_hours().isEmpty()) {
				JSONObject operatingHours = new JSONObject();
				for (Map.Entry<String, DayHours> entry : loc.getOperating_hours().entrySet()) {
					String shortDay = entry.getKey().toLowerCase();
					String fullDay = DAY_MAPPING.getOrDefault(shortDay, shortDay); // fallback if not mapped

					DayHours dh = entry.getValue();
					JSONObject dayJson = new JSONObject();
					dayJson.put("open", dh.isOpen());
					dayJson.put("opening", dh.getOpening());
					dayJson.put("closing", dh.getClosing());

					operatingHours.put(fullDay, dayJson);
				}
				locationJson.put("operating_hours", operatingHours);
			}

			// Special Hours from HolidayEntry
			if (loc.getSpecial_hours() != null && !loc.getSpecial_hours().isEmpty()) {
				JSONArray specialHours = new JSONArray();

				for (Object obj : loc.getSpecial_hours()) {
					if (obj instanceof HolidayEntry he) {
						JSONObject shJson = new JSONObject();
						shJson.put("label", he.getName());

						// Format date as yyyy-MM-dd
						String date = String.format("2025-%s-%02d", getMonthNumber(he.getMonth()), he.getDay());
						shJson.put("date", date);

						if (he.getHours() != null && !he.getHours().isEmpty()) {
							String[] times = he.getHours().get(0).split("\\|");
							if (times.length == 2) {
								shJson.put("open", true);
								shJson.put("opening", times[0].trim());
								shJson.put("closing", times[1].trim());
							} else {
								shJson.put("open", false);
								shJson.put("opening", "00:00");
								shJson.put("closing", "00:00");
							}
						} else {
							shJson.put("open", false);
							shJson.put("opening", "00:00");
							shJson.put("closing", "00:00");
						}

						shJson.put("all_day", false); // Adjust if required
						shJson.put("annual", true); // Assumes recurring annually
						specialHours.put(shJson);
					}
				}

				locationJson.put("special_hours", specialHours);
			}

			result.put(locationJson);
		}

		return result;
	}

	private static String getMonthNumber(String monthAbbrev) {
		Map<String, String> months = new HashMap<>();
		months.put("JAN", "01");
		months.put("FEB", "02");
		months.put("MAR", "03");
		months.put("APR", "04");
		months.put("MAY", "05");
		months.put("JUN", "06");
		months.put("JUL", "07");
		months.put("AUG", "08");
		months.put("SEP", "09");
		months.put("OCT", "10");
		months.put("NOV", "11");
		months.put("DEC", "12");

		return months.getOrDefault(monthAbbrev.toUpperCase(), "01");
	}
}
