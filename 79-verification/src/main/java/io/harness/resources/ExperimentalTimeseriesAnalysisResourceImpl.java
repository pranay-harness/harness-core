package io.harness.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.resources.intfc.ExperimentalMetricAnalysisResource;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.sm.StateType;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Resource implementation for Timeseries Experimental Task.
 *
 * Created by Pranjal on 08/14/2018
 */
@Api(ExperimentalMetricAnalysisResource.LEARNING_EXP_URL)
@Path(ExperimentalMetricAnalysisResource.LEARNING_EXP_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class ExperimentalTimeseriesAnalysisResourceImpl implements ExperimentalMetricAnalysisResource {
  @Inject private LearningEngineService learningEngineService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;

  /**
   * API implementation to save ML Analysed Records back to Mongo DB
   *
   * @param accountId
   * @param applicationId
   * @param stateType
   * @param stateExecutionId
   * @param workflowExecutionId
   * @param workflowId
   * @param serviceId
   * @param groupName
   * @param analysisMinute
   * @param taskId
   * @param baseLineExecutionId
   * @param mlAnalysisResponse
   * @return {@link RestResponse}
   */
  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(ExperimentalMetricAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateType") StateType stateType,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("workflowId") final String workflowId, @QueryParam("serviceId") final String serviceId,
      @QueryParam("groupName") final String groupName, @QueryParam("analysisMinute") Integer analysisMinute,
      @QueryParam("taskId") String taskId, @QueryParam("baseLineExecutionId") String baseLineExecutionId,
      @QueryParam("cvConfigId") String cvConfigId, ExperimentalMetricAnalysisRecord mlAnalysisResponse) {
    if (mlAnalysisResponse == null) {
      learningEngineService.markExpTaskCompleted(taskId);
      return new RestResponse<>(true);
    } else {
      mlAnalysisResponse.setAppId(applicationId);
      mlAnalysisResponse.setStateExecutionId(stateExecutionId);
      mlAnalysisResponse.setAnalysisMinute(analysisMinute);
      mlAnalysisResponse.setBaseLineExecutionId(baseLineExecutionId);
      mlAnalysisResponse.setStateType(stateType);
      mlAnalysisResponse.setAppId(applicationId);
      mlAnalysisResponse.setWorkflowExecutionId(workflowExecutionId);
    }
    return new RestResponse<>(timeSeriesAnalysisService.saveAnalysisRecordsML(stateType, accountId, applicationId,
        stateExecutionId, workflowExecutionId, workflowId, serviceId, groupName, analysisMinute, taskId,
        baseLineExecutionId, cvConfigId, mlAnalysisResponse));
  }
}
