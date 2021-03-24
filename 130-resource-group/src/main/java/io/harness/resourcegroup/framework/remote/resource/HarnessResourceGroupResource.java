package io.harness.resourcegroup.framework.remote.resource;

import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

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
import java.util.Optional;

import static io.harness.utils.PageUtils.getNGPageResponse;

@Api("/resourcegroup")
@Path("resourcegroup")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class HarnessResourceGroupResource {
  ResourceGroupService resourceGroupService;

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets a resource group by identifier", nickname = "getResourceGroup")
  public ResponseDTO<ResourceGroupResponse> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    Optional<ResourceGroupResponse> resourceGroupResponseOpt =
        resourceGroupService.find(identifier, accountIdentifier, orgIdentifier, projectIdentifier);
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
    ResourceGroupResponse resourceGroupResponse = resourceGroupService.create(resourceGroupRequest.getResourceGroup());
    return ResponseDTO.newResponse(resourceGroupResponse);
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
    Optional<ResourceGroupResponse> resourceGroupResponseOpt =
        resourceGroupService.update(resourceGroupRequest.getResourceGroup());
    return ResponseDTO.newResponse(resourceGroupResponseOpt.orElse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Deletes a resource group", nickname = "deleteResourceGroup")
  public ResponseDTO<Boolean> delete(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    boolean deleted = resourceGroupService.delete(identifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(deleted);
  }
}
