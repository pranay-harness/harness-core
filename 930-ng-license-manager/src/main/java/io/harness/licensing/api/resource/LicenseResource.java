package io.harness.licensing.api.resource;

import static io.harness.licensing.accesscontrol.LicenseAccessControlPermissions.VIEW_LICENSE_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.licensing.ModuleType;
import io.harness.licensing.accesscontrol.ResourceTypes;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialDTO;
import io.harness.licensing.beans.response.CheckExpiryResultDTO;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.helpers.ModuleLicenseSummaryHelper;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
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
  @Deprecated
  @ApiOperation(
      value = "Gets Module License By Account And ModuleType", nickname = "getModuleLicenseByAccountAndModuleType")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO>
  getModuleLicense(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(MODULE_TYPE_KEY) ModuleType moduleType) {
    return ResponseDTO.newResponse(licenseService.getModuleLicense(accountIdentifier, moduleType));
  }

  @GET
  @Path("/modules/{accountIdentifier}")
  @ApiOperation(
      value = "Gets Module Licenses By Account And ModuleType", nickname = "getModuleLicensesByAccountAndModuleType")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<List<ModuleLicenseDTO>>
  getModuleLicenses(
      @NotNull @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(MODULE_TYPE_KEY) ModuleType moduleType) {
    return ResponseDTO.newResponse(licenseService.getModuleLicenses(accountIdentifier, moduleType));
  }

  @GET
  @Path("{accountIdentifier}/summary")
  @ApiOperation(
      value = "Gets Module Licenses With Summary By Account And ModuleType", nickname = "getLicensesAndSummary")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<LicensesWithSummaryDTO>
  getLicensesWithSummary(
      @NotNull @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(MODULE_TYPE_KEY) ModuleType moduleType) {
    List<ModuleLicenseDTO> moduleLicenses = licenseService.getModuleLicenses(accountIdentifier, moduleType);
    if (moduleLicenses.isEmpty()) {
      return ResponseDTO.newResponse(null);
    }

    LicensesWithSummaryDTO response = ModuleLicenseSummaryHelper.generateSummary(moduleType, moduleLicenses);
    return ResponseDTO.newResponse(response);
  }

  @GET
  @Path("account")
  @ApiOperation(value = "Gets All Module License Information in Account", nickname = "getAccountLicenses")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<AccountLicenseDTO> getAccountLicensesDTO(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    AccountLicenseDTO accountLicenses = licenseService.getAccountLicense(accountIdentifier);
    return ResponseDTO.newResponse(accountLicenses);
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets Module License", nickname = "getModuleLicenseById")
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
      @NotNull @Valid @Body StartTrialDTO startTrialRequestDTO) {
    return ResponseDTO.newResponse(licenseService.startTrialLicense(accountIdentifier, startTrialRequestDTO));
  }

  @POST
  @Path("extend-trial")
  @ApiOperation(value = "Extends Trail License For A Module", nickname = "extendTrialLicense")
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO> extendTrialLicense(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid @Body StartTrialDTO startTrialRequestDTO) {
    return ResponseDTO.newResponse(licenseService.extendTrialLicense(accountIdentifier, startTrialRequestDTO));
  }

  @GET
  @Path("{accountId}/check-expiry")
  @ApiOperation(
      value = "Deprecated Check All Inactive", nickname = "checkNGLicensesAllInactiveDeprecated", hidden = true)
  @InternalApi
  public ResponseDTO<CheckExpiryResultDTO>
  checkExpiry(@PathParam("accountId") String accountId) {
    return ResponseDTO.newResponse(licenseService.checkExpiry(accountId));
  }

  @GET
  @Path("{accountId}/soft-delete")
  @ApiOperation(value = "Deprecated Soft Delete", nickname = "softDeleteDeprecated", hidden = true)
  @InternalApi
  public ResponseDTO<Boolean> softDelete(@PathParam("accountId") String accountId) {
    licenseService.softDelete(accountId);
    return ResponseDTO.newResponse(Boolean.TRUE);
  }
}
