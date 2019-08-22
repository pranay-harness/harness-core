package io.harness.batch.processing.config;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.batch.processing.entities.BatchProcessingMorphiaClasses;
import io.harness.event.app.EventServiceConfig;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;

import java.util.Set;

@EnableGuiceModules
@Configuration
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
  public ProviderModule morphiaClasses(EventServiceConfig eventServiceConfig) {
    return new ProviderModule() {
      @Provides
      @Named("morphiaClasses")
      Set<Class> classes() {
        return morphiaClasses;
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return eventServiceConfig.getMongoConnectionFactory();
      }
    };
  }
}
