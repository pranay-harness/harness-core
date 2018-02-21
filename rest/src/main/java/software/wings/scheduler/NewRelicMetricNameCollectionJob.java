package software.wings.scheduler;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.common.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.TaskType;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.impl.newrelic.NewRelicMetricNames.WorkflowInfo;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by sriram_parthasarathy on 12/14/17.
 */
@DisallowConcurrentExecution
public class NewRelicMetricNameCollectionJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicMetricNameCollectionJob.class);
  private static final int DEFAULT_NEWRELIC_COLLECTION_TIMEOUT_MINS = 60;
  @Inject private SettingsService settingsService;
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;
  @Inject protected WaitNotifyEngine waitNotifyEngine;

  @Override
  public void execute(JobExecutionContext context) {
    List<NewRelicMetricNames> newRelicMetricNamesList = metricDataAnalysisService.listMetricNamesWithWorkflows();
    if (newRelicMetricNamesList == null) {
      logger.info("Skipping batch new relic collection. Nothing to collect");
      return;
    }

    DelegateCallbackHandler delegateCallbackHandler = new DelegateCallbackHandler();
    Map<String, NewRelicConfig> newRelicAppToConfigMap = new HashMap<>();

    newRelicMetricNamesList.stream()
        .filter(metricNames
            -> !newRelicAppToConfigMap.containsKey(
                metricNames.getNewRelicAppId() + "-" + metricNames.getNewRelicConfigId()))
        .filter(
            metricNames -> System.currentTimeMillis() - metricNames.getLastUpdatedTime() > TimeUnit.DAYS.toMillis(1))
        .forEach(metricNames -> {
          try {
            for (WorkflowInfo workflowInfo : metricNames.getRegisteredWorkflows()) {
              // TODO validate and cleanup stale records
              SettingValue settingAttribute =
                  settingsService
                      .getGlobalSettingAttributesById(workflowInfo.getAccountId(), metricNames.getNewRelicConfigId())
                      .getValue();
              if (settingAttribute == null) {
                logger.warn("No NewRelic connector found for account {} , NewRelic server config id {}",
                    workflowInfo.getAccountId(), metricNames.getNewRelicConfigId());
                continue;
              }
              NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute;

              if (newRelicAppToConfigMap.containsKey(
                      metricNames.getNewRelicAppId() + "-" + metricNames.getNewRelicConfigId())) {
                logger.info(
                    "Skipping NewRelic metric names collection for NewRelic app id {}, NewRelic server config id {} ",
                    metricNames.getNewRelicAppId(), metricNames.getNewRelicConfigId());
                continue;
              }
              newRelicAppToConfigMap.put(
                  metricNames.getNewRelicAppId() + "-" + metricNames.getNewRelicConfigId(), newRelicConfig);
              NewRelicDataCollectionInfo dataCollectionInfo =
                  NewRelicDataCollectionInfo.builder()
                      .newRelicConfig(newRelicConfig)
                      .newRelicAppId(Long.parseLong(metricNames.getNewRelicAppId()))
                      .encryptedDataDetails(
                          secretManager.getEncryptionDetails(newRelicConfig, workflowInfo.getAppId(), ""))
                      .settingAttributeId(metricNames.getNewRelicConfigId())
                      .build();
              logger.info("Scheduling new relic metric name collection task {}", dataCollectionInfo);
              if (System.currentTimeMillis() - metricNames.getLastUpdatedTime()
                  > (TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(6))) {
                logger.error("[learning-engine] NewRelic metric name collection task past due over 6 hours {} ",
                    dataCollectionInfo);
              }
              String waitId = generateUuid();
              DelegateTask delegateTask =
                  aDelegateTask()
                      .withTaskType(TaskType.NEWRELIC_COLLECT_METRIC_NAMES)
                      .withAccountId(newRelicConfig.getAccountId())
                      .withAppId(workflowInfo.getAppId())
                      .withParameters(new Object[] {dataCollectionInfo})
                      .withEnvId(workflowInfo.getEnvId())
                      .withInfrastructureMappingId(workflowInfo.getInfraMappingId())
                      .withTimeout(TimeUnit.MINUTES.toMillis(DEFAULT_NEWRELIC_COLLECTION_TIMEOUT_MINS))
                      .withWaitId(waitId)
                      .build();

              delegateService.queueTask(delegateTask);

              waitNotifyEngine.waitForAll(delegateCallbackHandler, waitId);
            }
          } catch (Exception ex) {
            logger.error("Unable to schedule new relic task", ex);
          }
        });
  }

  private static class DelegateCallbackHandler implements NotifyCallback {
    @Override
    public void notify(Map<String, NotifyResponseData> response) {
      final DataCollectionTaskResult result = (DataCollectionTaskResult) response.values().iterator().next();
      if (result.getStatus() == DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE) {
        logger.warn("NewRelic metric name collection task failed {} ", result.getErrorMessage());
      }
    }

    @Override
    public void notifyError(Map<String, NotifyResponseData> response) {}
  }
}
