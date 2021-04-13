package io.harness.project;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.project.remote.ProjectClient;
import io.harness.project.remote.ProjectHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PL)
public class ProjectClientModule extends AbstractModule {
  private final ServiceHttpClientConfig projectManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public ProjectClientModule(
      ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret, String clientId) {
    this.projectManagerClientConfig = projectManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private ProjectHttpClientFactory projectManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ProjectHttpClientFactory(
        projectManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(ProjectClient.class).toProvider(ProjectHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
