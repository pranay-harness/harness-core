/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.delegate.resources;

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
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Hidden
@Tag(name = "Delegate Configuration Management",
    description = "Contains APIs related to Delegate Configuration management")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
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
  @ApiOperation(value = "Gets Delegate Configuration (profile)", nickname = "getDelegateProfileNg")
  @Timed
  @ExceptionMetered
  @Hidden
  @Operation(operationId = "getDelegateConfigrationDetails",
      summary = "Retrieves Delegate Configuration details for given Delegate Configuration Id.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "A Delegate Configuration. "
                + "It includes uuid, accountId, name, description, startupScript, scopingRules, selectors, numberOfDelegates and other info.")
      })
  public RestResponse<DelegateProfileDetailsNg>
  get(@Parameter(description = "Delegate Configuration Id") @PathParam(
          "delegateProfileId") @NotEmpty String delegateProfileId,
      @Parameter(description = "Account id") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization Id") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project Id") @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_VIEW_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.get(accountId, delegateProfileId));
  }

  @POST
  @ApiOperation(value = "Adds a Delegate Configuration (profile)", nickname = "addDelegateProfileNg")
  @Hidden
  @Operation(operationId = "createDelegateConfiguration",
      summary = "Creates Delegate Configuration specified by Configuration details in body",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Delegate Configuration which was created.")
      })
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg>
  add(@Parameter(description = "Account id") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization Id") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project Id") @QueryParam("projectId") String projectId,
      @RequestBody(required = true,
          description =
              "Delegate Configuration to be created. These include uuid, identifier, accountId, orgId, projId, name, startupScript, scopingRules, selectors...")
      DelegateProfileDetailsNg delegateProfile) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_EDIT_PERMISSION);

    delegateProfile.setAccountId(accountId);
    return new RestResponse<>(delegateProfileManagerNgService.add(delegateProfile));
  }

  @PUT
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Updates a Delegate profile", nickname = "updateDelegateProfileNg")
  @Timed
  @ExceptionMetered
  @Hidden
  @Operation(operationId = "updateDelegateConfiguration", summary = "Updates Delegate Configuration specified by Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Delegate Configuration which was updated.")
      })
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg>
  update(@Parameter(description = "Delegate Configuration Id") @PathParam(
             "delegateProfileId") @NotEmpty String delegateProfileId,
      @Parameter(description = "Account id") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization Id") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project Id") @QueryParam("projectId") String projectId,
      @RequestBody(required = true,
          description =
              "Delegate Configuration to be created. These include uuid, identifier, accountId, orgId, projId, name, startupScript, scopingRules, selectors...")
      DelegateProfileDetailsNg delegateProfile) {
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
  @ApiOperation(value = "Updates the scoping rules inside the Delegate profile", nickname = "updateScopingRulesNg")
  @Hidden
  @Operation(operationId = "updateScopingRules",
      summary = "Updates Scoping Rules for the Delegate Configuration specified by Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Delegate Configuration which was updated.")
      })
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg>
  updateScopingRules(@Parameter(description = "Delegate Configuration Id") @PathParam(
                         "delegateProfileId") @NotEmpty String delegateProfileId,
      @Parameter(description = "Account id") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization Id") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project Id") @QueryParam("projectId") String projectId,
      @RequestBody(description = "Delegate Scoping Rules to be updated") List<ScopingRuleDetailsNg> scopingRules) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateProfileId), DELEGATE_CONFIG_EDIT_PERMISSION);

    return new RestResponse<>(
        delegateProfileManagerNgService.updateScopingRules(accountId, delegateProfileId, scopingRules));
  }

  @DELETE
  @Path("/{delegateProfileId}")
  @ApiOperation(value = "Deletes a Delegate Configuration (profile)", nickname = "deleteDelegateProfileNg")
  @Timed
  @ExceptionMetered
  @Hidden
  @Operation(operationId = "deleteDelegateConfig", summary = "Deletes Delegate Configuration specified by Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Boolean value resulting true if deletion was successful.")
      })
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<Void>
  delete(@Parameter(description = "Delegate Configuration Id") @PathParam(
             "delegateProfileId") @NotEmpty String delegateProfileId,
      @Parameter(description = "Account id") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization Id") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project Id") @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateProfileId), DELEGATE_CONFIG_DELETE_PERMISSION);

    delegateProfileManagerNgService.delete(accountId, delegateProfileId);
    return new RestResponse<>();
  }

  @GET
  @ApiOperation(value = "Lists the Delegate Configurations (profiles)", nickname = "listDelegateProfilesNg")
  @Timed
  @ExceptionMetered
  @Hidden
  @Operation(operationId = "getDelegateConfigurationsForAccount",
      summary = "Lists Delegate Configuration for specified Account, Organization and Project",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "A list of Delegate Configurations for the Account, Organization and Project")
      })
  public RestResponse<PageResponse<DelegateProfileDetailsNg>>
  list(@BeanParam PageRequest<DelegateProfileDetailsNg> pageRequest,
      @Parameter(description = "Account id") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization Id") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project Id") @QueryParam("projectId") String projectId) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, null), DELEGATE_CONFIG_VIEW_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.list(accountId, pageRequest, orgId, projectId));
  }

  @PUT
  @Path("/{delegateProfileId}/selectors")
  @ApiOperation(value = "Updates the selectors inside the Delegate profile", nickname = "updateSelectorsNg")
  @Timed
  @ExceptionMetered
  @Hidden
  @Operation(operationId = "updateDelegateSelectors",
      summary = "Updates Delegate Selectors for Delegate Configuration specified by Id",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Delegate Configuration which was updated.")
      })
  //@AuthRule(permissionType = MANAGE_DELEGATE_PROFILES)
  // This NG specific, switching to NG access control
  public RestResponse<DelegateProfileDetailsNg>
  updateSelectors(@Parameter(description = "Delegate Configuration Id") @PathParam(
                      "delegateProfileId") @NotEmpty String delegateProfileId,
      @Parameter(description = "Account Id") @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = "Organization Id") @QueryParam("orgId") String orgId,
      @Parameter(description = "Project Id") @QueryParam("projectId") String projectId,
      @RequestBody(description = "Delegate Selectors to be updated") List<String> selectors) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
        Resource.of(DELEGATE_CONFIG_RESOURCE_TYPE, delegateProfileId), DELEGATE_CONFIG_EDIT_PERMISSION);

    return new RestResponse<>(delegateProfileManagerNgService.updateSelectors(accountId, delegateProfileId, selectors));
  }
}
