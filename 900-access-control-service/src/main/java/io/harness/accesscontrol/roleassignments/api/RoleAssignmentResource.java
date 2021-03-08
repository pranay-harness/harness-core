package io.harness.accesscontrol.roleassignments.api;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTOMapper.fromDTO;
import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTOMapper.toResponseDTO;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.api.ResourceGroupDTOMapper;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.api.RoleDTOMapper;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.accesscontrol.scopes.core.Scope;
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
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
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
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor(access = PACKAGE, onConstructor = @__({ @Inject }))
public class RoleAssignmentResource {
  RoleAssignmentService roleAssignmentService;
  HarnessResourceGroupService harnessResourceGroupService;
  ScopeService scopeService;
  RoleService roleService;
  ResourceGroupService resourceGroupService;

  @GET
  @ApiOperation(value = "Get Role Assignments", nickname = "getRoleAssignmentList")
  public ResponseDTO<PageResponse<RoleAssignmentResponseDTO>> get(
      @BeanParam PageRequest pageRequest, @BeanParam HarnessScopeParams harnessScopeParams) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    PageResponse<RoleAssignment> pageResponse =
        roleAssignmentService.list(pageRequest, scopeIdentifier, RoleAssignmentFilter.buildEmpty());
    return ResponseDTO.newResponse(pageResponse.map(RoleAssignmentDTOMapper::toResponseDTO));
  }

  @POST
  @Path("filter")
  @ApiOperation(value = "Get Filtered Role Assignments", nickname = "getFilteredRoleAssignmentList")
  public ResponseDTO<PageResponse<RoleAssignmentResponseDTO>> get(@BeanParam PageRequest pageRequest,
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentFilterDTO roleAssignmentFilter) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    PageResponse<RoleAssignment> pageResponse =
        roleAssignmentService.list(pageRequest, scopeIdentifier, fromDTO(roleAssignmentFilter));
    return ResponseDTO.newResponse(pageResponse.map(RoleAssignmentDTOMapper::toResponseDTO));
  }

  @POST
  @Path("aggregate")
  @ApiOperation(
      value = "Get Filtered Role Assignments for aggregation", nickname = "getFilteredRoleAssignmentForAggregation")
  public ResponseDTO<RoleAssignmentAggregateResponseDTO>
  getAggregated(@BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentFilterDTO roleAssignmentFilter) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    PageRequest pageRequest = PageRequest.builder().pageSize(1000).build();
    List<RoleAssignment> roleAssignments =
        roleAssignmentService.list(pageRequest, scopeIdentifier, fromDTO(roleAssignmentFilter)).getContent();
    List<String> roleIdentifiers =
        roleAssignments.stream().map(RoleAssignment::getRoleIdentifier).distinct().collect(toList());
    List<String> resourceGroupIdentifiers =
        roleAssignments.stream().map(RoleAssignment::getResourceGroupIdentifier).distinct().collect(toList());
    List<RoleResponseDTO> roleResponseDTOs =
        roleService.list(roleIdentifiers, scopeIdentifier).stream().map(RoleDTOMapper::toResponseDTO).collect(toList());
    List<ResourceGroupDTO> resourceGroupDTOs = resourceGroupService.list(resourceGroupIdentifiers, scopeIdentifier)
                                                   .stream()
                                                   .map(ResourceGroupDTOMapper::toDTO)
                                                   .collect(toList());
    List<RoleAssignmentDTO> roleAssignmentDTOs =
        roleAssignments.stream().map(RoleAssignmentDTOMapper::toDTO).collect(toList());
    return ResponseDTO.newResponse(RoleAssignmentAggregateResponseDTOMapper.toDTO(
        roleAssignmentDTOs, scopeIdentifier, roleResponseDTOs, resourceGroupDTOs));
  }

  @POST
  @ApiOperation(value = "Create Role Assignment", nickname = "createRoleAssignment")
  public ResponseDTO<RoleAssignmentResponseDTO> create(
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentDTO roleAssignmentDTO) {
    Scope scope = scopeService.buildScopeFromParams(harnessScopeParams);
    harnessResourceGroupService.sync(roleAssignmentDTO.getResourceGroupIdentifier(), scope);
    RoleAssignment createdRoleAssignment = roleAssignmentService.create(fromDTO(scope.toString(), roleAssignmentDTO));
    return ResponseDTO.newResponse(toResponseDTO(createdRoleAssignment));
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
    return ResponseDTO.newResponse(toResponseDTO(deletedRoleAssignment));
  }
}
