package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.ExperimentalMetricAnalysisResource;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.io.IOException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Resource implementation for Timeseries Experimental Task
 *
 * Created by Pranjal on 08/14/2018
 */
@Api(ExperimentalMetricAnalysisResource.LEARNING_EXP_URL)
@Path(ExperimentalMetricAnalysisResource.LEARNING_EXP_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class ExperimentalTimeseriesAnalysisResourceImpl implements ExperimentalMetricAnalysisResource {
  @Inject private LearningEngineService learningEngineService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

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
   * @throws IOException
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
      ExperimentalMetricAnalysisRecord mlAnalysisResponse) throws IOException {
    if (mlAnalysisResponse == null) {
      learningEngineService.markExpTaskCompleted(taskId);
      return new RestResponse<>(true);
    } else {
      StateExecutionInstance stateExecutionInstance =
          workflowExecutionService.getStateExecutionData(applicationId, stateExecutionId);
      mlAnalysisResponse.setWorkflowExecutionId(stateExecutionInstance.getExecutionUuid());
      WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(applicationId, stateExecutionInstance.getExecutionUuid());
      if (workflowExecution.getEnvId() == null) {
        mlAnalysisResponse.setEnvId("build-workflow");
      } else {
        mlAnalysisResponse.setEnvId(workflowExecution.getEnvId());
      }
      mlAnalysisResponse.setAppId(applicationId);
      mlAnalysisResponse.setStateExecutionId(stateExecutionId);
      mlAnalysisResponse.setAnalysisMinute(analysisMinute);
      mlAnalysisResponse.setBaseLineExecutionId(baseLineExecutionId);
      mlAnalysisResponse.setStateType(stateType);
      mlAnalysisResponse.setAppId(applicationId);
      mlAnalysisResponse.setWorkflowExecutionId(workflowExecutionId);
    }
    return new RestResponse<>(metricDataAnalysisService.saveAnalysisRecordsML(stateType, accountId, applicationId,
        stateExecutionId, workflowExecutionId, workflowId, serviceId, groupName, analysisMinute, taskId,
        baseLineExecutionId, mlAnalysisResponse));
  }
}
