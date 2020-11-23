package io.harness;

import io.harness.annotation.HarnessRepo;
import io.harness.notification.MongoBackendConfiguration;
import io.harness.springdata.HMongoTemplate;
import io.harness.springdata.SpringPersistenceConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.notification"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class NotificationChannelPersistenceConfig extends SpringPersistenceConfig {
  private final MongoBackendConfiguration mongoBackendConfiguration;

  @Inject
  public NotificationChannelPersistenceConfig(Injector injector) {
    super(injector, Collections.emptyList());
    this.mongoBackendConfiguration =
        (MongoBackendConfiguration) injector.getInstance(Key.get(NotificationClientApplicationConfiguration.class))
            .getNotificationClientConfiguration()
            .getNotificationClientBackendConfiguration();
  }

  @Bean
  MongoTransactionManager transactionManager(MongoDbFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @Bean(name = "notification-channel")
  @Primary
  @Override
  public MongoTemplate mongoTemplate() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(this.mongoDbFactory());
    MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(this.mongoClient(), this.getDatabaseName());
    MongoMappingContext mappingContext = this.mongoMappingContext();
    mappingContext.setAutoIndexCreation(false);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCustomConversions(collectConverters());
    converter.setCodecRegistryProvider(mongoDbFactory);
    converter.afterPropertiesSet();
    return new HMongoTemplate(mongoDbFactory, mappingMongoConverter());
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
}
