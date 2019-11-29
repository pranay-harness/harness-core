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
import org.mongodb.morphia.AdvancedDatastore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import java.util.Set;

@Configuration
@Slf4j
public class BatchMongoConfiguration {
  private static final Set<Class> morphiaClasses =
      ImmutableSet.<Class>builder().addAll(BatchProcessingMorphiaClasses.classes).build();

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
  public MongoDbFactory mongoDbFactory(@Qualifier("primaryDatastore") AdvancedDatastore primaryDatastore) {
    return new SimpleMongoDbFactory(primaryDatastore.getMongo(), primaryDatastore.getDB().getName());
  }

  @Bean
  public MongoTemplate mongoTemplate(MongoDbFactory mongoDbFactory) {
    return new MongoTemplate(mongoDbFactory);
  }

  @Bean
  @Profile("!test")
  public MongoModule mongoModule() {
    return new MongoModule();
  }
}
