package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.PL)
@NoArgsConstructor
public class NonPrivilegedAccessControlClientImpl extends PrivilegedAccessControlClientImpl {
  @Inject
  public NonPrivilegedAccessControlClientImpl(
      @Named("NON_PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    super(accessControlHttpClient);
  }
}
