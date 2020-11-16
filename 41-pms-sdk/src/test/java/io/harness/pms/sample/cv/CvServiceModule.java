package io.harness.pms.sample.cv;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.PmsSdkModule;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.pms.sample.cv.creator.CvPlanCreatorProvider;
import io.harness.pms.sdk.creator.PlanCreatorProvider;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.PmsSdkModuleRegistrars;
import io.harness.spring.AliasRegistrar;
import org.mongodb.morphia.converters.TypeConverter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class CvServiceModule extends AbstractModule {
  private final CvServiceConfiguration config;

  public CvServiceModule(CvServiceConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(MongoModule.getInstance());
    bind(HPersistence.class).to(MongoPersistence.class);

    install(PmsSdkModule.getInstance());
  }

  @Provides
  @Singleton
  @Named("pms-grpc-server-config")
  public GrpcServerConfig pmsGrpcServerConfig() {
    return config.getPmsSdkGrpcServerConfig();
  }

  @Provides
  @Singleton
  public PlanCreatorProvider planCreatorProvider() {
    return new CvPlanCreatorProvider();
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(PmsSdkModuleRegistrars.kryoRegistrars).build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(PmsSdkModuleRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
    return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
        .addAll(PmsSdkModuleRegistrars.aliasRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder()
        .addAll(PmsSdkModuleRegistrars.morphiaConverters)
        .build();
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
}
