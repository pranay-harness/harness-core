package io.harness.analyserservice;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.serializer.KryoRegistrar;
import io.harness.service.QueryStatsService;
import io.harness.service.QueryStatsServiceImpl;
import io.harness.springdata.SpringPersistenceModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(PIPELINE)
@Slf4j
public class AnalyserServiceModule extends AbstractModule {
  private static AnalyserServiceModule instance;
  private final AnalyserServiceConfiguration config;

  private AnalyserServiceModule(AnalyserServiceConfiguration config) {
    this.config = config;
  }

  public static synchronized AnalyserServiceModule getInstance(AnalyserServiceConfiguration config) {
    if (instance == null) {
      instance = new AnalyserServiceModule(config);
    }
    return instance;
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return new HashMap<>();
  }

  @Override
  protected void configure() {
    install(new AbstractMongoModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return new HashSet<>();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return new HashSet<>();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();
      }

      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });

    install(new EventsFrameworkModule(config.getEventsFrameworkConfiguration()));
    install(new SpringPersistenceModule());
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(QueryStatsService.class).to(QueryStatsServiceImpl.class);
  }

  @Provides
  @Singleton
  public MongoConfig mongoConfig(AnalyserServiceConfiguration configuration) {
    return configuration.getMongoConfig();
  }
}
