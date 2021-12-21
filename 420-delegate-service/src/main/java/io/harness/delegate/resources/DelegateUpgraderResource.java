package io.harness.delegate.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.UpgradeCheckResult;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.service.intfc.DelegateUpgraderService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/upgrade-check")
@Path("/upgrade-check")
@Produces("application/json")
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateUpgraderResource {
  private final DelegateUpgraderService delegateUpgraderService;

  @Inject
  public DelegateUpgraderResource(DelegateUpgraderService delegateUpgraderService) {
    this.delegateUpgraderService = delegateUpgraderService;
  }

  @DelegateAuth
  @GET
  @Path("/delegate")
  @Timed
  @ExceptionMetered
  public RestResponse<UpgradeCheckResult> getDelegateImageTag(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("currentDelegateImageTag") @NotEmpty String currentDelegateImageTag) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateUpgraderService.getDelegateImageTag(accountId, currentDelegateImageTag));
    }
  }

  @DelegateAuth
  @GET
  @Path("/upgrader")
  @Timed
  @ExceptionMetered
  public RestResponse<UpgradeCheckResult> getUpgraderImageTag(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("currentUpgraderImageTag") @NotEmpty String currentUpgraderImageTag) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateUpgraderService.getUpgraderImageTag(accountId, currentUpgraderImageTag));
    }
  }
}
