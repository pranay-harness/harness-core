package io.harness.ng;

import static com.google.inject.Key.get;
import static com.google.inject.name.Names.named;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;

import com.mongodb.MongoClient;
import io.harness.annotation.HarnessRepo;
import io.harness.exception.GeneralException;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.mongodb.morphia.AdvancedDatastore;
import org.reflections.Reflections;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.guice.annotation.GuiceModule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@EnableMongoRepositories(
    basePackages = {"io.harness.engine", "io.harness.ng"}, includeFilters = @ComponentScan.Filter(HarnessRepo.class))
@EnableMongoAuditing
@Configuration
@GuiceModule
public class SpringMongoConfig extends AbstractMongoConfiguration {
  private final AdvancedDatastore advancedDatastore;
  private static final Collection<String> BASE_PACKAGES = ImmutableList.of("io.harness");

  private final List<Converter> converters = collectConverters();

  @Inject
  public SpringMongoConfig(Injector injector) {
    advancedDatastore = injector.getProvider(get(AdvancedDatastore.class, named("primaryDatastore"))).get();
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
  public CustomConversions customConversions() {
    return new MongoCustomConversions(converters);
  }

  @Override
  protected Collection<String> getMappingBasePackages() {
    return BASE_PACKAGES;
  }

  private List<Converter> collectConverters() {
    Set<Converter> converterSet = new ConcurrentHashSet<>();
    try {
      Reflections reflections = new Reflections("io.harness.serializer.springdata");
      for (Class clazz : reflections.getSubTypesOf(SpringConverterRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final SpringConverterRegistrar converterRegistrar = (SpringConverterRegistrar) constructor.newInstance();
        converterRegistrar.registerConverters(converterSet);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new GeneralException("Failed initializing Spring Data Converters", e);
    }
    return new ArrayList<>(converterSet);
  }
}
