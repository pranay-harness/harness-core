package io.harness.notification;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.HMongoTemplate;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.ng"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class),
    mongoTemplateRef = "notification-channel")
public class NotificationChannelPersistenceConfig extends AbstractMongoConfiguration {
  private final MongoBackendConfiguration mongoBackendConfiguration;

  @Inject
  public NotificationChannelPersistenceConfig(Injector injector) {
    this.mongoBackendConfiguration = (MongoBackendConfiguration) injector.getInstance(NotificationConfiguration.class)
                                         .getNotificationClientConfiguration()
                                         .getNotificationClientBackendConfiguration();
  }

  @Override
  protected String getDatabaseName() {
    return new MongoClientURI(mongoBackendConfiguration.getUri()).getDatabase();
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

  @Bean(name = "notification-channel")
  @Primary
  @Override
  public MongoTemplate mongoTemplate() throws Exception {
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
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(this.mongoDbFactory());
    MongoDbFactory mongoDbFactory =
        new SimpleMongoDbFactory(new MongoClient(uri), Objects.requireNonNull(uri.getDatabase()));
    MongoMappingContext mappingContext = this.mongoMappingContext();
    mappingContext.setAutoIndexCreation(false);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCodecRegistryProvider(mongoDbFactory);
    converter.afterPropertiesSet();
    return new HMongoTemplate(mongoDbFactory, mappingMongoConverter());
  }
}