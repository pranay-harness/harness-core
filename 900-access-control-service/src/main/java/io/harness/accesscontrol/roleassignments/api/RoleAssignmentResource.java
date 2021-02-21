package io.harness.accesscontrol.roleassignments.api;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTOMapper.fromDTO;
import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTOMapper.toDTO;

import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
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
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
  public ResponseDTO<PageResponse<RoleAssignmentResponseDTO>> get(@BeanParam PageRequest pageRequest,
      @BeanParam HarnessScopeParams harnessScopeParams, @QueryParam("principalIdentifier") String principalIdentifier,
      @QueryParam("roleIdentifier") String roleIdentifier) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    PageResponse<RoleAssignment> pageResponse =
        roleAssignmentService.getAll(pageRequest, scopeIdentifier, principalIdentifier, roleIdentifier);
    return ResponseDTO.newResponse(pageResponse.map(RoleAssignmentDTOMapper::toDTO));
  }

  @POST
  @ApiOperation(value = "Create Role Assignment", nickname = "createRoleAssignment")
  public ResponseDTO<RoleAssignmentResponseDTO> create(
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentDTO roleAssignmentDTO) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    RoleAssignment createdRoleAssignment = roleAssignmentService.create(fromDTO(scopeIdentifier, roleAssignmentDTO));
    return ResponseDTO.newResponse(toDTO(createdRoleAssignment));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete Role Assignment", nickname = "deleteRoleAssignment")
  public ResponseDTO<RoleAssignmentResponseDTO> delete(
      @BeanParam HarnessScopeParams harnessScopeParams, @NotEmpty @PathParam(IDENTIFIER_KEY) String identifier) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    RoleAssignment deletedRoleAssignment =
        roleAssignmentService.delete(identifier, scopeIdentifier).<NotFoundException>orElseThrow(() -> {
          throw new NotFoundException("Role Assignment not found with the given scope and identifier");
        });
    return ResponseDTO.newResponse(toDTO(deletedRoleAssignment));
  }
}
