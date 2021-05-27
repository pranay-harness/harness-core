package io.harness.pms.sdk.core;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.MongoConfig;
import io.harness.serializer.PmsSdkCoreModuleRegistrars;
import io.harness.springdata.HMongoTemplate;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@OwnedBy(PIPELINE)
@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.pms.sdk"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "pmsSdkMongoTemplate")
public class PmsSdkPersistenceConfig extends AbstractMongoConfiguration {
  protected final Injector injector;
  private final MongoConfig mongoConfig;
  protected final List<Class<? extends Converter<?, ?>>> springConverters;

  @Inject
  public PmsSdkPersistenceConfig(Injector injector) {
    this.injector = injector;
    this.mongoConfig = injector.getInstance(Key.get(MongoConfig.class, Names.named("pmsSdkMongoConfig")));
    this.springConverters = ImmutableList.<Class<? extends Converter<?, ?>>>builder()
                                .addAll(PmsSdkCoreModuleRegistrars.springConverters)
                                .build();
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

  @Override
  protected Collection<String> getMappingBasePackages() {
    return Collections.singleton("io.harness");
  }

  @Bean(name = "pmsSdkMongoTemplate")
  public MongoTemplate mongoTemplate() throws Exception {
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
    MongoClient mongoClient = new MongoClient(uri);
    MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(mongoClient, Objects.requireNonNull(uri.getDatabase()));
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
    MongoMappingContext mappingContext = this.mongoMappingContext();
    mappingContext.setAutoIndexCreation(false);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCustomConversions(customConversions());
    converter.setCodecRegistryProvider(mongoDbFactory);
    converter.afterPropertiesSet();
    return new HMongoTemplate(mongoDbFactory, converter);
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
