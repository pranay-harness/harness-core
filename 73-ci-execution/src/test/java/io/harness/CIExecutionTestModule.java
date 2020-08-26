package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.managerclient.ManagerCIResource;
import io.harness.managerclient.ManagerClientFactory;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.CiExecutionRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;
import io.harness.serializer.morphia.TestPersistenceMorphiaRegistrar;

import java.util.Set;

public class CIExecutionTestModule extends AbstractModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(CiExecutionRegistrars.kryoRegistrars)
        .add(TestPersistenceKryoRegistrar.class)
        .build();
  }

  @Provides
  @Singleton
  Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(CiExecutionRegistrars.morphiaRegistrars)
        .add(TestPersistenceMorphiaRegistrar.class)
        .build();
  }

  @Provides
  @Singleton
  ServiceTokenGenerator ServiceTokenGenerator() {
    return new ServiceTokenGenerator();
  }

  @Provides
  @Named("serviceSecret")
  String serviceSecret() {
    return "j6ErHMBlC2dn6WctNQKt0xfyo_PZuK7ls0Z4d6XCaBg";
  }

  @Provides
  @Singleton
  ManagerClientFactory managerClientFactory(
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    return new ManagerClientFactory("https://localhost:9090/api/", tokenGenerator, kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(ManagerCIResource.class).toProvider(ManagerClientFactory.class);
  }
}
