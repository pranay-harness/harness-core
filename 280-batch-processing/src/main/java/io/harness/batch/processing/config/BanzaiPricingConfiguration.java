/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config;

import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.batch.processing.pricing.banzai.BanzaiPricingClient;
import io.harness.network.Http;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@Configuration
public class BanzaiPricingConfiguration {
  private String BASE_PRICING_SERVICE_URL_TEMPLATE = "%s:%s/";

  @Bean
  public BanzaiPricingClient banzaiPricingClient(BatchMainConfig batchMainConfig) {
    BanzaiConfig banzaiConfig = batchMainConfig.getBanzaiConfig();
    String pricingServiceUrl =
        String.format(BASE_PRICING_SERVICE_URL_TEMPLATE, banzaiConfig.getHost(), banzaiConfig.getPort());
    log.info("BASE_PRICING_SERVICE_URL: {}", pricingServiceUrl);
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(120, TimeUnit.SECONDS)
                                    .readTimeout(120, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(pricingServiceUrl))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(pricingServiceUrl)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(BanzaiPricingClient.class);
  }
}
