package io.harness.gitsync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.clients.YamlGitConfigClient;
import io.harness.gitsync.clients.YamlGitConfigHttpFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.PL)
public class GitSyncConfigClientModule extends AbstractModule {
  private final ServiceHttpClientConfig secretManagerConfig;
  private final String serviceSecret;
  private final String clientId;

  public GitSyncConfigClientModule(ServiceHttpClientConfig secretManagerConfig, String serviceSecret, String clientId) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Override
  protected void configure() {
    bind(YamlGitConfigClient.class).toProvider(YamlGitConfigHttpFactory.class).in(Scopes.SINGLETON);
  }

  @Provides
  private YamlGitConfigHttpFactory yamlGitConfigHttpFactory() {
    return new YamlGitConfigHttpFactory(
        secretManagerConfig, serviceSecret, new ServiceTokenGenerator(), null, clientId);
  }
}
