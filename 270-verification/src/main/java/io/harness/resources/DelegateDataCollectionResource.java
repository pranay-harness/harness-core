/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resources;

import static software.wings.common.VerificationConstants.DELEGATE_DATA_COLLECTION;

import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;

import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.sm.StateType;
import software.wings.verification.CVActivityLog;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(DELEGATE_DATA_COLLECTION)
@Path("/" + DELEGATE_DATA_COLLECTION)
@Produces("application/json")
@Scope(ResourceType.SETTING)
@DelegateAuth
public class DelegateDataCollectionResource {
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private LogAnalysisService analysisService;
  @Inject private CVActivityLogService cvActivityLogService;
  @Inject private CVTaskService cvTaskService;
  @POST
  @Path("/save-metrics")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricData(@QueryParam("accountId") final String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("delegateTaskId") String delegateTaskId, List<NewRelicMetricDataRecord> metricData) {
    return new RestResponse<>(timeSeriesAnalysisService.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, metricData));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowId") String workflowId, @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("appId") final String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("clusterLevel") ClusterLevel clusterLevel, @QueryParam("delegateTaskId") String delegateTaskId,
      @QueryParam("stateType") StateType stateType, List<LogElement> logData) {
    return new RestResponse<>(analysisService.saveLogData(stateType, accountId, appId, cvConfigId, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, clusterLevel, delegateTaskId, logData));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(VerificationConstants.SAVE_CV_ACTIVITY_LOGS_PATH)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Void> saveActivityLogs(
      @QueryParam("accountId") @Valid final String accountId, List<CVActivityLog> cvActivityLogs) {
    cvActivityLogService.saveActivityLogs(cvActivityLogs);
    return new RestResponse<>(null);
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(VerificationConstants.CV_TASK_STATUS_UPDATE_PATH)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Void> updateCVTaskStatus(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("cvTaskId") String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult) {
    cvTaskService.updateTaskStatus(cvTaskId, dataCollectionTaskResult);
    return new RestResponse<>(null);
  }
}
