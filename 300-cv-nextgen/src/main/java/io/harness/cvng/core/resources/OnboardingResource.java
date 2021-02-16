package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.DataSourceService;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.cvng.core.services.api.StackdriverService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.HashMap;
import java.util.Map;
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
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Inject private AppDynamicsService appDynamicsService;
  @Inject private StackdriverService stackdriverService;
  @Inject private SplunkService splunkService;

  Map<DataSourceType, DataSourceService> typeServiceMap = new HashMap<DataSourceType, DataSourceService>() {
    {
      put(DataSourceType.APP_DYNAMICS, appDynamicsService);
      put(DataSourceType.STACKDRIVER, stackdriverService);
      put(DataSourceType.SPLUNK, splunkService);
      put(DataSourceType.KUBERNETES, kubernetesActivitySourceService);
    }
  };

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "onboarding api response", nickname = "getOnboardingResponse")
  public RestResponse<OnboardingResponseDTO> getOnboardingResponse(
      @QueryParam("accountId") @NotNull String accountId, OnboardingRequestDTO onboardingRequestDTO) {
    return new RestResponse<>(onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/connector")
  @ApiOperation(value = "connector api response", nickname = "validateConnector")
  public RestResponse<String> validateConnector(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("connectorIdentifier") @NotNull String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("tracingId") @NotNull String tracingId, DataSourceType dataSourceType) {
    Preconditions.checkNotNull(typeServiceMap.containsKey(dataSourceType));
    String response =
        typeServiceMap.get(dataSourceType)
            .checkConnectivity(accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId);
    return new RestResponse<>(response);
  }
}
