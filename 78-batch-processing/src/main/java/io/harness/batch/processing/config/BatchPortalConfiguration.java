package io.harness.batch.processing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

@Slf4j
@Configuration
@EnableGuiceModules
public class BatchPortalConfiguration {
  @Bean
  public BatchProcessingModule batchProcessingWingsModule() {
    return new BatchProcessingModule();
  }

  @Bean
  public BatchProcessingTimescaleModule batchProcessingTimescaleModule(BatchMainConfig batchMainConfig) {
    return new BatchProcessingTimescaleModule(batchMainConfig.getTimeScaleDBConfig());
  }
}
