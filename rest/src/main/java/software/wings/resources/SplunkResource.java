package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.splunk.SplunkLogDataRecord;
import software.wings.service.impl.splunk.SplunkLogElement;
import software.wings.service.impl.splunk.SplunkLogRequest;
import software.wings.service.impl.splunk.SplunkLogMLAnalysisRecord;
import software.wings.service.impl.splunk.SplunkMLAnalysisRequest;
import software.wings.service.impl.splunk.SplunkMLAnalysisSummary;
import software.wings.service.intfc.splunk.SplunkService;

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
  @Inject private SplunkService splunkService;

  @POST
  @Path("/save-logs")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveSplunkLogData(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("appId") final String appId,
      List<SplunkLogElement> logData) throws IOException {
    return new RestResponse<>(splunkService.saveLogData(appId, stateExecutionId, workflowExecutionId, logData));
  }

  @POST
  @Path("/get-logs")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<List<SplunkLogDataRecord>> getSplunkLogData(
      @QueryParam("accountId") String accountId, SplunkLogRequest logRequest) throws IOException {
    return new RestResponse<>(splunkService.getSplunkLogData(logRequest));
  }

  @POST
  @Path("/save-analysis-records")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<Boolean> saveSplunkAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      SplunkLogMLAnalysisRecord mlAnalysisResponse) throws IOException {
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    return new RestResponse<>(splunkService.saveSplunkAnalysisRecords(mlAnalysisResponse));
  }

  @POST
  @Path("/get-analysis-records")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<SplunkLogMLAnalysisRecord> getplunkAnalysisRecords(
      @QueryParam("accountId") String accountId, SplunkMLAnalysisRequest mlAnalysisRequest) throws IOException {
    return new RestResponse<>(splunkService.getSplunkAnalysisRecords(
        mlAnalysisRequest.getApplicationId(), mlAnalysisRequest.getStateExecutionId(), mlAnalysisRequest.getQuery()));
  }

  @GET
  @Path("/get-splunk-analysis-summary")
  @Timed
  @ExceptionMetered
  public RestResponse<SplunkMLAnalysisSummary> getAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException {
    return new RestResponse<>(splunkService.getAnalysisSummary(stateExecutionId, applicationId));
  }
}
