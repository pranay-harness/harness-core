package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.datadog.DataDogFetchConfig;
import software.wings.service.intfc.analysis.APMVerificationService;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState;

import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("datadog")
@Path("/datadog")
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class DatadogResource {
  @Inject private APMVerificationService verificationService;

  @GET
  @Path("/metric-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<DatadogState.Metric>> getAllMetricNames(@QueryParam("accountId") String accountId)
      throws IOException {
    return new RestResponse<>(DatadogState.metricNames());
  }

  @POST
  @Path("/node-data")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @QueryParam("serverConfigId") String serverConfigId,
      @Valid DataDogFetchConfig fetchConfig) {
    return new RestResponse<>(
        verificationService.getMetricsWithDataForNode(accountId, serverConfigId, fetchConfig, StateType.DATA_DOG));
  }
}
