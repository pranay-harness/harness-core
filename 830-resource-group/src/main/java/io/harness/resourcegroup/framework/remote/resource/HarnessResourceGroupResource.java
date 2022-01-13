/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.resourcegroup.framework.remote.resource;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_CODE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.resourcegroup.ResourceGroupPermissions.DELETE_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupPermissions.EDIT_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupPermissions.VIEW_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupResourceTypes.RESOURCE_GROUP;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccessDeniedErrorDTO;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelectorByScope;
import io.harness.resourcegroup.remote.dto.ManagedFilter;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Harness Resource Group", description = "This contains APIs specific to the Harness Resource Group")
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = BAD_REQUEST_CODE, description = BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
    })
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
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
  @ApiOperation(value = "Get a resource group by Identifier", nickname = "getResourceGroup")
  @Operation(operationId = "getResourceGroup", summary = "Get a resource group by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This returns a Resource Group specific to the Identifier")
      })
  public ResponseDTO<ResourceGroupResponse>
  get(@Parameter(description = "This is the Identifier of the Entity", required = true) @NotNull @PathParam(
          NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, identifier), VIEW_RESOURCEGROUP_PERMISSION);
    Optional<ResourceGroupResponse> resourceGroupResponseOpt = resourceGroupService.get(
        Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), identifier, ManagedFilter.NO_FILTER);
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @GET
  @Path("internal/{identifier}")
  @ApiOperation(
      value = "Get a resource group by identifier internal", nickname = "getResourceGroupInternal", hidden = true)
  @Operation(operationId = "getResourceGroupInternal", summary = "Get a resource group by Identifier internal",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Internal API to return Resource Group by Identifier")
      },
      hidden = true)
  @InternalApi
  public ResponseDTO<ResourceGroupResponse>
  getInternal(@Parameter(description = "This is the Identifier of the Entity") @NotNull @PathParam(
                  NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    Optional<ResourceGroupResponse> resourceGroupResponseOpt =
        resourceGroupService.get(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), identifier,
            isEmpty(accountIdentifier) ? ManagedFilter.ONLY_MANAGED : ManagedFilter.NO_FILTER);
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @GET
  @ApiOperation(value = "Get list of resource groups", nickname = "getResourceGroupList")
  @Operation(operationId = "getResourceGroupList", summary = "Get list of resource groups",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This contains a list of Resource Groups")
      })
  public ResponseDTO<PageResponse<ResourceGroupResponse>>
  list(@Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
           NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(
          description =
              "Details of all the resource groups having this string in their name or identifier will be returned.")
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @BeanParam PageRequest pageRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, null), VIEW_RESOURCEGROUP_PERMISSION);
    return ResponseDTO.newResponse(getNGPageResponse(resourceGroupService.list(
        Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), pageRequest, searchTerm)));
  }

  @POST
  @Path("filter")
  @ApiOperation(value = "Get filtered resource group list", nickname = "getFilterResourceGroupList")
  @Operation(operationId = "getFilterResourceGroupList",
      description = "This fetches a filtered list of Resource Groups",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "This fetches the list of Resource Groups filtered by multiple fields.")
      })
  public ResponseDTO<PageResponse<ResourceGroupResponse>>
  list(@RequestBody(description = "Filter Resource Groups based on multiple parameters", required = true)
       @NotNull ResourceGroupFilterDTO resourceGroupFilterDTO, @BeanParam PageRequest pageRequest) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(resourceGroupFilterDTO.getAccountIdentifier(), resourceGroupFilterDTO.getOrgIdentifier(),
            resourceGroupFilterDTO.getProjectIdentifier()),
        Resource.of(RESOURCE_GROUP, null), VIEW_RESOURCEGROUP_PERMISSION);
    return ResponseDTO.newResponse(getNGPageResponse(resourceGroupService.list(resourceGroupFilterDTO, pageRequest)));
  }

  @POST
  @ApiOperation(value = "Create a resource group", nickname = "createResourceGroup")
  @Operation(operationId = "createResourceGroup", summary = "Create a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully created a Resource Group")
      })
  @FeatureRestrictionCheck(FeatureRestrictionName.CUSTOM_RESOURCE_GROUPS)
  public ResponseDTO<ResourceGroupResponse>
  create(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @RequestBody(description = "This contains the details required to create a Resource Group",
          required = true) @Valid ResourceGroupRequest resourceGroupRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, null), EDIT_RESOURCEGROUP_PERMISSION);
    resourceGroupRequest.getResourceGroup().setAllowedScopeLevels(
        Sets.newHashSet(ScopeLevel.of(accountIdentifier, orgIdentifier, projectIdentifier).toString().toLowerCase()));
    verifySupportedResourceSelectorsAreUsed(resourceGroupRequest);
    ResourceGroupResponse resourceGroupResponse =
        resourceGroupService.create(resourceGroupRequest.getResourceGroup(), false);
    return ResponseDTO.newResponse(resourceGroupResponse);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a resource group", nickname = "updateResourceGroup")
  @Operation(operationId = "updateResourceGroup", summary = "Update a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully updated a Resource Group")
      })
  public ResponseDTO<ResourceGroupResponse>
  update(@Parameter(description = "Identifier for the entity") @NotNull @PathParam(
             NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @RequestBody(description = "This contains the details required to create a Resource Group",
          required = true) @Valid ResourceGroupRequest resourceGroupRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, identifier), EDIT_RESOURCEGROUP_PERMISSION);
    resourceGroupRequest.getResourceGroup().setAllowedScopeLevels(
        Sets.newHashSet(ScopeLevel.of(accountIdentifier, orgIdentifier, projectIdentifier).toString().toLowerCase()));
    verifySupportedResourceSelectorsAreUsed(resourceGroupRequest);
    Optional<ResourceGroupResponse> resourceGroupResponseOpt =
        resourceGroupService.update(resourceGroupRequest.getResourceGroup(), true, false);
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a resource group", nickname = "deleteResourceGroup")
  @Operation(operationId = "deleteResourceGroup", summary = "Delete a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully deleted a Resource Group")
      })
  @Produces("application/json")
  @Consumes()
  public ResponseDTO<Boolean>
  delete(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, identifier), DELETE_RESOURCEGROUP_PERMISSION);
    return ResponseDTO.newResponse(
        resourceGroupService.delete(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), identifier));
  }

  private void verifySupportedResourceSelectorsAreUsed(ResourceGroupRequest resourceGroupRequest) {
    if (resourceGroupRequest == null || resourceGroupRequest.getResourceGroup() == null) {
      return;
    }
    ResourceGroupDTO resourceGroupDTO = resourceGroupRequest.getResourceGroup();
    if (resourceGroupDTO.isFullScopeSelected()) {
      throw new InvalidRequestException("Full scope selected cannot be provided for custom resource groups.");
    }
    if (isNotEmpty(resourceGroupDTO.getResourceSelectors())) {
      resourceGroupDTO.getResourceSelectors().forEach(resourceSelector -> {
        if (resourceSelector instanceof ResourceSelectorByScope) {
          throw new InvalidRequestException("Resource Selector by scope not supported yet.");
        }
        if ((resourceSelector instanceof DynamicResourceSelector)
            && Boolean.TRUE.equals(((DynamicResourceSelector) resourceSelector).getIncludeChildScopes())) {
          throw new InvalidRequestException("Including child scopes not supported yet.");
        }
      });
    }
  }
}
