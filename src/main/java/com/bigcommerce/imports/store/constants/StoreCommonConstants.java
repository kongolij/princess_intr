package com.bigcommerce.imports.store.constants;

public class StoreCommonConstants {

	public static final String STORE_DATA_NAMESPACE = "location_data";
	public static final String STORE_LOCALIZATION_NAMESPACE = "store_localization";

//	metafield visible in the storefront	✅ Yes
//	other apps or frontend scripts to be able to update it	✅ Yes
//	maximum flexibility	✅ Yes
//	You want private or secure data	❌ No — use app_only instead

	public static final String PERMISION_SET = "write_and_sf_access";

}
