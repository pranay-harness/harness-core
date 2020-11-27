package io.harness.projectmanagerclient;

import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.projectmanagerclient.remote.ProjectManagerHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class ProjectManagementClientModule extends AbstractModule {
  private final ServiceHttpClientConfig projectManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  public ProjectManagementClientModule(
      ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret, String clientId) {
    this.projectManagerClientConfig = projectManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private ProjectManagerHttpClientFactory projectManagerHttpClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ProjectManagerHttpClientFactory(
        projectManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    bind(ProjectManagerClient.class).toProvider(ProjectManagerHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
