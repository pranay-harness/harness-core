package io.harness.jobs;

import static software.wings.common.VerificationConstants.GA_PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.PER_MINUTE_CV_STATES;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/24/17.
 */
@Deprecated
@DisallowConcurrentExecution
@Slf4j
public class LogClusterManagerJob implements Job {
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Inject private VerificationManagerClient managerClient;

  @Inject private LogAnalysisService analysisService;

  @Inject private LearningEngineService learningEngineService;

  @Inject private DataStoreService dataStoreService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.warn("Deprecating LogClusterManagerJob ...");
  }

  @AllArgsConstructor
  public static class LogClusterTask implements Runnable {
    private LogAnalysisService analysisService;
    private VerificationManagerClientHelper managerClientHelper;
    private JobExecutionContext jobExecutionContext;
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
      ThirdPartyApiCallLog apiCallLog = ThirdPartyApiCallLog.builder()
                                            .stateExecutionId(context.getStateExecutionId())
                                            .title("Triggering L0->L1 clustering task")
                                            .requestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli())
                                            .build();

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

                final LogRequest logRequest = LogRequest.builder()
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
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        apiCallLog.addFieldToResponse(
            HttpStatus.SC_INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(ex), FieldType.TEXT);
        dataStoreService.save(ThirdPartyApiCallLog.class, Lists.newArrayList(apiCallLog), false);
      } finally {
        // Delete cron.
        try {
          if (!learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
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
            cluster();
            break;
          case SPLUNKV2:
            jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
            break;
          default:
            jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
            logger.error("Verification invalid state: " + context.getStateType());
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
