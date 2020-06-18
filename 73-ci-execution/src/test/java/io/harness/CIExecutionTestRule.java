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
import io.harness.serializer.kryo.ApiServiceKryoRegister;
import io.harness.serializer.kryo.CIBeansRegistrar;
import io.harness.serializer.kryo.CIExecutionRegistrar;
import io.harness.serializer.kryo.CVNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.CVNextGenRestBeansKryoRegistrar;
import io.harness.serializer.kryo.CommonsKryoRegistrar;
import io.harness.serializer.kryo.DelegateAgentKryoRegister;
import io.harness.serializer.kryo.DelegateKryoRegister;
import io.harness.serializer.kryo.DelegateTasksKryoRegister;
import io.harness.serializer.kryo.ManagerKryoRegistrar;
import io.harness.serializer.kryo.NGKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationBeansKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationKryoRegister;
import io.harness.serializer.kryo.PersistenceRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;

import java.util.Set;

public class CIExecutionTestRule extends AbstractModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar>> registrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .add(ApiServiceKryoRegister.class)
        .add(CIBeansRegistrar.class)
        .add(CIExecutionRegistrar.class)
        .add(CommonsKryoRegistrar.class)
        .add(CVNextGenCommonsBeansKryoRegistrar.class)
        .add(CVNextGenRestBeansKryoRegistrar.class)
        .add(DelegateAgentKryoRegister.class)
        .add(DelegateKryoRegister.class)
        .add(DelegateTasksKryoRegister.class)
        .add(ManagerKryoRegistrar.class)
        .add(NGKryoRegistrar.class)
        .add(OrchestrationBeansKryoRegistrar.class)
        .add(OrchestrationKryoRegister.class)
        .add(PersistenceRegistrar.class)
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
