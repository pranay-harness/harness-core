package io.harness.accesscontrol.clients;

import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class RoleAssignmentClientModule extends AbstractModule {
  private final ServiceHttpClientConfig roleAssignmentClientConfig;
  private final String roleAssignmentClientSecret;
  private final String clientId;
  private static RoleAssignmentClientModule instance;

  private RoleAssignmentClientModule(
      ServiceHttpClientConfig roleAssignmentClientConfig, String secret, String clientId) {
    this.roleAssignmentClientConfig = roleAssignmentClientConfig;
    this.roleAssignmentClientSecret = secret;
    this.clientId = clientId;
  }

  public static synchronized RoleAssignmentClientModule getInstance(
      ServiceHttpClientConfig roleAssignmentClientConfig, String secret, String clientId) {
    if (instance == null) {
      instance = new RoleAssignmentClientModule(roleAssignmentClientConfig, secret, clientId);
    }
    return instance;
  }

  @Provides
  public RoleAssignmentHttpClientFactory roleAssignmentHttpClientFactory() {
    return new RoleAssignmentHttpClientFactory(roleAssignmentClientConfig, roleAssignmentClientSecret,
        new ServiceTokenGenerator(), null, clientId, false, ClientMode.PRIVILEGED);
  }

  @Override
  protected void configure() {
    bind(RoleAssignmentClient.class).toProvider(RoleAssignmentHttpClientFactory.class);
  }
}
