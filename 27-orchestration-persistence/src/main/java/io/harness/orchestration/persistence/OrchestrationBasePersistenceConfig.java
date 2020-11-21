package io.harness.orchestration.persistence;

import io.harness.exception.GeneralException;
import io.harness.spring.AliasRegistrar;
import io.harness.springdata.SpringPersistenceConfig;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoTypeMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

public abstract class OrchestrationBasePersistenceConfig extends SpringPersistenceConfig {
  private static final String ORCHESTRATION_TYPE_KEY = "_orchestrationClass";
  private final Set<Class<? extends AliasRegistrar>> aliasRegistrars;

  @Inject
  public OrchestrationBasePersistenceConfig(Injector injector, Set<Class<? extends AliasRegistrar>> aliasRegistrars,
      List<Class<? extends Converter>> converters) {
    super(injector, converters);
    this.aliasRegistrars = aliasRegistrars;
  }

  @Bean(name = "orchestrationMongoTemplate")
  public MongoTemplate orchestrationMongoTemplate() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
    TypeInformationMapper typeMapper =
        OrchestrationTypeInformationMapper.builder().aliasMap(collectAliasMap(aliasRegistrars)).build();
    MongoTypeMapper mongoTypeMapper =
        new DefaultMongoTypeMapper(ORCHESTRATION_TYPE_KEY, Collections.singletonList(typeMapper));
    MongoMappingContext mappingContext = mongoMappingContext();
    mappingContext.setAutoIndexCreation(false);
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setTypeMapper(mongoTypeMapper);
    converter.setCustomConversions(collectConverters());
    converter.setCodecRegistryProvider(mongoDbFactory());
    converter.afterPropertiesSet();
    return new OrchestrationMongoTemplate(mongoDbFactory(), converter);
  }

  private Map<String, Class<?>> collectAliasMap(Set<Class<? extends AliasRegistrar>> registrars) {
    Map<String, Class<?>> aliases = new ConcurrentHashMap<>();
    try {
      for (Class<? extends AliasRegistrar> clazz : registrars) {
        Constructor<?> constructor = clazz.getConstructor();
        final AliasRegistrar aliasRegistrar = (AliasRegistrar) constructor.newInstance();
        aliasRegistrar.register(aliases);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new GeneralException("Failed initializing Spring Data Converters", e);
    }
    return aliases;
  }
}
