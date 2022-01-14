package io.harness.accesscontrol;

import static io.harness.eraro.ErrorCode.NG_ACCESS_DENIED;

import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.AccessDeniedException;

import java.util.EnumSet;
import java.util.List;
import lombok.Getter;

@OwnedBy(HarnessTeam.PL)
@Getter
public class NGAccessDeniedException extends AccessDeniedException {
  private final List<PermissionCheckDTO> failedPermissionChecks;

  public NGAccessDeniedException(
      String message, EnumSet<ReportTarget> reportTarget, List<PermissionCheckDTO> failedPermissionChecks) {
    super(message, NG_ACCESS_DENIED, reportTarget);
    this.failedPermissionChecks = failedPermissionChecks;
  }
}
