package io.harness.enforcement.client.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.enforcement.beans.FeatureRestrictionUsageDTO;
import io.harness.enforcement.client.services.EnforcementSdkRegisterService;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
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

@Api(value = "/enforcement/client", hidden = true)
@Path("/enforcement/client")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class EnforcementClientResource {
  private static final String FEATURE_RESTRICTION_NAME = "featureRestrictionName";
  @Inject private EnforcementSdkRegisterService enforcementSdkRegisterService;

  @GET
  @Path("usage/{featureRestrictionName}")
  @InternalApi
  public ResponseDTO<FeatureRestrictionUsageDTO> getFeatureUsage(
      @NotNull @PathParam(FEATURE_RESTRICTION_NAME) FeatureRestrictionName featureRestrictionName,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    RestrictionUsageInterface planFeatureUsage = enforcementSdkRegisterService.get(featureRestrictionName);
    if (planFeatureUsage == null) {
      return ResponseDTO.newResponse(FeatureRestrictionUsageDTO.builder().count(0).build());
    }
    return ResponseDTO.newResponse(
        FeatureRestrictionUsageDTO.builder().count(planFeatureUsage.getCurrentValue(accountIdentifier)).build());
  }
}
