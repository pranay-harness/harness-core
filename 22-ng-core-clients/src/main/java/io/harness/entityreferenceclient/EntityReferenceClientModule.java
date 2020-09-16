package io.harness.entityreferenceclient;

import static io.harness.annotations.dev.HarnessTeam.DX;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.entityreferenceclient.remote.EntityReferenceClient;
import io.harness.entityreferenceclient.remote.EntityReferenceHttpClientFactory;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

@OwnedBy(DX)
public class EntityReferenceClientModule extends AbstractModule {
  private final NGManagerClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public EntityReferenceClientModule(
      NGManagerClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private EntityReferenceHttpClientFactory entityReferenceHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new EntityReferenceHttpClientFactory(
        ngManagerClientConfig, serviceSecret, clientId, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(EntityReferenceClient.class).toProvider(EntityReferenceHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
