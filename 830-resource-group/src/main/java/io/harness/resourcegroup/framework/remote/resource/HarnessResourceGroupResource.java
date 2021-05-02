package io.harness.resourcegroup.framework.remote.resource;

import static io.harness.resourcegroup.ResourceGroupPermissions.DELETE_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupPermissions.EDIT_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupResourceTypes.RESOURCE_GROUP;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
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
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.PL)
public class HarnessResourceGroupResource {
  ResourceGroupService resourceGroupService;
  AccessControlClient accessControlClient;

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets a resource group by identifier", nickname = "getResourceGroup")
  public ResponseDTO<ResourceGroupResponse> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    Optional<ResourceGroupResponse> resourceGroupResponseOpt =
        resourceGroupService.get(identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @GET
  @ApiOperation(value = "Get Resource Group list", nickname = "getResourceGroupList")
  public ResponseDTO<PageResponse<ResourceGroupResponse>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    return ResponseDTO.newResponse(getNGPageResponse(
        resourceGroupService.list(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, searchTerm)));
  }

  @POST
  @ApiOperation(value = "Creates a resource group", nickname = "createResourceGroup")
  public ResponseDTO<ResourceGroupResponse> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Valid ResourceGroupRequest resourceGroupRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.NONE, EDIT_RESOURCEGROUP_PERMISSION);
    ResourceGroupResponse resourceGroupResponse = resourceGroupService.create(resourceGroupRequest.getResourceGroup());
    return ResponseDTO.newResponse(resourceGroupResponse);
  }

  @POST
  @Path("/createInternal")
  @InternalApi
  @ApiOperation(
      value = "create default/harneess managed resource group", nickname = "createResourceGroupInternal", hidden = true)
  public ResponseDTO<Boolean>
  createManagedResourceGroup(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Valid ResourceGroupDTO resourceGroupDTO) {
    resourceGroupService.createManagedResourceGroup(
        accountIdentifier, orgIdentifier, projectIdentifier, resourceGroupDTO);
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
        resourceGroupService.update(resourceGroupRequest.getResourceGroup());
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Deletes a resource group", nickname = "deleteResourceGroup")
  @Produces("application/json")
  @Consumes()
  public ResponseDTO<Boolean> delete(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(RESOURCE_GROUP, identifier), DELETE_RESOURCEGROUP_PERMISSION);
    boolean deleted =
        resourceGroupService.delete(identifier, accountIdentifier, orgIdentifier, projectIdentifier, false);
    return ResponseDTO.newResponse(deleted);
  }
}
