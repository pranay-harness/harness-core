/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.resources.intfc.ExperimentalLogAnalysisResource;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;

import software.wings.beans.WorkflowExecution;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(ExperimentalLogAnalysisResource.LEARNING_EXP_URL)
@Path(ExperimentalLogAnalysisResource.LEARNING_EXP_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class ExperimentalLogAnalysisResourceImpl implements ExperimentalLogAnalysisResource {
  @Inject private LogAnalysisService analysisService;
  @Inject private LearningEngineService learningEngineService;
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Inject private VerificationManagerClient managerClient;

  @VisibleForTesting
  @Inject
  public ExperimentalLogAnalysisResourceImpl(LogAnalysisService analysisService,
      LearningEngineService learningEngineService, VerificationManagerClient managerClient,
      VerificationManagerClientHelper managerClientHelper) {
    this.analysisService = analysisService;
    this.learningEngineService = learningEngineService;
    this.managerClient = managerClient;
    this.managerClientHelper = managerClientHelper;
  }

  @Override
  @POST
  @Path(ExperimentalLogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Produces({"application/json", "application/v1+json"})
  public RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("logCollectionMinute") Integer logCollectionMinute,
      @QueryParam("isBaselineCreated") boolean isBaselineCreated, @QueryParam("taskId") String taskId,
      @QueryParam("stateType") StateType stateType, ExperimentalLogMLAnalysisRecord mlAnalysisResponse)
      throws IOException {
    if (mlAnalysisResponse == null) {
      learningEngineService.markExpTaskCompleted(taskId);
      return new RestResponse<>(true);
    } else {
      WorkflowExecution workflowExecution =
          managerClientHelper.callManagerWithRetry(managerClient.getWorkflowExecution(applicationId, stateExecutionId))
              .getResource();
      if (workflowExecution != null) {
        mlAnalysisResponse.setWorkflowExecutionId(workflowExecution.getUuid());
        mlAnalysisResponse.setEnvId(
            isEmpty(workflowExecution.getEnvId()) ? "build-workflow" : workflowExecution.getEnvId());
      }

      mlAnalysisResponse.setAppId(applicationId);
      mlAnalysisResponse.setStateExecutionId(stateExecutionId);
      mlAnalysisResponse.setLogCollectionMinute(logCollectionMinute);
      mlAnalysisResponse.setBaseLineCreated(isBaselineCreated);
      return new RestResponse<>(
          analysisService.saveExperimentalLogAnalysisRecords(mlAnalysisResponse, stateType, Optional.of(taskId)));
    }
  }

  @Override
  @GET
  @Path(ExperimentalLogAnalysisResource.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<ExpAnalysisInfo>> getLogExpAnalysisInfo(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(analysisService.getExpAnalysisInfoList());
  }

  @PUT
  @Path(ExperimentalLogAnalysisResource.ANALYSIS_STATE_RE_QUEUE_TASK)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> experimentalTask(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateExecutionId") String stateExecutionId) throws IOException {
    return new RestResponse<>(analysisService.reQueueExperimentalTask(appId, stateExecutionId));
  }
}
