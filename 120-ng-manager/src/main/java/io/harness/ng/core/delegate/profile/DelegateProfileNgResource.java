/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.delegate.profile;

import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_DELETE_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_EDIT_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_RESOURCE_TYPE;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_CONFIG_VIEW_PERMISSION;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.ScopingRuleDetailsNg;
import io.harness.ng.core.api.DelegateProfileManagerNgService;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;

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
@AuthRule(permissionType = LOGGED_IN)
//@Scope(DELEGATE_SCOPE)
// This NG specific, switching to NG access control. AuthRule to be removed also when NG access control is fully
// enabled.
@OwnedBy(HarnessTeam.DEL)
public class DelegateProfileNgResource {
  private final DelegateProfileManagerNgService delegateProfileManagerNgService;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateProfileNgResource(
      DelegateProfileManagerNgService delegateProfileManagerNgService, AccessControlClient accessControlClient) {
    this.delegateProfileManagerNgService = delegateProfileManagerNgService;
    this.accessControlClient = accessControlClient;
  }

  @GET
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Gets delegate profile", nickname = "getDelegateProfileNg")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileDetailsNg> get(@PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_VIEW_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.get(accountId, delegateProfileId));
  }

  @POST
  @ApiOperation(value = "Adds a delegate profile", nickname = "addDelegateProfileNg")
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg> add(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("orgId") String orgId, @QueryParam("projectId") String projectId,
      DelegateProfileDetailsNg delegateProfile) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_EDIT_PERMISSION);

    delegateProfile.setAccountId(accountId);
    return new RestResponse<>(delegateProfileManagerNgService.add(delegateProfile));
  }

  @PUT
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Updates a delegate profile", nickname = "updateDelegateProfileNg")
  @Timed
  @ExceptionMetered
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg> update(
      @PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, DelegateProfileDetailsNg delegateProfile) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateProfileId), DELEGATE_CONFIG_EDIT_PERMISSION);

    delegateProfile.setAccountId(accountId);
    delegateProfile.setUuid(delegateProfileId);
    return new RestResponse<>(delegateProfileManagerNgService.update(delegateProfile));
  }

  @PUT
  @Path("/{delegateProfileId}/scoping-rules")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Updates the scoping rules inside the delegate profile", nickname = "updateScopingRulesNg")
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg> updateScopingRules(
      @PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, List<ScopingRuleDetailsNg> scopingRules) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateProfileId), DELEGATE_CONFIG_EDIT_PERMISSION);

    return new RestResponse<>(
        delegateProfileManagerNgService.updateScopingRules(accountId, delegateProfileId, scopingRules));
  }

  @DELETE
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Deletes a delegate profile", nickname = "deleteDelegateProfileNg")
  @Timed
  @ExceptionMetered
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<Void> delete(@PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateProfileId), DELEGATE_CONFIG_DELETE_PERMISSION);

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
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_VIEW_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.list(accountId, pageRequest, orgId, projectId));
  }

  @PUT
  @Path("/{delegateProfileId}/selectors")
  @ApiOperation(value = "Updates the selectors inside the delegate profile", nickname = "updateSelectorsNg")
  @Timed
  @ExceptionMetered
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg> updateSelectors(
      @PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("orgId") String orgId,
      @QueryParam("projectId") String projectId, List<String> selectors) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateProfileId), DELEGATE_CONFIG_EDIT_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.updateSelectors(accountId, delegateProfileId, selectors));
  }
}
