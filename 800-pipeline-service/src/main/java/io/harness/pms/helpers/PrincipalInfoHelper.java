package io.harness.pms.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.plan.PrincipalType.API_KEY;
import static io.harness.pms.contracts.plan.PrincipalType.SERVICE;
import static io.harness.pms.contracts.plan.PrincipalType.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.plan.PrincipalType;
import io.harness.security.SecurityContextBuilder;

import java.util.Objects;

@OwnedBy(PIPELINE)
public class PrincipalInfoHelper {
  public static ExecutionPrincipalInfo getPrincipalInfoFromSecurityContext() {
    io.harness.security.dto.Principal principalInContext = SecurityContextBuilder.getPrincipal();
    if (principalInContext == null || principalInContext.getName() == null || principalInContext.getType() == null) {
      return ExecutionPrincipalInfo.newBuilder().build();
    }
    return ExecutionPrincipalInfo.newBuilder()
        .setPrincipal(principalInContext.getName())
        .setPrincipalType(Objects.requireNonNull(fromSecurityPrincipalType(principalInContext.getType())))
        .build();
  }

  private static PrincipalType fromSecurityPrincipalType(io.harness.security.dto.PrincipalType principalType) {
    switch (principalType) {
      case SERVICE:
        return SERVICE;
      case API_KEY:
        return API_KEY;
      case USER:
        return USER;
      default:
        return null;
    }
  }
}
