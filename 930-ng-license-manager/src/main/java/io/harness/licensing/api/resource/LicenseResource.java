package io.harness.licensing.api.resource;

import static io.harness.licensing.accesscontrol.LicenseAccessControlPermissions.VIEW_LICENSE_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.licensing.ModuleType;
import io.harness.licensing.accesscontrol.ResourceTypes;
import io.harness.licensing.beans.modules.AccountLicensesDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("/licenses")
@Path("/licenses")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class LicenseResource {
  private static final String MODULE_TYPE_KEY = "moduleType";
  private final LicenseService licenseService;

  @Inject
  public LicenseResource(LicenseService licenseService) {
    this.licenseService = licenseService;
  }

  @GET
  @ApiOperation(
      value = "Gets Module License By Account And ModuleType", nickname = "getModuleLicenseByAccountAndModuleType")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO>
  getModuleLicense(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(MODULE_TYPE_KEY) ModuleType moduleType) {
    ModuleLicenseDTO moduleLicenses = licenseService.getModuleLicense(accountIdentifier, moduleType);
    return ResponseDTO.newResponse(moduleLicenses);
  }

  @GET
  @Path("account")
  @ApiOperation(value = "Gets All Module License Information in Account", nickname = "getAccountLicenses")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<AccountLicensesDTO> getAccountLicensesDTO(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    AccountLicensesDTO accountLicenses = licenseService.getAccountLicense(accountIdentifier);
    return ResponseDTO.newResponse(accountLicenses);
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets Module License", nickname = "getModuleLicense")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO> get(@PathParam("identifier") String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicenseById(identifier);
    return ResponseDTO.newResponse(moduleLicense);
  }

  @POST
  @Path("trial")
  @ApiOperation(value = "Starts Trail License For A Module", nickname = "startTrialLicense")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO> startTrialLicense(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid @Body StartTrialRequestDTO startTrialRequestDTO) {
    return ResponseDTO.newResponse(licenseService.startTrialLicense(accountIdentifier, startTrialRequestDTO));
  }
}
