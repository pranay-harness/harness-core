package io.harness.pms.sample.cv;

import io.harness.OrchestrationModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.OrchestrationBeansRegistrars;
import io.harness.serializer.PmsSdkModuleRegistrars;
import io.harness.springdata.SpringPersistenceModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

public class CvServiceModule extends AbstractModule {
  private final CvServiceConfiguration config;

  public CvServiceModule(CvServiceConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(MongoModule.getInstance());
    install(new SpringPersistenceModule());
    bind(HPersistence.class).to(MongoPersistence.class);
    install(OrchestrationModule.getInstance());
    install(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });
  }

  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(PmsSdkModuleRegistrars.kryoRegistrars)
        .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(PmsSdkModuleRegistrars.morphiaRegistrars)
        .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder()
        .addAll(PmsSdkModuleRegistrars.morphiaConverters)
        .addAll(OrchestrationBeansRegistrars.morphiaConverters)
        .build();
  }

  @Provides
  @Singleton
  List<Class<? extends Converter<?, ?>>> springConverters() {
    return ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();
  }

  @Provides
  @Singleton
  public MongoConfig mongoConfig() {
    return config.getMongoConfig();
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return Collections.emptyMap();
  }

  @Provides
  @Singleton
  public OrchestrationModuleConfig orchestrationModuleConfig() {
    return OrchestrationModuleConfig.builder()
        .serviceName("CV_SAMPLE")
        .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
        .withPMS(true)
        .build();
  }
}
