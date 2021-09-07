package io.harness.usermembership;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.usermembership.remote.UserMembershipClient;
import io.harness.usermembership.remote.UserMembershipHttpClientFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

@OwnedBy(PL)
public class UserMembershipClientModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public UserMembershipClientModule(
      ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  @Named("PRIVILEGED")
  private UserMembershipHttpClientFactory privilegedUserMembershipHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new UserMembershipHttpClientFactory(serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.PRIVILEGED);
  }

  @Provides
  @Named("NON_PRIVILEGED")
  private UserMembershipHttpClientFactory nonPrivilegedUserMembershipHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new UserMembershipHttpClientFactory(serviceHttpClientConfig, serviceSecret, new ServiceTokenGenerator(),
        kryoConverterFactory, clientId, ClientMode.NON_PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(UserMembershipClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(UserMembershipHttpClientFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
    bind(UserMembershipClient.class)
        .toProvider(Key.get(UserMembershipHttpClientFactory.class, Names.named(ClientMode.NON_PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
  }
}
