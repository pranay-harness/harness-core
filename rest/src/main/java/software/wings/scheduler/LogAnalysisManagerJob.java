package software.wings.scheduler;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;

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
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogAnalysisContext;
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
import software.wings.sm.StateType;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class LogAnalysisManagerJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LogAnalysisManagerJob.class);
  private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

  @Transient @Inject private WingsPersistence wingsPersistence;

  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject private AnalysisService analysisService;

  @Transient @Inject private DelegateService delegateService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      long timestamp = jobExecutionContext.getMergedJobDataMap().getLong("timestamp");
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      String delegateTaskId = jobExecutionContext.getMergedJobDataMap().getString("delegateTaskId");

      LogAnalysisContext context = LogAnalysisContext.fromJson(params);
      if (!LogAnalysisTask.stateExecutionLocks.contains(context.getStateExecutionInstanceId())) {
        UUID id = UUID.randomUUID();
        if (LogAnalysisTask.stateExecutionLocks.putIfAbsent(context.getStateExecutionInstanceId(), id) == null) {
          // TODO unbounded task queue
          executorService.submit(new LogAnalysisTask(wingsPersistence, analysisService, waitNotifyEngine,
              delegateService, context, jobExecutionContext, delegateTaskId, id));
        }
      }
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
    // TODO create apis around this
    public static final ConcurrentHashMap<String, UUID> stateExecutionLocks = new ConcurrentHashMap<>();

    private WingsPersistence wingsPersistence;
    private AnalysisService analysisService;
    private WaitNotifyEngine waitNotifyEngine;
    private DelegateService delegateService;

    private LogAnalysisContext context;
    private JobExecutionContext jobExecutionContext;
    private String delegateTaskId;
    private UUID uuid;

    protected void preProcess(int logAnalysisMinute, String query) {
      if (context.getTestNodes() == null) {
        throw new RuntimeException("Test nodes empty! " + context.toJson());
      }

      LogRequest logRequest = new LogRequest(query, context.getAppId(), context.getStateExecutionInstanceId(),
          context.getWorkflowId(), context.getServiceId(), context.getTestNodes(), logAnalysisMinute);

      switch (StateType.valueOf(context.getType())) {
        case ELK:
        case LOGZ:
          new LogMLClusterGenerator(context.getClusterContext(), ClusterLevel.L1, ClusterLevel.L2, logRequest).run();
          analysisService.deleteClusterLevel(context.getType(), context.getStateExecutionInstanceId(),
              context.getAppId(), logRequest.getQuery(), logRequest.getNodes(), logRequest.getLogCollectionMinute(),
              ClusterLevel.L1);
          break;
        case SPLUNKV2:
          analysisService.bumpClusterLevel(context.getType(), context.getStateExecutionInstanceId(), context.getAppId(),
              logRequest.getQuery(), logRequest.getNodes(), logRequest.getLogCollectionMinute(), ClusterLevel.L1,
              ClusterLevel.L2);
          break;
        default:
          throw new RuntimeException("Unknown verification state " + context.getType());
      }
    }

    @Override
    public void run() {
      boolean completeCron = false;

      try {
        UUID uuid = stateExecutionLocks.get(context.getStateExecutionInstanceId());
        if (!uuid.equals(this.uuid)) {
          logger.error(" UUIDs dont match " + context.toJson());
          return;
        }
        /*
         * Work flow is invalid
         * exit immediately
         */
        if (!analysisService.isStateValid(context.getAppId(), context.getStateExecutionInstanceId())) {
          return;
        }

        // TODO support multiple queries
        if (analysisService.isProcessingComplete(context.getQueries().iterator().next(), context.getAppId(),
                context.getStateExecutionInstanceId(), context.getType(),
                Integer.parseInt(context.getTimeDuration()))) {
          completeCron = true;
        } else {
          // TODO support multiple queries
          int logAnalysisMinute = analysisService.getCollectionMinuteForL1(context.getQueries().iterator().next(),
              context.getAppId(), context.getStateExecutionInstanceId(), context.getType(), context.getTestNodes());
          if (logAnalysisMinute != -1) {
            boolean hasRecords = analysisService.hasDataRecords(context.getQueries().iterator().next(),
                context.getAppId(), context.getStateExecutionInstanceId(), context.getType(), context.getTestNodes(),
                ClusterLevel.L1, logAnalysisMinute);

            if (hasRecords) {
              preProcess(logAnalysisMinute, context.getQueries().iterator().next());
              new LogMLAnalysisGenerator(wingsPersistence, context, logAnalysisMinute, analysisService, null).run();
            }
            analysisService.bumpClusterLevel(context.getType(), context.getStateExecutionInstanceId(),
                context.getAppId(), context.getQueries().iterator().next(), context.getTestNodes(), logAnalysisMinute,
                ClusterLevel.getHeartBeatLevel(ClusterLevel.L1), ClusterLevel.getFinal());
          }
        }

        // if no data generated till this time, create a dummy summary so UI can get a response
        final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
            context.getStateExecutionInstanceId(), context.getAppId(), StateType.valueOf(context.getType()));
        if (analysisSummary == null) {
          analysisService.createAndSaveSummary(context.getType(), context.getAppId(),
              context.getStateExecutionInstanceId(), StringUtils.join(context.getQueries(), ","),
              "No data found for the given queries yet. Check if the load is running");
        }

      } catch (Exception ex) {
        completeCron = true;
        logger.warn("analysis failed", ex);
      } finally {
        try {
          stateExecutionLocks.remove(context.getStateExecutionInstanceId());
          // send notification to state manager and delete cron.
          if (completeCron
              || !analysisService.isStateValid(context.getAppId(), context.getStateExecutionInstanceId())) {
            try {
              delegateService.abortTask(context.getAccountId(), delegateTaskId);
              sendStateNotification(context);
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

    private void sendStateNotification(LogAnalysisContext context) {
      final LogAnalysisExecutionData executionData =
          LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
              .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
              .withServerConfigID(context.getAnalysisServerConfigId())
              .withQueries(context.getQueries())
              .withAnalysisDuration(Integer.parseInt(context.getTimeDuration()))
              .withStatus(ExecutionStatus.SUCCESS)
              .withCanaryNewHostNames(context.getTestNodes())
              .withLastExecutionNodes(context.getControlNodes() == null ? new HashSet<>() : context.getControlNodes())
              .withCorrelationId(context.getCorrelationId())
              .build();
      final LogAnalysisResponse response = aLogAnalysisResponse()
                                               .withLogAnalysisExecutionData(executionData)
                                               .withExecutionStatus(ExecutionStatus.SUCCESS)
                                               .build();
      waitNotifyEngine.notify(response.getLogAnalysisExecutionData().getCorrelationId(), response);
    }
  }
}
