package io.harness.ng.core.acl.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Api("roleassignments")
@Path("roleassignments")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor(access = PACKAGE, onConstructor = @__({ @Inject }))
public class RoleAssignmentResource {
  MockRoleAssignmentService roleAssignmentService;

  @GET
  public ResponseDTO<Boolean> dummy() {
    return ResponseDTO.newResponse(true);
  }

  //  @POST
  //  @Path("filter")
  //  @ApiOperation(value = "Get Filtered Role Assignments", nickname = "getFilteredRoleAssignmentList")
  //  public ResponseDTO<PageResponse<RoleAssignmentResponseDTO>> get(@BeanParam PageRequest pageRequest,
  //      @NotNull @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
  //      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
  //      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
  //      @Body RoleAssignmentFilterDTO roleAssignmentFilter) {
  //    PageResponse<RoleAssignmentResponseDTO> pageResponse = roleAssignmentService.list(
  //        accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilter, pageRequest);
  //    return ResponseDTO.newResponse(pageResponse);
  //  }
  //
  //  @POST
  //  @ApiOperation(value = "Create Role Assignment", nickname = "createRoleAssignment")
  //  public ResponseDTO<RoleAssignmentResponseDTO> create(
  //      @NotNull @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
  //      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
  //      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Body RoleAssignmentDTO
  //      roleAssignmentDTO) {
  //    RoleAssignmentResponseDTO createdRoleAssignment =
  //        roleAssignmentService.create(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentDTO);
  //    return ResponseDTO.newResponse(createdRoleAssignment);
  //  }
  //
  //  /**
  //   * idempotent call, calling it multiple times won't create any side effect,
  //   * returns all role assignments which were created ignoring duplicates or failures, if any.
  //   * @return
  //   */
  //  @POST
  //  @Path("/multi")
  //  @ApiOperation(value = "Create Multiple Role Assignments", nickname = "createRoleAssignments")
  //  public ResponseDTO<List<RoleAssignmentResponseDTO>> create(
  //      @NotNull @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
  //      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
  //      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
  //      @Body RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO) {
  //    List<RoleAssignmentDTO> roleAssignmentsPayload = roleAssignmentCreateRequestDTO.getRoleAssignments();
  //    return ResponseDTO.newResponse(
  //        roleAssignmentService.createMulti(accountIdentifier, orgIdentifier, projectIdentifier,
  //        roleAssignmentsPayload));
  //  }
}
