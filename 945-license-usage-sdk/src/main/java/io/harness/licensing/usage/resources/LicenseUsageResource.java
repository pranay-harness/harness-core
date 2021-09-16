package io.harness.licensing.usage.resources;

import io.harness.ModuleType;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.usage.beans.LicenseUsageDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/usage")
@Path("/usage")
@Produces({"application/json"})
@Consumes({"application/json"})
public class LicenseUsageResource {
  @Inject LicenseUsageInterface licenseUsageInterface;

  @GET
  @Path("{module}")
  @NGAccessControlCheck(resourceType = "LICENSE", permission = "core_license_view")
  public ResponseDTO<LicenseUsageDTO> getLicenseUsage(
      @QueryParam("accountIdentifier") @AccountIdentifier String accountIdentifier, @PathParam("module") String module,
      @QueryParam("timestamp") long timestamp) {
    try {
      ModuleType moduleType = ModuleType.fromString(module);
      return ResponseDTO.newResponse(licenseUsageInterface.getLicenseUsage(accountIdentifier, moduleType, timestamp));
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException("Module is invalid", e);
    }
  }
}
