package io.harness.ng.core.smtp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

@OwnedBy(PL)
public class NgSMTPSettingsHttpClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;
  private static NgSMTPSettingsHttpClientModule instance;

  public NgSMTPSettingsHttpClientModule(
      ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  public static NgSMTPSettingsHttpClientModule getInstance(
      ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    if (instance == null) {
      instance = new NgSMTPSettingsHttpClientModule(ngManagerClientConfig, serviceSecret, clientId);
    }
    return instance;
  }

  @Provides
  @Named("PRIVILEGED")
  private NgSMTPSettingsHttpClientFactory privilegedNgSMTPSettingsHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new NgSMTPSettingsHttpClientFactory(this.ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }

  @Provides
  @Named("NON_PRIVILEGED")
  private NgSMTPSettingsHttpClientFactory nonPrivilegedNgSMTPSettingsHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new NgSMTPSettingsHttpClientFactory(this.ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(NgSMTPSettingsHttpClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(NgSMTPSettingsHttpClientFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
    bind(NgSMTPSettingsHttpClient.class)
        .toProvider(Key.get(NgSMTPSettingsHttpClientFactory.class, Names.named(ClientMode.NON_PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
  }
}
