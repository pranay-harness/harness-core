package software.wings.integration;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.newrelic.MetricAnalysisJob.MetricAnalysisGenerator;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.AbstractAnalysisState;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
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
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(asList(NewRelicMetricDataRecord.class));
    deleteAllDocuments(asList(NewRelicMetricAnalysisRecord.class));
    deleteAllDocuments(asList(SettingAttribute.class));
    hosts.clear();
    hosts.add("ip-172-31-2-144");
    hosts.add("ip-172-31-4-253");
    hosts.add("ip-172-31-12-51");

    SettingAttribute newRelicSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("NewRelic")
            .withAccountId(accountId)
            .withValue(NewRelicConfig.builder()
                           .accountId(accountId)
                           .newRelicUrl("https://api.newrelic.com")
                           .apiKey("5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11".toCharArray())
                           .build())
            .build();
    wingsPersistence.saveAndGet(SettingAttribute.class, newRelicSettingAttribute);
  }

  @Test
  public void getNewRelicApplications() throws Exception {
    // find new relic setting id

    PageRequest<SettingAttribute> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("name", EQ, "NewRelic").build();
    PageResponse<SettingAttribute> settings = settingsService.list(pageRequest);

    assertEquals(1, settings.size());

    WebTarget target = client.target(
        API_BASE + "/newrelic/applications?settingId=" + settings.get(0).getUuid() + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertFalse(restResponse.getResource().isEmpty());

    for (NewRelicApplication app : restResponse.getResource()) {
      assertTrue(app.getId() > 0);
      assertFalse(StringUtils.isBlank(app.getName()));
    }
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

            record.setThroughput(r.nextDouble());
            record.setAverageResponseTime(r.nextDouble());
            record.setError(r.nextDouble());
            record.setApdexScore(r.nextDouble());

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

              record.setThroughput(r.nextDouble());
              record.setAverageResponseTime(r.nextDouble());
              record.setError(r.nextDouble());
              record.setApdexScore(r.nextDouble());

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

      Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class);
      assertEquals((batchNum + 1) * numOfMetricsPerBatch * hosts.size() * numOfMinutes, query.asList().size());
    }
  }

  @Test
  public void testAnalysisSorted() throws Exception {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = UUID.randomUUID().toString();

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                                    .workflowId(workflowId)
                                                    .workflowExecutionId(workflowExecutionId)
                                                    .stateExecutionId(stateExecutionId)
                                                    .applicationId(applicationId)
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

    WebTarget target = client.target(API_BASE + "/newrelic/generate-metrics?accountId=" + accountId
        + "&stateExecutionId=" + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId);
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
  public void noControlNoTest() throws IOException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(applicationId)
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
        accountId, applicationId, prevStateExecutionId, delegateTaskId, Collections.singletonList(record));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(applicationId)
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
        accountId, applicationId, stateExecutionId, delegateTaskId, Collections.singletonList(record));

    String prevWorkflowExecutionID = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(applicationId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .testNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .stateBaseUrl("/newrelic")
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

    new MetricAnalysisGenerator(metricDataAnalysisService, waitNotifyEngine, delegateService, analysisContext,
        jobExecutionContext, delegateTaskId)
        .run();

    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(StateType.NEW_RELIC, stateExecutionId, workflowExecutionId);
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
    final String applicationId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(applicationId)
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
    record1.setRequestsPerMinute(20);
    record1.setAverageResponseTime(50);
    record1.setError(-1);
    record1.setApdexScore(1.0);
    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, applicationId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(applicationId)
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
        accountId, applicationId, stateExecutionId, delegateTaskId, Collections.singletonList(record));

    String prevWorkflowExecutionID = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(applicationId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .testNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .stateBaseUrl("newrelic")
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

    new MetricAnalysisGenerator(metricDataAnalysisService, waitNotifyEngine, delegateService, analysisContext,
        jobExecutionContext, delegateTaskId)
        .run();

    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(StateType.NEW_RELIC, stateExecutionId, workflowExecutionId);

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
    final String applicationId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(applicationId)
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
        accountId, applicationId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(applicationId)
                            .withName(workflowId + "-curr-execution-" + 0)
                            .withStatus(ExecutionStatus.RUNNING)
                            .build();
    wingsPersistence.save(workflowExecution);

    record = new NewRelicMetricDataRecord();
    record.setName("New Relic Heartbeat");
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
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(workflowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(stateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);
    record1.setRequestsPerMinute(20);
    record1.setAverageResponseTime(50);
    record1.setError(-1);
    record1.setApdexScore(1.0);
    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    String prevWorkflowExecutionID = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(applicationId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .testNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .stateBaseUrl("newrelic")
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

    new MetricAnalysisGenerator(metricDataAnalysisService, waitNotifyEngine, delegateService, analysisContext,
        jobExecutionContext, delegateTaskId)
        .run();

    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(StateType.NEW_RELIC, stateExecutionId, workflowExecutionId);

    assertEquals(RiskLevel.LOW, metricsAnalysis.getRiskLevel());
    assertFalse(metricsAnalysis.isShowTimeSeries());
    assertEquals("No problems found", metricsAnalysis.getMessage());
  }

  @Test
  public void txnInTestButNotControl() throws IOException {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = UUID.randomUUID().toString();
    final String delegateTaskId = UUID.randomUUID().toString();

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    String prevStateExecutionId = UUID.randomUUID().toString();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(prevStateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withWorkflowId(workflowId)
            .withAppId(applicationId)
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
    record1.setRequestsPerMinute(20);
    record1.setAverageResponseTime(50);
    record1.setError(-1);
    record1.setApdexScore(1.0);
    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, applicationId, prevStateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    stateExecutionInstance.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(stateExecutionInstance);

    stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(applicationId);
    stateExecutionInstance.setUuid(stateExecutionId);
    stateExecutionInstance.setStatus(ExecutionStatus.RUNNING);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecution = aWorkflowExecution()
                            .withUuid(workflowExecutionId)
                            .withWorkflowId(workflowId)
                            .withAppId(applicationId)
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

    record1 = new NewRelicMetricDataRecord();
    record1.setName("Dummy txn2");
    record1.setWorkflowId(workflowId);
    record1.setWorkflowExecutionId(workflowExecutionId);
    record1.setServiceId(serviceId);
    record1.setStateExecutionId(stateExecutionId);
    record1.setTimeStamp(System.currentTimeMillis());
    record1.setDataCollectionMinute(0);
    record1.setRequestsPerMinute(20);
    record1.setAverageResponseTime(50);
    record1.setError(-1);
    record1.setApdexScore(1.0);
    record1.setHost("host1");
    record1.setStateType(StateType.NEW_RELIC);

    metricDataAnalysisService.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    String prevWorkflowExecutionID = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, workflowId, serviceId);
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(applicationId)
            .workflowId(workflowId)
            .workflowExecutionId(workflowExecutionId)
            .stateExecutionId(stateExecutionId)
            .serviceId(serviceId)
            .controlNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .testNodes(com.google.common.collect.Sets.newHashSet("host1"))
            .isSSL(true)
            .appPort(9090)
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .timeDuration(1)
            .stateType(StateType.NEW_RELIC)
            .stateBaseUrl("newrelic")
            .authToken(AbstractAnalysisState.generateAuthToken("nhUmut2NMcUnsR01OgOz0e51MZ51AqUwrOATJ3fJ"))
            .correlationId(UUID.randomUUID().toString())
            .prevWorkflowExecutionId(prevWorkflowExecutionID == null ? "-1" : prevWorkflowExecutionID)
            .smooth_window(1)
            .parallelProcesses(1)
            .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new MetricAnalysisGenerator(metricDataAnalysisService, waitNotifyEngine, delegateService, analysisContext,
        jobExecutionContext, delegateTaskId)
        .run();

    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(StateType.NEW_RELIC, stateExecutionId, workflowExecutionId);

    assertEquals(RiskLevel.LOW, metricsAnalysis.getRiskLevel());
    assertTrue(metricsAnalysis.isShowTimeSeries());
    assertEquals("No problems found", metricsAnalysis.getMessage());
    assertEquals(1, metricsAnalysis.getMetricAnalyses().size());
  }
}
