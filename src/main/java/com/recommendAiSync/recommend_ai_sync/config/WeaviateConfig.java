package com.recommendAiSync.recommend_ai_sync.config;

import io.weaviate.client.base.Result;
import io.weaviate.client.v1.misc.model.Meta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import io.weaviate.client.Config;

@Configuration
public class WeaviateConfig {
    private final Logger LOGGER = LoggerFactory.getLogger(WeaviateConfig.class);

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public io.weaviate.client.WeaviateClient weaviateClientMethod() {
        LOGGER.info("CREATING weaviateClientMethod");
        Config config = new Config("http", "147.135.253.234:8080");
        io.weaviate.client.WeaviateClient client = new io.weaviate.client.WeaviateClient(config);
        Result<Meta> meta = client.misc().metaGetter().run();
        if (meta.getError() == null) {
            System.out.printf("meta.hostname: %s\n", meta.getResult().getHostname());
            System.out.printf("meta.version: %s\n", meta.getResult().getVersion());
            System.out.printf("meta.modules: %s\n", meta.getResult().getModules());
        } else {
            System.out.printf("Error: %s\n", meta.getError().getMessages());
        }
        return client;
    }
}