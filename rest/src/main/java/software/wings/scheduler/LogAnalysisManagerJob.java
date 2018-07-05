package software.wings.scheduler;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import lombok.AllArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisGenerator;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterGenerator;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */

@DisallowConcurrentExecution
public class LogAnalysisManagerJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LogAnalysisManagerJob.class);

  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject private AnalysisService analysisService;

  @Transient @Inject private DelegateService delegateService;

  @Transient @Inject private LearningEngineService learningEngineService;

  @Transient @Inject private FeatureFlagService featureFlagService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      String delegateTaskId = jobExecutionContext.getMergedJobDataMap().getString("delegateTaskId");

      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      logger.info("Starting log analysis cron " + JsonUtils.asJson(context));
      new LogAnalysisTask(analysisService, waitNotifyEngine, delegateService, context, jobExecutionContext,
          delegateTaskId, learningEngineService, featureFlagService)
          .run();
      logger.info("Finish log analysis cron " + context.getStateExecutionId());
    } catch (Exception ex) {
      logger.warn("Log analysis cron failed with error", ex);
      try {
        jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      } catch (SchedulerException e) {
        logger.error("Unable to clean up cron", e);
      }
    }
  }

  @AllArgsConstructor
  public static class LogAnalysisTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LogAnalysisTask.class);
    private AnalysisService analysisService;
    private WaitNotifyEngine waitNotifyEngine;
    private DelegateService delegateService;

    private AnalysisContext context;
    private JobExecutionContext jobExecutionContext;
    private String delegateTaskId;
    private LearningEngineService learningEngineService;
    private FeatureFlagService featureFlagService;

    protected void preProcess(int logAnalysisMinute, String query, Set<String> nodes) {
      if (context.getTestNodes() == null) {
        throw new RuntimeException("Test nodes empty! " + JsonUtils.asJson(context));
      }

      LogRequest logRequest = new LogRequest(query, context.getAppId(), context.getStateExecutionId(),
          context.getWorkflowId(), context.getServiceId(), nodes, logAnalysisMinute);

      switch (context.getStateType()) {
        case SUMO:
        case ELK:
        case LOGZ:
          new LogMLClusterGenerator(
              learningEngineService, context.getClusterContext(), ClusterLevel.L1, ClusterLevel.L2, logRequest)
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
        Set<String> nodes = Sets.newHashSet(context.getControlNodes());
        nodes.addAll(context.getTestNodes());
        return nodes;
      } else {
        return Sets.newHashSet(context.getTestNodes());
      }
    }

    @Override
    public void run() {
      boolean completeCron = false;
      boolean error = false;
      String errorMsg = "";
      try {
        logger.info("running log ml analysis for " + context.getStateExecutionId());
        /*
         * Work flow is invalid
         * exit immediately
         */
        boolean createExperiment = false;
        if (!analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
          logger.warn(" log ml analysis : state is not valid " + context.getStateExecutionId());
          return;
        }

        if (analysisService.isProcessingComplete(context.getQuery(), context.getAppId(), context.getStateExecutionId(),
                context.getStateType(), context.getTimeDuration())) {
          completeCron = true;
        } else {
          int logAnalysisClusteringTestMinute =
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

          int logAnalysisMinute = analysisService.getCollectionMinuteForLevel(context.getQuery(), context.getAppId(),
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
                learningEngineService, featureFlagService)
                .run();

          } else {
            logger.info("No data for log ml analysis " + context.getStateExecutionId());
          }
        }

        // if no data generated till this time, create a dummy summary so UI can get a response
        final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
            context.getStateExecutionId(), context.getAppId(), context.getStateType());
        if (analysisSummary == null) {
          analysisService.createAndSaveSummary(context.getStateType(), context.getAppId(),
              context.getStateExecutionId(), context.getQuery(), "No data found for the given queries.");
        }

      } catch (Exception ex) {
        completeCron = true;
        error = true;
        errorMsg = ex.getMessage();
        logger.warn("analysis failed", ex);
      } finally {
        try {
          // send notification to state manager and delete cron.
          if (completeCron || !analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            try {
              for (String id : delegateTaskId.split(",")) {
                try {
                  logger.info("aborting delegate task {}, for state execution {}", id, context.getStateExecutionId());
                  delegateService.abortTask(context.getAccountId(), id);
                } catch (Exception e) {
                  logger.error("Delegate abort failed for log analysis manager for delegate task id " + id, e);
                }
              }
              sendStateNotification(context, error, errorMsg);
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
    }

    private void sendStateNotification(AnalysisContext context, boolean error, String errorMsg) {
      if (analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
        final ExecutionStatus status = error ? ExecutionStatus.ERROR : ExecutionStatus.SUCCESS;

        LogAnalysisExecutionData logAnalysisExecutionData =
            LogAnalysisExecutionData.builder()
                .stateExecutionInstanceId(context.getStateExecutionId())
                .serverConfigId(context.getAnalysisServerConfigId())
                .query(context.getQuery())
                .timeDuration(context.getTimeDuration())
                .canaryNewHostNames(context.getTestNodes())
                .lastExecutionNodes(context.getControlNodes() == null ? new HashSet<>() : context.getControlNodes())
                .correlationId(context.getCorrelationId())
                .build();

        logAnalysisExecutionData.setStatus(status);

        if (error) {
          logAnalysisExecutionData.setErrorMsg(errorMsg);
        }

        final LogAnalysisResponse response = aLogAnalysisResponse()
                                                 .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                                 .withExecutionStatus(status)
                                                 .build();
        logger.info("Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
        waitNotifyEngine.notify(response.getLogAnalysisExecutionData().getCorrelationId(), response);
      }
    }
  }
}
