package io.harness.projectmanagerclient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.projectmanagerclient.remote.ProjectManagerHttpClientFactory;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

public class ProjectManagementClientModule extends AbstractModule {
  private final ProjectManagerClientConfig projectManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public ProjectManagementClientModule(
      ProjectManagerClientConfig projectManagerClientConfig, String serviceSecret, String clientId) {
    this.projectManagerClientConfig = projectManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private ProjectManagerHttpClientFactory projectManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ProjectManagerHttpClientFactory(
        projectManagerClientConfig, serviceSecret, clientId, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(ProjectManagerClient.class).toProvider(ProjectManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
