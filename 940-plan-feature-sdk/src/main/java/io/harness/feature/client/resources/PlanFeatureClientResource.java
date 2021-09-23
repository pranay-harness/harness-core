package io.harness.feature.client.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.exception.InvalidRequestException;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.FeatureUsageDTO;
import io.harness.feature.client.custom.CustomFeatureRestriction;
import io.harness.feature.client.custom.CustomRestrictionResult;
import io.harness.feature.client.services.PlanFeatureRegisterService;
import io.harness.feature.client.usage.PlanFeatureUsageInterface;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
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

@Api(value = "/feature-usage", hidden = true)
@Path("/feature-usage")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class PlanFeatureClientResource {
  private static final String FEATURE_NAME = "featureName";
  @Inject private PlanFeatureRegisterService planFeatureRegisterService;

  @GET
  @Path("/{featureName}")
  @InternalApi
  public ResponseDTO<FeatureUsageDTO> getFeatureUsage(@NotNull @PathParam(FEATURE_NAME) FeatureRestriction featureName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    PlanFeatureUsageInterface planFeatureUsage = planFeatureRegisterService.get(featureName);
    if (planFeatureUsage == null) {
      return ResponseDTO.newResponse(FeatureUsageDTO.builder().count(0).build());
    }
    return ResponseDTO.newResponse(
        FeatureUsageDTO.builder().count(planFeatureUsage.getCurrentValue(accountIdentifier)).build());
  }

  @POST
  @Path("/custom/{featureName}")
  @InternalApi
  public ResponseDTO<FeatureDetailsDTO> getCustomFeatureRestrictionDetail(
      @NotNull @PathParam(FEATURE_NAME) FeatureRestriction featureName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid @Body LicensesWithSummaryDTO licensesWithSummaryDTO) {
    CustomFeatureRestriction customFeatureRestriction = planFeatureRegisterService.getCustom(featureName);
    if (customFeatureRestriction == null) {
      throw new InvalidRequestException(
          String.format("Custom feature restriction [%s] cannot be found in PlanFeatureRegisterService", featureName));
    }

    CustomRestrictionResult customRestrictionResult =
        customFeatureRestriction.evaluateCustomRestriction(featureName, accountIdentifier, licensesWithSummaryDTO);
    return ResponseDTO.newResponse(customFeatureRestriction.toFeatureDetailsDTO(customRestrictionResult));
  }
}
