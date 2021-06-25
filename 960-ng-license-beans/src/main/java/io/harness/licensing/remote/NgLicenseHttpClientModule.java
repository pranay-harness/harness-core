package io.harness.licensing.remote;

import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NgLicenseHttpClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Provides
  private NgLicenseHttpClientFactory ngLicenseHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new NgLicenseHttpClientFactory(
        this.ngManagerClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(NgLicenseHttpClient.class).toProvider(NgLicenseHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
