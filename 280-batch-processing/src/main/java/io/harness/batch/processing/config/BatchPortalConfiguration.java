package io.harness.batch.processing.config;

import io.harness.cf.AbstractCfModule;
import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.govern.ProviderModule;
import io.harness.serializer.PersistenceRegistrars;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.guice.annotation.EnableGuiceModules;

@Slf4j
@Configuration
@EnableGuiceModules
public class BatchPortalConfiguration {
  @Bean
  public BatchProcessingModule batchProcessingWingsModule(BatchMainConfig batchMainConfig) {
    return new BatchProcessingModule(batchMainConfig);
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
            .put(DelegateTaskProgressResponse.class, "delegateTaskProgressResponses")
            .build();
      }
    };
  }

  @Bean
  @Profile("!test")
  public Module morphiaConverterModule() {
    return new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(PersistenceRegistrars.morphiaConverters)
            .build();
      }
    };
  }

  @Bean
  public BatchProcessingTimescaleModule batchProcessingTimescaleModule(BatchMainConfig batchMainConfig) {
    return new BatchProcessingTimescaleModule(batchMainConfig.getTimeScaleDBConfig());
  }

  @Bean
  @Profile("!test")
  AbstractCfModule cfModule(BatchMainConfig batchMainConfig) {
    return new AbstractCfModule() {
      @Override
      public CfClientConfig cfClientConfig() {
        return batchMainConfig.getCfClientConfig();
      }

      @Override
      public CfMigrationConfig cfMigrationConfig() {
        return batchMainConfig.getCfMigrationConfig();
      }
    };
  }
}
