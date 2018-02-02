package software.wings.scheduler;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.TaskType;
import software.wings.common.UUIDGenerator;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.impl.newrelic.NewRelicMetricNames.WorkflowInfo;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
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
              NewRelicConfig newRelicConfig =
                  (NewRelicConfig) settingsService
                      .getGlobalSettingAttributesById(workflowInfo.getAccountId(), metricNames.getNewRelicConfigId())
                      .getValue();
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
              String waitId = UUIDGenerator.getUuid();
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
              waitNotifyEngine.waitForAll(new NotifyCallback() {
                @Override
                public void notify(Map<String, NotifyResponseData> response) {
                  // TODO implement this
                }

                @Override
                public void notifyError(Map<String, NotifyResponseData> response) {}
              }, waitId);

              delegateService.queueTask(delegateTask);
            }
          } catch (Exception ex) {
            logger.error("Unable to schedule new relic task", ex);
          }
        });
  }
}
