package io.harness.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.swagger.annotations.Api;
import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(VerificationConstants.DELEGATE_DATA_COLLETION)
@Path("/" + VerificationConstants.DELEGATE_DATA_COLLETION)
@Produces("application/json")
@Scope(ResourceType.SETTING)
@DelegateAuth
public class DelegateDataCollectionResource {
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private LogAnalysisService analysisService;

  @POST
  @Path("/save-metrics")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricData(@QueryParam("accountId") final String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("delegateTaskId") String delegateTaskId, List<NewRelicMetricDataRecord> metricData) {
    return new RestResponse<>(timeSeriesAnalysisService.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, metricData));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowId") String workflowId, @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("appId") final String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("clusterLevel") ClusterLevel clusterLevel, @QueryParam("delegateTaskId") String delegateTaskId,
      @QueryParam("stateType") StateType stateType, List<LogElement> logData) {
    return new RestResponse<>(analysisService.saveLogData(stateType, accountId, appId, cvConfigId, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, clusterLevel, delegateTaskId, logData));
  }
}
