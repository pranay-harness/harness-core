package software.wings.integration.migration.legacy;

import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Graph;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SearchFilter;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.rules.SetupScheduler;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by sriram_parthasarathy on 12/14/17.
 */
@Integration
@Ignore
@SetupScheduler
public class NewRelicMetricNamesMigrationUtil extends WingsBaseTest {
  @Inject private AccountService accountService;
  @Inject private SettingsService settingsService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Test
  public void createNewRelicMetricNames() {
    List<Account> accounts =
        accountService.list(aPageRequest().withLimit(PageRequest.UNLIMITED).addFieldsIncluded("uuid").build());
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
                       try {
                         NewRelicMetricNames metricNames = metricDataAnalysisService.getMetricNames(
                             (String) node.getProperties().get("applicationId"),
                             (String) node.getProperties().get("analysisServerConfigId"));

                         NewRelicConfig newRelicConfig =
                             (NewRelicConfig) settingsServiceMap.get(node.getProperties().get("analysisServerConfigId"))
                                 .getValue();
                         NewRelicMetricNames newRelicMetricNames =
                             NewRelicMetricNames.builder()
                                 .newRelicConfigId((String) node.getProperties().get("analysisServerConfigId"))
                                 .newRelicAppId((String) node.getProperties().get("applicationId"))
                                 .registeredWorkflows(
                                     Collections.singletonList(WorkflowInfo.builder()
                                                                   .accountId(newRelicConfig.getAccountId())
                                                                   .appId(workflow.getAppId())
                                                                   .workflowId(workflow.getUuid())
                                                                   .envId(workflow.getEnvId())
                                                                   .infraMappingId(workflow.getInfraMappingId())
                                                                   .build()))
                                 .build();

                         if (metricNames == null
                             || System.currentTimeMillis() - metricNames.getCreatedAt() > TimeUnit.DAYS.toMillis(1)) {
                           metricDataAnalysisService.saveMetricNames(
                               newRelicConfig.getAccountId(), newRelicMetricNames);
                         } else {
                           metricDataAnalysisService.addMetricNamesWorkflowInfo(
                               newRelicConfig.getAccountId(), newRelicMetricNames);
                         }
                       } catch (Exception ex) {
                         ex.printStackTrace();
                       }

                     }));
        });
  }
}
