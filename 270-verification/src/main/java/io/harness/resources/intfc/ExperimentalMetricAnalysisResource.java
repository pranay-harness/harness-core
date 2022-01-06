/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resources.intfc;

import io.harness.resources.ExperimentalTimeseriesAnalysisResourceImpl;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.GET;
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
  String GET_METRIC_TEMPLATE = "/get-metric-template-experimental";

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
      @QueryParam("baseLineExecutionId") String baseLineExecutionId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("experimentName") String experimentName, ExperimentalMetricAnalysisRecord mlAnalysisResponse)
      throws IOException;

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path(GET_METRIC_TEMPLATE)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> getMetricTemplateExperimental(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("stateType") StateType stateType, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("serviceId") String serviceId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("groupName") String groupName);
}
