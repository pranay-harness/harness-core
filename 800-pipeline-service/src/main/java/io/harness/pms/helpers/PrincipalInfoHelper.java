package io.harness.pms.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.plan.PrincipalType.API_KEY;
import static io.harness.pms.contracts.plan.PrincipalType.SERVICE;
import static io.harness.pms.contracts.plan.PrincipalType.USER;

import io.harness.PipelineServiceConfiguration;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.WingsException;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.plan.PrincipalType;
import io.harness.security.SecurityContextBuilder;

import com.google.inject.Inject;
import java.util.Objects;

@OwnedBy(PIPELINE)
public class PrincipalInfoHelper {
  @Inject PipelineServiceConfiguration configuration;

  public ExecutionPrincipalInfo getPrincipalInfoFromSecurityContext() {
    io.harness.security.dto.Principal principalInContext = SecurityContextBuilder.getPrincipal();
    if (principalInContext == null || principalInContext.getName() == null || principalInContext.getType() == null) {
      throw new AccessDeniedException("Principal cannot be null", ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
    return ExecutionPrincipalInfo.newBuilder()
        .setPrincipal(principalInContext.getName())
        .setPrincipalType(Objects.requireNonNull(fromSecurityPrincipalType(principalInContext.getType())))
        .setShouldValidateRbac(true)
        .build();
  }

  private PrincipalType fromSecurityPrincipalType(io.harness.security.dto.PrincipalType principalType) {
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
