package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@OwnedBy(HarnessTeam.PL)
public class PrivilegedAccessControlClientImpl extends AbstractAccessControlClient {
  private final AccessControlHttpClient accessControlHttpClient;

  @Inject
  public PrivilegedAccessControlClientImpl(@Named("PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    this.accessControlHttpClient = accessControlHttpClient;
  }

  @Override
  protected AccessCheckResponseDTO checkForAccess(AccessCheckRequestDTO accessCheckRequestDTO) {
    return NGRestUtils.getResponse(accessControlHttpClient.checkForAccess(accessCheckRequestDTO));
  }
}
