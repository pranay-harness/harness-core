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
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.kryo.KryoConverterFactory;
import org.mongodb.morphia.converters.TypeConverter;

import java.util.Set;

public class CIExecutionTestModule extends AbstractModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(CiExecutionRegistrars.kryoRegistrars).build();
  }

  @Provides
  @Singleton
  Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(CiExecutionRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder().addAll(ManagerRegistrars.morphiaConverters).build();
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
