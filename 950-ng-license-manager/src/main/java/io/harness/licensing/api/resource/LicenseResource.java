package io.harness.licensing.api.resource;

import io.harness.NGCommonEntityConstants;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicensesDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("/licenses")
@Path("/licenses")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
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
  public ResponseDTO<ModuleLicenseDTO>
  getModuleLicense(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(MODULE_TYPE_KEY) ModuleType moduleType) {
    ModuleLicenseDTO moduleLicenses = licenseService.getModuleLicense(accountIdentifier, moduleType);
    return ResponseDTO.newResponse(moduleLicenses);
  }

  @GET
  @Path("account")
  @ApiOperation(value = "Gets All Module License Information in Account", nickname = "getAccountLicenses")
  public ResponseDTO<AccountLicensesDTO> getAccountLicensesDTO(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    AccountLicensesDTO accountLicenses = licenseService.getAccountLicense(accountIdentifier);
    return ResponseDTO.newResponse(accountLicenses);
  }

  @GET
  @Path("{identifier}}")
  @ApiOperation(value = "Gets Module License", nickname = "getModuleLicense")
  public ResponseDTO<ModuleLicenseDTO> get(@PathParam("identifier") String identifier) {
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicenseById(identifier);
    return ResponseDTO.newResponse(moduleLicense);
  }

  @POST
  @ApiOperation(value = "Creates Module License", nickname = "createModuleLicense")
  @InternalApi
  public ResponseDTO<ModuleLicenseDTO> create(@NotNull @Valid ModuleLicenseDTO moduleLicenseDTO) {
    ModuleLicenseDTO created = licenseService.createModuleLicense(moduleLicenseDTO);
    return ResponseDTO.newResponse(created);
  }

  @PUT
  @ApiOperation(value = "Updates Module License", nickname = "updateModuleLicense")
  @InternalApi
  public ResponseDTO<ModuleLicenseDTO> update(@NotNull @Valid ModuleLicenseDTO moduleLicenseDTO) {
    ModuleLicenseDTO updated = licenseService.updateModuleLicense(moduleLicenseDTO);
    return ResponseDTO.newResponse(updated);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Deletes Module License", nickname = "deleteModuleLicense")
  @InternalApi
  public ResponseDTO<ModuleLicenseDTO> delete(@NotNull @PathParam("identifier") String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    ModuleLicenseDTO deleted = licenseService.deleteModuleLicense(identifier, accountIdentifier);
    return ResponseDTO.newResponse(deleted);
  }

  @POST
  @Path("trial")
  @ApiOperation(value = "Starts Trail License For A Module", nickname = "startTrialLicense")
  public ResponseDTO<ModuleLicenseDTO> startTrialLicense(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Valid @Body StartTrialRequestDTO startTrialRequestDTO) {
    return ResponseDTO.newResponse(licenseService.startTrialLicense(accountIdentifier, startTrialRequestDTO));
  }
}
