package software.wings.scheduler;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.common.UUIDGenerator.generateUuid;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.beans.Account;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.DelegateTask;
import software.wings.beans.Graph;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SearchFilter;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.Workflow;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
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
 * Created by sriram_parthasarathy on 12/13/17.
 */
public class NewRelicMetricCollectionJobIntegrationTest extends BaseIntegrationTest {
  @Inject private AccountService accountService;
  @Inject private SettingsService settingsService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  //  @Override
  //  public void execute(JobExecutionContext context) throws JobExecutionException {
  //    List<Account> accounts =
  //    accountService.list(aPageRequest().withLimit(PageRequest.UNLIMITED).addFieldsIncluded("uuid").build());
  //
  //    Map<String, NewRelicState> newRelicAppToConfigMap = new HashMap<>();
  //    accounts.stream()
  //        .map(account -> settingsService.getGlobalSettingAttributesByType(account.getUuid(),
  //        StateType.NEW_RELIC.getName())) .filter(settingAttributeList -> !settingAttributeList.isEmpty())
  //        .forEach(settingAttributeList ->
  //          appService.getAppsByAccountId(settingAttributeList.get(0).getAccountId()).stream()
  //            .map(appId -> workflowService.listWorkflows(aPageRequest().withLimit(UNLIMITED)
  //            .addFilter("appId", SearchFilter.Operator.EQ, appId).build()).getResponse())
  //            .flatMap(list -> list.stream())
  //            .map(workflow -> workflowService.readLatestStateMachine(((Workflow) workflow).getAppId(), ((Workflow)
  //            workflow).getUuid())) .map(sm -> sm.getStates()) .flatMap(states -> states.stream()) .filter(state
  //            -> state instanceof NewRelicState) .map(state -> (NewRelicState) state) .forEach(newRelicState ->
  //            newRelicAppToConfigMap.put(newRelicState.getApplicationId(), newRelicState)));
  //
  //
  //  }

  public void migrate() {
    List<Account> accounts =
        accountService.list(aPageRequest().withLimit(PageRequest.UNLIMITED).addFieldsIncluded("uuid").build());
    Map<Object, Map<String, Object>> newRelicAppToConfigMap = new HashMap<>();
    Map<String, SettingAttribute> settingsServiceMap = new HashMap<>();

    accounts.stream()
        .map(account -> settingsService.getGlobalSettingAttributesByType(account.getUuid(), StateType.NEW_RELIC.name()))
        .filter(settingAttributeList -> !settingAttributeList.isEmpty())
        .forEach(settingAttributeList -> {
          settingAttributeList.stream().forEach(
              settingAttribute -> settingsServiceMap.put(settingAttribute.getUuid(), settingAttribute));

          appService.getAppsByAccountId(settingAttributeList.get(0).getAccountId())
              .stream()
              .map(app
                  -> workflowService
                         .listWorkflows(aPageRequest()
                                            .withLimit(UNLIMITED)
                                            .addFilter("appId", SearchFilter.Operator.EQ, app.getUuid())
                                            .build())
                         .getResponse())
              .flatMap(Collection::stream)
              .map(workflow -> (Workflow) workflow)
              .forEach(workflow
                  -> Stream.of(((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases())
                         .flatMap(Collection::stream)
                         .map(phase -> phase.getPhaseSteps())
                         .flatMap(Collection::stream)
                         .map(step -> step.getSteps())
                         .flatMap(Collection::stream)
                         .filter(node -> node.getType().equals(StateType.NEW_RELIC.name()))
                         .forEach(node -> {
                           // TODO get the property keys using refelction on NewRelicState
                           if (!newRelicAppToConfigMap.containsKey(node.getProperties().get("applicationId") + "-"
                                   + node.getProperties().get("analysisServerConfigId"))) {
                             try {
                               NewRelicMetricNames metricNames = metricDataAnalysisService.getMetricNames(
                                   (String) node.getProperties().get("applicationId"),
                                   (String) node.getProperties().get("analysisServerConfigId"));
                               if (metricNames == null
                                   || System.currentTimeMillis() - metricNames.getCreatedAt()
                                       > TimeUnit.DAYS.toMillis(1)) {
                                 newRelicAppToConfigMap.put(node.getProperties().get("applicationId") + "-"
                                         + node.getProperties().get("analysisServerConfigId"),
                                     node.getProperties());
                                 NewRelicConfig newRelicConfig =
                                     (NewRelicConfig) settingsServiceMap
                                         .get(node.getProperties().get("analysisServerConfigId"))
                                         .getValue();
                                 NewRelicDataCollectionInfo dataCollectionInfo =
                                     NewRelicDataCollectionInfo.builder()
                                         .newRelicConfig(newRelicConfig)
                                         .newRelicAppId(
                                             Long.parseLong((String) node.getProperties().get("applicationId")))
                                         .encryptedDataDetails(secretManager.getEncryptionDetails(
                                             newRelicConfig, workflow.getAppId(), ""))
                                         .settingAttributeId(
                                             (String) node.getProperties().get("analysisServerConfigId"))
                                         .build();
                                 String waitId = generateUuid();
                                 logger.info("Scheduling new relic metric name collection task {}", dataCollectionInfo);
                                 DelegateTask delegateTask =
                                     aDelegateTask()
                                         .withTaskType(TaskType.NEWRELIC_COLLECT_METRIC_NAMES)
                                         .withAccountId(newRelicConfig.getAccountId())
                                         .withAppId(workflow.getAppId())
                                         .withParameters(new Object[] {dataCollectionInfo})
                                         .withWaitId(waitId)
                                         .withEnvId(workflow.getEnvId())
                                         .withInfrastructureMappingId(workflow.getInfraMappingId())
                                         .build();
                                 delegateService.queueTask(delegateTask);
                               }
                             } catch (Exception ex) {
                               logger.error(
                                   "Unable to schedule new relic metric collection newRelicAppId {}, serverConfigId {} ",
                                   node.getProperties().get("applicationId"),
                                   node.getProperties().get("analysisServerConfigId"), ex);
                             }
                           }
                         }));
        });
  }

  @Test
  public void testQuery() {
    List<Account> accounts =
        accountService.list(aPageRequest().withLimit(PageRequest.UNLIMITED).addFieldsIncluded("uuid").build());
    Map<Object, Map<String, Object>> newRelicAppToConfigMap = new HashMap<>();
    Map<String, SettingAttribute> settingsServiceMap = new HashMap<>();

    accounts.stream()
        .map(account -> settingsService.getGlobalSettingAttributesByType(account.getUuid(), StateType.NEW_RELIC.name()))
        .filter(settingAttributeList -> !settingAttributeList.isEmpty())
        .forEach(settingAttributeList -> {
          settingAttributeList.stream().forEach(
              settingAttribute -> settingsServiceMap.put(settingAttribute.getUuid(), settingAttribute));

          Stream<Workflow> workflowStream =
              appService.getAppsByAccountId(settingAttributeList.get(0).getAccountId())
                  .stream()
                  .map(app
                      -> workflowService
                             .listWorkflows(aPageRequest()
                                                .withLimit(UNLIMITED)
                                                .addFilter("appId", SearchFilter.Operator.EQ, app.getUuid())
                                                .build())
                             .getResponse())
                  .flatMap(Collection::stream);

          workflowStream.forEach(workflow
              -> Stream
                     .of((CanaryOrchestrationWorkflow) workflowService
                             .readLatestStateMachine(workflow.getAppId(), workflow.getUuid())
                             .getOrchestrationWorkflow())
                     .map(CanaryOrchestrationWorkflow::getGraph)
                     .map(Graph::getSubworkflows)
                     .map(Map::values)
                     .flatMap(Collection::stream)
                     .filter(graph -> graph.getGraphName().equals(Constants.VERIFY_SERVICE))
                     .map(Graph::getNodes)
                     .flatMap(Collection::stream)
                     .filter(node -> node.getType().equals(StateType.NEW_RELIC.name()))
                     .forEach(node -> {
                       // TODO get the property keys using refelction on NewRelicState
                       if (!newRelicAppToConfigMap.containsKey(node.getProperties().get("applicationId") + "-"
                               + node.getProperties().get("analysisServerConfigId"))) {
                         try {
                           NewRelicMetricNames metricNames = metricDataAnalysisService.getMetricNames(
                               (String) node.getProperties().get("applicationId"),
                               (String) node.getProperties().get("analysisServerConfigId"));
                           if (metricNames == null
                               || System.currentTimeMillis() - metricNames.getCreatedAt() > TimeUnit.DAYS.toMillis(1)) {
                             newRelicAppToConfigMap.put(node.getProperties().get("applicationId") + "-"
                                     + node.getProperties().get("analysisServerConfigId"),
                                 node.getProperties());
                             NewRelicConfig newRelicConfig =
                                 (NewRelicConfig) settingsServiceMap
                                     .get(node.getProperties().get("analysisServerConfigId"))
                                     .getValue();
                             NewRelicDataCollectionInfo dataCollectionInfo =
                                 NewRelicDataCollectionInfo.builder()
                                     .newRelicConfig(newRelicConfig)
                                     .newRelicAppId(Long.parseLong((String) node.getProperties().get("applicationId")))
                                     .encryptedDataDetails(
                                         secretManager.getEncryptionDetails(newRelicConfig, workflow.getAppId(), ""))
                                     .build();
                             String waitId = generateUuid();

                             DelegateTask delegateTask = aDelegateTask()
                                                             .withTaskType(TaskType.NEWRELIC_COLLECT_METRIC_NAMES)
                                                             .withAccountId(newRelicConfig.getAccountId())
                                                             .withAppId(workflow.getAppId())
                                                             .withParameters(new Object[] {dataCollectionInfo})
                                                             .withWaitId(waitId)
                                                             .withEnvId(workflow.getEnvId())
                                                             .withInfrastructureMappingId(workflow.getInfraMappingId())
                                                             .build();
                             delegateService.queueTask(delegateTask);
                           }
                         } catch (Exception ex) {
                           logger.error(
                               "Unable to schedule new relic metric collection newRelicAppId {}, serverConfigId {} ",
                               node.getProperties().get("applicationId"),
                               node.getProperties().get("analysisServerConfigId"), ex);
                         }
                       }
                     }));

        });

    newRelicAppToConfigMap.values().stream().forEach(config
        -> {

        });
    logger.info("" + newRelicAppToConfigMap.size());
  }
}
