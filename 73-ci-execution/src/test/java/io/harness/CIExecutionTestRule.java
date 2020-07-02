package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.managerclient.KryoConverterFactory;
import io.harness.managerclient.ManagerCIResource;
import io.harness.managerclient.ManagerClientFactory;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.kryo.CIBeansRegistrar;
import io.harness.serializer.kryo.CIExecutionRegistrar;
import io.harness.serializer.kryo.CVNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;

import java.util.Set;

public class CIExecutionTestRule extends AbstractModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar>> registrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(ManagerRegistrars.kryoRegistrars)
        .add(CIBeansRegistrar.class)
        .add(CIExecutionRegistrar.class)
        .add(CVNextGenCommonsBeansKryoRegistrar.class)
        .add(TestPersistenceKryoRegistrar.class)
        .build();
  }

  @Provides
  @Singleton
  ServiceTokenGenerator ServiceTokenGenerator() {
    return new ServiceTokenGenerator();
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
