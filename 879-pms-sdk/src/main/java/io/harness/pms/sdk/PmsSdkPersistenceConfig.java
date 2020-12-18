package io.harness.pms.sdk;

import io.harness.annotation.HarnessRepo;
import io.harness.mongo.MongoConfig;
import io.harness.springdata.HMongoTemplate;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.pms.sdk"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "pmsSdkMongoTemplate")
public class PmsSdkPersistenceConfig extends AbstractMongoConfiguration {
  protected final Injector injector;
  private final MongoConfig mongoConfig;
  protected final List<Class<? extends Converter<?, ?>>> springConverters;

  @Inject
  public PmsSdkPersistenceConfig(Injector injector, List<Class<? extends Converter<?, ?>>> springConverters) {
    this.injector = injector;
    this.mongoConfig = injector.getInstance(Key.get(MongoConfig.class, Names.named("pmsSdkMongoConfig")));
    this.springConverters = springConverters;
  }

  @Override
  public MongoClient mongoClient() {
    MongoClientOptions primaryMongoClientOptions = MongoClientOptions.builder()
                                                       .retryWrites(true)
                                                       .connectTimeout(mongoConfig.getConnectTimeout())
                                                       .serverSelectionTimeout(mongoConfig.getServerSelectionTimeout())
                                                       .maxConnectionIdleTime(mongoConfig.getMaxConnectionIdleTime())
                                                       .connectionsPerHost(mongoConfig.getConnectionsPerHost())
                                                       .readPreference(ReadPreference.primary())
                                                       .build();
    MongoClientURI uri =
        new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(primaryMongoClientOptions));
    return new MongoClient(uri);
  }

  @Override
  protected String getDatabaseName() {
    return new MongoClientURI(mongoConfig.getUri()).getDatabase();
  }

  @Bean(name = "pmsSdkMongoTemplate")
  public MongoTemplate mongoTemplate() throws Exception {
    return new HMongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Bean
  public CustomConversions customConversions() {
    List<?> converterInstances = springConverters.stream().map(injector::getInstance).collect(Collectors.toList());
    return new MongoCustomConversions(converterInstances);
  }

  @Override
  protected boolean autoIndexCreation() {
    return false;
  }
}
