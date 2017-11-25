package software.wings.scheduler;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisGenerator;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterGenerator;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class LogAnalysisManagerJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LogAnalysisManagerJob.class);

  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject private AnalysisService analysisService;

  @Transient @Inject private DelegateService delegateService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      long timestamp = jobExecutionContext.getMergedJobDataMap().getLong("timestamp");
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      String delegateTaskId = jobExecutionContext.getMergedJobDataMap().getString("delegateTaskId");

      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      logger.info("Starting log analysis cron " + JsonUtils.asJson(context));
      new LogAnalysisTask(
          analysisService, waitNotifyEngine, delegateService, context, jobExecutionContext, delegateTaskId)
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

    protected void preProcess(int logAnalysisMinute, String query) {
      if (context.getTestNodes() == null) {
        throw new RuntimeException("Test nodes empty! " + JsonUtils.asJson(context));
      }

      LogRequest logRequest = new LogRequest(query, context.getAppId(), context.getStateExecutionId(),
          context.getWorkflowId(), context.getServiceId(), context.getTestNodes(), logAnalysisMinute);

      switch (context.getStateType()) {
        case SUMO:
        case ELK:
        case LOGZ:
          new LogMLClusterGenerator(context.getClusterContext(), ClusterLevel.L1, ClusterLevel.L2, logRequest).run();
          analysisService.deleteClusterLevel(context.getStateType(), context.getStateExecutionId(), context.getAppId(),
              logRequest.getQuery(), logRequest.getNodes(), logRequest.getLogCollectionMinute(), ClusterLevel.L1);
          break;
        case SPLUNKV2:
          analysisService.bumpClusterLevel(context.getStateType(), context.getStateExecutionId(), context.getAppId(),
              logRequest.getQuery(), logRequest.getNodes(), logRequest.getLogCollectionMinute(), ClusterLevel.L1,
              ClusterLevel.L2);
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
      try {
        logger.info("running log ml analysis for " + context.getStateExecutionId());
        /*
         * Work flow is invalid
         * exit immediately
         */
        if (!analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
          logger.warn(" log ml analysis : state is not valid " + context.getStateExecutionId());
          return;
        }

        // TODO support multiple queries
        if (analysisService.isProcessingComplete(context.getQueries().iterator().next(), context.getAppId(),
                context.getStateExecutionId(), context.getStateType(), context.getTimeDuration())) {
          completeCron = true;
        } else {
          // TODO support multiple queries
          int logAnalysisMinute = analysisService.getCollectionMinuteForL1(context.getQueries().iterator().next(),
              context.getAppId(), context.getStateExecutionId(), context.getStateType(), getCollectedNodes());
          if (logAnalysisMinute != -1) {
            boolean hasTestRecords = analysisService.hasDataRecords(context.getQueries().iterator().next(),
                context.getAppId(), context.getStateExecutionId(), context.getStateType(), context.getTestNodes(),
                ClusterLevel.L1, logAnalysisMinute);

            if (hasTestRecords) {
              preProcess(logAnalysisMinute, context.getQueries().iterator().next());
            }
            /*
             * Run even if we don't have test data, since we may have control data for this minute.
             * If not, then the control data for this minute will be lost forever. The analysis job
             * should fail with error code - 200 if no control and no test data is provided. The manager
             * should ignore failures with status code - 200. If control is present and no test, the control data
             * is processed and added to the result. If test is present, but no control, the test events
             * are saved for future processing.
             */
            new LogMLAnalysisGenerator(context, logAnalysisMinute, analysisService).run();
            analysisService.bumpClusterLevel(context.getStateType(), context.getStateExecutionId(), context.getAppId(),
                context.getQueries().iterator().next(), context.getTestNodes(), logAnalysisMinute,
                ClusterLevel.getHeartBeatLevel(ClusterLevel.L1), ClusterLevel.getFinal());
          } else {
            logger.warn("No data for log ml analysis " + context.getStateExecutionId());
          }
        }

        // if no data generated till this time, create a dummy summary so UI can get a response
        final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
            context.getStateExecutionId(), context.getAppId(), context.getStateType());
        if (analysisSummary == null) {
          analysisService.createAndSaveSummary(context.getStateType(), context.getAppId(),
              context.getStateExecutionId(), StringUtils.join(context.getQueries(), ","),
              "No data found for the given queries.");
        }

      } catch (Exception ex) {
        completeCron = true;
        error = true;
        logger.warn("analysis failed", ex);
      } finally {
        try {
          // send notification to state manager and delete cron.
          if (completeCron || !analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            try {
              for (String id : delegateTaskId.split(",")) {
                try {
                  delegateService.abortTask(context.getAccountId(), delegateTaskId);
                } catch (Exception e) {
                  logger.error("Delegate abort failed for log analysis manager for delegate task id " + id, e);
                }
              }
              sendStateNotification(context, error);
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

    private void sendStateNotification(AnalysisContext context, boolean error) {
      final ExecutionStatus status = error ? ExecutionStatus.FAILED : ExecutionStatus.SUCCESS;
      final LogAnalysisExecutionData executionData =
          LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
              .withStateExecutionInstanceId(context.getStateExecutionId())
              .withServerConfigID(context.getAnalysisServerConfigId())
              .withQueries(context.getQueries())
              .withAnalysisDuration(context.getTimeDuration())
              .withStatus(status)
              .withCanaryNewHostNames(context.getTestNodes())
              .withLastExecutionNodes(context.getControlNodes() == null ? new HashSet<>() : context.getControlNodes())
              .withCorrelationId(context.getCorrelationId())
              .build();
      final LogAnalysisResponse response =
          aLogAnalysisResponse().withLogAnalysisExecutionData(executionData).withExecutionStatus(status).build();
      waitNotifyEngine.notify(response.getLogAnalysisExecutionData().getCorrelationId(), response);
    }
  }
}
