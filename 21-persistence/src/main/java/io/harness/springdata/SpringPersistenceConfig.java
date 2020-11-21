package io.harness.springdata;

import static com.google.inject.Key.get;
import static com.google.inject.name.Names.named;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.mongodb.MongoClient;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.mongodb.morphia.AdvancedDatastore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.GuiceModule;

@Configuration
@GuiceModule
@EnableMongoRepositories(mongoTemplateRef = "primary")
@EnableMongoAuditing
public abstract class SpringPersistenceConfig extends AbstractMongoConfiguration {
  protected final Injector injector;
  protected final AdvancedDatastore advancedDatastore;
  protected final List<Class<? extends Converter>> converters;

  private static final Collection<String> BASE_PACKAGES = ImmutableList.of("io.harness");

  public SpringPersistenceConfig(Injector injector, List<Class<? extends Converter>> converters) {
    this.injector = injector;
    this.advancedDatastore = injector.getProvider(get(AdvancedDatastore.class, named("primaryDatastore"))).get();
    this.converters = converters;
  }

  @Override
  public MongoClient mongoClient() {
    return advancedDatastore.getMongo();
  }

  @Override
  protected String getDatabaseName() {
    return advancedDatastore.getDB().getName();
  }

  @Override
  protected Collection<String> getMappingBasePackages() {
    return BASE_PACKAGES;
  }

  @Bean(name = "primary")
  @Primary
  @Override
  public MongoTemplate mongoTemplate() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(this.mongoDbFactory());
    MongoMappingContext mappingContext = this.mongoMappingContext();
    mappingContext.setAutoIndexCreation(false);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCustomConversions(collectConverters());
    converter.setCodecRegistryProvider(this.mongoDbFactory());
    converter.afterPropertiesSet();
    return new HMongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  protected CustomConversions collectConverters() {
    List<?> converterInstances = converters.stream().map(injector::getInstance).collect(Collectors.toList());
    return new MongoCustomConversions(converterInstances);
  }
}
