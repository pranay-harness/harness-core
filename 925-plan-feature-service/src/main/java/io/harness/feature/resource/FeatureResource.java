package io.harness.feature.resource;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.feature.beans.FeatureDetailRequestDTO;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.services.FeatureService;
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

  @POST
  @ApiOperation(value = "Gets Feature Detail", nickname = "getFeatureDetail")
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<FeatureDetailsDTO> getFeatureDetails(@NotNull @Valid @Body FeatureDetailRequestDTO requestDTO,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getFeatureDetail(requestDTO.getFeatureName(), accountIdentifier));
  }

  @GET
  @Path("/enabled")
  @ApiOperation(
      value = "Gets List of Enabled Feature Details for The Account", nickname = "getEnabledFeatureDetailsByAccountId")
  @NGAccessControlCheck(resourceType = RESOURCE_TYPE, permission = PERMISSION)
  public ResponseDTO<List<FeatureDetailsDTO>>
  getEnabledFeaturesForAccount(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(featureService.getEnabledFeatureDetails(accountIdentifier));
  }

  @GET
  @Path("/{featureName}/check")
  @ApiOperation(value = "Check feature availability", nickname = "checkFeatureAvailability", hidden = true)
  @InternalApi
  public ResponseDTO<Boolean> checkFeatureAvailability(@NotNull @PathParam(FEATURE_NAME) FeatureRestriction featureName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam("currentUsage") long currentUsage) {
    featureService.checkAvailabilityWithUsage(featureName, accountIdentifier, currentUsage);
    return ResponseDTO.newResponse(true);
  }
}
