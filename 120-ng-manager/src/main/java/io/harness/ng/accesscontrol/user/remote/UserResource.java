package io.harness.ng.accesscontrol.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.user.UserAggregateDTO;
import io.harness.ng.accesscontrol.user.UserService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
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

@Api("user")
@Path("user")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@OwnedBy(PL)
public class UserResource {
  UserService userService;

  @POST
  @Path("aggregate")
  @ApiOperation(value = "Get a page of active users for access control", nickname = "getActiveUsersAggregated")
  public ResponseDTO<PageResponse<UserAggregateDTO>> getActiveUsers(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("searchTerm") String searchTerm, @BeanParam PageRequest pageRequest,
      ACLAggregateFilter aclAggregateFilter) {
    PageResponse<UserAggregateDTO> aclUserAggregateDTOs = userService.getUsers(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest, aclAggregateFilter);
    return ResponseDTO.newResponse(aclUserAggregateDTOs);
  }

  @DELETE
  @Path("{userId}")
  @Produces("application/json")
  @Consumes()
  @ApiOperation(value = "Remove user as the collaborator from the scope", nickname = "deleteActiveUser")
  public ResponseDTO<Boolean> deleteActiveUser(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @PathParam("userId") String userId) {
    return ResponseDTO.newResponse(userService.deleteUser(accountIdentifier, orgIdentifier, projectIdentifier, userId));
  }
}
