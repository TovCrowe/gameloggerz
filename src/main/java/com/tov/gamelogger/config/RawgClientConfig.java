package com.tov.gamelogger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RawgClientConfig {

    @Bean
    public RestClient rawgRestClient(RestClient.Builder restClientBuilder,
                                      @Value("${rawg.base-url}") String rawgBaseUrl) {
        return restClientBuilder.baseUrl(rawgBaseUrl).build();
    }
}
