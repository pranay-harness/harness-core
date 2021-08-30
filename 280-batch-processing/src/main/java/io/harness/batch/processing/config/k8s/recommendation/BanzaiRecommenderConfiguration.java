package io.harness.batch.processing.config.k8s.recommendation;

import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.pricing.banzai.BanzaiRecommenderClient;
import io.harness.network.Http;
import io.harness.remote.client.ServiceHttpClientConfig;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@Configuration
public class BanzaiRecommenderConfiguration {
  @Bean
  public BanzaiRecommenderClient banzaiRecommenderClient(BatchMainConfig batchMainConfig) {
    ServiceHttpClientConfig config = batchMainConfig.getBanzaiRecommenderConfig();

    log.info("BanzaiRecommender base URL: {}", config.getBaseUrl());

    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(config.getConnectTimeOutSeconds(), TimeUnit.SECONDS)
                                    .readTimeout(config.getReadTimeOutSeconds(), TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(config.getBaseUrl()))
                                    .retryOnConnectionFailure(true)
                                    .build();

    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(config.getBaseUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();

    return retrofit.create(BanzaiRecommenderClient.class);
  }
}
