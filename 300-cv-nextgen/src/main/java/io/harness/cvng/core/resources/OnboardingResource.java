package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("onboarding")
@Path("/onboarding")
@Produces("application/json")
@NextGenManagerAuth
@ExposeInternalException
public class OnboardingResource {
  @Inject private OnboardingService onboardingService;
  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "onboarding api response", nickname = "getOnboardingResponse")
  public RestResponse<OnboardingResponseDTO> getOnboardingResponse(
      @QueryParam("accountId") @NotNull String accountId, OnboardingRequestDTO onboardingRequestDTO) {
    return new RestResponse<>(onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO));
  }
}
