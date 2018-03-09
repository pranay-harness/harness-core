package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRequest;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 08/04/17.
 *
 * For api versioning see documentation of {@link NewRelicResource}.
 */
@Api(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class ElkResource implements LogAnalysisResource {
  private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);

  @Inject private ElkAnalysisService analysisService;

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  @Timed
  @DelegateAuth
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("appId") final String appId,
      @QueryParam("serviceId") String serviceId, @QueryParam("clusterLevel") ClusterLevel clusterLevel,
      @QueryParam("delegateTaskId") String delegateTaskId, List<LogElement> logData) throws IOException {
    return new RestResponse<>(analysisService.saveLogData(StateType.ELK, accountId, appId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, clusterLevel, delegateTaskId, logData));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<LogDataRecord>> getRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("clusterLevel") ClusterLevel clusterLevel, @QueryParam("compareCurrent") boolean compareCurrent,
      LogRequest logRequest) throws IOException {
    return new RestResponse<>(
        analysisService.getLogData(logRequest, compareCurrent, workflowExecutionId, clusterLevel, StateType.ELK));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("logCollectionMinute") Integer logCollectionMinute,
      @QueryParam("isBaselineCreated") boolean isBaselineCreated, @QueryParam("taskId") String taskId,
      @QueryParam("baseLineExecutionId") String baseLineExecutionId, LogMLAnalysisRecord mlAnalysisResponse)
      throws IOException {
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setLogCollectionMinute(logCollectionMinute);
    mlAnalysisResponse.setBaseLineCreated(isBaselineCreated);
    mlAnalysisResponse.setBaseLineExecutionId(baseLineExecutionId);
    return new RestResponse<>(
        analysisService.saveLogAnalysisRecords(mlAnalysisResponse, StateType.ELK, Optional.of(taskId)));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
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
    Map<String, Map<String, List<Map>>> result = null;
    try {
      result = (Map<String, Map<String, List<Map>>>) analysisService.getLogSample(
          accountId, analysisServerConfigId, index, StateType.ELK);
      return new RestResponse<>(result.get("hits").get("hits").get(0).get("_source"));
    } catch (Exception ex) {
      logger.warn("Failed to get elk sample record " + result, ex);
    }
    return new RestResponse<>();
  }

  @GET
  @Path(LogAnalysisResource.ELK_GET_INDICES_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, ElkIndexTemplate>> getIndices(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId) throws IOException {
    try {
      return new RestResponse<>(analysisService.getIndices(accountId, analysisServerConfigId));
    } catch (Exception ex) {
      logger.warn("Unable to get indices", ex);
    }
    return new RestResponse<>(null);
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<Boolean> createUserFeedback(@QueryParam("accountId") String accountId, LogMLFeedback feedback)
      throws IOException {
    if (!isEmpty(feedback.getLogMLFeedbackId())) {
      throw new WingsException("feedback id should not be set in POST call. to update feedback use PUT");
    }
    return new RestResponse<>(analysisService.saveFeedback(feedback, StateType.ELK));
  }

  @PUT
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<Boolean> updateUserFeedback(@QueryParam("accountId") String accountId, LogMLFeedback feedback)
      throws IOException {
    if (isEmpty(feedback.getLogMLFeedbackId())) {
      throw new WingsException("logMlFeedBackId should be set for update");
    }
    return new RestResponse<>(analysisService.saveFeedback(feedback, StateType.ELK));
  }

  @DELETE
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<Boolean> deleteUserFeedback(@QueryParam("accountId") String accountId, LogMLFeedback feedback)
      throws IOException {
    return new RestResponse<>(analysisService.deleteFeedback(feedback));
  }

  @GET
  @Path(LogAnalysisResource.ELK_VALIDATE_QUERY)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> validateQuery(
      @QueryParam("accountId") String accountId, @QueryParam("query") String query) throws IOException {
    try {
      new ElkLogFetchRequest(query, "logstash-*", "beat.hostname", "message", "@timestamp",
          Sets.newHashSet("ip-172-31-8-144", "ip-172-31-12-79", "ip-172-31-13-153"),
          1518724315175L - TimeUnit.MINUTES.toMillis(1), 1518724315175L)
          .toElasticSearchJsonObject();
      return new RestResponse<>(true);
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR).addParam("reason", ex.getMessage());
    }
  }
}
