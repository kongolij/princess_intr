package com.bigcommerce.imports.store;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bigcommerce.imports.catalog.mapper.BigCommerceCategoryMapper;
import com.bigcommerce.imports.store.dto.Address;
import com.bigcommerce.imports.store.dto.DayHours;
import com.bigcommerce.imports.store.dto.GeoCoordinates;
import com.bigcommerce.imports.store.dto.Holiday;
import com.bigcommerce.imports.store.dto.HolidayEntry;
import com.bigcommerce.imports.store.dto.Location;
import com.bigcommerce.imports.store.dto.StoreHours;
import com.bigcommerce.imports.store.dto.StoreLocationBundle;
import com.bigcommerce.imports.store.dto.WorkingDay;
import com.bigcommerce.imports.store.mapper.BigCommerceLocationMapper;
import com.bigcommerce.imports.store.service.BigCommerceStoreLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;

@Component
public class ImportStoreLocationFromCVS implements CommandLineRunner {

	 
	    private final BigCommerceStoreLocationService  bigCommerceStoreLocationService;
	
	    private final ObjectMapper objectMapper = new ObjectMapper();

	   public ImportStoreLocationFromCVS(BigCommerceStoreLocationService  bigCommerceStoreLocationService) {
		   this.bigCommerceStoreLocationService=bigCommerceStoreLocationService;
		   
	   }
	    @Override
	    public void run(String... args) {
	        long startTime = System.currentTimeMillis();

	    		
	        InputStream inputStream = getClass().getClassLoader()
	            .getResourceAsStream("PAL_StoreAddresses_20250324-extended.csv");
	        
	        
	        InputStreamReader utf8Reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
	        

	        if (inputStream == null) {
	            System.err.println("❌ CSV file not found in resources!");
	            return;
	        }

	        try (CSVReader reader = new CSVReader(utf8Reader)) {
	            List<String[]> rows = reader.readAll();
	            String[] headers = rows.get(0);
	            Map<String, StoreLocationBundle> locationMap = buildLocationListWithMetafields(rows, headers, objectMapper);
	            printLocationDetails(locationMap);
	            bigCommerceStoreLocationService.importStoresToBc(locationMap);
//	            JSONArray a = BigCommerceLocationMapper.mapLocationToBigCommerce(locations.get(0));
	            List<Location> locationList = locationMap.values()
                        .stream()
                        .map(StoreLocationBundle::getLocation)
                        .toList();
	            
	            JSONArray a = BigCommerceLocationMapper.mapLocationToBigCommerce(Collections.singletonList(locationList.get(0)));
	            // Do something with locations...
	        	long endTime = System.currentTimeMillis(); // End timing
				long durationMillis = endTime - startTime;
				double durationMinutes = durationMillis / 1000.0 / 60.0;
	            System.out.println("✅ Done! runnig time in min " + durationMinutes); 
				System.exit(0);
	        } catch (Exception e) {
				// TODO Auto-generated catch block
	        	System.err.println("❌ Error while processing CSV: " + e.getMessage());
				e.printStackTrace();
			}
	    }

	    public List<Location> buildLocationList(List<String[]> rows, String[] headers, ObjectMapper objectMapper) {
	        List<Location> locations = new ArrayList<>();

	        for (int i = 1; i < rows.size(); i++) {
	            String[] row = rows.get(i);

	            try {
	                Location loc = new Location();
	                String idStr = getValue(headers, row, "id");
	                loc.setId(Integer.parseInt(idStr));
	                loc.setCode("store-" + idStr);
	                loc.setLabel(getValue(headers, row, "name"));
	                loc.setDescription("");
	                loc.setType_id("PHYSICAL");
	                loc.setEnabled(true);
	                loc.setStorefront_visibility(Boolean.parseBoolean(getValue(headers, row, "showOnPDP")));
	                loc.setShowBopisMessage(Boolean.parseBoolean(getValue(headers, row, "showBopisMessage")));

	                Address address = new Address();
	                address.setAddress1(getValue(headers, row, "address_1"));
	                address.setAddress2(getValue(headers, row, "address_2"));
	                address.setCity(getValue(headers, row, "city"));
	                address.setState(getValue(headers, row, "province_state_code"));
	                address.setZip(getValue(headers, row, "zip"));
	                address.setEmail(getValue(headers, row, "email"));
	                address.setPhone(getValue(headers, row, "phoneNumber"));
	                address.setCountry_code(getValue(headers, row, "country"));

	                GeoCoordinates geo = new GeoCoordinates();
	                geo.setLatitude(Double.parseDouble(getValue(headers, row, "latitude")));
	                geo.setLongitude(Double.parseDouble(getValue(headers, row, "longitude")));
	                address.setGeo_coordinates(geo);
	                loc.setAddress(address);

	                String hoursJson = getValue(headers, row, "hours");
	                if (hoursJson != null && !hoursJson.isBlank()) {
	                    StoreHours storeHours = objectMapper.readValue(hoursJson, StoreHours.class);
	                    loc.setTime_zone(storeHours.getTimeZone());

	                    Map<String, DayHours> operatingHours = new HashMap<>();
	                    for (WorkingDay wd : storeHours.getWorkingDays()) {
	                        DayHours dh = new DayHours();
	                        if (wd.getWorkingHours() != null && !wd.getWorkingHours().isEmpty()) {
	                            String[] parts = wd.getWorkingHours().get(0).split("\\|");
	                            if (parts.length == 2) {
	                                dh.setOpen(true);
	                                dh.setOpening(parts[0].trim());
	                                dh.setClosing(parts[1].trim());
	                            }
	                        } else {
	                            dh.setOpen(false);
	                            dh.setOpening("00:00");
	                            dh.setClosing("00:00");
	                        }
	                        operatingHours.put(wd.getDay().toLowerCase(), dh);
	                    }
	                    loc.setOperating_hours(operatingHours);
	                    
	                 // Normalize holidays into a List<HolidayEntry> instead of raw map
	                    List<HolidayEntry> holidayList = new ArrayList<>();
	                    Map<String, List<Holiday>> holidayMap = storeHours.getHolidays();

	                    if (holidayMap != null) {
	                        for (Map.Entry<String, List<Holiday>> entry : holidayMap.entrySet()) {
	                            String month = entry.getKey();
	                            for (Holiday h : entry.getValue()) {
	                                HolidayEntry entryObj = new HolidayEntry();
	                                entryObj.setMonth(month);
	                                entryObj.setName(h.getName());
	                                entryObj.setDay(h.getDay());
	                                entryObj.setHours(h.getHours());
	                                holidayList.add(entryObj);
	                            }
	                        }
	                    }
	                    loc.setSpecial_hours(holidayList);

	                }

	                locations.add(loc);
	            } catch (Exception e) {
	                System.err.println("❌ Error processing row " + i + ": " + Arrays.toString(row));
	                e.printStackTrace();
	            }
	        }

	        return locations;
	    }

	    private static String getValue(String[] headers, String[] row, String columnName) {
	        for (int i = 0; i < headers.length; i++) {
	            if (headers[i].equalsIgnoreCase(columnName)) {
	                return i < row.length ? row[i] : "";
	            }
	        }
	        return "";
	    }
	    
	    public Map<String, StoreLocationBundle> buildLocationListWithMetafields(List<String[]> rows, String[] headers, ObjectMapper objectMapper) {
	        
	        Map<String, String[]> enRows = new HashMap<>();
	        Map<String, String[]> frRows = new HashMap<>();

	        for (int i = 1; i < rows.size(); i++) {
	            String[] row = rows.get(i);
	            String id = getValue(headers, row, "id");
	            String language = getValue(headers, row, "language");
	            if (language.equalsIgnoreCase("fr_CA")) {
	                frRows.put(id, row);
	            } else {
	                enRows.put(id, row);
	            }
	        }

	     
	        
	        Map<String, StoreLocationBundle> result = new HashMap<>();

	        for (Map.Entry<String, String[]> entry : enRows.entrySet()) {
	            String id = entry.getKey();
	            String[] enRow = entry.getValue();

	            try {
	                Location loc = parseRowToLocation(headers, enRow, objectMapper);
	                Map<String, String> frMeta = frRows.containsKey(id)
	                        ? extractFrMetafields(headers, frRows.get(id), objectMapper)
	                        : Collections.emptyMap();

	                result.put(id, new StoreLocationBundle(loc, frMeta));
	            } catch (Exception e) {
	                System.err.println("❌ Failed to parse store ID " + id);
	                e.printStackTrace();
	            }
	        }

	        return result;

	        
	        
	    }
	    
	    private Location parseRowToLocation(String[] headers, String[] row, ObjectMapper objectMapper) throws Exception {
	        Location loc = new Location();
	        String idStr = getValue(headers, row, "id");
	        loc.setId(Integer.parseInt(idStr));
	        loc.setCode("store-" + idStr);
	        loc.setLabel(getValue(headers, row, "name"));
	        loc.setDescription("");
	        loc.setType_id("PHYSICAL");
	        loc.setEnabled(true);
	        loc.setStorefront_visibility(Boolean.parseBoolean(getValue(headers, row, "showOnPDP")));
	        loc.setShowBopisMessage(Boolean.parseBoolean(getValue(headers, row, "showBopisMessage")));
            loc.setManaged_by_external_source(true);
            
	        Address address = new Address();
	        address.setAddress1(getValue(headers, row, "address_1"));
	        address.setAddress2(getValue(headers, row, "address_2"));
	        address.setCity(getValue(headers, row, "city"));
	        address.setState(getValue(headers, row, "province_state_code"));
	        address.setZip(getValue(headers, row, "zip"));
	        address.setEmail(getValue(headers, row, "email"));
	        address.setPhone(getValue(headers, row, "phoneNumber"));
	        address.setCountry_code(getValue(headers, row, "country"));

	        GeoCoordinates geo = new GeoCoordinates();
	        geo.setLatitude(Double.parseDouble(getValue(headers, row, "latitude")));
	        geo.setLongitude(Double.parseDouble(getValue(headers, row, "longitude")));
	        address.setGeo_coordinates(geo);
	        loc.setAddress(address);

	        String hoursJson = getValue(headers, row, "hours");
	        if (hoursJson != null && !hoursJson.isBlank()) {
	            StoreHours storeHours = objectMapper.readValue(hoursJson, StoreHours.class);
	            loc.setTime_zone(storeHours.getTimeZone());

	            Map<String, DayHours> operatingHours = new HashMap<>();
	            for (WorkingDay wd : storeHours.getWorkingDays()) {
	                DayHours dh = new DayHours();
	                if (wd.getWorkingHours() != null && !wd.getWorkingHours().isEmpty()) {
	                    String[] parts = wd.getWorkingHours().get(0).split("\\|");
	                    if (parts.length == 2) {
	                        dh.setOpen(true);
	                        dh.setOpening(cleanTime(parts[0].trim()));
	                        dh.setClosing(cleanTime(parts[1].trim()));
	                    }
	                } else {
	                    dh.setOpen(false);
	                    dh.setOpening("00:00");
	                    dh.setClosing("00:00");
	                }
	                operatingHours.put(wd.getDay().toLowerCase(), dh);
	            }
	            loc.setOperating_hours(operatingHours);

	            List<HolidayEntry> holidayList = new ArrayList<>();
	            Map<String, List<Holiday>> holidayMap = storeHours.getHolidays();
	            if (holidayMap != null) {
	                for (Map.Entry<String, List<Holiday>> entry : holidayMap.entrySet()) {
	                    String month = entry.getKey();
	                    for (Holiday h : entry.getValue()) {
	                        HolidayEntry entryObj = new HolidayEntry();
	                        entryObj.setMonth(month);
	                        entryObj.setName(h.getName());
	                        entryObj.setDay(h.getDay());
	                        entryObj.setHours(h.getHours());
	                        holidayList.add(entryObj);
	                    }
	                }
	            }
	            loc.setSpecial_hours(holidayList);
	        }

	        return loc;
	    }

	    private Map<String, String> extractFrMetafields(String[] headers, String[] row, ObjectMapper objectMapper) {
	        Map<String, String> meta = new HashMap<>();
	        meta.put("address1_fr", getValue(headers, row, "address_1"));
	        meta.put("extraPhone", getValue(headers, row, "extraPhone"));
	        meta.put("pickUp", getValue(headers, row, "pickUp"));
	        meta.put("showOnPDP", getValue(headers, row, "showOnPDP"));
	        meta.put("showBopisMessage", getValue(headers, row, "showBopisMessage"));
	        meta.put("language", getValue(headers, row, "language"));

	        try {
	            String hoursJson = getValue(headers, row, "hours");
	            if (hoursJson != null && !hoursJson.isBlank()) {
	                StoreHours storeHours = objectMapper.readValue(hoursJson, StoreHours.class);
	                for (Map.Entry<String, List<Holiday>> entry : storeHours.getHolidays().entrySet()) {
	                    String month = entry.getKey();
	                    for (Holiday h : entry.getValue()) {
	                        meta.put(String.format("special_hours.%s.label_fr", month), h.getName());
	                    }
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        return meta;
	    }
	    
	    private void printLocationDetails(Map<String, StoreLocationBundle> locationMap) {
	        System.out.println("\n🛒 Imported Store Locations:");
	        for (Map.Entry<String, StoreLocationBundle> entry : locationMap.entrySet()) {
	            Location loc = entry.getValue().getLocation();

	            System.out.println("--------------------------------------------------");
	            System.out.printf("ID       : %d\n", loc.getId());
	            System.out.printf("Code     : %s\n", loc.getCode());
	            System.out.printf("Label    : %s\n", loc.getLabel());
	            System.out.printf("Type     : %s\n", loc.getType_id());
	            System.out.printf("Timezone : %s\n", loc.getTime_zone());
	            System.out.printf("Enabled  : %s\n", loc.isEnabled());
	            System.out.printf("Visible  : %s\n", loc.isStorefront_visibility());

	            Address addr = loc.getAddress();
	            System.out.println("📍 Address:");
	            System.out.printf("  %s %s\n", addr.getAddress1(), addr.getAddress2());
	            System.out.printf("  %s, %s %s, %s\n", addr.getCity(), addr.getState(), addr.getZip(), addr.getCountry_code());
	            System.out.printf("  📧 %s | 📞 %s\n", addr.getEmail(), addr.getPhone());

	            System.out.println("🕒 Operating Hours:");
	            loc.getOperating_hours().forEach((day, dh) -> {
	                String status = dh.isOpen() ? String.format("%s - %s", dh.getOpening(), dh.getClosing()) : "Closed";
	                System.out.printf("  %-9s : %s\n", capitalize(day), status);
	            });

	            if (loc.getSpecial_hours() != null && !loc.getSpecial_hours().isEmpty()) {
	                System.out.println("📅 Holiday Hours:");
	                for (HolidayEntry he : loc.getSpecial_hours()) {
	                    String hours = (he.getHours() != null && !he.getHours().isEmpty())
	                            ? String.join(", ", he.getHours())
	                            : "Closed";
	                    System.out.printf("  %s %2d (%s): %s\n", he.getMonth(), he.getDay(), he.getName(), hours);
	                }
	            }
	        }
	    }

	    
	    
	    private void printLocationDetails(List<Location> locations) {
	        System.out.println("\n🛒 Imported Store Locations:");
	        for (Location loc : locations) {
	            System.out.println("--------------------------------------------------");
	            System.out.printf("ID       : %d\n", loc.getId());
	            System.out.printf("Code     : %s\n", loc.getCode());
	            System.out.printf("Label    : %s\n", loc.getLabel());
	            System.out.printf("Type     : %s\n", loc.getType_id());
	            System.out.printf("Timezone : %s\n", loc.getTime_zone());
	            System.out.printf("Enabled  : %s\n", loc.isEnabled());
	            System.out.printf("Visible  : %s\n", loc.isStorefront_visibility());

	            Address addr = loc.getAddress();
	            System.out.println("📍 Address:");
	            System.out.printf("  %s %s\n", addr.getAddress1(), addr.getAddress2());
	            System.out.printf("  %s, %s %s, %s\n", addr.getCity(), addr.getState(), addr.getZip(), addr.getCountry_code());
	            System.out.printf("  📧 %s | 📞 %s\n", addr.getEmail(), addr.getPhone());

	            System.out.println("🕒 Operating Hours:");
	            loc.getOperating_hours().forEach((day, dh) -> {
	                String status = dh.isOpen() ? String.format("%s - %s", dh.getOpening(), dh.getClosing()) : "Closed";
	                System.out.printf("  %-9s : %s\n", capitalize(day), status);
	            });

	            if (loc.getSpecial_hours() != null && !loc.getSpecial_hours().isEmpty()) {
	                System.out.println("📅 Holiday Hours:");
	                for (HolidayEntry he : loc.getSpecial_hours()) {
	                    String hours = (he.getHours() != null && !he.getHours().isEmpty())
	                            ? String.join(", ", he.getHours())
	                            : "Closed";
	                    System.out.printf("  %s %2d (%s): %s\n", he.getMonth(), he.getDay(), he.getName(), hours);
	                }
	            }
	        }
	    }

	    private String capitalize(String input) {
	        if (input == null || input.isBlank()) return "";
	        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
	    }

	    private String cleanTime(String time) {
	        return time == null ? "00:00" : time.replaceAll("\\s+", "");
	    }

}