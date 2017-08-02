package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.ExternalServiceAuth;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRequest;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 */
@Api("splunk")
@Path("/splunk")
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class SplunkResource {
  @Inject private AnalysisService analysisService;

  @POST
  @Path("/save-logs")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveSplunkLogData(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("appId") final String appId, List<LogElement> logData) throws IOException {
    return new RestResponse<>(
        analysisService.saveLogData(StateType.SPLUNKV2, appId, stateExecutionId, workflowId, logData));
  }

  @POST
  @Path("/get-logs")
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<List<LogDataRecord>> getSplunkLogData(@QueryParam("accountId") String accountId,
      @QueryParam("compareCurrent") boolean compareCurrent, LogRequest logRequest) throws IOException {
    return new RestResponse<>(analysisService.getLogData(logRequest, compareCurrent, StateType.SPLUNKV2));
  }

  @POST
  @Path("/save-analysis-records")
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<Boolean> saveSplunkAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      LogMLAnalysisRecord mlAnalysisResponse) throws IOException {
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    return new RestResponse<>(analysisService.saveLogAnalysisRecords(mlAnalysisResponse, StateType.SPLUNKV2));
  }

  @POST
  @Path("/get-analysis-records")
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<LogMLAnalysisRecord> getplunkAnalysisRecords(
      @QueryParam("accountId") String accountId, LogMLAnalysisRequest mlAnalysisRequest) throws IOException {
    return new RestResponse<>(analysisService.getLogAnalysisRecords(mlAnalysisRequest.getApplicationId(),
        mlAnalysisRequest.getStateExecutionId(), mlAnalysisRequest.getQuery(), StateType.SPLUNKV2));
  }

  @GET
  @Path("/get-splunk-analysis-summary")
  @Timed
  @ExceptionMetered
  public RestResponse<LogMLAnalysisSummary> getAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException {
    return new RestResponse<>(analysisService.getAnalysisSummary(stateExecutionId, applicationId, StateType.SPLUNKV2));
  }
}
