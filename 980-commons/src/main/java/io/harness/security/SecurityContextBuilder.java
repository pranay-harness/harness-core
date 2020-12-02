package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.dto.ApiKeyPrincipal;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.ServicePrincipal;
import io.harness.security.dto.UserPrincipal;

import com.auth0.jwt.interfaces.Claim;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class SecurityContextBuilder {
  public static final String PRINCIPAL_TYPE = "type";
  public static final String PRINCIPAL_NAME = "name";
  public static final String ACCOUNT_ID = "accountId";

  public void setContext(Map<String, Claim> claimMap) {
    Principal principal = null;
    if (claimMap.get(PRINCIPAL_TYPE) != null) {
      PrincipalType type = claimMap.get(PRINCIPAL_TYPE).as(PrincipalType.class);
      switch (type) {
        case USER:
          principal = UserPrincipal.getPrincipal(claimMap);
          break;
        case API_KEY:
          principal = ApiKeyPrincipal.getPrincipal(claimMap);
          break;
        case SERVICE:
          principal = ServicePrincipal.getPrincipal(claimMap);
          break;
      }
    }
    PrincipalThreadLocal.set(principal);
  }

  public Principal getPrincipal() {
    return PrincipalThreadLocal.get();
  }

  public void unsetContext() {
    PrincipalThreadLocal.unset();
  }
}
