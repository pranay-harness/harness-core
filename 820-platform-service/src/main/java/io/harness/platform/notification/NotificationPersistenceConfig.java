package io.harness.platform.notification;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.MongoConfig;
import io.harness.platform.PlatformConfiguration;
import io.harness.springdata.HMongoTemplate;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.Collection;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@OwnedBy(PL)
@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.notification.repositories"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class NotificationPersistenceConfig extends AbstractMongoConfiguration {
  private final MongoConfig mongoBackendConfiguration;

  @Inject
  public NotificationPersistenceConfig(Injector injector) {
    mongoBackendConfiguration =
        injector.getInstance(Key.get(PlatformConfiguration.class)).getNotificationServiceConfig().getMongoConfig();
  }

  @Override
  public MongoClient mongoClient() {
    MongoClientOptions primaryMongoClientOptions =
        MongoClientOptions.builder()
            .retryWrites(true)
            .connectTimeout(mongoBackendConfiguration.getConnectTimeout())
            .serverSelectionTimeout(mongoBackendConfiguration.getServerSelectionTimeout())
            .maxConnectionIdleTime(mongoBackendConfiguration.getMaxConnectionIdleTime())
            .connectionsPerHost(mongoBackendConfiguration.getConnectionsPerHost())
            .readPreference(ReadPreference.primary())
            .build();
    MongoClientURI uri =
        new MongoClientURI(mongoBackendConfiguration.getUri(), MongoClientOptions.builder(primaryMongoClientOptions));
    return new MongoClient(uri);
  }

  @Override
  protected String getDatabaseName() {
    return new MongoClientURI(mongoBackendConfiguration.getUri()).getDatabase();
  }

  @Bean
  MongoTransactionManager transactionManager(MongoDbFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @Override
  protected Collection<String> getMappingBasePackages() {
    return Collections.singleton("io.harness");
  }

  @Bean
  public MongoTemplate mongoTemplate() throws Exception {
    MongoDbFactory mongoDbFactory = mongoDbFactory();
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
    MongoMappingContext mappingContext = mongoMappingContext();
    mappingContext.setAutoIndexCreation(false);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCodecRegistryProvider(mongoDbFactory);
    converter.afterPropertiesSet();
    return new HMongoTemplate(mongoDbFactory, mappingMongoConverter(), mongoBackendConfiguration.getTraceMode());
  }
}
