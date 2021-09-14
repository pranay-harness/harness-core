/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
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
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

  @POST
  @Path("filter")
  @ApiOperation(value = "Get Filtered Role Assignments", nickname = "getFilteredRoleAssignmentList")
  public ResponseDTO<PageResponse<RoleAssignmentResponseDTO>> get(@BeanParam PageRequest pageRequest,
      @NotNull @QueryParam(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      RoleAssignmentFilterDTO roleAssignmentFilter) {
    PageResponse<RoleAssignmentResponseDTO> pageResponse = roleAssignmentService.list(
        accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentFilter, pageRequest);
    return ResponseDTO.newResponse(pageResponse);
  }

  @POST
  @ApiOperation(value = "Create Role Assignment", nickname = "createRoleAssignment")
  public ResponseDTO<RoleAssignmentResponseDTO> create(
      @NotNull @QueryParam(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, RoleAssignmentDTO roleAssignmentDTO) {
    RoleAssignmentResponseDTO createdRoleAssignment =
        roleAssignmentService.create(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentDTO);
    return ResponseDTO.newResponse(createdRoleAssignment);
  }

  @POST
  @Path("/multi/internal")
  @ApiOperation(value = "Create Multiple Role Assignments", nickname = "createRoleAssignments")
  public ResponseDTO<List<RoleAssignmentResponseDTO>> create(
      @NotNull @QueryParam(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("managed") @DefaultValue("false") Boolean managed,
      RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO) {
    List<RoleAssignmentDTO> roleAssignmentsPayload = roleAssignmentCreateRequestDTO.getRoleAssignments();
    return ResponseDTO.newResponse(roleAssignmentService.createMulti(
        accountIdentifier, orgIdentifier, projectIdentifier, roleAssignmentsPayload, managed));
  }
}
