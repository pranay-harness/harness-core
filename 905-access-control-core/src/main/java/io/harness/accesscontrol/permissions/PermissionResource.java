package io.harness.accesscontrol.permissions;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

import io.harness.accesscontrol.scopes.HarnessScopeUtils;
import io.harness.accesscontrol.scopes.Scope;
import io.harness.accesscontrol.scopes.ScopeService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/permissions")
@Path("/permissions")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@ValidateOnExecution
public class PermissionResource {
  private final PermissionService permissionService;
  private final ScopeService scopeService;

  @Inject
  public PermissionResource(PermissionService permissionService, ScopeService scopeService) {
    this.permissionService = permissionService;
    this.scopeService = scopeService;
  }

  @GET
  @ApiOperation(value = "Get All Permissions", nickname = "getPermissionList")
  public ResponseDTO<List<PermissionDTO>> get(@NotEmpty @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @QueryParam("resourceType") String resourceType) {
    Scope scope =
        scopeService.getScope(HarnessScopeUtils.getIdentifierMap(accountIdentifier, orgIdentifier, projectIdentifier));
    return ResponseDTO.newResponse(permissionService.list(scope, resourceType));
  }
}
