package io.harness.accesscontrol.roleassignments;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.accesscontrol.scopes.HarnessScopeUtils.getIdentifierMap;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.scopes.ScopeService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Api("roleassignments")
@Path("roleassignments")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class RoleAssignmentResource {
  private final RoleAssignmentService roleAssignmentService;
  private final ScopeService scopeService;

  @Inject
  public RoleAssignmentResource(RoleAssignmentService roleAssignmentService, ScopeService scopeService) {
    this.roleAssignmentService = roleAssignmentService;
    this.scopeService = scopeService;
  }

  @GET
  @ApiOperation(value = "Get Role Assignments", nickname = "getRoleAssignmentList")
  public ResponseDTO<PageResponse<RoleAssignmentDTO>> get(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, @QueryParam("principalIdentifier") String principalIdentifier,
      @QueryParam("roleIdentifier") String roleIdentifier,
      @QueryParam("includeInheritedAssignments") boolean includeInheritedAssignments) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    PageRequest pageRequest = PageRequest.builder().pageIndex(page).pageSize(size).build();
    return ResponseDTO.newResponse(roleAssignmentService.getAll(
        pageRequest, parentIdentifier, principalIdentifier, roleIdentifier, includeInheritedAssignments));
  }

  @POST
  @ApiOperation(value = "Create Role Assignment", nickname = "createRoleAssignment")
  public ResponseDTO<RoleAssignmentDTO> create(@NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @Body RoleAssignmentDTO roleAssignmentDTO) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    roleAssignmentDTO.setParentIdentifier(parentIdentifier);
    return ResponseDTO.newResponse(roleAssignmentService.create(roleAssignmentDTO));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete Role Assignment", nickname = "deleteRoleAssignment")
  public ResponseDTO<Boolean> delete(@NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @NotEmpty @PathParam(IDENTIFIER_KEY) String identifier) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    return ResponseDTO.newResponse(roleAssignmentService.delete(identifier, parentIdentifier) != null);
  }
}
