package io.harness.licensing.api.resource;

import static io.harness.licensing.accesscontrol.LicenseAccessControlPermissions.VIEW_LICENSE_PERMISSION;

import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.licensing.ModuleType;
import io.harness.licensing.accesscontrol.ResourceTypes;
import io.harness.licensing.beans.activity.CCMActivityDTO;
import io.harness.licensing.beans.activity.CDActivityDTO;
import io.harness.licensing.beans.activity.CIActivityDTO;
import io.harness.licensing.beans.activity.CVActivityDTO;
import io.harness.licensing.beans.activity.FFActivityDTO;
import io.harness.licensing.beans.activity.LicenseActivityDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/licenses/usage")
@Path("/licenses/usage")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class LicenseUsageResource {
  @GET
  @Path("{accountId}")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<LicenseActivityDTO> getCurrentModuleLicenseUsage(
      @PathParam("accountId") String accountId, @NotNull @QueryParam("module") ModuleType module) {
    switch (module) {
      case CD:
        return ResponseDTO.newResponse(CDActivityDTO.builder().build());
      case CE:
        return ResponseDTO.newResponse(CCMActivityDTO.builder().build());
      case CI:
        return ResponseDTO.newResponse(CIActivityDTO.builder().build());
      case CF:
        return ResponseDTO.newResponse(FFActivityDTO.builder().build());
      case CV:
        return ResponseDTO.newResponse(CVActivityDTO.builder().build());
      default:
        throw new IllegalStateException("Unexpected value: " + module);
    }
  }
}
