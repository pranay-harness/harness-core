package io.harness.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.VerificationBaseIntegrationTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.jobs.MetricAnalysisJob;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.rest.RestResponse;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import software.wings.APMFetchConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

@Slf4j
public class DataDogIntegrationTest extends VerificationBaseIntegrationTest {
  @Inject private ScmSecret scmSecret;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private VerificationManagerClientHelper managerClient;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void fetch() {
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
                        .value(scmSecret.decryptToString(new SecretName("datadog_application_key")))
                        .encrypted(false)
                        .build());
    config.setOptionsList(optionsList);

    SettingAttribute settingAttribute =
        wingsPersistence.createQuery(SettingAttribute.class).filter("name", "datadog_connector").get();

    String serverConfigId;
    if (settingAttribute == null) {
      serverConfigId = wingsPersistence
                           .save(Lists.newArrayList(SettingAttribute.Builder.aSettingAttribute()
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
  @Repeat(times = 5, successes = 1)
  @Category(IntegrationTests.class)
  public void txnDatadog() throws InterruptedException {
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
    record.setStateType(StateType.DATA_DOG);

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
    record1.getValues().put("Hits", 20.0);
    record1.getValues().put("Request Duration", 2.0);
    record1.setHost("host1");
    record1.setStateType(StateType.DATA_DOG);

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

    timeSeriesAnalysisService.saveMetricData(
        accountId, appId, stateExecutionId, delegateTaskId, Lists.newArrayList(record, record1));

    timeSeriesAnalysisService.saveMetricTemplates(appId, StateType.DATA_DOG, stateExecutionId,
        DatadogState.metricDefinitions(
            DatadogState.metrics(Lists.newArrayList("trace.servlet.request.duration", "trace.servlet.request.hits"), "")
                .values()));

    String lastSuccessfulWorkflowExecutionIdWithData =
        timeSeriesAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
            StateType.DATA_DOG, appId, workflowId, serviceId);
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
                                          .stateType(StateType.DATA_DOG)
                                          .correlationId(UUID.randomUUID().toString())
                                          .prevWorkflowExecutionId(lastSuccessfulWorkflowExecutionIdWithData)
                                          .smooth_window(1)
                                          .parallelProcesses(1)
                                          .comparisonWindow(1)
                                          .tolerance(1)
                                          .prevWorkflowExecutionId(prevWorkflowExecutionId)
                                          .build();
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = mock(JobDataMap.class);
    when(jobDataMap.getLong("timestamp")).thenReturn(System.currentTimeMillis());
    when(jobDataMap.getString("jobParams")).thenReturn(JsonUtils.asJson(analysisContext));
    when(jobDataMap.getString("delegateTaskId")).thenReturn(UUID.randomUUID().toString());
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(mock(Scheduler.class));
    when(jobExecutionContext.getJobDetail()).thenReturn(mock(JobDetail.class));

    new MetricAnalysisJob
        .MetricAnalysisGenerator(
            timeSeriesAnalysisService, learningEngineService, managerClient, analysisContext, jobExecutionContext)
        .run();

    // TODO I know....
    Thread.sleep(10000);
    List<NewRelicMetricAnalysisRecord> metricAnalysisRecords =
        timeSeriesAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId);
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
