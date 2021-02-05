package io.harness.accesscontrol.roles;

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
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
@ValidateOnExecution
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
  public ResponseDTO<PageResponse<RoleDTO>> get(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, @QueryParam("includeDefault") Boolean includeDefault) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    PageRequest pageRequest = PageRequest.builder().pageIndex(page).pageSize(size).build();
    return ResponseDTO.newResponse(roleService.getAll(pageRequest, parentIdentifier, includeDefault));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Role", nickname = "getRole")
  public ResponseDTO<RoleDTO> get(@NotEmpty @PathParam(IDENTIFIER_KEY) String identifier,
      @NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    return ResponseDTO.newResponse(roleService.get(identifier, parentIdentifier).<NotFoundException>orElseThrow(() -> {
      throw new NotFoundException("Role not found with the given scope and identifier");
    }));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update Role", nickname = "updateRole")
  public ResponseDTO<RoleDTO> update(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, @Body RoleDTO role) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    role.setParentIdentifier(parentIdentifier);
    return ResponseDTO.newResponse(roleService.update(role));
  }

  @POST
  @ApiOperation(value = "Create Role", nickname = "createRole")
  public ResponseDTO<RoleDTO> create(@NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @Body RoleDTO role) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    role.setParentIdentifier(parentIdentifier);
    return ResponseDTO.newResponse(roleService.create(role));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete Role", nickname = "deleteRole")
  public ResponseDTO<Boolean> delete(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    String parentIdentifier =
        scopeService.getScopeIdentifier(getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    return ResponseDTO.newResponse(roleService.delete(identifier, parentIdentifier) != null);
  }
}
