package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;

import com.google.inject.Inject;

@OwnedBy(PL)
public class SecretPermissionValidator {
  private final AccessControlClient accessControlClient;

  @Inject
  public SecretPermissionValidator(AccessControlClient accessControlClient) {
    this.accessControlClient = accessControlClient;
  }

  public void checkForAccessOrThrow(
      ResourceScope resourceScope, Resource resource, String permission, Principal owner) {
    if (owner != null) {
      Principal currentPrincipal = SourcePrincipalContextBuilder.getSourcePrincipal();
      if (currentPrincipal == null) {
        currentPrincipal = SecurityContextBuilder.getPrincipal();
      }
      if (currentPrincipal == null || !currentPrincipal.getType().equals(owner.getType())
          || !currentPrincipal.getName().equals(owner.getName())) {
        throw new AccessDeniedException("Not authorized", ErrorCode.NG_ACCESS_DENIED, USER);
      }
      return;
    }
    accessControlClient.checkForAccessOrThrow(resourceScope, resource, permission);
  }
}
