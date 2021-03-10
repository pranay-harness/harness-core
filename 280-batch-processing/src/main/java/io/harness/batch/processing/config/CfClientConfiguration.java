package io.harness.batch.processing.config;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.cf.ApiException;
import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.api.CfClientException;
import io.harness.cf.client.api.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CfClientConfiguration {
  @Bean
  public CfClient cfClient(io.harness.batch.processing.config.BatchMainConfig batchMainConfig)
      throws CfClientException, ApiException {
    io.harness.batch.processing.config.CfConfig cfConfig = batchMainConfig.getCfConfig();
    if (cfConfig == null || isBlank(cfConfig.getApiKey())) {
      return null;
    }
    log.info("Using CF API key {}", cfConfig.getApiKey());
    String apiKey = cfConfig.getApiKey();
    return new CfClient(apiKey, Config.builder().baseUrl(cfConfig.getBaseUrl()).build());
  }
}
