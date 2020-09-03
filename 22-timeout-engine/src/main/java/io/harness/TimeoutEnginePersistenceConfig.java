package io.harness;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotation.HarnessRepo;
import io.harness.exception.GeneralException;
import io.harness.spring.AliasRegistrar;
import io.harness.springdata.HMongoTemplate;
import io.harness.springdata.SpringPersistenceConfig;
import org.reflections.Reflections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoTypeMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableMongoRepositories(basePackages = {"io.harness.timeout"},
    includeFilters = @ComponentScan.Filter(HarnessRepo.class), mongoTemplateRef = "timeoutEngineMongoTemplate")
public class TimeoutEnginePersistenceConfig extends SpringPersistenceConfig {
  @Inject
  public TimeoutEnginePersistenceConfig(Injector injector) {
    super(injector);
  }

  @Bean(name = "timeoutEngineMongoTemplate")
  public MongoTemplate orchestrationMongoTemplate() throws Exception {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory());
    TypeInformationMapper typeMapper = TimeoutEngineTypeInformationMapper.builder().aliasMap(collectAliasMap()).build();
    MongoTypeMapper mongoTypeMapper =
        new DefaultMongoTypeMapper(DefaultMongoTypeMapper.DEFAULT_TYPE_KEY, Collections.singletonList(typeMapper));
    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, new MongoMappingContext());
    converter.setTypeMapper(mongoTypeMapper);
    converter.setCustomConversions(customConversions());
    converter.setCodecRegistryProvider(mongoDbFactory());
    converter.afterPropertiesSet();
    return new HMongoTemplate(mongoDbFactory(), converter);
  }

  private Map<String, Class<?>> collectAliasMap() {
    Map<String, Class<?>> aliases = new ConcurrentHashMap<>();
    try {
      Reflections reflections = new Reflections("io.harness.serializer.spring");
      for (Class clazz : reflections.getSubTypesOf(AliasRegistrar.class)) {
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
