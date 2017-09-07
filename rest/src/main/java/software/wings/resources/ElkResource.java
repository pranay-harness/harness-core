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
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 08/04/17.
 */
@Api(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class ElkResource implements LogAnalysisResource {
  @Inject private ElkAnalysisService analysisService;

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  @Timed
  @DelegateAuth
  @ExternalServiceAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("appId") final String appId,
      @QueryParam("serviceId") String serviceId, @QueryParam("clusterLevel") ClusterLevel clusterLevel,
      @QueryParam("delegateTaskId") String delegateTaskId, List<LogElement> logData) throws IOException {
    return new RestResponse<>(analysisService.saveLogData(StateType.ELK, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, clusterLevel, delegateTaskId, logData));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL)
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<List<LogDataRecord>> getRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("compareCurrent") boolean compareCurrent, @QueryParam("clusterLevel") ClusterLevel clusterLevel,
      LogRequest logRequest) throws IOException {
    return new RestResponse<>(analysisService.getLogData(logRequest, compareCurrent, clusterLevel, StateType.ELK));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("logCollectionMinute") Integer logCollectionMinute, LogMLAnalysisRecord mlAnalysisResponse)
      throws IOException {
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setLogCollectionMinute(logCollectionMinute);
    return new RestResponse<>(analysisService.saveLogAnalysisRecords(mlAnalysisResponse, StateType.ELK));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<LogMLAnalysisRecord> getLogMLAnalysisRecords(
      @QueryParam("accountId") String accountId, LogMLAnalysisRequest mlAnalysisRequest) throws IOException {
    return new RestResponse<>(analysisService.getLogAnalysisRecords(mlAnalysisRequest.getApplicationId(),
        mlAnalysisRequest.getStateExecutionId(), mlAnalysisRequest.getQuery(), StateType.ELK,
        mlAnalysisRequest.getLogCollectionMinute()));
  }

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException {
    return new RestResponse<>(analysisService.getAnalysisSummary(stateExecutionId, applicationId, StateType.ELK));
  }

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_SAMPLE_RECORD_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Object> getSampleLogRecord(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId, @QueryParam("index") String index)
      throws IOException {
    return new RestResponse<>(analysisService.getLogSample(accountId, analysisServerConfigId, index, StateType.ELK));
  }

  @GET
  @Path(LogAnalysisResource.ELK_GET_INDICES_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, ElkIndexTemplate>> getIndices(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId) throws IOException {
    return new RestResponse<>(analysisService.getIndices(accountId, analysisServerConfigId));
  }
}
