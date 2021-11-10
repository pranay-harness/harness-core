package io.harness.licensing.api.resource;

import com.google.inject.Inject;
import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.exception.IllegalArgumentException;
import io.harness.exception.WingsException;
import io.harness.licensing.Edition;
import io.harness.licensing.accesscontrol.ResourceTypes;
import io.harness.licensing.beans.EditionActionDTO;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialDTO;
import io.harness.licensing.beans.modules.UpgradeLicenseDTO;
import io.harness.licensing.beans.response.CheckExpiryResultDTO;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import retrofit2.http.Body;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.harness.licensing.accesscontrol.LicenseAccessControlPermissions.EDIT_LICENSE_PERMISSION;
import static io.harness.licensing.accesscontrol.LicenseAccessControlPermissions.VIEW_LICENSE_PERMISSION;

@Api("licenses")
@Path("licenses")
@Produces({"application/json"})
@Consumes({"application/json"})
@Tag(name = "Licenses", description = "This contains APIs related to licenses as defined in Harness")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
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
  @Operation(operationId = "getModuleLicenseByAccountAndModuleType",
      summary = "Gets Module License By Account And ModuleType",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a module's license")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  @Deprecated
  public ResponseDTO<ModuleLicenseDTO>
  getModuleLicense(@Parameter(description = "Account id to get a module license.") @NotNull @QueryParam(
                       NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = "A Harness Platform module.") @NotNull @QueryParam(
          MODULE_TYPE_KEY) ModuleType moduleType) {
    validateModuleType(moduleType);
    return ResponseDTO.newResponse(licenseService.getModuleLicense(accountIdentifier, moduleType));
  }

  @GET
  @Path("/modules/{accountIdentifier}")
  @ApiOperation(
      value = "Gets Module Licenses By Account And ModuleType", nickname = "getModuleLicensesByAccountAndModuleType")
  @Operation(operationId = "getModuleLicensesByAccountAndModuleType",
      summary = "Gets Module Licenses By Account And ModuleType",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all of a module's licenses")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<List<ModuleLicenseDTO>>
  getModuleLicenses(@Parameter(description = "Account id to get a module license.") @NotNull @PathParam(
                        NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = "A Harness Platform module.") @NotNull @QueryParam(
          MODULE_TYPE_KEY) ModuleType moduleType) {
    validateModuleType(moduleType);
    return ResponseDTO.newResponse(licenseService.getModuleLicenses(accountIdentifier, moduleType));
  }

  @GET
  @Path("{accountIdentifier}/summary")
  @ApiOperation(
      value = "Gets Module Licenses With Summary By Account And ModuleType", nickname = "getLicensesAndSummary")
  @Operation(operationId = "getLicensesAndSummary",
      summary = "Gets Module Licenses With Summary By Account And ModuleType",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a module's license summary")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<LicensesWithSummaryDTO>
  getLicensesWithSummary(@Parameter(description = "Account id to get a module license with summary.") @NotNull
                         @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = "A Harness Platform module.") @NotNull @QueryParam(
          MODULE_TYPE_KEY) ModuleType moduleType) {
    validateModuleType(moduleType);
    return ResponseDTO.newResponse(licenseService.getLicenseSummary(accountIdentifier, moduleType));
  }

  @GET
  @Path("account")
  @ApiOperation(value = "Gets All Module License Information in Account", nickname = "getAccountLicenses")
  @Operation(operationId = "getAccountLicenses", summary = "Gets All Module License Information in Account",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all licenses for an account")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<AccountLicenseDTO>
  getAccountLicensesDTO(@Parameter(description = "Accouunt id to get all module licenses.") @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    AccountLicenseDTO accountLicenses = licenseService.getAccountLicense(accountIdentifier);
    return ResponseDTO.newResponse(accountLicenses);
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets Module License", nickname = "getModuleLicenseById")
  @Operation(operationId = "getModuleLicenseById", summary = "Gets Module License",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a module's license")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO>
  get(@Parameter(description = "The module license identifier") @PathParam("identifier") String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @Parameter(
          description = "Account id to get a module license from an account.")
      @AccountIdentifier String accountIdentifier) {
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicenseById(identifier);
    return ResponseDTO.newResponse(moduleLicense);
  }

  @POST
  @Path("free")
  @ApiOperation(value = "Starts Free License For A Module", nickname = "startFreeLicense")
  @Operation(operationId = "startFreeLicense", summary = "Starts Free License For A Module",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
                ApiResponse(responseCode = "default", description = "Returns a free module license")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO>
  startFreeLicense(@Parameter(description = "Account id to start a free license") @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
                   @Parameter(description = "A Harness Platform module.") @NotNull @QueryParam(
                           NGCommonEntityConstants.MODULE_TYPE) ModuleType moduleType) {
    return ResponseDTO.newResponse(licenseService.startFreeLicense(accountIdentifier, moduleType));
  }

  @PUT
  @Path("upgrade")
  @ApiOperation(value = "Upgrades existing license For A Module", nickname = "upgradeLicense")
  @Operation(operationId = "upgradeLicense", summary = "Starts Free License For A Module",
          responses =
                  {
                          @io.swagger.v3.oas.annotations.responses.
                                  ApiResponse(responseCode = "default", description = "Returns upgraded module license")
                  })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = EDIT_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO>
  upgradeLicense(@Parameter(description = "Account id to upgrade a license") @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
                 @NotNull @Valid @Body UpgradeLicenseDTO upgradeLicense
  ) {
    return ResponseDTO.newResponse(licenseService.upgradeLicense(accountIdentifier, upgradeLicense));
  }


  @POST
  @Path("community")
  @ApiOperation(value = "Starts Community License For A Module", nickname = "startCommunityLicense")
  @InternalApi
  public ResponseDTO<ModuleLicenseDTO> startCommunityLicense(
          @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
          @NotNull @QueryParam(NGCommonEntityConstants.MODULE_TYPE) ModuleType moduleType) {
    return ResponseDTO.newResponse(licenseService.startCommunityLicense(accountIdentifier, moduleType));
  }

  @POST
  @Path("trial")
  @ApiOperation(value = "Starts Trial License For A Module", nickname = "startTrialLicense")
  @Operation(operationId = "startTrialLicense", summary = "Starts Trial License For A Module",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a trial module license")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO>
  startTrialLicense(@Parameter(description = "Account id to start a trial license") @NotNull @QueryParam(
                        NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid @Body StartTrialDTO startTrialRequestDTO) {
    return ResponseDTO.newResponse(licenseService.startTrialLicense(accountIdentifier, startTrialRequestDTO));
  }

  @POST
  @Path("extend-trial")
  @ApiOperation(value = "Extends Trail License For A Module", nickname = "extendTrialLicense")
  @Operation(operationId = "extendTrialLicense", summary = "Extends Trial License For A Module",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns a trial module license")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<ModuleLicenseDTO>
  extendTrialLicense(@Parameter(description = "Account id to extend a trial") @NotNull @QueryParam(
                         NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid @Body StartTrialDTO startTrialRequestDTO) {
    return ResponseDTO.newResponse(licenseService.extendTrialLicense(accountIdentifier, startTrialRequestDTO));
  }

  @GET
  @Path("actions")
  @ApiOperation(value = "Get Allowed Actions Under Each Edition", nickname = "getEditionActions")
  @Operation(operationId = "getEditionActions", summary = "Get Allowed Actions Under Each Edition",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all actions under each edition")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<Map<Edition, Set<EditionActionDTO>>>
  getEditionActions(@Parameter(description = "Account id to get the allowed actions.") @NotNull @QueryParam(
                        NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = "A Harness Platform module.") @NotNull @QueryParam(
          NGCommonEntityConstants.MODULE_TYPE) ModuleType moduleType) {
    return ResponseDTO.newResponse(licenseService.getEditionActions(accountIdentifier, moduleType));
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

  private void validateModuleType(ModuleType moduleType) {
    if (moduleType.isInternal()) {
      throw new IllegalArgumentException("ModuleType is invalid", WingsException.USER);
    }
  }
}
