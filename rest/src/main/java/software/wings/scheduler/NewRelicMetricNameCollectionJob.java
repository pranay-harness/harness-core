package software.wings.scheduler;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SearchFilter;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.Workflow;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.impl.newrelic.NewRelicMetricNames.WorkflowInfo;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by sriram_parthasarathy on 12/14/17.
 */
@DisallowConcurrentExecution
public class NewRelicMetricNameCollectionJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicMetricNameCollectionJob.class);

  @Inject private AccountService accountService;
  @Inject private SettingsService settingsService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
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
            metricNames -> (System.currentTimeMillis() - metricNames.getLastUpdatedTime() > TimeUnit.DAYS.toMillis(1)))
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
              String waitId = UUIDGenerator.getUuid();
              logger.info("Scheduling new relic metric name collection task {}", dataCollectionInfo);
              DelegateTask delegateTask = aDelegateTask()
                                              .withTaskType(TaskType.NEWRELIC_COLLECT_METRIC_NAMES)
                                              .withAccountId(newRelicConfig.getAccountId())
                                              .withAppId(workflowInfo.getAppId())
                                              .withParameters(new Object[] {dataCollectionInfo})
                                              .withWaitId(waitId)
                                              .withEnvId(workflowInfo.getEnvId())
                                              .withInfrastructureMappingId(workflowInfo.getInfraMappingId())
                                              .build();
              delegateService.queueTask(delegateTask);
            }
          } catch (Exception ex) {
            logger.error("Unable to schedule new relic task ");
          }
        });
  }
}
