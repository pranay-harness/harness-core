package io.harness.accesscontrol.roles.api;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.roles.api.RoleDTOMapper.fromDTO;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("/roles")
@Path("/roles")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class RoleResource {
  private final RoleService roleService;
  private final ScopeService scopeService;
  private final RoleDTOMapper roleDTOMapper;

  @Inject
  public RoleResource(RoleService roleService, ScopeService scopeService, RoleDTOMapper roleDTOMapper) {
    this.roleService = roleService;
    this.scopeService = scopeService;
    this.roleDTOMapper = roleDTOMapper;
  }

  @GET
  @ApiOperation(value = "Get Roles", nickname = "getRoleList")
  public ResponseDTO<PageResponse<RoleResponseDTO>> get(
      @BeanParam PageRequest pageRequest, @BeanParam HarnessScopeParams harnessScopeParams) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    RoleFilter roleFilter = RoleFilter.builder().scopeIdentifier(scopeIdentifier).managedFilter(NO_FILTER).build();
    PageResponse<Role> pageResponse = roleService.list(pageRequest, roleFilter);
    return ResponseDTO.newResponse(pageResponse.map(roleDTOMapper::toResponseDTO));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Role", nickname = "getRole")
  public ResponseDTO<RoleResponseDTO> get(@NotEmpty @PathParam(IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams, @QueryParam("harnessManaged") boolean isHarnessManaged) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    return ResponseDTO.newResponse(roleDTOMapper.toResponseDTO(
        roleService.get(identifier, scopeIdentifier, NO_FILTER).<InvalidRequestException>orElseThrow(() -> {
          throw new InvalidRequestException("Role not found with the given scope and identifier");
        })));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update Role", nickname = "updateRole")
  public ResponseDTO<RoleResponseDTO> update(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleDTO roleDTO) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    if (!identifier.equals(roleDTO.getIdentifier())) {
      throw new InvalidRequestException("Role identifier in the request body and the url do not match");
    }
    return ResponseDTO.newResponse(roleDTOMapper.toResponseDTO(roleService.update(fromDTO(scopeIdentifier, roleDTO))));
  }

  @POST
  @ApiOperation(value = "Create Role", nickname = "createRole")
  public ResponseDTO<RoleResponseDTO> create(@BeanParam HarnessScopeParams harnessScopeParams, @Body RoleDTO roleDTO) {
    Scope scope = scopeService.buildScopeFromParams(harnessScopeParams);
    if (isEmpty(roleDTO.getAllowedScopeLevels())) {
      roleDTO.setAllowedScopeLevels(Sets.newHashSet(scope.getLevel().toString()));
    }
    return ResponseDTO.newResponse(roleDTOMapper.toResponseDTO(roleService.create(fromDTO(scope.toString(), roleDTO))));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete Role", nickname = "deleteRole")
  public ResponseDTO<RoleResponseDTO> delete(
      @NotNull @PathParam(IDENTIFIER_KEY) String identifier, @BeanParam HarnessScopeParams harnessScopeParams) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    return ResponseDTO.newResponse(roleDTOMapper.toResponseDTO(roleService.delete(identifier, scopeIdentifier)));
  }
}
