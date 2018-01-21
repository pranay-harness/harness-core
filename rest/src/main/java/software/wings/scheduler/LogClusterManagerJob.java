package software.wings.scheduler;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import lombok.AllArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogMLClusterGenerator;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;

/**
 * Created by sriram_parthasarathy on 8/24/17.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class LogClusterManagerJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LogAnalysisManagerJob.class);

  @org.simpleframework.xml.Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @org.simpleframework.xml.Transient @Inject private AnalysisService analysisService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      long timestamp = jobExecutionContext.getMergedJobDataMap().getLong("timestamp");
      String delegateTaskId = jobExecutionContext.getMergedJobDataMap().getString("delegateTaskId");
      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      new LogClusterTask(analysisService, waitNotifyEngine, jobExecutionContext, context).run();
    } catch (Exception ex) {
      logger.warn("Log cluster cron failed with error", ex);
      try {
        jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      } catch (SchedulerException e) {
        logger.error("Unable to cleanup cron", e);
      }
    }
  }

  @AllArgsConstructor
  public static class LogClusterTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LogClusterTask.class);
    private AnalysisService analysisService;
    private WaitNotifyEngine waitNotifyEngine;
    private JobExecutionContext jobExecutionContext;
    private AnalysisContext context;

    private void cluster() {
      boolean completeCron = false;
      boolean keepProcessing = true;
      try {
        // TODO handle pause
        while (keepProcessing) {
          logger.info("Running cluster task for " + context.getStateExecutionId());
          /*
           * Work flow is invalid
           * exit immediately
           */
          if (!analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            logger.info("Log Cluster : State no longer valid. skipping." + context.getStateExecutionId());
            break;
          }

          keepProcessing =
              analysisService
                  .getLogDataRecordForL0(context.getAppId(), context.getStateExecutionId(), context.getStateType())
                  .map(log -> {
                    try {
                      /**
                       * Process L0 records.
                       */
                      boolean hasDataRecords = analysisService.hasDataRecords(log.getQuery(), context.getAppId(),
                          context.getStateExecutionId(), context.getStateType(), Sets.newHashSet(log.getHost()),
                          ClusterLevel.L0, log.getLogCollectionMinute());

                      final LogRequest logRequest = new LogRequest(log.getQuery(), context.getAppId(),
                          context.getStateExecutionId(), context.getWorkflowId(), context.getServiceId(),
                          Collections.singleton(log.getHost()), log.getLogCollectionMinute());

                      if (hasDataRecords) {
                        logger.info("Running cluster task for " + context.getStateExecutionId() + " , minute "
                            + logRequest.getLogCollectionMinute());
                        new LogMLClusterGenerator(
                            context.getClusterContext(), ClusterLevel.L0, ClusterLevel.L1, logRequest)
                            .run();

                        analysisService.deleteClusterLevel(context.getStateType(), context.getStateExecutionId(),
                            context.getAppId(), logRequest.getQuery(), logRequest.getNodes(),
                            logRequest.getLogCollectionMinute(), ClusterLevel.L0);
                      } else {
                        logger.info(" skipping cluster task no data found. for " + context.getStateExecutionId()
                            + " , minute " + logRequest.getLogCollectionMinute());
                      }
                      analysisService.bumpClusterLevel(context.getStateType(), context.getStateExecutionId(),
                          context.getAppId(), logRequest.getQuery(), logRequest.getNodes(),
                          logRequest.getLogCollectionMinute(), ClusterLevel.getHeartBeatLevel(ClusterLevel.L0),
                          ClusterLevel.getHeartBeatLevel(ClusterLevel.L0).next());
                      logger.info(" finish cluster task for " + context.getStateExecutionId() + " , minute "
                          + logRequest.getLogCollectionMinute());
                      return true;
                    } catch (Exception ex) {
                      logger.error("Unknown error ", ex);
                      return false;
                    }
                  })
                  .orElse(false);
        }
      } catch (Exception ex) {
        completeCron = true;
        throw new RuntimeException("Verification L0 => L1 cluster failed", ex);
      } finally {
        // Delete cron.
        try {
          if (completeCron || !analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
          }
        } catch (Exception ex) {
          throw new RuntimeException("Verification unable to delete cluster cron", ex);
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
          if (analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            final LogAnalysisExecutionData executionData =
                LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
                    .withStatus(ExecutionStatus.FAILED)
                    .withErrorMsg(ex.getMessage())
                    .build();
            waitNotifyEngine.notify(context.getCorrelationId(),
                aLogAnalysisResponse()
                    .withLogAnalysisExecutionData(executionData)
                    .withExecutionStatus(ExecutionStatus.FAILED)
                    .build());
          }
        } catch (Exception e) {
          logger.error("Verification cluster manager cleanup failed", e);
        }
      }
    }
  }
}
