package io.harness.accesscontrol.clients;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NonPrivilegedAccessControlClientImpl extends PrivilegedAccessControlClientImpl {
  @Inject
  public NonPrivilegedAccessControlClientImpl(
      @Named("NON_PRIVILEGED") AccessControlHttpClient accessControlHttpClient) {
    super(accessControlHttpClient);
  }
}
