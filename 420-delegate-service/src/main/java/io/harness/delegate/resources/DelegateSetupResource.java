package io.harness.delegate.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateSetupService;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/setup/delegates/v2")
@Path("/setup/delegates/v2")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupResource {
  private final DelegateSetupService delegateSetupService;

  @Inject
  public DelegateSetupResource(DelegateSetupService delegateSetupService) {
    this.delegateSetupService = delegateSetupService;
  }

  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<DelegateGroupListing> list(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateSetupService.listDelegateGroupDetails(accountId, orgId, projectId));
    }
  }

  @GET
  @Path("{delegateGroupId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<DelegateGroupDetails> get(@PathParam("delegateGroupId") @NotEmpty String delegateGroupId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateSetupService.getDelegateGroupDetails(accountId, delegateGroupId));
    }
  }
}
