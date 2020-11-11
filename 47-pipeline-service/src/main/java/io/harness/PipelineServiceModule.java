package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.grpc.server.PipelineServiceGrpcModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.PipelineServiceModuleRegistrars;
import io.harness.spring.AliasRegistrar;
import org.mongodb.morphia.converters.TypeConverter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class PipelineServiceModule extends AbstractModule {
  private final PipelineServiceConfiguration appConfig;

  public PipelineServiceModule(PipelineServiceConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {
    install(MongoModule.getInstance());
    install(new PipelineServiceGrpcModule(appConfig));
    bind(HPersistence.class).to(MongoPersistence.class);
  }

  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.kryoRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
    return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.aliasRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder()
        .addAll(PipelineServiceModuleRegistrars.morphiaConverters)
        .build();
  }

  @Provides
  @Singleton
  public MongoConfig mongoConfig() {
    return appConfig.getMongoConfig();
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return Collections.emptyMap();
  }
}
