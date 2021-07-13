package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PL)
public class NonPrivilegedAccessControlClientImpl extends AbstractAccessControlClient implements AccessControlClient {
  @Inject
  public NonPrivilegedAccessControlClientImpl(
      @Named("NON_PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    super(accessControlHttpClient);
  }
}
