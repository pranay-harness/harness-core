package io.harness.orchestration.persistence;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.exception.GeneralException;
import io.harness.spring.AliasRegistrar;
import io.harness.springdata.SpringPersistenceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoTypeMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class OrchestrationBasePersistenceConfig extends SpringPersistenceConfig {
  private static final String ORCHESTRATION_TYPE_KEY = "_orchestrationClass";
  protected final Injector injector;
  private final Set<Class<? extends AliasRegistrar>> aliasRegistrars;

  @Inject
  public OrchestrationBasePersistenceConfig(Injector injector, Set<Class<? extends AliasRegistrar>> aliasRegistrars) {
    super(injector);
    this.injector = injector;
    this.aliasRegistrars = aliasRegistrars;
  }

  @Bean(name = "orchestrationMongoTemplate")
  public MongoTemplate orchestrationMongoTemplate() throws GeneralException {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
    TypeInformationMapper typeMapper =
        OrchestrationTypeInformationMapper.builder().aliasMap(collectAliasMap(aliasRegistrars)).build();
    MongoTypeMapper mongoTypeMapper =
        new DefaultMongoTypeMapper(ORCHESTRATION_TYPE_KEY, Collections.singletonList(typeMapper));
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, new MongoMappingContext());
    converter.setTypeMapper(mongoTypeMapper);
    converter.setCustomConversions(customConversions());
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
