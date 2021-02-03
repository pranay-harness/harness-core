package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.scopes.Scope;
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

@Api("/permissions")
@Path("/permissions")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@ValidateOnExecution
public class PermissionResource {
  private final PermissionService permissionService;

  @Inject
  public PermissionResource(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @GET
  @ApiOperation(value = "Get All Permissions", nickname = "getPermissionList")
  public ResponseDTO<List<PermissionDTO>> get(
      @QueryParam("scope") Scope scope, @QueryParam("resourceType") String resourceType) {
    return ResponseDTO.newResponse(permissionService.list(scope, resourceType));
  }
}
