package io.harness.springdata;

import static com.google.inject.Key.get;
import static com.google.inject.name.Names.named;

import io.harness.annotation.HarnessRepo;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mongodb.morphia.AdvancedDatastore;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.config.MongoConfigurationSupport;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoSimpleTypes;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.GuiceModule;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Configuration
@GuiceModule
@EnableMongoRepositories(basePackages = {"io.harness.repositories"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "primary")
@EnableMongoAuditing
public class SpringPersistenceConfig {
  protected final Injector injector;
  protected final AdvancedDatastore advancedDatastore;
  protected final List<Class<? extends Converter<?, ?>>> springConverters;

  private static final Collection<String> BASE_PACKAGES = ImmutableList.of("io.harness");

  public SpringPersistenceConfig(Injector injector, List<Class<? extends Converter<?, ?>>> springConverters) {
    this.injector = injector;
    this.advancedDatastore = injector.getProvider(get(AdvancedDatastore.class, named("primaryDatastore"))).get();
    this.springConverters = springConverters;
  }

  @Bean
  public MongoDbFactory mongoDbFactory() {
    return new SimpleMongoDbFactory(advancedDatastore.getMongo(), advancedDatastore.getDB().getName());
  }

  @Bean(name = "primary")
  @Primary
  public MongoTemplate mongoTemplate() throws Exception {
    return new HMongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Bean
  public MongoMappingContext mongoMappingContext() throws ClassNotFoundException {
    MongoMappingContext mappingContext = new MongoMappingContext();
    mappingContext.setInitialEntitySet(getInitialEntitySet());
    mappingContext.setSimpleTypeHolder(MongoSimpleTypes.HOLDER);
    mappingContext.setFieldNamingStrategy(null);
    mappingContext.setAutoIndexCreation(false);
    return mappingContext;
  }

  @Bean
  public MappingMongoConverter mappingMongoConverter() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext());
    converter.setCustomConversions(collectConverters());
    converter.setCodecRegistryProvider(mongoDbFactory());
    converter.afterPropertiesSet();
    return converter;
  }

  protected CustomConversions collectConverters() {
    List<?> converterInstances = springConverters.stream().map(injector::getInstance).collect(Collectors.toList());
    return new MongoCustomConversions(converterInstances);
  }

  protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {
    Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();
    for (String basePackage : SpringPersistenceConfig.BASE_PACKAGES) {
      initialEntitySet.addAll(scanForEntities(basePackage));
    }
    return initialEntitySet;
  }

  protected Set<Class<?>> scanForEntities(String basePackage) throws ClassNotFoundException {
    if (!StringUtils.hasText(basePackage)) {
      return Collections.emptySet();
    }

    Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();

    if (StringUtils.hasText(basePackage)) {
      ClassPathScanningCandidateComponentProvider componentProvider =
          new ClassPathScanningCandidateComponentProvider(false);
      componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
      componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

      for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
        initialEntitySet.add(
            ClassUtils.forName(candidate.getBeanClassName(), MongoConfigurationSupport.class.getClassLoader()));
      }
    }

    return initialEntitySet;
  }
}
