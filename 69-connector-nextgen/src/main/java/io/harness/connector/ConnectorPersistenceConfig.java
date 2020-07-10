package io.harness.connector;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.exception.GeneralException;
import io.harness.ng.SpringPersistenceConfig;
import org.reflections.Reflections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoTypeMapper;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableMongoRepositories(
    basePackages = {"io.harness.connector"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
public class ConnectorPersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public ConnectorPersistenceConfig(Injector injector) {
    super(injector);
  }

  @Bean(name = "connectorMongoTemplate")
  @Primary
  public MongoTemplate connectorMongoTemplate() throws Exception {
    return new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Bean
  @Override
  public MappingMongoConverter mappingMongoConverter() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
    TypeInformationMapper informationMapper =
        ConnectorTypeInformationMapper.builder().aliasMap(collectAliasMap()).build();
    MongoTypeMapper typeMapper = new DefaultMongoTypeMapper(
        DefaultMongoTypeMapper.DEFAULT_TYPE_KEY, Collections.singletonList(informationMapper));
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext());
    converter.setCustomConversions(customConversions());
    converter.setCodecRegistryProvider(mongoDbFactory());
    converter.setTypeMapper(typeMapper);
    return converter;
  }

  private Map<String, Class<?>> collectAliasMap() {
    Map<String, Class<?>> aliases = new ConcurrentHashMap<>();
    try {
      Reflections reflections = new Reflections("io.harness.connector.entities");
      Collection<Class<?>> classes = reflections.getTypesAnnotatedWith(TypeAlias.class);
      for (Class<? extends Object> typeAliasedClass : classes) {
        TypeAlias typeAliasAnnot = typeAliasedClass.getAnnotation(TypeAlias.class);
        String alias = typeAliasAnnot.value();
        aliases.put(alias, typeAliasedClass);
      }
    } catch (Exception e) {
      throw new GeneralException("Failed initializing Spring Data Converters", e);
    }
    return aliases;
  }
}
