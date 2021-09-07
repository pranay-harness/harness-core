package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PL)
public class NonPrivilegedAccessControlClientImpl extends AbstractAccessControlClient {
  private final AccessControlHttpClient accessControlHttpClient;

  @Inject
  public NonPrivilegedAccessControlClientImpl(
      @Named("NON_PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    this.accessControlHttpClient = accessControlHttpClient;
  }

  @Override
  protected AccessCheckResponseDTO checkForAccess(AccessCheckRequestDTO accessCheckRequestDTO) {
    return NGRestUtils.getResponse(accessControlHttpClient.checkForAccess(accessCheckRequestDTO));
  }
}
