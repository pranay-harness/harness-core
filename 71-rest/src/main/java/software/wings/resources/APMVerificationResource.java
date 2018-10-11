package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.APMFetchConfig;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.beans.RestResponse;
import software.wings.common.VerificationConstants;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.intfc.analysis.APMVerificationService;
import software.wings.sm.StateType;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("apm")
@Path("/apm")
@Produces("application/json")
public class APMVerificationResource {
  @Inject private APMVerificationService apmVerificationService;

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param serverConfigId
   * @param fetchConfig
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @QueryParam("serverConfigId") String serverConfigId,
      APMFetchConfig fetchConfig) {
    return new RestResponse<>(apmVerificationService.getMetricsWithDataForNode(accountId, serverConfigId, fetchConfig));
  }

  @POST
  @Path(VerificationConstants.NOTIFY_METRIC_STATE)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> sendNotifyForMetricAnalysis(
      @QueryParam("correlationId") String correlationId, MetricDataAnalysisResponse response) {
    return new RestResponse<>(apmVerificationService.sendNotifyForMetricAnalysis(correlationId, response));
  }

  @GET
  @Path(VerificationConstants.COLLECT_24_7_DATA)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> collect247CVData(@QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("stateType") StateType stateType, @QueryParam("startTime") long startTime,
      @QueryParam("endTime") long endTime) {
    return new RestResponse<>(apmVerificationService.collect247Data(cvConfigId, stateType, startTime, endTime));
  }
}
