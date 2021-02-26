package io.harness.accesscontrol.permissions.api;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/permissions")
@Path("/permissions")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class PermissionResource {
  private final PermissionService permissionService;
  private final ScopeService scopeService;

  @Inject
  public PermissionResource(PermissionService permissionService, ScopeService scopeService) {
    this.permissionService = permissionService;
    this.scopeService = scopeService;
  }

  @GET
  @ApiOperation(value = "Get All Permissions in a Scope", nickname = "getPermissionList")
  public ResponseDTO<List<PermissionResponseDTO>> get(@BeanParam HarnessScopeParams scopeParams,
      @QueryParam("resourceType") String resourceType, @QueryParam("scopeFilterDisabled") boolean scopeFilterDisabled) {
    Set<String> scopeFilter = new HashSet<>();
    if (!scopeFilterDisabled) {
      Scope scope = scopeService.buildScopeFromParams(scopeParams);
      scopeFilter.add(scope.getLevel().toString());
    }
    PermissionFilter query = PermissionFilter.builder()
                                 .allowedScopeLevelsFilter(scopeFilter)
                                 .identifierFilter(new HashSet<>())
                                 .statusFilter(new HashSet<>())
                                 .build();
    List<Permission> permissions = permissionService.list(query);
    return ResponseDTO.newResponse(
        permissions.stream()
            .map(permission
                -> PermissionDTOMapper.toDTO(
                    permission, permissionService.getResourceTypeFromPermission(permission).orElse(null)))
            .collect(Collectors.toList()));
  }
}
