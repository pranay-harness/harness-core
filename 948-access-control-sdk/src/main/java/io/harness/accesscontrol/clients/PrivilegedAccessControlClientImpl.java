package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.NoArgsConstructor;

@Singleton
@OwnedBy(HarnessTeam.PL)
@NoArgsConstructor
public class PrivilegedAccessControlClientImpl extends AbstractAccessControlClient implements AccessControlClient {
  @Inject
  public PrivilegedAccessControlClientImpl(@Named("PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    super(accessControlHttpClient);
  }
}
