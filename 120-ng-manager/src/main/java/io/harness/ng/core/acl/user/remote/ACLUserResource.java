package io.harness.ng.core.acl.user.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.acl.user.ACLUserAggregateDTO;
import io.harness.ng.core.acl.user.ACLUserService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("acl/user")
@Path("acl/user")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class ACLUserResource {
  ACLUserService aclUserService;

  @POST
  @Path("aggregate/active")
  @ApiOperation(value = "Get a page of active users for access control", nickname = "getActiveUsersAggregated")
  public ResponseDTO<PageResponse<ACLUserAggregateDTO>> getActiveUsers(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @BeanParam PageRequest pageRequest,
      ACLAggregateFilter aclAggregateFilter) {
    PageResponse<ACLUserAggregateDTO> aclUserAggregateDTOs = aclUserService.getActiveUsers(
        accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, aclAggregateFilter);
    return ResponseDTO.newResponse(aclUserAggregateDTOs);
  }

  @POST
  @Path("aggregate/pending")
  @ApiOperation(value = "Get a page of pending users for access control", nickname = "getPendingUsersAggregated")
  public ResponseDTO<PageResponse<ACLUserAggregateDTO>> getPendingUsers(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @BeanParam PageRequest pageRequest,
      ACLAggregateFilter aclAggregateFilter) {
    PageResponse<ACLUserAggregateDTO> aclUserAggregateDTOs = aclUserService.getPendingUsers(
        accountIdentifier, orgIdentifier, projectIdentifier, pageRequest, aclAggregateFilter);
    return ResponseDTO.newResponse(aclUserAggregateDTOs);
  }

  @DELETE
  @Path("/{userId}")
  @ApiOperation(value = "Remove user as the collaborator for the scope", nickname = "deleteUser")
  public ResponseDTO<Boolean> delete(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @PathParam("userId") String userId) {
    return ResponseDTO.newResponse(aclUserService.delete(accountIdentifier, orgIdentifier, projectIdentifier, userId));
  }
}
