package io.harness.integration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.PRANJAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRIRAM;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
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

import io.harness.VerificationBaseIntegrationTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.jobs.workflow.timeseries.WorkflowTimeSeriesAnalysisJob.MetricAnalysisGenerator;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import software.wings.api.HostElement;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.MetricDataAnalysisServiceImpl;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.newrelic.MetricUtilHelper;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicSetupTestNodeData;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.APMVerificationStateTest;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 9/7/17.
 */
@Slf4j
public class NewRelicIntegrationTest extends VerificationBaseIntegrationTest {
  public static String NEW_RELIC_CONNECTOR_NAME = "NewRelic";

  private Set<String> hosts = new HashSet<>();
  @Inject private MetricUtilHelper metricUtilHelper;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private LearningEngineService learningEngineService;
  @Inject private VerificationManagerClient mgrClient;
  @Inject private VerificationManagerClientHelper managerClient;
  @Inject private WingsPersistence wingsPersistence;
  private MetricDataAnalysisService metricDataAnalysisService;

  private String newRelicConfigId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    hosts.clear();
    hosts.add("ip-172-31-2-144");
    hosts.add("ip-172-31-4-253");
    hosts.add("ip-172-31-12-51");

    newRelicConfigId = wingsPersistence.createQuery(SettingAttribute.class)
                           .filter(SettingAttributeKeys.name, NEW_RELIC_CONNECTOR_NAME)
                           .filter(SettingAttributeKeys.accountId, accountId)
                           .get()
                           .getUuid();
    metricDataAnalysisService = new MetricDataAnalysisServiceImpl();
    FieldUtils.writeField(metricDataAnalysisService, "wingsPersistence", wingsPersistence, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(IntegrationTests.class)
  public void testFeatureEnabled() {
    WebTarget target = client.target(API_BASE + "/account/feature-flag-enabled?accountId=" + accountId
        + "&featureName=" + FeatureName.values()[0].name());
    RestResponse<Boolean> restResponse =
        getRequestBuilderWithLearningAuthHeader(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResource()).isFalse();

    target = client.target(
        API_BASE + "/account/feature-flag-enabled?accountId=" + accountId + "&featureName=" + generateUuid());
    restResponse = getRequestBuilderWithLearningAuthHeader(target).get(new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResource()).isFalse();
  }

  @Test
  @Owner(developers = RAGHU, intermittent = true)
  @Category(IntegrationTests.class)
  public void getNewRelicApplications() throws Exception {
    WebTarget target =
        client.target(API_BASE + "/newrelic/applications?settingId=" + newRelicConfigId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource().isEmpty()).isFalse();

    for (NewRelicApplication app : restResponse.getResource()) {
      assertThat(app.getId() > 0).isTrue();
      assertThat(isBlank(app.getName())).isFalse();
    }
  }

  @Test
  @Owner(developers = RAGHU, intermittent = true)
  @Category(IntegrationTests.class)
  public void getAllTxnNames() throws Exception {
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, newRelicConfigId);
    NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();

    WebTarget target =
        client.target(API_BASE + "/newrelic/applications?settingId=" + newRelicConfigId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource().isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(IntegrationTests.class)
  public void getNewRelicApplicationInstances() throws Exception {
    WebTarget target = client.target(API_BASE + "/newrelic/nodes?settingId=" + newRelicConfigId
        + "&accountId=" + accountId + "&applicationId=" + 107019083);
    RestResponse<List<NewRelicApplicationInstance>> restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicApplicationInstance>>>() {});

    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource().isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void getNewRelicTxnsWithData() throws Exception {
    WebTarget target = client.target(API_BASE + "/newrelic/txns-with-data?settingId=" + newRelicConfigId
        + "&accountId=" + accountId + "&applicationId=" + 107019083);
    RestResponse<List<NewRelicMetric>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicMetric>>>() {});

    assertThat(restResponse.getResponseMessages()).isEmpty();
    assertThat(restResponse.getResource().size() >= 0).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL, intermittent = true)
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void getNewRelicDataForNode() {
    String appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    String workflowId = wingsPersistence.save(aWorkflow().appId(appId).name(generateUuid()).build());
    String workflowExecutionId = wingsPersistence.save(
        WorkflowExecution.builder().appId(appId).workflowId(workflowId).status(ExecutionStatus.SUCCESS).build());
    wingsPersistence.save(aStateExecutionInstance()
                              .executionUuid(workflowExecutionId)
                              .stateType(StateType.PHASE.name())
                              .appId(appId)
                              .displayName(generateUuid())
                              .build());
    WebTarget target = client.target(API_BASE + "/newrelic/nodes?settingId=" + newRelicConfigId
        + "&accountId=" + accountId + "&applicationId=" + 107019083);
    RestResponse<List<NewRelicApplicationInstance>> nodesResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicApplicationInstance>>>() {});
    List<NewRelicApplicationInstance> nodes = nodesResponse.getResource();
    assertThat(nodes.isEmpty()).isFalse();
    long toTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(60);
    NewRelicApplicationInstance node = nodes.iterator().next();

    NewRelicSetupTestNodeData testNodeData =
        NewRelicSetupTestNodeData.builder()
            .newRelicAppId(107019083)
            .appId(appId)
            .settingId(newRelicConfigId)
            .instanceName(generateUuid())
            .hostExpression("${host.hostName}")
            .workflowId(workflowId)
            .toTime(toTime)
            .fromTime(toTime - TimeUnit.MINUTES.toMillis(120))
            .instanceElement(
                anInstanceElement().host(HostElement.Builder.aHostElement().hostName(node.getHost()).build()).build())
            .build();

    target = client.target(API_BASE + "/newrelic/node-data?settingId=" + newRelicConfigId + "&accountId=" + accountId);
    RestResponse<VerificationNodeDataSetupResponse> metricResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(testNodeData, APPLICATION_JSON), new GenericType<RestResponse<VerificationNodeDataSetupResponse>>() {});
    assertThat(metricResponse.getResponseMessages()).isEmpty();
    assertThat(metricResponse.getResource().isProviderReachable()).isTrue();
    assertThat(metricResponse.getResource().getLoadResponse().isLoadPresent()).isTrue();
    assertThat(metricResponse.getResource().getLoadResponse().getLoadResponse()).isNotNull();
    List<NewRelicMetric> txnsWithData =
        (List<NewRelicMetric>) metricResponse.getResource().getLoadResponse().getLoadResponse();
    assertThat(txnsWithData.isEmpty()).isFalse();
    NewRelicMetricData newRelicMetricData =
        JsonUtils.asObject(JsonUtils.asJson(metricResponse.getResource().getDataForNode()), NewRelicMetricData.class);
    // found at least a node with data
    if (!newRelicMetricData.getMetrics_found().isEmpty()) {
      assertThat(newRelicMetricData.getMetrics().size() > 0).isTrue();
      newRelicMetricData.getMetrics().forEach(newRelicMetricSlice -> {
        assertThat(!isEmpty(newRelicMetricSlice.getName())).isTrue();
        assertThat(newRelicMetricSlice.getTimeslices().size() > 0).isTrue();
      });

      return;
    }
    fail("No node with data found");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(IntegrationTests.class)
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
          }
        }
      }

      StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
      stateExecutionInstance.setUuid(stateExecutionId);
      stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
      stateExecutionInstance.setAppId(applicationId);
      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

      WebTarget target = client.target(VERIFICATION_API_BASE + "/" + VerificationConstants.DELEGATE_DATA_COLLETION
          + "/save-metrics?accountId=" + accountId + "&applicationId=" + applicationId
          + "&stateExecutionId=" + stateExecutionId + "&delegateTaskId=" + delegateTaskId);
      RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
          entity(metricDataRecords, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      assertThat(restResponse.getResource()).isTrue();

      target = client.target(VERIFICATION_API_BASE + "/" + VerificationConstants.DELEGATE_DATA_COLLETION
          + "/save-metrics?accountId=" + accountId + "&applicationId=" + applicationId
          + "&stateExecutionId=" + stateExecutionId + "&delegateTaskId=" + delegateTaskId);
      restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
          entity(metricDataRecords, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      assertThat(restResponse.getResource()).isTrue();

      target = client.target(VERIFICATION_API_BASE + "/timeseries/get-metrics?accountId=" + accountId
          + "&appId=" + applicationId + "&compareCurrent=" + true + "&groupName=" + DEFAULT_GROUP_NAME);
      RestResponse<Set<NewRelicMetricDataRecord>> metricResponse =
          getRequestBuilderWithLearningAuthHeader(target).post(entity(TSRequest.builder()
                                                                          .stateExecutionId(stateExecutionId)
                                                                          .nodes(hosts)
                                                                          .analysisMinute(numOfMinutes)
                                                                          .analysisStartMinute(0)
                                                                          .build(),
                                                                   APPLICATION_JSON),
              new GenericType<RestResponse<Set<NewRelicMetricDataRecord>>>() {});

      final Set<NewRelicMetricDataRecord> metricsFromDb = metricResponse.getResource();
      assertThat(metricsFromDb).hasSize((batchNum + 1) * numOfMetricsPerBatch * hosts.size() * numOfMinutes);
      Query<NewRelicMetricDataRecord> query =
          wingsPersistence.createQuery(NewRelicMetricDataRecord.class).filter("stateExecutionId", stateExecutionId);
      assertThat(query.count()).isEqualTo((batchNum + 1) * numOfMetricsPerBatch * hosts.size() * numOfMinutes * 2);
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
                                .filter(SettingAttributeKeys.accountId, accountId)
                                .filter(SettingAttributeKeys.name, "newrelic_prod"));

    String serverConfigId = wingsPersistence.save(
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withName("newrelic_prod").build());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    stateExecutionInstance.setStateType(StateType.NEW_RELIC.name());
    stateExecutionInstance.setDisplayName("Relic_Fail");
    Map<String, StateExecutionData> hashMap = new HashMap();
    hashMap.put("Relic_Fail", VerificationStateAnalysisExecutionData.builder().serverConfigId(serverConfigId).build());
    stateExecutionInstance.setStateExecutionMap(hashMap);
    stateExecutionInstance.setAppId(appId);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                                    .workflowExecutionId("CV-Demo")
                                                    .stateExecutionId("CV-Demo-TS-Success-NEW_RELIC")
                                                    .appId("CV-Demo")
                                                    .stateType(StateType.NEW_RELIC)
                                                    .metricAnalyses(new ArrayList<>())
                                                    .message("CV-demo")
                                                    .build();
    wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(record));

    WebTarget target = client.target(API_BASE + "/timeseries/generate-metrics-appdynamics?accountId=" + accountId
        + "&stateExecutionId=" + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId + "&appId=" + appId);
    RestResponse<List<NewRelicMetricAnalysisRecord>> restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicMetricAnalysisRecord>>>() {});
    List<NewRelicMetricAnalysisRecord> savedRecords = restResponse.getResource();
    assertThat(savedRecords).hasSize(1);
    NewRelicMetricAnalysisRecord savedRecord = savedRecords.get(0);
    assertThat(savedRecord.getWorkflowExecutionId()).isNull();

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", accountId));

    restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicMetricAnalysisRecord>>>() {});
    savedRecords = restResponse.getResource();
    assertThat(savedRecords).hasSize(1);
    savedRecord = savedRecords.get(0);
    assertThat(savedRecord.getWorkflowExecutionId()).isEqualTo("CV-Demo");
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
                                .filter(SettingAttributeKeys.accountId, accountId)
                                .filter(SettingAttributeKeys.name, "newrelic_dev"));

    String serverConfigId = wingsPersistence.save(
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withName("newrelic_dev").build());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.FAILED);
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setStateType(StateType.NEW_RELIC.name());
    stateExecutionInstance.setDisplayName("Relic_Fail");
    Map<String, StateExecutionData> hashMap = new HashMap();
    hashMap.put("Relic_Fail", VerificationStateAnalysisExecutionData.builder().serverConfigId(serverConfigId).build());
    stateExecutionInstance.setStateExecutionMap(hashMap);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(stateExecutionInstance));

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                                    .workflowExecutionId("CV-Demo")
                                                    .stateExecutionId("CV-Demo-TS-Failure-NEW_RELIC")
                                                    .appId("CV-Demo")
                                                    .stateType(StateType.NEW_RELIC)
                                                    .metricAnalyses(new ArrayList<>())
                                                    .build();
    wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(record));

    WebTarget target = client.target(API_BASE + "/timeseries/generate-metrics-appdynamics?accountId=" + accountId
        + "&stateExecutionId=" + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId + "&appId=" + appId);
    RestResponse<List<NewRelicMetricAnalysisRecord>> restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicMetricAnalysisRecord>>>() {});
    List<NewRelicMetricAnalysisRecord> savedRecords = restResponse.getResource();
    assertThat(savedRecords).hasSize(1);
    NewRelicMetricAnalysisRecord savedRecord = savedRecords.get(0);
    assertThat(savedRecord.getWorkflowExecutionId()).isNull();

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "CV_DEMO"),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).addToSet("accountIds", accountId));

    restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicMetricAnalysisRecord>>>() {});
    savedRecords = restResponse.getResource();
    assertThat(savedRecords).hasSize(1);
    savedRecord = savedRecords.get(0);
    assertThat(savedRecord.getWorkflowExecutionId()).isEqualTo("CV-Demo");
  }

  private void analysisSorted() {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = createApp(UUID.randomUUID().toString()).getUuid();

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
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
        NewRelicMetricAnalysis.builder()
            .riskLevel(RiskLevel.LOW)
            .metricName("metric1")
            .metricValues(
                Lists.newArrayList(NewRelicMetricAnalysisValue.builder().name(REQUSET_PER_MINUTE).testValue(5).build()))
            .build();
    record.addNewRelicMetricAnalysis(analysis3);

    final NewRelicMetricAnalysis analysis4 =
        NewRelicMetricAnalysis.builder()
            .riskLevel(RiskLevel.LOW)
            .metricValues(Lists.newArrayList(
                NewRelicMetricAnalysisValue.builder().name(REQUSET_PER_MINUTE).testValue(10).build()))
            .metricName("metric0")
            .build();
    record.addNewRelicMetricAnalysis(analysis4);

    final NewRelicMetricAnalysis analysis5 =
        NewRelicMetricAnalysis.builder()
            .riskLevel(RiskLevel.LOW)
            .metricName("abc")
            .metricValues(Lists.newArrayList(
                NewRelicMetricAnalysisValue.builder().name(REQUSET_PER_MINUTE).testValue(15).build()))
            .build();
    record.addNewRelicMetricAnalysis(analysis5);

    wingsPersistence.save(record);

    WebTarget target = client.target(API_BASE + "/timeseries/generate-metrics-appdynamics?accountId=" + accountId
        + "&stateExecutionId=" + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId
        + "&appId=" + applicationId);
    RestResponse<List<NewRelicMetricAnalysisRecord>> restResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<List<NewRelicMetricAnalysisRecord>>>() {});

    List<NewRelicMetricAnalysisRecord> savedRecords = restResponse.getResource();
    assertThat(savedRecords).hasSize(1);
    NewRelicMetricAnalysisRecord savedRecord = savedRecords.get(0);
    assertThat(savedRecord).isNotNull();

    final List<NewRelicMetricAnalysis> analyses = savedRecord.getMetricAnalyses();
    assertThat(analyses).hasSize(record.getMetricAnalyses().size());

    assertThat(analyses.get(0)).isEqualTo(analysis1);
    assertThat(analyses.get(1)).isEqualTo(analysis2);
    assertThat(analyses.get(2)).isEqualTo(analysis5);
    assertThat(analyses.get(3)).isEqualTo(analysis4);
    assertThat(analyses.get(4)).isEqualTo(analysis3);
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(IntegrationTests.class)
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
  @Owner(developers = SRIRAM)
  @Category(IntegrationTests.class)
  public void noControlNoTest() throws IOException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = wingsPersistence.save(anApplication().name(generateUuid()).accountId(accountId).build());
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .workflowId(workflowId)
            .appId(appId)
            .name(workflowId + "-prev-execution-" + 0)
            .status(ExecutionStatus.SUCCESS)
            .breakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkflowExecutionId = wingsPersistence.save(workflowExecution);

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.NEW_RELIC);

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Collections.singletonList(record));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = WorkflowExecution.builder()
                            .uuid(workflowExecutionId)
                            .workflowId(workflowId)
                            .appId(appId)
                            .name(workflowId + "-curr-execution-" + 0)
                            .status(ExecutionStatus.RUNNING)
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

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Collections.singletonList(record));

    String prevWorkflowExecutionID = timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
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

    new MetricAnalysisGenerator(timeSeriesAnalysisService, learningEngineService, managerClient, analysisContext,
        Optional.of(jobExecutionContext))
        .run();

    Set<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertThat(metricAnalysisRecords).hasSize(1);

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.iterator().next();
    assertThat(metricsAnalysis.getRiskLevel()).isEqualTo(RiskLevel.NA);
    assertThat(metricsAnalysis.isShowTimeSeries()).isFalse();
    assertThat(metricsAnalysis.getMessage()).isEqualTo("No data available");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(IntegrationTests.class)
  public void controlNoTest() {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = wingsPersistence.save(anApplication().name(generateUuid()).accountId(accountId).build());
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .workflowId(workflowId)
            .appId(appId)
            .name(workflowId + "-prev-execution-" + 0)
            .status(ExecutionStatus.SUCCESS)
            .breakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkflowExecutionId = wingsPersistence.save(workflowExecution);

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.NEW_RELIC);

    NewRelicMetricDataRecord record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn1");
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(prevWorkflowExecutionId);
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

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = WorkflowExecution.builder()
                            .uuid(workflowExecutionId)
                            .workflowId(workflowId)
                            .appId(appId)
                            .name(workflowId + "-curr-execution-" + 0)
                            .status(ExecutionStatus.RUNNING)
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

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Collections.singletonList(record));

    String prevWorkflowExecutionID = timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
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

    new MetricAnalysisGenerator(timeSeriesAnalysisService, learningEngineService, managerClient, analysisContext,
        Optional.of(jobExecutionContext))
        .run();

    Set<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertThat(metricAnalysisRecords).hasSize(1);

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.iterator().next();

    assertThat(metricsAnalysis.getRiskLevel()).isEqualTo(RiskLevel.NA);
    assertThat(metricsAnalysis.isShowTimeSeries()).isFalse();
    assertThat(metricsAnalysis.getMessage()).isEqualTo("No data available");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(IntegrationTests.class)
  public void testNoControl() {
    final String workflowId = generateUuid();
    final String workflowExecutionId = generateUuid();
    final String serviceId = generateUuid();
    final String stateExecutionId = generateUuid();
    final String appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .workflowId(workflowId)
            .appId(appId)
            .name(workflowId + "-prev-execution-" + 0)
            .status(ExecutionStatus.SUCCESS)
            .breakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkflowExecutionId = wingsPersistence.save(workflowExecution);

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkflowExecutionId);
    record.setServiceId(serviceId);
    record.setStateExecutionId(prevStateExecutionId);
    record.setTimeStamp(System.currentTimeMillis());
    record.setDataCollectionMinute(0);
    record.setLevel(ClusterLevel.HF);
    record.setStateType(StateType.NEW_RELIC);

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = WorkflowExecution.builder()
                            .uuid(workflowExecutionId)
                            .workflowId(workflowId)
                            .appId(appId)
                            .name(workflowId + "-curr-execution-" + 0)
                            .status(ExecutionStatus.RUNNING)
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

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    String prevWorkflowExecutionID = timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId);
    AnalysisContext analysisContext = AnalysisContext.builder()
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

    new MetricAnalysisGenerator(timeSeriesAnalysisService, learningEngineService, managerClient, analysisContext,
        Optional.of(jobExecutionContext))
        .run();

    Set<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertThat(metricAnalysisRecords).hasSize(1);

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.iterator().next();

    assertThat(metricsAnalysis.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    assertThat(metricsAnalysis.isShowTimeSeries()).isFalse();
    assertThat(metricsAnalysis.getMessage()).isEqualTo("No problems found");
  }

  @Test
  @Owner(developers = SRIRAM, intermittent = true)
  @Category(IntegrationTests.class)
  public void txnInTestButNotControl() throws InterruptedException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String appId = wingsPersistence.save(anApplication().name(generateUuid()).accountId(accountId).build());
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .workflowId(workflowId)
            .appId(appId)
            .serviceIds(Lists.newArrayList(serviceId))
            .name(workflowId + "-prev-execution-" + 0)
            .status(ExecutionStatus.SUCCESS)
            .breakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();
    String prevWorkflowExecutionId = wingsPersistence.save(workflowExecution);

    List<String> metricNames = new ArrayList<>();
    timeSeriesAnalysisService.saveMetricTemplates(appId, StateType.NEW_RELIC, stateExecutionId,
        metricUtilHelper.metricDefinitions(
            metricUtilHelper.getMetricsCorrespondingToMetricNames(metricNames).values()));

    NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
    record.setAppId(appId);
    record.setWorkflowId(workflowId);
    record.setWorkflowExecutionId(prevWorkflowExecutionId);
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
    record1.setWorkflowExecutionId(prevWorkflowExecutionId);
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

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = WorkflowExecution.builder()
                            .uuid(workflowExecutionId)
                            .workflowId(workflowId)
                            .appId(appId)
                            .name(workflowId + "-curr-execution-" + 0)
                            .status(ExecutionStatus.RUNNING)
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

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    String lastSuccessfulWorkflowExecutionIdWithData =
        timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
            StateType.NEW_RELIC, appId, workflowId, serviceId);
    AnalysisContext analysisContext = AnalysisContext.builder()
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

    new MetricAnalysisGenerator(timeSeriesAnalysisService, learningEngineService, managerClient, analysisContext,
        Optional.of(jobExecutionContext))
        .run();

    // TODO I know....
    Thread.sleep(10000);

    Set<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
    assertThat(metricAnalysisRecords).hasSize(1);

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisRecords.iterator().next();

    assertThat(metricsAnalysis.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    assertThat(metricsAnalysis.isShowTimeSeries()).isTrue();
    assertThat(metricsAnalysis.getMessage()).isEqualTo("No problems found");
    assertThat(metricsAnalysis.getMetricAnalyses()).hasSize(1);
  }

  private URIBuilder getURIBuilder(String path, int port) {
    URIBuilder uriBuilder = new URIBuilder();
    String scheme = StringUtils.isBlank(System.getenv().get("BASE_HTTP")) ? "https" : "http";
    uriBuilder.setScheme(scheme);
    uriBuilder.setHost("localhost");
    uriBuilder.setPort(port);
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
  @Owner(developers = SRIRAM)
  @Category(IntegrationTests.class)
  public void getMetricTemplate() throws IOException, InterruptedException {
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    String appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(appId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    List<String> metricNames = new ArrayList<>();
    timeSeriesAnalysisService.saveMetricTemplates(appId, StateType.NEW_RELIC, stateExecutionId,
        metricUtilHelper.metricDefinitions(
            metricUtilHelper.getMetricsCorrespondingToMetricNames(metricNames).values()));

    String metricTemplateUrl;
    URIBuilder metricTemplateUriBuilder = getURIBuilder("/verification/timeseries/get-metric-template", 7070);
    Map<String, String> params = getParamsForMetricTemplate(appId, stateExecutionId, serviceId);
    params.forEach((name, value) -> metricTemplateUriBuilder.addParameter(name, value));

    try {
      metricTemplateUrl = metricTemplateUriBuilder.build().toString();
      WebTarget target = client.target(metricTemplateUrl);
      RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> restResponse =
          getRequestBuilderWithLearningAuthHeader(target).post(entity("{}", APPLICATION_JSON),
              new GenericType<RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>>>() {});

      String expectedTemplate = Resources.toString(
          APMVerificationStateTest.class.getResource("/apm/NewRelicMetricTemplate.json"), Charsets.UTF_8);
      assertThat(JsonUtils.asJson(restResponse.getResource())).isEqualTo(expectedTemplate);
    } catch (URISyntaxException uriSyntaxException) {
      logger.error("Failed to build URL correctly.");
    }
  }
}
