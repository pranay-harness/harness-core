package io.harness.batch.processing.config;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.batch.processing.entities.BatchProcessingMorphiaClasses;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import java.util.Set;

@Slf4j
@Configuration
@EnableGuiceModules
public class BatchPortalConfiguration {
  private static final Set<Class> morphiaClasses =
      ImmutableSet.<Class>builder().addAll(BatchProcessingMorphiaClasses.classes).build();

  @Bean
  public BatchProcessingModule batchProcessingWingsModule() {
    return new BatchProcessingModule();
  }

  @Bean
  public MongoModule mongoModule() {
    return new MongoModule();
  }

  @Bean
  public ProviderModule morphiaClasses(BatchMainConfig batchMainConfig) {
    return new ProviderModule() {
      @Provides
      @Named("morphiaClasses")
      Set<Class> classes() {
        return morphiaClasses;
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return batchMainConfig.getMongoConnectionFactory();
      }
    };
  }

  @Bean
  public BatchProcessingTimescaleModule batchProcessingTimescaleModule(BatchMainConfig batchMainConfig) {
    return new BatchProcessingTimescaleModule(batchMainConfig.getTimeScaleDBConfig());
  }
}
