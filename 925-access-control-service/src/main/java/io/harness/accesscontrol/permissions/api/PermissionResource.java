/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.permissions.api;

import static io.harness.accesscontrol.permissions.PermissionStatus.ACTIVE;
import static io.harness.accesscontrol.permissions.PermissionStatus.DEPRECATED;
import static io.harness.accesscontrol.permissions.PermissionStatus.EXPERIMENTAL;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.resources.resourcetypes.ResourceType;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@OwnedBy(PL)
@Api("/permissions")
@Path("/permissions")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "permissions", description = "This contains the APIs related to permissions")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
public class PermissionResource {
  private final PermissionService permissionService;

  @Inject
  public PermissionResource(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @GET
  @ApiOperation(value = "Get All Permissions in a Scope", nickname = "getPermissionList")
  @Operation(operationId = "getPermissionList",
      summary = "Get all permissions in a scope or all permissions in the system.",
      responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "List of all permissions") })
  public ResponseDTO<List<PermissionResponseDTO>>
  get(@BeanParam HarnessScopeParams scopeParams,
      @Parameter(
          description =
              "This is to enable or disable filtering by scope. The default value is false. If the value is true, all the permissions in the system are fetched.")
      @QueryParam("scopeFilterDisabled") boolean scopeFilterDisabled) {
    List<Permission> permissions = getPermissions(scopeParams, scopeFilterDisabled);
    return ResponseDTO.newResponse(
        permissions.stream()
            .map(permission
                -> PermissionDTOMapper.toDTO(
                    permission, permissionService.getResourceTypeFromPermission(permission).orElse(null)))
            .collect(Collectors.toList()));
  }

  @GET
  @Path("/resourcetypes")
  @ApiOperation(
      value = "Get All Resource Types for Permissions in a Scope", nickname = "getPermissionResourceTypesList")
  @Operation(operationId = "getPermissionResourceTypesList",
      summary = "Get all resource types for permissions in a scope or in the system.",
      responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "List of resource types") })
  public ResponseDTO<Set<String>>
  getResourceTypes(@BeanParam HarnessScopeParams scopeParams,
      @Parameter(
          description =
              "This is to enable or disable filtering by scope. The default value is false. If the value is true, all the permissions in the system are fetched.")
      @QueryParam("scopeFilterDisabled") boolean scopeFilterDisabled) {
    List<Permission> permissions = getPermissions(scopeParams, scopeFilterDisabled);
    return ResponseDTO.newResponse(permissions.stream()
                                       .map(permissionService::getResourceTypeFromPermission)
                                       .filter(Optional::isPresent)
                                       .map(Optional::get)
                                       .map(ResourceType::getIdentifier)
                                       .collect(Collectors.toSet()));
  }

  private List<Permission> getPermissions(HarnessScopeParams scopeParams, boolean scopeFilterDisabled) {
    Set<String> scopeFilter = new HashSet<>();
    if (!scopeFilterDisabled) {
      Scope scope = ScopeMapper.fromParams(scopeParams);
      scopeFilter.add(scope.getLevel().toString());
    }
    PermissionFilter query = PermissionFilter.builder()
                                 .allowedScopeLevelsFilter(scopeFilter)
                                 .identifierFilter(new HashSet<>())
                                 .statusFilter(Sets.newHashSet(EXPERIMENTAL, ACTIVE, DEPRECATED))
                                 .build();
    return permissionService.list(query);
  }
}
