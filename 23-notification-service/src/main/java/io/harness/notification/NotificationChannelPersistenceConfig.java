package io.harness.notification;

import io.harness.annotation.HarnessRepo;
import io.harness.springdata.HMongoTemplate;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.MongoConfigurationSupport;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoSimpleTypes;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.notification"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "notification-channel")
public class NotificationChannelPersistenceConfig {
  private final MongoBackendConfiguration mongoBackendConfiguration;

  private static final Collection<String> BASE_PACKAGES = ImmutableList.of("io.harness");

  @Inject
  public NotificationChannelPersistenceConfig(
      Injector injector, @Named("notification-channel") MongoBackendConfiguration mongoBackendConfiguration) {
    this.mongoBackendConfiguration =
        (MongoBackendConfiguration) injector.getInstance(Key.get(NotificationConfiguration.class))
            .getNotificationClientConfiguration()
            .getNotificationClientBackendConfiguration();
  }

  @Bean
  public MongoDbFactory mongoDbFactory() {
    return new SimpleMongoDbFactory(new MongoClientURI(mongoBackendConfiguration.getUri()));
  }

  @Bean
  MongoTransactionManager transactionManager(MongoDbFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @Bean(name = "notification-channel")
  @Primary
  public MongoTemplate mongoTemplate() throws Exception {
    return new HMongoTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Bean
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
    converter.setCodecRegistryProvider(mongoDbFactory());
    converter.afterPropertiesSet();
    return converter;
  }

  protected String getDatabaseName() {
    return new MongoClientURI(mongoBackendConfiguration.getUri()).getDatabase();
  }

  protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {
    Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();
    for (String basePackage : NotificationChannelPersistenceConfig.BASE_PACKAGES) {
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
