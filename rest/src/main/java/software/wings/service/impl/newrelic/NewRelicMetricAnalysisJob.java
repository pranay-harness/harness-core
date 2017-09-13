package software.wings.service.impl.newrelic;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.delegatetasks.NewRelicDataCollectionTask;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.MetricDefinition.Threshold;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/11/17.
 */
public class NewRelicMetricAnalysisJob implements Job {
  private static final ConcurrentHashMap<String, UUID> stateExecutionLocks = new ConcurrentHashMap<>();
  private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

  @Inject private NewRelicService newRelicService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject private DelegateService delegateService;

  private static final Logger logger = LoggerFactory.getLogger(NewRelicMetricAnalysisJob.class);
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      long timestamp = jobExecutionContext.getMergedJobDataMap().getLong("timestamp");
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      String delegateTaskId = jobExecutionContext.getMergedJobDataMap().getString("delegateTaskId");

      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      if (!stateExecutionLocks.contains(context.getStateExecutionId())) {
        UUID id = UUID.randomUUID();
        if (stateExecutionLocks.putIfAbsent(context.getStateExecutionId(), id) == null) {
          // TODO unbounded task queue
          executorService.submit(new NewRelicAnalysisGenerator(context, jobExecutionContext, delegateTaskId, id));
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

  private class NewRelicAnalysisGenerator implements Runnable {
    private final AnalysisContext context;
    private final JobExecutionContext jobExecutionContext;
    private final String delegateTaskId;
    private final UUID uuid;

    private NewRelicAnalysisGenerator(
        AnalysisContext context, JobExecutionContext jobExecutionContext, String delegateTaskId, UUID uuid) {
      this.context = context;
      this.jobExecutionContext = jobExecutionContext;
      this.delegateTaskId = delegateTaskId;
      this.uuid = uuid;
    }

    @Override
    public void run() {
      boolean completeCron = false;
      UUID uuid = stateExecutionLocks.get(context.getStateExecutionId());
      if (!uuid.equals(this.uuid)) {
        logger.error(" UUIDs dont match " + JsonUtils.asJson(context));
        return;
      }

      try {
        /**
         * Work flow is invalid
         * exit immediately
         **/
        if (!newRelicService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
          completeCron = true;
          return;
        }

        final int analysisMinute = newRelicService.getCollectionMinuteToProcess(
            context.getStateExecutionId(), context.getWorkflowExecutionId(), context.getServiceId());

        if (analysisMinute > context.getTimeDuration() - 1) {
          logger.info("newrelic analysis finished after running for {minutes}", analysisMinute);
          completeCron = true;
          return;
        }
        logger.info("running new relic analysis for minute {}", analysisMinute);
        final List<NewRelicMetricDataRecord> controlRecords =
            context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS
            ? newRelicService.getPreviousSuccessfulRecords(
                  context.getWorkflowId(), context.getServiceId(), analysisMinute)
            : newRelicService.getRecords(context.getWorkflowExecutionId(), context.getStateExecutionId(),
                  context.getWorkflowId(), context.getServiceId(), context.getControlNodes(), analysisMinute);

        final List<NewRelicMetricDataRecord> testRecords =
            newRelicService.getRecords(context.getWorkflowExecutionId(), context.getStateExecutionId(),
                context.getWorkflowId(), context.getServiceId(), context.getTestNodes(), analysisMinute);

        Map<String, List<NewRelicMetricDataRecord>> controlRecordsByMetric = splitMetricsByName(controlRecords);
        Map<String, List<NewRelicMetricDataRecord>> testRecordsByMetric = splitMetricsByName(testRecords);

        NewRelicMetricAnalysisRecord analysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                          .stateExecutionId(context.getStateExecutionId())
                                                          .workflowExecutionId(context.getWorkflowExecutionId())
                                                          .workflowId(context.getWorkflowId())
                                                          .applicationId(context.getAppId())
                                                          .riskLevel(RiskLevel.LOW)
                                                          .metricAnalyses(new ArrayList<>())
                                                          .build();

        for (Entry<String, List<NewRelicMetricDataRecord>> metric : testRecordsByMetric.entrySet()) {
          final String metricName = metric.getKey();
          NewRelicMetricAnalysis metricAnalysis = NewRelicMetricAnalysis.builder()
                                                      .metricName(metricName)
                                                      .riskLevel(RiskLevel.LOW)
                                                      .metricValues(new ArrayList<>())
                                                      .build();

          for (Entry<String, List<Threshold>> valuesToAnalyze :
              NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
            NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                      .metricName(metricName)
                                                                      .metricValueName(valuesToAnalyze.getKey())
                                                                      .thresholds(valuesToAnalyze.getValue())
                                                                      .build();

            NewRelicMetricAnalysisValue metricAnalysisValue =
                metricValueDefinition.analyze(metric.getValue(), controlRecordsByMetric.get(metricName));
            metricAnalysis.addNewRelicMetricAnalysisValue(metricAnalysisValue);

            if (metricAnalysisValue.getRiskLevel().compareTo(metricAnalysis.getRiskLevel()) < 0) {
              metricAnalysis.setRiskLevel(metricAnalysisValue.getRiskLevel());
            }

            if (metricAnalysisValue.getRiskLevel().compareTo(analysisRecord.getRiskLevel()) < 0) {
              analysisRecord.setRiskLevel(metricAnalysisValue.getRiskLevel());
            }
          }
          analysisRecord.addNewRelicMetricAnalysis(metricAnalysis);
        }

        analysisRecord.setAnalysisMinute(analysisMinute);
        newRelicService.saveAnalysisRecords(analysisRecord);
        newRelicService.bumpCollectionMinuteToProcess(
            context.getStateExecutionId(), context.getWorkflowExecutionId(), context.getServiceId(), analysisMinute);
      } catch (Exception ex) {
        completeCron = true;
        logger.warn("analysis failed", ex);
      } finally {
        try {
          stateExecutionLocks.remove(context.getStateExecutionId());
          // send notification to state manager and delete cron.
          if (completeCron || !newRelicService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            try {
              delegateService.abortTask(context.getAccountId(), delegateTaskId);
              sendStateNotification(context);
            } catch (Exception e) {
              logger.error("Send notification failed for new relic analysis manager", e);
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

    private Map<String, List<NewRelicMetricDataRecord>> splitMetricsByName(List<NewRelicMetricDataRecord> records) {
      final Map<String, List<NewRelicMetricDataRecord>> rv = new HashMap<>();
      for (NewRelicMetricDataRecord record : records) {
        if (record.getName().equals(NewRelicDataCollectionTask.HARNESS_HEARTEAT_METRIC_NAME)) {
          continue;
        }
        if (!rv.containsKey(record.getName())) {
          rv.put(record.getName(), new ArrayList<>());
        }

        rv.get(record.getName()).add(record);
      }

      return rv;
    }

    private void sendStateNotification(AnalysisContext context) {
      final NewRelicExecutionData executionData =
          NewRelicExecutionData.Builder.anAnanlysisExecutionData()
              .withStateExecutionInstanceId(context.getStateExecutionId())
              .withServerConfigID(context.getAnalysisServerConfigId())
              .withAnalysisDuration(context.getTimeDuration())
              .withStatus(ExecutionStatus.SUCCESS)
              .withCanaryNewHostNames(context.getTestNodes())
              .withLastExecutionNodes(context.getControlNodes() == null ? new HashSet<>() : context.getControlNodes())
              .withCorrelationId(context.getCorrelationId())
              .build();
      final MetricDataAnalysisResponse response =
          MetricDataAnalysisResponse.builder().stateExecutionData(executionData).build();
      response.setExecutionStatus(ExecutionStatus.SUCCESS);
      waitNotifyEngine.notify(context.getCorrelationId(), response);
    }
  }
}
