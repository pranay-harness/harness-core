package io.harness.jobs;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by sriram_parthasarathy on 8/23//"17.
 */

@DisallowConcurrentExecution
@Slf4j
@Deprecated
public class LogAnalysisManagerJob implements Job {
  @Transient @Inject private LogAnalysisService analysisService;

  @Transient @Inject private LearningEngineService learningEngineService;

  @Transient @Inject private VerificationManagerClientHelper managerClientHelper;
  @Transient @Inject private VerificationManagerClient managerClient;

  @Inject private DataStoreService dataStoreService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.warn("Deprecating LogAnalysisManagerJob ...");
  }

  @AllArgsConstructor
  public static class LogAnalysisTask implements Callable<Long> {
    private LogAnalysisService analysisService;

    private AnalysisContext context;
    private JobExecutionContext jobExecutionContext;
    private LearningEngineService learningEngineService;
    private VerificationManagerClient managerClient;
    private VerificationManagerClientHelper managerClientHelper;
    private DataStoreService dataStoreService;

    protected void preProcess(long logAnalysisMinute, String query, Set<String> nodes) {
      if (context.getTestNodes() == null) {
        throw new RuntimeException("Test nodes empty! " + JsonUtils.asJson(context));
      }

      LogRequest logRequest = LogRequest.builder()
                                  .query(query)
                                  .applicationId(context.getAppId())
                                  .stateExecutionId(context.getStateExecutionId())
                                  .workflowId(context.getWorkflowId())
                                  .serviceId(context.getServiceId())
                                  .nodes(nodes)
                                  .logCollectionMinute(logAnalysisMinute)
                                  .build();

      switch (context.getStateType()) {
        case SUMO:
        case DATA_DOG_LOG:
          new LogMLClusterGenerator(learningEngineService, context.getClusterContext(), ClusterLevel.L1,
              ClusterLevel.L2, logRequest, (int) context.getStartDataCollectionMinute())
              .run();
          break;
        case ELK:
        case LOGZ:
        case BUG_SNAG:
        case LOG_VERIFICATION:
          new LogMLClusterGenerator(
              learningEngineService, context.getClusterContext(), ClusterLevel.L1, ClusterLevel.L2, logRequest, 0)
              .run();
          break;
        case SPLUNKV2:
          break;
        default:
          throw new RuntimeException("Unknown verification state " + context.getStateType());
      }
    }

    private Set<String> getCollectedNodes() {
      if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        Set<String> nodes = Sets.newHashSet(context.getControlNodes().keySet());
        nodes.addAll(context.getTestNodes().keySet());
        return nodes;
      } else {
        return Sets.newHashSet(context.getTestNodes().keySet());
      }
    }

    @Override
    public Long call() {
      boolean completeCron = false;
      boolean error = false;
      String errorMsg = "";
      long logAnalysisMinute = -1;
      ThirdPartyApiCallLog apiCallLog = ThirdPartyApiCallLog.builder()
                                            .stateExecutionId(context.getStateExecutionId())
                                            .title("Triggering Log analysis task")
                                            .requestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli())
                                            .build();
      try {
        logger.info("running log ml analysis for " + context.getStateExecutionId());
        /*
         * Work flow is invalid
         * exit immediately
         */
        boolean createExperiment;
        if (!learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
          logger.warn(" log ml analysis : state is not valid " + context.getStateExecutionId());
          return -1L;
        }

        if (analysisService.isProcessingComplete(context.getQuery(), context.getAppId(), context.getStateExecutionId(),
                context.getStateType(), context.getTimeDuration(), context.getStartDataCollectionMinute(),
                context.getAccountId())) {
          completeCron = true;
        } else {
          long logAnalysisClusteringTestMinute =
              analysisService.getCollectionMinuteForLevel(context.getQuery(), context.getAppId(),
                  context.getStateExecutionId(), context.getStateType(), ClusterLevel.L1, getCollectedNodes());
          if (logAnalysisClusteringTestMinute != -1) {
            boolean hasTestRecords =
                analysisService.hasDataRecords(context.getQuery(), context.getAppId(), context.getStateExecutionId(),
                    context.getStateType(), getCollectedNodes(), ClusterLevel.L1, logAnalysisClusteringTestMinute);

            if (hasTestRecords) {
              preProcess(logAnalysisClusteringTestMinute, context.getQuery(), getCollectedNodes());
            } else {
              analysisService.bumpClusterLevel(context.getStateType(), context.getStateExecutionId(),
                  context.getAppId(), context.getQuery(), getCollectedNodes(), logAnalysisClusteringTestMinute,
                  ClusterLevel.getHeartBeatLevel(ClusterLevel.L1), ClusterLevel.getHeartBeatLevel(ClusterLevel.L2));
            }
          }

          logAnalysisMinute = analysisService.getCollectionMinuteForLevel(context.getQuery(), context.getAppId(),
              context.getStateExecutionId(), context.getStateType(), ClusterLevel.L2, getCollectedNodes());
          if (logAnalysisMinute != -1) {
            if (learningEngineService.hasAnalysisTimedOut(
                    context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionId())) {
              learningEngineService.markStatus(context.getWorkflowExecutionId(), context.getStateExecutionId(),
                  logAnalysisMinute, ExecutionStatus.FAILED);
              throw new WingsException("Error running log analysis. Finished all retries. stateExecutionId: "
                  + context.getStateExecutionId());
            }

            /*
             * Run even if we don't have test data, since we may have control data for this minute.
             * If not, then the control data for this minute will be lost forever. If control is present
             * and no test, the control data is processed and added to the result. If test is present, but no control,
             * the test events are saved for future processing.
             */
            createExperiment = logAnalysisMinute >= context.getTimeDuration() - 1;
            new LogMLAnalysisGenerator(context, logAnalysisMinute, createExperiment, analysisService,
                learningEngineService, managerClient, managerClientHelper, MLAnalysisType.LOG_ML)
                .run();

          } else {
            logger.info("No data for log ml analysis " + context.getStateExecutionId());
          }
        }

        // if no data generated till this time, create a dummy summary so UI can get a response
        if (!analysisService.isAnalysisPresent(context.getStateExecutionId(), context.getAppId())) {
          analysisService.createAndSaveSummary(context.getStateType(), context.getAppId(),
              context.getStateExecutionId(), context.getQuery(), "No data found for the given queries.");
        }

        return logAnalysisMinute;
      } catch (Exception ex) {
        completeCron = true;
        error = true;
        errorMsg = ExceptionUtils.getMessage(ex);
        logger.info("Verification Analysis failed for {}", context.getStateExecutionId(), ex);
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        apiCallLog.addFieldToResponse(
            HttpStatus.SC_INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(ex), FieldType.TEXT);
        dataStoreService.save(ThirdPartyApiCallLog.class, Lists.newArrayList(apiCallLog), false);
      } finally {
        try {
          // send notification to state manager and delete cron.
          if (completeCron || !learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            try {
              logger.info(
                  "send notification to state manager and delete cron with error : {} errorMsg : {}", error, errorMsg);
              new LogMLAnalysisGenerator(context, logAnalysisMinute, false, analysisService, learningEngineService,
                  managerClient, managerClientHelper, MLAnalysisType.LOG_ML)
                  .sendStateNotification(context, error, errorMsg, (int) logAnalysisMinute);
            } catch (Exception e) {
              logger.error("Send notification failed for log analysis manager", e);
            } finally {
              try {
                jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
              } catch (Exception e) {
                logger.error("Delete cron failed", e);
              }
            }
          }
        } catch (Exception ex) {
          logger.error("analysis failed", ex);
        }
      }

      return -1L;
    }
  }
}