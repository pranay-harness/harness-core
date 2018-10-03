package io.harness.resources.intfc;

import com.google.inject.ImplementedBy;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.resources.ExperimentalTimeseriesAnalysisResourceImpl;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.sm.StateType;

import java.io.IOException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Resource for Timeseries Experimental Task
 * <p>
 * Created by Pranjal on 08/14/2018
 */
@ImplementedBy(ExperimentalTimeseriesAnalysisResourceImpl.class)
public interface ExperimentalMetricAnalysisResource {
  String LEARNING_EXP_URL = "timeseries-learning-exp";
  String ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL = "/save-timeseries-analysis-records";

  /**
   * API to save ML Analysed Records back to Mongo DB
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
  @Path(ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateType") StateType stateType,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("taskId") String taskId,
      @QueryParam("baseLineExecutionId") String baseLineExecutionId,
      ExperimentalMetricAnalysisRecord mlAnalysisResponse) throws IOException;
}
