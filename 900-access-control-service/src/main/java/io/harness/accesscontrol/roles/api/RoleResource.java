package io.harness.accesscontrol.roles.api;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.accesscontrol.roles.api.RoleDTOMapper.fromDTO;
import static io.harness.accesscontrol.roles.api.RoleDTOMapper.toResponseDTO;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
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

  @Inject
  public RoleResource(RoleService roleService, ScopeService scopeService) {
    this.roleService = roleService;
    this.scopeService = scopeService;
  }

  @GET
  @ApiOperation(value = "Get Roles", nickname = "getRoleList")
  public ResponseDTO<PageResponse<RoleResponseDTO>> get(@BeanParam PageRequest pageRequest,
      @BeanParam HarnessScopeParams harnessScopeParams,
      @QueryParam("includeHarnessManaged") boolean includeHarnessManaged) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    PageResponse<Role> pageResponse = roleService.list(pageRequest, scopeIdentifier, includeHarnessManaged);
    return ResponseDTO.newResponse(pageResponse.map(RoleDTOMapper::toResponseDTO));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Role", nickname = "getRole")
  public ResponseDTO<RoleResponseDTO> get(@NotEmpty @PathParam(IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams, @QueryParam("harnessManaged") boolean isHarnessManaged) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    return ResponseDTO.newResponse(toResponseDTO(
        roleService.get(identifier, scopeIdentifier, isHarnessManaged).<InvalidRequestException>orElseThrow(() -> {
          throw new InvalidRequestException("Role not found with the given scope and identifier");
        })));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update Role", nickname = "updateRole")
  public ResponseDTO<RoleResponseDTO> update(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleDTO roleDTO) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    return ResponseDTO.newResponse(toResponseDTO(roleService.update(fromDTO(scopeIdentifier, roleDTO))));
  }

  @POST
  @ApiOperation(value = "Create Role", nickname = "createRole")
  public ResponseDTO<RoleResponseDTO> create(@BeanParam HarnessScopeParams harnessScopeParams, @Body RoleDTO roleDTO) {
    Scope scope = scopeService.buildScopeFromParams(harnessScopeParams);
    if (isEmpty(roleDTO.getAllowedScopeLevels())) {
      roleDTO.setAllowedScopeLevels(Sets.newHashSet(scope.getLevel().toString()));
    }
    return ResponseDTO.newResponse(toResponseDTO(roleService.create(fromDTO(scope.toString(), roleDTO))));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete Role", nickname = "deleteRole")
  public ResponseDTO<RoleResponseDTO> delete(
      @NotNull @PathParam(IDENTIFIER_KEY) String identifier, @BeanParam HarnessScopeParams harnessScopeParams) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    return ResponseDTO.newResponse(toResponseDTO(roleService.delete(identifier, scopeIdentifier, false)));
  }
}
