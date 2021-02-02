package io.harness.accesscontrol.permissions;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.permissions.harness.HPermissionService;
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
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@ValidateOnExecution
public class PermissionResource {
  private final HPermissionService hPermissionService;

  @Inject
  public PermissionResource(HPermissionService hPermissionService) {
    this.hPermissionService = hPermissionService;
  }

  @GET
  @Path("available")
  @ApiOperation(value = "Get Available Permissions", nickname = "getAvailablePermissionList")
  public ResponseDTO<List<PermissionDTO>> get(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("resourceType") String resourceType) {
    return ResponseDTO.newResponse(
        hPermissionService.list(accountIdentifier, orgIdentifier, projectIdentifier, resourceType));
  }
}
