package com.bigcommerce.imports.attribtuelabels;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;

@Configuration
public class CosmosConfig {

	private static final String CONNECTION_STRING ="";

	// Known names
	private static final String DATABASE_NAME = "attribute-labels";
	private static final String CONTAINER_NAME = "attribute-labels";

	@Bean
	public CosmosClient cosmosClient() {
		String endpoint = CONNECTION_STRING.split(";")[0].split("=")[1];
		String key = CONNECTION_STRING.split(";")[1].split("=")[1];

		return new CosmosClientBuilder().endpoint(endpoint).key(key).consistencyLevel(ConsistencyLevel.EVENTUAL)
				.buildClient();
	}

	@Bean
	public CosmosContainer cosmosContainer(CosmosClient cosmosClient) {
		CosmosDatabase database = cosmosClient.getDatabase(DATABASE_NAME);
		return database.getContainer(CONTAINER_NAME);
	}
}
