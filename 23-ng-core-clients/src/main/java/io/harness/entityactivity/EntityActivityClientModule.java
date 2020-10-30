package io.harness.entityactivity;

import static io.harness.annotations.dev.HarnessTeam.DX;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.entityactivity.remote.EntityActivityClient;
import io.harness.entityactivity.remote.EntityActivityHttpClientFactory;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

@OwnedBy(DX)
public class EntityActivityClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;

  public EntityActivityClientModule(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private EntityActivityHttpClientFactory entityActivityHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new EntityActivityHttpClientFactory(
        ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(EntityActivityClient.class).toProvider(EntityActivityHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}