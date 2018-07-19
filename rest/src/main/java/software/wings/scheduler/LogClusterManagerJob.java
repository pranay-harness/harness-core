package software.wings.scheduler;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogMLClusterGenerator;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/24/17.
 */
@DisallowConcurrentExecution
public class LogClusterManagerJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LogAnalysisManagerJob.class);

  @org.simpleframework.xml.Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @org.simpleframework.xml.Transient @Inject private AnalysisService analysisService;

  @org.simpleframework.xml.Transient @Inject private LearningEngineService learningEngineService;

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      long timestamp = jobExecutionContext.getMergedJobDataMap().getLong("timestamp");
      String delegateTaskId = jobExecutionContext.getMergedJobDataMap().getString("delegateTaskId");
      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      new LogClusterTask(analysisService, waitNotifyEngine, jobExecutionContext, context, learningEngineService).run();
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
    private LearningEngineService learningEngineService;

    private Set<String> getCollectedNodes() {
      if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        Set<String> nodes = Sets.newHashSet(context.getControlNodes().keySet());
        nodes.addAll(context.getTestNodes().keySet());
        return nodes;
      } else {
        return Sets.newHashSet(context.getTestNodes().keySet());
      }
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    private void cluster() {
      boolean completeCron = false;
      boolean keepProcessing = true;
      try {
        Set<String> nodes = getCollectedNodes();
        // TODO handle pause
        for (String node : nodes) {
          logger.info("Running cluster task for {}, node {}", context.getStateExecutionId(), node);
          /*
           * Work flow is invalid
           * exit immediately
           */
          if (!analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            logger.info("Log Cluster : State no longer valid. skipping." + context.getStateExecutionId());
            break;
          }
          analysisService
              .getHearbeatRecordForL0(context.getAppId(), context.getStateExecutionId(), context.getStateType(), node)
              .map(log -> {
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
                      learningEngineService, context.getClusterContext(), ClusterLevel.L0, ClusterLevel.L1, logRequest)
                      .run();
                  logger.info(" queued cluster task for " + context.getStateExecutionId() + " , minute "
                      + logRequest.getLogCollectionMinute());

                } else {
                  logger.info(" skipping cluster task no data found. for " + context.getStateExecutionId()
                      + " , minute " + logRequest.getLogCollectionMinute());
                  analysisService.bumpClusterLevel(context.getStateType(), context.getStateExecutionId(),
                      context.getAppId(), logRequest.getQuery(), logRequest.getNodes(),
                      logRequest.getLogCollectionMinute(), ClusterLevel.getHeartBeatLevel(ClusterLevel.L0),
                      ClusterLevel.getHeartBeatLevel(ClusterLevel.L0).next());
                }

                return true;
              })
              .orElse(false);
        }
      } catch (Exception ex) {
        completeCron = true;
        throw new RuntimeException("Verification L0 => L1 cluster failed, " + Misc.getMessage(ex), ex);
      } finally {
        // Delete cron.
        try {
          if (completeCron || !analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
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
            final LogAnalysisExecutionData executionData = LogAnalysisExecutionData.builder().build();
            executionData.setStatus(ExecutionStatus.ERROR);
            executionData.setErrorMsg(ex.getMessage());
            logger.info(
                "Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
            waitNotifyEngine.notify(context.getCorrelationId(),
                aLogAnalysisResponse()
                    .withLogAnalysisExecutionData(executionData)
                    .withExecutionStatus(ExecutionStatus.ERROR)
                    .build());
          }
        } catch (Exception e) {
          logger.error("Verification cluster manager cleanup failed", e);
        }
      }
    }
  }
}
