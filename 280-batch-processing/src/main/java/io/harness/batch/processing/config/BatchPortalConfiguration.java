package io.harness.batch.processing.config;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.govern.ProviderModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.guice.annotation.EnableGuiceModules;

import java.util.Map;

@Slf4j
@Configuration
@EnableGuiceModules
public class BatchPortalConfiguration {
  @Bean
  public BatchProcessingModule batchProcessingWingsModule() {
    return new BatchProcessingModule();
  }

  @Bean
  public BatchProcessingRegistrarsModule batchProcessingRegistrars() {
    return new BatchProcessingRegistrarsModule();
  }

  @Bean
  @Profile("!test")
  public Module morphiaClassesModule() {
    return new ProviderModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "delegateAsyncTaskResponses")
            .build();
      }
    };
  }

  @Bean
  public BatchProcessingTimescaleModule batchProcessingTimescaleModule(BatchMainConfig batchMainConfig) {
    return new BatchProcessingTimescaleModule(batchMainConfig.getTimeScaleDBConfig());
  }
}
