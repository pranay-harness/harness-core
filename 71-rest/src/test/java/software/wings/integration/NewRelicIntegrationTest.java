package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.APDEX_SCORE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.ERROR;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.REQUSET_PER_MINUTE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.THROUGHPUT;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import io.harness.rule.OwnerRule.Owner;
import io.harness.rule.RepeatRule.Repeat;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import software.wings.APMFetchConfig;
import software.wings.api.HostElement;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.WorkflowExecution;
import software.wings.generator.ScmSecret;
import software.wings.generator.SecretGenerator;
import software.wings.generator.SecretName;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.MetricAnalysisJob.MetricAnalysisGenerator;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicSetupTestNodeData;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.APMVerificationStateTest;
import software.wings.sm.states.AbstractAnalysisState;
import software.wings.sm.states.DatadogState;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 9/7/17.
 */
public class NewRelicIntegrationTest extends BaseIntegrationTest {
  private Set<String> hosts = new HashSet<>();
  @Inject private NewRelicService newRelicService;
  @Inject private SettingsService settingsService;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;
  @Inject private LearningEngineService learningEngineService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private SecretGenerator secretGenerator;
  @Inject private ScmSecret scmSecret;

  private String newRelicConfigId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    hosts.clear();
    hosts.add("ip-172-31-2-144");
    hosts.add("ip-172-31-4-253");
    hosts.add("ip-172-31-12-51");

    SettingAttribute newRelicSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("NewRelic" + System.currentTimeMillis())
            .withAccountId(accountId)
            .withValue(NewRelicConfig.builder()
                           .accountId(accountId)
                           .newRelicUrl("https://api.newrelic.com")
                           .apiKey(scmSecret.decryptToCharArray(new SecretName("new_relic_api_key")))
                           .build())
            .build();
    newRelicConfigId = wingsPersistence.saveAndGet(SettingAttribute.class, newRelicSettingAttribute).getUuid();
  }

  @Test
  public void getNewRelicApplications() throws Exception {
    WebTarget target =
        client.target(API_BASE + "/newrelic/applications?settingId=" + newRelicConfigId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertFalse(restResponse.getResource().isEmpty());

    for (NewRelicApplication app : restResponse.getResource()) {
      assertTrue(app.getId() > 0);
      assertFalse(isBlank(app.getName()));
    }
  }

  @Test
  public void getAllTxnNames() throws Exception {
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, newRelicConfigId);
    NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();

    WebTarget target =
        client.target(API_BASE + "/newrelic/applications?settingId=" + newRelicConfigId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertFalse(restResponse.getResource().isEmpty());

    int totalTxns = 0;
    for (NewRelicApplication app : restResponse.getResource()) {
      assertTrue(app.getId() > 0);
      Set<NewRelicMetric> txnNameToCollect = newRelicDelegateService.getTxnNameToCollect(
          newRelicConfig, secretManager.getEncryptionDetails(newRelicConfig, null, null), app.getId(), null);
      totalTxns += txnNameToCollect.size();
    }

    assertTrue(totalTxns > 0);
  }

  @Test
  public void getNewRelicApplicationInstances() throws Exception {
    WebTarget target = client.target(API_BASE + "/newrelic/nodes?settingId=" + newRelicConfigId
        + "&accountId=" + accountId + "&applicationId=" + 107019083);
    RestResponse<List<NewRelicApplicationInstance>> restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicApplicationInstance>>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertFalse(restResponse.getResource().isEmpty());
  }

  @Test
  public void getNewRelicTxnsWithData() throws Exception {
    WebTarget target = client.target(API_BASE + "/newrelic/txns-with-data?settingId=" + newRelicConfigId
        + "&accountId=" + accountId + "&applicationId=" + 107019083);
    RestResponse<List<NewRelicMetric>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicMetric>>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(restResponse.getResource().size() >= 0);
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getNewRelicDataForNode() throws Exception {
    String appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
    String workflowId = wingsPersistence.save(aWorkflow().withAppId(appId).withName(generateUuid()).build());
    String workflowExecutionId = wingsPersistence.save(
        aWorkflowExecution().withAppId(appId).withWorkflowId(workflowId).withStatus(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .withExecutionUuid(workflowExecutionId)
                              .withStateType(StateType.PHASE.name())
                              .withAppId(appId)
                              .withDisplayName(generateUuid())
                              .build());
    WebTarget target = client.target(API_BASE + "/newrelic/nodes?settingId=" + newRelicConfigId
        + "&accountId=" + accountId + "&applicationId=" + 107019083);
    RestResponse<List<NewRelicApplicationInstance>> nodesResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicApplicationInstance>>>() {});
    List<NewRelicApplicationInstance> nodes = nodesResponse.getResource();
    assertFalse(nodes.isEmpty());

    for (NewRelicApplicationInstance node : nodes) {
      NewRelicSetupTestNodeData testNodeData =
          NewRelicSetupTestNodeData.builder()
              .newRelicAppId(107019083)
              .appId(appId)
              .settingId(newRelicConfigId)
              .instanceName(generateUuid())
              .hostExpression("${host.hostName}")
              .workflowId(workflowId)
              .instanceElement(anInstanceElement()
                                   .withHost(HostElement.Builder.aHostElement().withHostName(node.getHost()).build())
                                   .build())
              .build();
      target =
          client.target(API_BASE + "/newrelic/node-data?settingId=" + newRelicConfigId + "&accountId=" + accountId);
      RestResponse<VerificationNodeDataSetupResponse> metricResponse =
          getRequestBuilderWithAuthHeader(target).post(entity(testNodeData, APPLICATION_JSON),
              new GenericType<RestResponse<VerificationNodeDataSetupResponse>>() {});
      assertEquals(0, metricResponse.getResponseMessages().size());
      assertTrue(metricResponse.getResource().isProviderReachable());
      assertTrue(metricResponse.getResource().getLoadResponse().isLoadPresent());
      assertNotNull(metricResponse.getResource().getLoadResponse().getLoadResponse());
      List<NewRelicMetric> txnsWithData =
          (List<NewRelicMetric>) metricResponse.getResource().getLoadResponse().getLoadResponse();
      assertFalse(txnsWithData.isEmpty());
      NewRelicMetricData newRelicMetricData =
          JsonUtils.asObject(JsonUtils.asJson(metricResponse.getResource().getDataForNode()), NewRelicMetricData.class);
      // found at least a node with data
      if (!newRelicMetricData.getMetrics_found().isEmpty()) {
        assertTrue(newRelicMetricData.getMetrics().size() > 0);
        newRelicMetricData.getMetrics().forEach(newRelicMetricSlice -> {
          assertTrue(!isEmpty(newRelicMetricSlice.getName()));
          assertTrue(newRelicMetricSlice.getTimeslices().size() > 0);
        });

        return;
      }
    }

    fail("No node with data found");
  }

  @Test
  public void testMetricSave() throws Exception {
    final int numOfMinutes = 4;
    final int numOfBatches = 5;
    final int numOfMetricsPerBatch = 100;
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    Random r = new Random();

    for (int batchNum = 0; batchNum < numOfBatches; batchNum++) {
      List<NewRelicMetricDataRecord> metricDataRecords = new ArrayList<>();

      for (int metricNum = 0; metricNum < numOfMetricsPerBatch; metricNum++) {
        String metricName = "metric-" + batchNum * numOfMetricsPerBatch + metricNum;
        for (String host : hosts) {
          for (int collectionMin = 0; collectionMin < numOfMinutes; collectionMin++) {
            NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
            record.setName(metricName);
            record.setHost(host);
            record.setWorkflowId(workflowId);
            record.setWorkflowExecutionId(workflowExecutionId);
            record.setServiceId(serviceId);
            record.setStateExecutionId(stateExecutionId);
            record.setTimeStamp(collectionMin);
            record.setDataCollectionMinute(collectionMin);

            record.setValues(new HashMap<>());
            record.getValues().put(THROUGHPUT, r.nextDouble());
            record.getValues().put(AVERAGE_RESPONSE_TIME, r.nextDouble());
            record.getValues().put(ERROR, r.nextDouble());
            record.getValues().put(APDEX_SCORE, r.nextDouble());

            metricDataRecords.add(record);

            // add more records for duplicate records for the same time
            if (collectionMin > 0) {
              record = new NewRelicMetricDataRecord();
              record.setName(metricName);
              record.setHost(host);
              record.setWorkflowId(workflowId);
              record.setWorkflowExecutionId(workflowExecutionId);
              record.setServiceId(serviceId);
              record.setStateExecutionId(stateExecutionId);
              // duplicate for previous minute
              record.setTimeStamp(collectionMin - 1);
              record.setDataCollectionMinute(collectionMin);

              record.setValues(new HashMap<>());
              record.getValues().put(THROUGHPUT, r.nextDouble());
              record.getValues().put(AVERAGE_RESPONSE_TIME, r.nextDouble());
              record.getValues().put(ERROR, r.nextDouble());
              record.getValues().put(APDEX_SCORE, r.nextDouble());

              metricDataRecords.add(record);
            }
          }
        }
      }

      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setUuid(stateExecutionId);
      stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
      stateExecutionInstance.setAppId(applicationId);
      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

      WebTarget target = client.target(API_BASE + "/newrelic/save-metrics?accountId=" + accountId + "&applicationId="
          + applicationId + "&stateExecutionId=" + stateExecutionId + "&delegateTaskId=" + delegateTaskId);
      RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
          entity(metricDataRecords, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      assertTrue(restResponse.getResource());

      Query<NewRelicMetricDataRecord> query =
          wingsPersistence.createQuery(NewRelicMetricDataRecord.class).filter("stateExecutionId", stateExecutionId);
      assertEquals((batchNum + 1) * numOfMetricsPerBatch * hosts.size() * numOfMinutes, query.count());
    }
  }

  private void featureflagDemoSuccess() throws Exception {
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = createApp(UUID.randomUUID().toString()).getUuid();
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();

    wingsPersistence.delete(
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"));

    wingsPersistence.save(FeatureFlag.builder().name("CV_DEMO").build());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", "xyz"));

    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class)
                                .filter("accountId", accountId)
                                .filter("name", "newrelic_prod"));

    String serverConfigId = wingsPersistence.save(
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withName("newrelic_prod").build());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    stateExecutionInstance.setStateType(StateType.NEW_RELIC.name());
    stateExecutionInstance.setDisplayName("Relic_Fail");
    Map<String, StateExecutionData> hashMap = new HashMap();
    hashMap.put("Relic_Fail", MetricAnalysisExecutionData.builder().serverConfigId(serverConfigId).build());
    stateExecutionInstance.setStateExecutionMap(hashMap);
    stateExecutionInstance.setAppId(appId);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                                    .workflowId(workflowId)
                                                    .workflowExecutionId("CV-Demo")
                                                    .stateExecutionId("CV-Demo-TS-Success")
                                                    .appId("CV-Demo-" + StateType.NEW_RELIC)
                                                    .stateType(StateType.NEW_RELIC)
                                                    .metricAnalyses(new ArrayList<>())
                                                    .message("CV-demo")
                                                    .build();
    wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(record));

    WebTarget target = client.target(API_BASE + "/timeseries/generate-metrics?accountId=" + accountId
        + "&stateExecutionId=" + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId + "&appId=" + appId);
    RestResponse<NewRelicMetricAnalysisRecord> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicMetricAnalysisRecord>>() {});
    NewRelicMetricAnalysisRecord savedRecord = restResponse.getResource();
    assertNull(savedRecord.getWorkflowExecutionId());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", accountId));
    assertTrue(featureFlagService.isEnabled(FeatureName.CV_DEMO, accountId));
    restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicMetricAnalysisRecord>>() {});
    savedRecord = restResponse.getResource();
    assertEquals("CV-Demo", savedRecord.getWorkflowExecutionId());
  }

  private void featureflagDemoFail() throws Exception {
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = createApp(UUID.randomUUID().toString()).getUuid();
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();

    wingsPersistence.delete(
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"));

    wingsPersistence.save(FeatureFlag.builder().name("CV_DEMO").build());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", "xyz"));

    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class)
                                .filter("accountId", accountId)
                                .filter("name", "newrelic_dev"));

    String serverConfigId = wingsPersistence.save(
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withName("newrelic_dev").build());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.FAILED);
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setStateType(StateType.NEW_RELIC.name());
    stateExecutionInstance.setDisplayName("Relic_Fail");
    Map<String, StateExecutionData> hashMap = new HashMap();
    hashMap.put("Relic_Fail", MetricAnalysisExecutionData.builder().serverConfigId(serverConfigId).build());
    stateExecutionInstance.setStateExecutionMap(hashMap);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                                    .workflowId(workflowId)
                                                    .workflowExecutionId("CV-Demo")
                                                    .stateExecutionId("CV-Demo-TS-Failure")
                                                    .appId("CV-Demo-" + StateType.NEW_RELIC)
                                                    .stateType(StateType.NEW_RELIC)
                                                    .metricAnalyses(new ArrayList<>())
                                                    .build();
    wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(record));

    WebTarget target = client.target(API_BASE + "/timeseries/generate-metrics?accountId=" + accountId
        + "&stateExecutionId=" + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId + "&appId=" + appId);
    RestResponse<NewRelicMetricAnalysisRecord> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicMetricAnalysisRecord>>() {});
    NewRelicMetricAnalysisRecord savedRecord = restResponse.getResource();
    assertNull(savedRecord.getWorkflowExecutionId());

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", accountId));

    restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicMetricAnalysisRecord>>() {});
    savedRecord = restResponse.getResource();
    assertEquals("CV-Demo", savedRecord.getWorkflowExecutionId());
  }

  private void analysisSorted() throws Exception {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = createApp(UUID.randomUUID().toString()).getUuid();

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                                    .workflowId(workflowId)
                                                    .workflowExecutionId(workflowExecutionId)
                                                    .stateExecutionId(stateExecutionId)
                                                    .appId(applicationId)
                                                    .stateType(StateType.NEW_RELIC)
                                                    .metricAnalyses(new ArrayList<>())
                                                    .build();

    final NewRelicMetricAnalysis analysis1 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.HIGH).metricName("metric1").build();
    record.addNewRelicMetricAnalysis(analysis1);

    final NewRelicMetricAnalysis analysis2 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.MEDIUM).metricName("metric1").build();
    record.addNewRelicMetricAnalysis(analysis2);

    final NewRelicMetricAnalysis analysis3 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.LOW).metricName("metric1").build();
    record.addNewRelicMetricAnalysis(analysis3);

    final NewRelicMetricAnalysis analysis4 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.LOW).metricName("metric0").build();
    record.addNewRelicMetricAnalysis(analysis4);

    final NewRelicMetricAnalysis analysis5 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.LOW).metricName("abc").build();
    record.addNewRelicMetricAnalysis(analysis5);

    wingsPersistence.save(record);

    WebTarget target =
        client.target(API_BASE + "/timeseries/generate-metrics?accountId=" + accountId + "&stateExecutionId="
            + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId + "&appId=" + applicationId);
    RestResponse<NewRelicMetricAnalysisRecord> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicMetricAnalysisRecord>>() {});

    NewRelicMetricAnalysisRecord savedRecord = restResponse.getResource();
    assertNotNull(savedRecord);

    final List<NewRelicMetricAnalysis> analyses = savedRecord.getMetricAnalyses();
    assertEquals(record.getMetricAnalyses().size(), analyses.size());

    assertEquals(analysis1, analyses.get(0));
    assertEquals(analysis2, analyses.get(1));
    assertEquals(analysis5, analyses.get(2));
    assertEquals(analysis4, analyses.get(3));
    assertEquals(analysis3, analyses.get(4));
  }

  @Test
  public void generateMetricsTest() throws Exception {
    // because of the CV_DEMO flag tests, all generate metrics tests should go here.
    // the CV_DEMO tests should run last

    wingsPersistence.delete(
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"));

    analysisSorted();
    featureflagDemoFail();
    featureflagDemoSuccess();
  }

  @Test
  public void fetch() throws Exception {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setAccountId(accountId);
    config.setUrl("https://app.datadoghq.com/api/v1/");
    config.setValidationUrl("metrics?from=12345");
    List<APMVerificationConfig.KeyValues> optionsList = new ArrayList<>();
    optionsList.add(APMVerificationConfig.KeyValues.builder()
                        .key("api_key")
                        .value(scmSecret.decryptToString(new SecretName("apm_verfication_config_api_key")))
                        .encrypted(false)
                        .build());
    optionsList.add(APMVerificationConfig.KeyValues.builder()
                        .key("application_key")
                        .value(scmSecret.decryptToString(new SecretName("apm_verfication_config_app_key")))
                        .encrypted(false)
                        .build());
    config.setOptionsList(optionsList);

    SettingAttribute settingAttribute =
        wingsPersistence.createQuery(SettingAttribute.class).filter("name", "datadog_connector").get();

    String serverConfigId;
    if (settingAttribute == null) {
      serverConfigId = wingsPersistence
                           .saveIgnoringDuplicateKeys(Lists.newArrayList(SettingAttribute.Builder.aSettingAttribute()
                                                                             .withAccountId(accountId)
                                                                             .withName("datadog_connector")
                                                                             .withValue(config)
                                                                             .build()))
                           .get(0);
    } else {
      serverConfigId = settingAttribute.getUuid();
    }

    WebTarget target =
        client.target(API_BASE + "/timeseries/fetch?accountId=" + accountId + "&serverConfigId=" + serverConfigId);

    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(APMFetchConfig.builder()
                   .url("metrics?from=" + System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1))
                   .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<String>>() {});
    assertNotNull(restResponse.getResource());
  }

  @Test
  public void noControlNoTest() throws IOException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withName(workflowId + "-prev-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkFlowExecutionId = wingsPersistence.save(workflowExecution);

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Collections.singletonList(record));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-curr-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(workflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(stateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.H0);
    record.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Collections.singletonList(record));

    String prevWorkflowExecutionID = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .testNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .prevWorkflowExecutionId(prevWorkflowExecutionID == null ? "-1" : prevWorkflowExecutionID)
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new MetricAnalysisGenerator(metricDataAnalysisService, learningEngineService, waitNotifyEngine, delegateService,
        analysisContext, jobExecutionContext, delegateTaskId)
        .run();

    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);
    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertFalse(metricsAnalysis.isShowTimeSeries());
    assertEquals("No data available", metricsAnalysis.getMessage());
  }

  @Test
  public void controlNoTest() throws IOException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withName(workflowId + "-prev-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkFlowExecutionId = wingsPersistence.save(workflowExecution);

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.NEW_RELIC);

    NewRelicMetricDataRecord record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn1");
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(prevStateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);

    record1.setValues(new HashMap<>());
    record1.getValues().put(REQUSET_PER_MINUTE, 20.0);
    record1.getValues().put(AVERAGE_RESPONSE_TIME, 50.0);
    record1.getValues().put(APDEX_SCORE, 1.0);

    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-curr-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(workflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(stateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.H0);
    record.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Collections.singletonList(record));

    String prevWorkflowExecutionID = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .testNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .prevWorkflowExecutionId(prevWorkflowExecutionID == null ? "-1" : prevWorkflowExecutionID)
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new MetricAnalysisGenerator(metricDataAnalysisService, learningEngineService, waitNotifyEngine, delegateService,
        analysisContext, jobExecutionContext, delegateTaskId)
        .run();

    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);

    assertEquals(RiskLevel.NA, metricsAnalysis.getRiskLevel());
    assertFalse(metricsAnalysis.isShowTimeSeries());
    assertEquals("No data available", metricsAnalysis.getMessage());
  }

  @Test
  public void testNoControl() throws IOException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withName(workflowId + "-prev-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkFlowExecutionId = wingsPersistence.save(workflowExecution);

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-curr-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setHost("");
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(workflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(stateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.H0);
    record.setStateType(StateType.NEW_RELIC);

    NewRelicMetricDataRecord record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn1");
    record1.setAppId(appId);
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(workflowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(stateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);

    record1.setValues(new HashMap<>());
    record1.getValues().put(REQUSET_PER_MINUTE, 20.0);
    record1.getValues().put(AVERAGE_RESPONSE_TIME, 50.0);
    record1.getValues().put(APDEX_SCORE, 1.0);
    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    String prevWorkflowExecutionID = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .testNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .prevWorkflowExecutionId(prevWorkflowExecutionID)
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new MetricAnalysisGenerator(metricDataAnalysisService, learningEngineService, waitNotifyEngine, delegateService,
        analysisContext, jobExecutionContext, delegateTaskId)
        .run();

    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);

    assertEquals(RiskLevel.LOW, metricsAnalysis.getRiskLevel());
    assertFalse(metricsAnalysis.isShowTimeSeries());
    assertEquals("No problems found", metricsAnalysis.getMessage());
  }

  @Test
  public void txnInTestButNotControl() throws IOException, InterruptedException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withServiceIds(Lists.newArrayList(serviceId))
            .withName(workflowId + "-prev-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkFlowExecutionId = wingsPersistence.save(workflowExecution);

    List<String> metricNames = new ArrayList<>();
    metricDataAnalysisService.saveMetricTemplates(appId, StateType.NEW_RELIC, stateExecutionId,
        newRelicService.metricDefinitions(newRelicService.getMetricsCorrespondingToMetricNames(metricNames).values()));

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.NEW_RELIC);

    NewRelicMetricDataRecord record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn1");
    record1.setAppId(appId);
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(prevStateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);
    record1.setValues(new HashMap<>());
    record1.getValues().put(REQUSET_PER_MINUTE, 20.0);
    record1.getValues().put(AVERAGE_RESPONSE_TIME, 50.0);
    record1.getValues().put(APDEX_SCORE, 1.0);
    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-curr-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(workflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(stateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.H0);
    record.setStateType(StateType.NEW_RELIC);

    record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn2");
    record1.setAppId(appId);
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(workflowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(stateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);
    record1.setValues(new HashMap<>());
    record1.getValues().put(REQUSET_PER_MINUTE, 20.0);
    record1.getValues().put(AVERAGE_RESPONSE_TIME, 50.0);
    record1.getValues().put(APDEX_SCORE, 1.0);
    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    String lastSuccessfulWorkflowExecutionIdWithData =
        metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
            StateType.NEW_RELIC, appId, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .testNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .prevWorkflowExecutionId(lastSuccessfulWorkflowExecutionIdWithData)
            .smooth_window(1)
            .parallelProcesses(1)
            .comparisonWindow(1)
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new MetricAnalysisGenerator(metricDataAnalysisService, learningEngineService, waitNotifyEngine, delegateService,
        analysisContext, jobExecutionContext, delegateTaskId)
        .run();

    // TODO I know....
    Thread.sleep(10000);

    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);

    assertEquals(RiskLevel.LOW, metricsAnalysis.getRiskLevel());
    assertTrue(metricsAnalysis.isShowTimeSeries());
    assertEquals("No problems found", metricsAnalysis.getMessage());
    assertEquals(1, metricsAnalysis.getMetricAnalyses().size());
  }

  private URIBuilder getURIBuilder(String path) {
    URIBuilder uriBuilder = new URIBuilder();
    String scheme = StringUtils.isBlank(System.getenv().get("BASE_HTTP")) ? "https" : "http";
    uriBuilder.setScheme(scheme);
    uriBuilder.setHost("localhost");
    uriBuilder.setPort(9090);
    uriBuilder.setPath(path);
    return uriBuilder;
  }

  private Map<String, String> getParamsForMetricTemplate(String appId, String stateExecutionId, String serviceId) {
    Map<String, String> params = new HashMap<>();
    params.put("accountId", accountId);
    params.put("appId", appId);
    params.put("stateType", "NEW_RELIC");
    params.put("stateExecutionId", stateExecutionId);
    params.put("serviceId", serviceId);
    params.put("groupName", "default");
    return params;
  }

  @Test
  public void getMetricTemplate() throws IOException, InterruptedException {
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    String appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    List<String> metricNames = new ArrayList<>();
    metricDataAnalysisService.saveMetricTemplates(appId, StateType.NEW_RELIC, stateExecutionId,
        newRelicService.metricDefinitions(newRelicService.getMetricsCorrespondingToMetricNames(metricNames).values()));

    String metricTemplateUrl;
    URIBuilder metricTemplateUriBuilder = getURIBuilder("/api/timeseries/get-metric-template");
    Map<String, String> params = getParamsForMetricTemplate(appId, stateExecutionId, serviceId);
    params.forEach((name, value) -> metricTemplateUriBuilder.addParameter(name, value));

    try {
      metricTemplateUrl = metricTemplateUriBuilder.build().toString();
      WebTarget target = client.target(metricTemplateUrl);
      RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> restResponse =
          getRequestBuilderWithAuthHeader(target).post(entity("{}", APPLICATION_JSON),
              new GenericType<RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>>>() {});

      String expectedTemplate = Resources.toString(
          APMVerificationStateTest.class.getResource("/apm/NewRelicMetricTemplate.json"), Charsets.UTF_8);
      assertEquals(expectedTemplate, JsonUtils.asJson(restResponse.getResource()));
    } catch (URISyntaxException uriSyntaxException) {
      logger.error("Failed to build URL correctly.");
    }
  }

  @Test
  @Owner(emails = {"raghu@harness.io", "sriram@harness.io"}, intermittent = true)
  public void txnDatadog() throws IOException, InterruptedException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(appId)
            .withServiceIds(Lists.newArrayList(serviceId))
            .withName(workflowId + "-prev-execution-" + 0)
            .withStatus(ExecutionStatus.SUCCESS)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkFlowExecutionId = wingsPersistence.save(workflowExecution);

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.DATA_DOG);

    NewRelicMetricDataRecord record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn1");
    record1.setAppId(appId);
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(prevWorkFlowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(prevStateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);
    record1.setValues(new HashMap<>());
    record1.getValues().put("Hits", 20.0);
    record1.getValues().put("Request Duration", 2.0);
    record1.setHost("host1");
    record1.setStateType(StateType.DATA_DOG);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(appId)
                            .withName(workflowId + "-curr-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(workflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(stateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.H0);
    record.setStateType(StateType.DATA_DOG);

    record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn1");
    record1.setAppId(appId);
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(workflowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(stateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);
    record1.setValues(new HashMap<>());
    record1.getValues().put("Hits", 20.0);
    record1.getValues().put("Request Duration", 2.0);
    record1.setTag("Servlet");
    record1.setHost("host1");
    record1.setStateType(StateType.DATA_DOG);

    metricDataAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    metricDataAnalysisService.saveMetricTemplates(appId, StateType.DATA_DOG, stateExecutionId,
        DatadogState.metricDefinitions(
            DatadogState.metrics(Lists.newArrayList("trace.servlet.request.duration", "trace.servlet.request.hits"))
                .values()));

    String lastSuccessfulWorkflowExecutionIdWithData =
        metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
            StateType.DATA_DOG, appId, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(appId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .testNodes(Collections.singletonMap("host1", DEFAULT_GROUP_NAME))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.DATA_DOG)
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .prevWorkflowExecutionId(lastSuccessfulWorkflowExecutionIdWithData)
            .smooth_window(1)
            .parallelProcesses(1)
            .comparisonWindow(1)
            .tolerance(1)
            .prevWorkflowExecutionId(prevWorkFlowExecutionId)
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new MetricAnalysisGenerator(metricDataAnalysisService, learningEngineService, waitNotifyEngine, delegateService,
        analysisContext, jobExecutionContext, delegateTaskId)
        .run();

    // TODO I know....
    Thread.sleep(10000);
    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertEquals(1, metricAnalysisRecords.size());

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.get(0);

    assertEquals(RiskLevel.LOW, metricsAnalysis.getRiskLevel());
    assertTrue(metricsAnalysis.isShowTimeSeries());
    assertEquals("No problems found", metricsAnalysis.getMessage());
    assertEquals(1, metricsAnalysis.getMetricAnalyses().size());
    assertEquals("Dummy txn1", metricsAnalysis.getMetricAnalyses().get(0).getMetricName());
    assertEquals(2, metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().size());
    assertEquals("Servlet", metricsAnalysis.getMetricAnalyses().get(0).getTag());
    assertEquals(0, metricsAnalysis.getAnalysisMinute());

    assertEquals("Hits", metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(0).getName());
    assertEquals(RiskLevel.LOW, metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(0).getRiskLevel());
    assertEquals(20.0, metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(0).getTestValue(), 0.001);
    assertEquals(20.0, metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(0).getControlValue(), 0.001);

    assertEquals("Request Duration", metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(1).getName());
    assertEquals(RiskLevel.LOW, metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(1).getRiskLevel());
    assertEquals(2.0, metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(1).getTestValue(), 0.001);
    assertEquals(2.0, metricsAnalysis.getMetricAnalyses().get(0).getMetricValues().get(1).getControlValue(), 0.001);
  }
}
