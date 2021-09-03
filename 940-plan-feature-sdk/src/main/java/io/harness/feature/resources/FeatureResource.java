package io.harness.feature.resources;

import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.exception.InvalidRequestException;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.configs.FeatureName;
import io.harness.feature.services.FeatureService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/plan-feature")
@Path("/plan-feature")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class FeatureResource {
  private static final String FEATURE_NAME = "featureName";
  private static final String RESOURCE_TYPE = "ACCOUNT";
  private static final String PERMISSION = "core_account_view";
  @Inject FeatureService featureService;

  @GET
  @Path("/{featureName}")
  @ApiOperation(value = "Gets Feature Details", nickname = "getFeatureDetails")
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<FeatureDetailsDTO> getFeatureDetails(@NotNull @PathParam(FEATURE_NAME) FeatureName featureName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureDetail(featureName, accountIdentifier));
  }

  @GET
  @Path("/{featureName}/available")
  @ApiOperation(value = "Check feature availability", nickname = "isFeatureAvailable")
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<Boolean> isFeatureAvailable(@NotNull @PathParam(FEATURE_NAME) FeatureName featureName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.isFeatureAvailable(featureName, accountIdentifier));
  }

  @GET
  @Path("/enabled")
  @ApiOperation(
      value = "Gets List of Enabled Feature Details for The Account", nickname = "getEnabledFeatureDetailsByAccountId")
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<List<FeatureDetailsDTO>>
  getEnabledFeaturesForAccount(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("moduleType") String moduleType) {
    ModuleType module = ModuleType.fromString(moduleType);
    if (module.isInternal() && !ModuleType.CORE.equals(module)) {
      throw new InvalidRequestException("Invalid module type");
    }
    return ResponseDTO.newResponse(featureService.getEnabledFeatureDetails(accountIdentifier, module));
  }

  @GET
  @Path("/names")
  @ApiOperation(value = "Gets List of Feature Names", nickname = "getAllFeatureNames")
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<Set<FeatureName>> getAllFeatureNames(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getAllFeatureNames());
  }
}
