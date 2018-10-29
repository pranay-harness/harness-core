package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.exception.WingsException;
import io.swagger.annotations.Api;
import software.wings.api.InstanceElement;
import software.wings.beans.FeatureName;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(LogAnalysisResource.LOG_ANALYSIS)
@Path("/" + LogAnalysisResource.LOG_ANALYSIS)
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class LogMLResource {
  @Inject private AnalysisService analysisService;
  @Inject private FeatureFlagService featureFlagService;

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("stateType") StateType stateType) throws IOException {
    if (featureFlagService.isEnabledReloadCache(FeatureName.CV_DEMO, accountId)) {
      return new RestResponse<>(analysisService.getAnalysisSummaryForDemo(stateExecutionId, applicationId, stateType));
    } else {
      return new RestResponse<>(analysisService.getAnalysisSummary(stateExecutionId, applicationId, stateType));
    }
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> createUserFeedback(
      @QueryParam("accountId") String accountId, @QueryParam("stateType") StateType stateType, LogMLFeedback feedback) {
    if (!isEmpty(feedback.getLogMLFeedbackId())) {
      throw new WingsException("feedback id should not be set in POST call. to update feedback use PUT");
    }
    return new RestResponse<>(analysisService.saveFeedback(feedback, stateType));
  }

  @DELETE
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK + "/{feedbackId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteUserFeedback(
      @QueryParam("accountId") String accountId, @PathParam("feedbackId") String feedbackId) throws IOException {
    return new RestResponse<>(analysisService.deleteFeedback(feedbackId));
  }

  @GET
  @Produces({"application/json", "application/v1+json"})
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<LogMLFeedbackRecord>> getFeedback(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("workflowId") String workflowId, @QueryParam("workflowExecutionId") String workflowExecutionId) {
    return new RestResponse<>(analysisService.getMLFeedback(appId, serviceId, workflowId, workflowExecutionId));
  }

  @GET
  @Produces({"application/json", "application/v1+json"})
  @Path(LogAnalysisResource.LAST_EXECUTION_NODES)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, InstanceElement>> getLastExecutionNodes(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("workflowId") String workflowId) {
    return new RestResponse<>(analysisService.getLastExecutionNodes(appId, workflowId));
  }
}
