/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.remote.resource;

import static io.harness.resourcegroup.ResourceGroupPermissions.DELETE_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupPermissions.EDIT_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupPermissions.VIEW_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupResourceTypes.RESOURCE_GROUP;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccessDeniedErrorDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("/resourcegroup")
@Path("resourcegroup")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error"),
          @ApiResponse(code = 403, response = AccessDeniedErrorDTO.class, message = "Unauthorized")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.PL)
public class HarnessResourceGroupResource {
  ResourceGroupService resourceGroupService;
  AccessControlClient accessControlClient;

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get a resource group by identifier", nickname = "getResourceGroup")
  public ResponseDTO<ResourceGroupResponse> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, identifier), VIEW_RESOURCEGROUP_PERMISSION);
    Optional<ResourceGroupResponse> resourceGroupResponseOpt =
        resourceGroupService.get(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), identifier);
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @GET
  @ApiOperation(value = "Get list of resource groups", nickname = "getResourceGroupList")
  public ResponseDTO<PageResponse<ResourceGroupResponse>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, null), VIEW_RESOURCEGROUP_PERMISSION);
    return ResponseDTO.newResponse(getNGPageResponse(resourceGroupService.list(
        Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), pageRequest, searchTerm)));
  }

  @POST
  @Path("filter")
  @ApiOperation(value = "Get filtered resource group list", nickname = "getFilterResourceGroupList")
  public ResponseDTO<PageResponse<ResourceGroupResponse>> list(
      @NotNull ResourceGroupFilterDTO resourceGroupFilterDTO, @BeanParam PageRequest pageRequest) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(resourceGroupFilterDTO.getAccountIdentifier(), resourceGroupFilterDTO.getOrgIdentifier(),
            resourceGroupFilterDTO.getProjectIdentifier()),
        Resource.of(RESOURCE_GROUP, null), VIEW_RESOURCEGROUP_PERMISSION);
    return ResponseDTO.newResponse(getNGPageResponse(resourceGroupService.list(resourceGroupFilterDTO, pageRequest)));
  }

  @POST
  @ApiOperation(value = "Create a resource group", nickname = "createResourceGroup")
  public ResponseDTO<ResourceGroupResponse> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Valid ResourceGroupRequest resourceGroupRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, null), EDIT_RESOURCEGROUP_PERMISSION);
    ResourceGroupResponse resourceGroupResponse = resourceGroupService.create(resourceGroupRequest.getResourceGroup());
    return ResponseDTO.newResponse(resourceGroupResponse);
  }

  @POST
  @Path("/createManaged")
  @InternalApi
  @ApiOperation(
      value = "Create default/harness managed resource group", nickname = "createManagedResourceGroup", hidden = true)
  public ResponseDTO<Boolean>
  createManagedResourceGroup(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    resourceGroupService.createManagedResourceGroup(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier));
    return ResponseDTO.newResponse(true);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a resource group", nickname = "updateResourceGroup")
  public ResponseDTO<ResourceGroupResponse> update(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Valid ResourceGroupRequest resourceGroupRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, identifier), EDIT_RESOURCEGROUP_PERMISSION);
    Optional<ResourceGroupResponse> resourceGroupResponseOpt =
        resourceGroupService.update(resourceGroupRequest.getResourceGroup(), true);
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a resource group", nickname = "deleteResourceGroup")
  @Produces("application/json")
  @Consumes()
  public ResponseDTO<Boolean> delete(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, identifier), DELETE_RESOURCEGROUP_PERMISSION);
    resourceGroupService.delete(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), identifier);
    return ResponseDTO.newResponse(true);
  }
}
