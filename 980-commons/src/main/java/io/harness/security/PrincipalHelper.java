package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.dto.Principal;
import io.harness.security.dto.ServiceAccountPrincipal;
import io.harness.security.dto.UserPrincipal;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class PrincipalHelper {
  public String getUuid(Principal principal) {
    return principal == null ? null : principal.getName();
  }

  public String getIdentifier(Principal principal) {
    return principal == null ? null : principal.getName();
  }

  public String getEmail(Principal principal) {
    if (principal == null) {
      return null;
    }
    switch (principal.getType()) {
      case USER:
        return ((UserPrincipal) principal).getEmail();
      case SERVICE_ACCOUNT:
        return ((ServiceAccountPrincipal) principal).getEmail();
      default:
        return null;
    }
  }

  public String getUsername(Principal principal) {
    if (principal == null) {
      return null;
    }
    switch (principal.getType()) {
      case USER:
        return ((UserPrincipal) principal).getUsername();
      case SERVICE_ACCOUNT:
        return ((ServiceAccountPrincipal) principal).getUsername();
      default:
        return null;
    }
  }
}
