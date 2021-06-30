package io.harness.token;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.HarnessCacheManager;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.token.remote.TokenClient;
import io.harness.token.remote.TokenClientHttpFactory;
import io.harness.version.VersionInfoManager;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import javax.cache.Cache;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

@OwnedBy(PL)
public class TokenClientModule extends AbstractModule {
  public static final String NG_HARNESS_API_KEY_CACHE = "NGHarnessApiKeyCache";
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public TokenClientModule(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  @Named("PRIVILEGED")
  private TokenClientHttpFactory privilegedTokenHttpClientFactory() {
    return new TokenClientHttpFactory(
        ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), clientId, ClientMode.PRIVILEGED);
  }

  @Provides
  @Named("NON_PRIVILEGED")
  private TokenClientHttpFactory nonPrivilegedTokenHttpClientFactory() {
    return new TokenClientHttpFactory(
        ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), clientId, ClientMode.NON_PRIVILEGED);
  }

  @Provides
  @Singleton
  @Named(NG_HARNESS_API_KEY_CACHE)
  public Cache<String, TokenDTO> getTokenCache(
      HarnessCacheManager harnessCacheManager, VersionInfoManager versionInfoManager) {
    return harnessCacheManager.getCache(NG_HARNESS_API_KEY_CACHE, String.class, TokenDTO.class,
        AccessedExpiryPolicy.factoryOf(Duration.TEN_MINUTES), versionInfoManager.getVersionInfo().getBuildNo());
  }

  @Override
  protected void configure() {
    requireBinding(HarnessCacheManager.class);
    requireBinding(VersionInfoManager.class);
    bind(TokenClient.class)
        .annotatedWith(Names.named(ClientMode.PRIVILEGED.name()))
        .toProvider(Key.get(TokenClientHttpFactory.class, Names.named(ClientMode.PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
    bind(TokenClient.class)
        .toProvider(Key.get(TokenClientHttpFactory.class, Names.named(ClientMode.NON_PRIVILEGED.name())))
        .in(Scopes.SINGLETON);
    bindCaches();
  }

  private void bindCaches() {
    MapBinder<String, Cache<?, ?>> mapBinder =
        MapBinder.newMapBinder(binder(), TypeLiteral.get(String.class), new TypeLiteral<Cache<?, ?>>() {});
    mapBinder.addBinding(NG_HARNESS_API_KEY_CACHE).to(Key.get(new TypeLiteral<Cache<String, TokenDTO>>() {
    }, Names.named(NG_HARNESS_API_KEY_CACHE)));
  }
}
