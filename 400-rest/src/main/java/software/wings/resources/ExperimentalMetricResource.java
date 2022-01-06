/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.GT;
import static io.harness.beans.SearchFilter.Operator.LT;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentPerformance;
import software.wings.service.impl.analysis.ExperimentStatus;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord.ExperimentalMetricAnalysisRecordKeys;
import software.wings.service.impl.analysis.MetricAnalysisRecord.MetricAnalysisRecordKeys;
import software.wings.service.impl.newrelic.ExperimentalMetricRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.ExperimentalAnalysisService;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api(VerificationConstants.LEARNING_METRIC_EXP_URL)
@Path("/" + VerificationConstants.LEARNING_METRIC_EXP_URL)
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
@Slf4j
public class ExperimentalMetricResource {
  @Inject private ExperimentalAnalysisService analysisService;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @GET
  @Path(VerificationConstants.GET_EXP_PERFORMANCE_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<ExperimentPerformance> getMetricExpAnalysisAccuracyImprovement(
      @QueryParam("experimentName") String experimentName, @QueryParam("startTime") Long startTime,
      @QueryParam("endTime") Long endTime) {
    PageRequest<ExperimentalMetricAnalysisRecord> pageRequest =
        aPageRequest().withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE)).build();

    if (experimentName != null) {
      pageRequest.addFilter(ExperimentalMetricAnalysisRecordKeys.experimentName, EQ, experimentName);
    }

    if (startTime != null) {
      pageRequest.addFilter(ExperimentalMetricAnalysisRecord.BaseKeys.createdAt, GT, startTime);
    }

    if (endTime != null) {
      pageRequest.addFilter(ExperimentalMetricAnalysisRecord.BaseKeys.createdAt, LT, endTime);
    }

    return new RestResponse<>(analysisService.getMetricExpAnalysisPerformance(pageRequest));
  }

  @GET
  @Path(VerificationConstants.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ExpAnalysisInfo>> getMetricExpAnalysisInfo(@QueryParam("offset") String offset,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("experimentName") String experimentName,
      @QueryParam("startTime") Long startTime, @QueryParam("endTime") Long endTime,
      @QueryParam("mismatch") Boolean mismatch, @QueryParam("experimentStatus") String experimentStatus) {
    PageRequest<ExperimentalMetricAnalysisRecord> pageRequest =
        aPageRequest().withOffset(offset).withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE)).build();

    if (stateExecutionId != null) {
      pageRequest.addFilter(MetricAnalysisRecordKeys.stateExecutionId, EQ, stateExecutionId);
    }

    if (experimentName != null) {
      pageRequest.addFilter(ExperimentalMetricAnalysisRecordKeys.experimentName, EQ, experimentName);
    }

    if (startTime != null) {
      pageRequest.addFilter(ExperimentalMetricAnalysisRecord.BaseKeys.createdAt, GT, startTime);
    }

    if (endTime != null) {
      pageRequest.addFilter(ExperimentalMetricAnalysisRecord.BaseKeys.createdAt, LT, endTime);
    }

    if (mismatch != null) {
      pageRequest.addFilter(ExperimentalMetricAnalysisRecordKeys.mismatched, EQ, mismatch);
    }

    if (experimentStatus != null) {
      pageRequest.addFilter(
          ExperimentalMetricAnalysisRecordKeys.experimentStatus, EQ, ExperimentStatus.valueOf(experimentStatus));
    }

    return new RestResponse<>(analysisService.getMetricExpAnalysisInfoList(pageRequest));
  }

  @GET
  @Path(VerificationConstants.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<ExperimentalMetricRecord> getExperimentalAnalysisSummary(
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("stateType") StateType stateType,
      @QueryParam("expName") String expName) {
    return new RestResponse<>(
        analysisService.getExperimentalMetricAnalysisSummary(stateExecutionId, stateType, expName));
  }

  @GET
  @Path(VerificationConstants.MARK_EXP_STATUS)
  @Timed
  @ExceptionMetered
  public RestResponse<ExperimentalMetricRecord> markExperimentStatus(
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("stateType") StateType stateType,
      @QueryParam("expName") String expName, @QueryParam("expStatus") String expStatus) {
    return new RestResponse<>(analysisService.markExperimentStatus(
        stateExecutionId, stateType, expName, ExperimentStatus.valueOf(expStatus)));
  }

  @POST
  @Path(VerificationConstants.UPDATE_MISMATCH)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> updateMismatchState(
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("analysisMinute") Integer analysisMinute) {
    try {
      analysisService.updateMismatchStatusForExperimentalRecord(stateExecutionId, analysisMinute);
    } catch (Exception e) {
      log.info("Exception while updating experimental record {}", stateExecutionId, e);
    }
    return new RestResponse<>(true);
  }
}
