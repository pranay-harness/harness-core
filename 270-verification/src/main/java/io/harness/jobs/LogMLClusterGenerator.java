/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.jobs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.service.intfc.LearningEngineService;

import software.wings.common.VerificationConstants;
import software.wings.service.impl.analysis.LogClusterContext;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by sriram_parthasarathy on 8/24/17.
 */
@Slf4j
public class LogMLClusterGenerator implements Runnable {
  private LearningEngineService learningEngineService;

  private final LogClusterContext context;
  private final ClusterLevel fromLevel;
  private final ClusterLevel toLevel;
  private final LogRequest logRequest;
  private final int startDataCollectionMinute;

  public LogMLClusterGenerator(LearningEngineService learningEngineService, LogClusterContext context,
      ClusterLevel fromLevel, ClusterLevel toLevel, LogRequest logRequest, int startDataCollectionMinute) {
    this.learningEngineService = learningEngineService;
    this.context = context;
    this.fromLevel = fromLevel;
    this.toLevel = toLevel;
    this.logRequest = logRequest;
    this.startDataCollectionMinute = startDataCollectionMinute;
  }

  @Override
  public void run() {
    final String taskId = generateUuid();
    final String inputLogsUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + context.getAccountId()
        + "&workflowExecutionId=" + context.getWorkflowExecutionId()
        + "&compareCurrent=true&clusterLevel=" + fromLevel.name() + "&stateType=" + context.getStateType();
    String clusteredLogSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL + "?accountId=" + context.getAccountId()
        + "&stateExecutionId=" + context.getStateExecutionId() + "&workflowId=" + context.getWorkflowId()
        + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&serviceId=" + context.getServiceId()
        + "&appId=" + context.getAppId() + "&clusterLevel=" + toLevel.name() + "&stateType=" + context.getStateType();
    String failureUrl = "/verification/" + LearningEngineService.RESOURCE_URL
        + VerificationConstants.NOTIFY_LEARNING_FAILURE + "?taskId=" + taskId;
    log.info("Creating Learning Engine Analysis Task for Log ML clustering with context {}", context);

    LearningEngineAnalysisTask analysisTask = LearningEngineAnalysisTask.builder()
                                                  .accountId(context.getAccountId())
                                                  .control_input_url(inputLogsUrl)
                                                  .analysis_save_url(clusteredLogSaveUrl)
                                                  .analysis_failure_url(failureUrl)
                                                  .workflow_id(context.getWorkflowId())
                                                  .workflow_execution_id(context.getWorkflowExecutionId())
                                                  .state_execution_id(context.getStateExecutionId())
                                                  .service_id(context.getServiceId())
                                                  .control_nodes(logRequest.getNodes())
                                                  .sim_threshold(0.99)
                                                  .analysis_minute(logRequest.getLogCollectionMinute())
                                                  .analysis_start_min(startDataCollectionMinute)
                                                  .cluster_level(toLevel.getLevel())
                                                  .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                                                  .stateType(context.getStateType())
                                                  .query(Lists.newArrayList(logRequest.getQuery().split(" ")))
                                                  .priority(0)
                                                  .build();
    analysisTask.setAppId(context.getAppId());
    analysisTask.setUuid(taskId);

    final boolean taskQueued = learningEngineService.addLearningEngineAnalysisTask(analysisTask);
    if (taskQueued) {
      log.info("Clustering queued for {}", logRequest);
    }
  }
}
