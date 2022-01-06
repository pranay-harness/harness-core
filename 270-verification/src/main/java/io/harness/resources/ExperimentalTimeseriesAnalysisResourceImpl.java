/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resources;

import io.harness.resources.intfc.ExperimentalMetricAnalysisResource;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;

import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.Version;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Map;
import javax.ws.rs.GET;
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
  @Override
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
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("experimentName") String experimentName,
      ExperimentalMetricAnalysisRecord mlAnalysisResponse) {
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
      mlAnalysisResponse.setExperimentName(experimentName);
    }
    return new RestResponse<>(timeSeriesAnalysisService.saveAnalysisRecordsML(accountId, stateType, applicationId,
        stateExecutionId, workflowExecutionId, groupName, analysisMinute, taskId, baseLineExecutionId, cvConfigId,
        mlAnalysisResponse, null));
  }

  @Override
  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path(ExperimentalMetricAnalysisResource.GET_METRIC_TEMPLATE)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> getMetricTemplateExperimental(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("stateType") StateType stateType, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("serviceId") String serviceId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("groupName") String groupName) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricTemplateWithCategorizedThresholds(
        appId, stateType, stateExecutionId, serviceId, cvConfigId, groupName, Version.EXPERIMENT));
  }
}
