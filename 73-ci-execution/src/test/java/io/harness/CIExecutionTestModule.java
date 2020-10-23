package io.harness;

import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.connector.apis.client.ConnectorResourceClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.logserviceclient.CILogServiceClientModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.secrets.SecretNGManagerClientModule;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.CiExecutionRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
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

  @Override
  protected void configure() {
    bind(DelegateServiceGrpcClient.class).toInstance(mock(DelegateServiceGrpcClient.class));
    install(new ConnectorResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:3457/").build(), "test_secret"));
    install(new SecretNGManagerClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:7457/").build(), "test_secret"));
    install(new SecretManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:3457/").build(), "test_secret"));
    install(new CILogServiceClientModule(
        LogServiceConfig.builder().baseUrl("http://localhost:8079").globalToken("token").build()));
  }
}
