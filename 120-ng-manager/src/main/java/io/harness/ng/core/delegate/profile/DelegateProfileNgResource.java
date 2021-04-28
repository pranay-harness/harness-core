package io.harness.ng.core.delegate.profile;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATE_PROFILES;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE_SCOPE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.ScopingRuleDetailsNg;
import io.harness.ng.core.api.DelegateProfileManagerNgService;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Path("/delegate-profiles/ng")
@Api("delegate-profiles/ng")
@Produces("application/json")
@Scope(DELEGATE_SCOPE)
@AuthRule(permissionType = LOGGED_IN)
@OwnedBy(HarnessTeam.DEL)
public class DelegateProfileNgResource {
  private final DelegateProfileManagerNgService delegateProfileManagerNgService;

  @Inject
  public DelegateProfileNgResource(DelegateProfileManagerNgService delegateProfileManagerNgService) {
    this.delegateProfileManagerNgService = delegateProfileManagerNgService;
  }

  @GET
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Gets delegate profile", nickname = "getDelegateProfileNg")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileDetailsNg> get(@PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateProfileManagerNgService.get(accountId, delegateProfileId));
  }

  @POST
  @ApiOperation(value = "Adds a delegate profile", nickname = "addDelegateProfileNg")
  @AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  public RestResponse<DelegateProfileDetailsNg> add(
      @QueryParam("accountId") @NotEmpty String accountId, DelegateProfileDetailsNg delegateProfile) {
    delegateProfile.setAccountId(accountId);
    return new RestResponse<>(delegateProfileManagerNgService.add(delegateProfile));
  }

  @PUT
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Updates a delegate profile", nickname = "updateDelegateProfileNg")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  public RestResponse<DelegateProfileDetailsNg> update(
      @PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateProfileDetailsNg delegateProfile) {
    delegateProfile.setAccountId(accountId);
    delegateProfile.setUuid(delegateProfileId);
    return new RestResponse<>(delegateProfileManagerNgService.update(delegateProfile));
  }

  @PUT
  @Path("/{delegateProfileId}/scoping-rules")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Updates the scoping rules inside the delegate profile", nickname = "updateScopingRulesNg")
  @AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  public RestResponse<DelegateProfileDetailsNg> updateScopingRules(
      @PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, List<ScopingRuleDetailsNg> scopingRules) {
    return new RestResponse<>(
        delegateProfileManagerNgService.updateScopingRules(accountId, delegateProfileId, scopingRules));
  }

  @DELETE
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Deletes a delegate profile", nickname = "deleteDelegateProfileNg")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  public RestResponse<Void> delete(@PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    delegateProfileManagerNgService.delete(accountId, delegateProfileId);
    return new RestResponse<>();
  }

  @GET
  @ApiOperation(value = "Lists the delegate profiles", nickname = "listDelegateProfilesNg")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<DelegateProfileDetailsNg>> list(
      @BeanParam PageRequest<DelegateProfileDetailsNg> pageRequest, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId) {
    return new RestResponse<>(delegateProfileManagerNgService.list(accountId, pageRequest, orgId, projectId));
  }

  @PUT
  @Path("/{delegateProfileId}/selectors")
  @ApiOperation(value = "Updates the selectors inside the delegate profile", nickname = "updateSelectorsNg")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  public RestResponse<DelegateProfileDetailsNg> updateSelectors(
      @PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, List<String> selectors) {
    return new RestResponse<>(delegateProfileManagerNgService.updateSelectors(accountId, delegateProfileId, selectors));
  }
}
