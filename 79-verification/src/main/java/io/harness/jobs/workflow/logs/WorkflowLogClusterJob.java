package io.harness.jobs.workflow.logs;

import static software.wings.beans.FeatureName.REMOVE_WORKFLOW_VERIFICATION_CLUSTERING_CRON;
import static software.wings.common.VerificationConstants.GA_PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.PER_MINUTE_CV_STATES;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.jobs.LogMLClusterGenerator;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@DisallowConcurrentExecution
@Slf4j
public class WorkflowLogClusterJob implements Job, MongoPersistenceIterator.Handler<AnalysisContext> {
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Inject private VerificationManagerClient managerClient;

  @Inject private LogAnalysisService analysisService;

  @Inject private LearningEngineService learningEngineService;

  @Inject private DataStoreService dataStoreService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    try {
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      if (managerClientHelper
              .callManagerWithRetry(
                  managerClient.isFeatureEnabled(REMOVE_WORKFLOW_VERIFICATION_CLUSTERING_CRON, context.getAccountId()))
              .getResource()) {
        logger.info(
            "The feature REMOVE_WORKFLOW_VERIFICATION_CLUSTERING_CRON is enabled for {}, it will be handled by the iterators",
            context.getAccountId());
      } else {
        logger.info("Executing workflow Log Cluster cron job with context : {} and params : {}", context, params);
        new WorkflowLogClusterJob
            .LogClusterTask(analysisService, managerClientHelper, Optional.of(jobExecutionContext), context,
                learningEngineService, managerClient, dataStoreService)
            .run();
      }
      if (!learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
        logger.info("The state {} is no longer valid, so we are deleting the cron now.", context.getStateExecutionId());
        jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      }

    } catch (Exception ex) {
      logger.warn("Log cluster cron failed with error", ex);
      try {
        jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      } catch (SchedulerException e) {
        logger.error("Unable to cleanup cron", e);
      }
    }
  }

  @Override
  public void handle(AnalysisContext analysisContext) {
    if (!managerClientHelper
             .callManagerWithRetry(managerClient.isFeatureEnabled(
                 REMOVE_WORKFLOW_VERIFICATION_CLUSTERING_CRON, analysisContext.getAccountId()))
             .getResource()) {
      logger.info(
          "The feature REMOVE_WORKFLOW_VERIFICATION_CLUSTERING_CRON is not enabled for {}, it will be handled by the cron",
          analysisContext.getAccountId());
      return;
    }
    if (ExecutionStatus.QUEUED == analysisContext.getExecutionStatus()) {
      learningEngineService.markJobStatus(analysisContext, ExecutionStatus.RUNNING);
      analysisContext.replaceUnicodeInControlNodesAndTestNodes();
    }
    logger.info(
        "Handling the clustering for stateExecutionId {} using the iterators", analysisContext.getStateExecutionId());
    new WorkflowLogClusterJob
        .LogClusterTask(analysisService, managerClientHelper, Optional.empty(), analysisContext, learningEngineService,
            managerClient, dataStoreService)
        .run();
  }

  @AllArgsConstructor
  public static class LogClusterTask implements Runnable {
    private LogAnalysisService analysisService;
    private VerificationManagerClientHelper managerClientHelper;
    private Optional<JobExecutionContext> jobExecutionContext;
    private AnalysisContext context;
    private LearningEngineService learningEngineService;
    private VerificationManagerClient managerClient;
    private DataStoreService dataStoreService;

    private Set<String> getCollectedNodes() {
      if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        Set<String> nodes = Sets.newHashSet(context.getControlNodes().keySet());
        nodes.addAll(context.getTestNodes().keySet());
        return nodes;
      } else {
        return Sets.newHashSet(context.getTestNodes().keySet());
      }
    }

    private void cluster() {
      try {
        Set<String> nodes = getCollectedNodes();
        // TODO handle pause
        for (String node : nodes) {
          logger.info("Running cluster task for {}, node {}", context.getStateExecutionId(), node);
          /*
           * Work flow is invalid
           * exit immediately
           */
          if (!learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            logger.info("Log Cluster : State no longer valid. skipping." + context.getStateExecutionId());
            break;
          }
          analysisService
              .getHearbeatRecordForL0(context.getAppId(), context.getStateExecutionId(), context.getStateType(), node)
              .map(log -> {
                // ***
                // *** Process L0 records. ***
                // ***
                boolean hasDataRecords = analysisService.hasDataRecords(log.getQuery(), context.getAppId(),
                    context.getStateExecutionId(), context.getStateType(), Sets.newHashSet(log.getHost()),
                    ClusterLevel.L0, log.getLogCollectionMinute());

                LogRequest logRequest = LogRequest.builder()
                                            .query(log.getQuery())
                                            .applicationId(context.getAppId())
                                            .stateExecutionId(context.getStateExecutionId())
                                            .workflowId(context.getWorkflowId())
                                            .serviceId(context.getServiceId())
                                            .nodes(Collections.singleton(log.getHost()))
                                            .logCollectionMinute(log.getLogCollectionMinute())
                                            .build();

                if (hasDataRecords) {
                  logger.info("Running cluster task for stateExecutionId {}, minute {}, stateType {}, ",
                      context.getStateExecutionId(), logRequest.getLogCollectionMinute(), context.getStateType());
                  if (PER_MINUTE_CV_STATES.contains(context.getStateType())
                      || GA_PER_MINUTE_CV_STATES.contains(context.getStateType())) {
                    new LogMLClusterGenerator(learningEngineService, context.getClusterContext(), ClusterLevel.L0,
                        ClusterLevel.L1, logRequest, (int) context.getStartDataCollectionMinute())
                        .run();
                  } else {
                    new LogMLClusterGenerator(learningEngineService, context.getClusterContext(), ClusterLevel.L0,
                        ClusterLevel.L1, logRequest, 0)
                        .run();
                  }
                  logger.info(" queued cluster task for " + context.getStateExecutionId() + " , minute "
                      + logRequest.getLogCollectionMinute());

                } else {
                  logger.info("Skipping cluster task no data found. for " + context.getStateExecutionId() + " , minute "
                      + logRequest.getLogCollectionMinute());
                  analysisService.bumpClusterLevel(context.getStateType(), context.getStateExecutionId(),
                      context.getAppId(), logRequest.getQuery(), logRequest.getNodes(),
                      logRequest.getLogCollectionMinute(), ClusterLevel.getHeartBeatLevel(ClusterLevel.L0),
                      ClusterLevel.getHeartBeatLevel(ClusterLevel.L0).next());
                }
                return true;
              });
        }
      } catch (Exception ex) {
        logger.info("Verification L0 => L1 cluster failed for {}", context.getStateExecutionId(), ex);
      } finally {
        // Delete cron.
        try {
          if (!learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())
              && jobExecutionContext.isPresent()) {
            jobExecutionContext.get().getScheduler().deleteJob(jobExecutionContext.get().getJobDetail().getKey());
          }
        } catch (SchedulerException e) {
          logger.error("", e);
        }
      }
    }

    @Override
    public void run() {
      try {
        switch (context.getStateType()) {
          case SUMO:
          case ELK:
          case LOGZ:
          case BUG_SNAG:
          case LOG_VERIFICATION:
          case DATA_DOG_LOG:
          case STACK_DRIVER_LOG:
            cluster();
            break;
          case SPLUNKV2:
            if (jobExecutionContext.isPresent()) {
              jobExecutionContext.get().getScheduler().deleteJob(jobExecutionContext.get().getJobDetail().getKey());
            }
            break;
          default:
            if (jobExecutionContext.isPresent()) {
              jobExecutionContext.get().getScheduler().deleteJob(jobExecutionContext.get().getJobDetail().getKey());
              logger.error("Verification invalid state: " + context.getStateType());
            }
        }
      } catch (Exception ex) {
        try {
          logger.error("Verification L0 => L1 cluster failed", ex);
          if (learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            final VerificationStateAnalysisExecutionData executionData =
                VerificationStateAnalysisExecutionData.builder().build();
            executionData.setStatus(ExecutionStatus.ERROR);
            executionData.setErrorMsg(ex.getMessage());
            logger.info(
                "Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
            final VerificationDataAnalysisResponse analysisResponse =
                VerificationDataAnalysisResponse.builder().stateExecutionData(executionData).build();
            analysisResponse.setExecutionStatus(ExecutionStatus.ERROR);
            managerClientHelper.notifyManagerForVerificationAnalysis(context, analysisResponse);
          }
        } catch (Exception e) {
          logger.error("Verification cluster manager cleanup failed", e);
        }
      }
    }
  }
}
