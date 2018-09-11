package software.wings.service.impl.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.resources.LogMLResource;
import software.wings.scheduler.LogAnalysisManagerJob;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 5/21/18.
 */
public class LogAnalysisManagerJobTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String groupName;
  private String delegateTaskId;
  private String analysisServerConfigId;
  private String correlationId;
  private String preWorkflowExecutionId;
  private String query;
  private AnalysisContext analysisContext;

  @Inject private LogAnalysisManagerJob logAnalysisManagerJob;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LearningEngineService learningEngineService;
  @Inject private LogMLResource logMLResource;
  @Mock private AnalysisService analysisService;
  @Mock JobExecutionContext jobExecutionContext;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    groupName = UUID.randomUUID().toString();
    delegateTaskId = UUID.randomUUID().toString();
    analysisServerConfigId = UUID.randomUUID().toString();
    correlationId = UUID.randomUUID().toString();
    preWorkflowExecutionId = UUID.randomUUID().toString();
    query = UUID.randomUUID().toString();

    analysisContext = AnalysisContext.builder()
                          .accountId(accountId)
                          .appId(appId)
                          .workflowId(workflowId)
                          .workflowExecutionId(workflowExecutionId)
                          .stateExecutionId(stateExecutionId)
                          .serviceId(serviceId)
                          .controlNodes(ImmutableMap.<String, String>builder()
                                            .put("control1", DEFAULT_GROUP_NAME)
                                            .put("control2", DEFAULT_GROUP_NAME)
                                            .build())
                          .testNodes(ImmutableMap.<String, String>builder()
                                         .put("test1", DEFAULT_GROUP_NAME)
                                         .put("test2", DEFAULT_GROUP_NAME)
                                         .build())
                          .isSSL(true)
                          .appPort(1234)
                          .comparisonStrategy(COMPARE_WITH_PREVIOUS)
                          .timeDuration(15)
                          .stateType(StateType.SPLUNKV2)
                          .analysisServerConfigId(analysisServerConfigId)
                          .correlationId(correlationId)
                          .prevWorkflowExecutionId(preWorkflowExecutionId)
                          .query(query)
                          .build();

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("jobParams", JsonUtils.asJson(analysisContext));
    jobDataMap.put("delegateTaskId", delegateTaskId);
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    setInternalState(logAnalysisManagerJob, "analysisService", analysisService);
    setInternalState(logAnalysisManagerJob, "learningEngineService", learningEngineService);
  }

  @Test
  public void testMlJobQueuedPrevious() throws Exception {
    when(analysisService.isStateValid(appId, stateExecutionId)).thenReturn(true);
    when(analysisService.getCollectionMinuteForLevel(query, appId, stateExecutionId, analysisContext.getStateType(),
             ClusterLevel.L1, analysisContext.getTestNodes().keySet()))
        .thenReturn(10);

    when(analysisService.hasDataRecords(query, appId, stateExecutionId, analysisContext.getStateType(),
             analysisContext.getTestNodes().keySet(), ClusterLevel.L1, 10))
        .thenReturn(true);

    when(analysisService.getCollectionMinuteForLevel(query, appId, stateExecutionId, analysisContext.getStateType(),
             ClusterLevel.L2, analysisContext.getTestNodes().keySet()))
        .thenReturn(10);
    logAnalysisManagerJob.execute(jobExecutionContext);
    LearningEngineAnalysisTask learningEngineAnalysisTask =
        learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1);
    assertNotNull(learningEngineAnalysisTask);
    assertEquals(workflowId, learningEngineAnalysisTask.getWorkflow_id());
    assertEquals(workflowExecutionId, learningEngineAnalysisTask.getWorkflow_execution_id());
    assertEquals(stateExecutionId, learningEngineAnalysisTask.getState_execution_id());
    assertEquals(serviceId, learningEngineAnalysisTask.getService_id());
    assertEquals(0, learningEngineAnalysisTask.getAnalysis_start_min());
    assertEquals(10, learningEngineAnalysisTask.getAnalysis_minute());
    assertEquals(analysisContext.getControlNodes().keySet(), learningEngineAnalysisTask.getControl_nodes());
    assertEquals(analysisContext.getTestNodes().keySet(), learningEngineAnalysisTask.getTest_nodes());
    assertEquals(analysisContext.getStateType(), learningEngineAnalysisTask.getStateType());
    assertEquals(MLAnalysisType.LOG_ML, learningEngineAnalysisTask.getMl_analysis_type());
    assertEquals(ExecutionStatus.RUNNING, learningEngineAnalysisTask.getExecutionStatus());
    assertEquals(appId, learningEngineAnalysisTask.getAppId());
  }

  @Test
  public void testGetLastExecutionNodesNoSuccessfulWorkflow() {
    wingsPersistence.save(
        aWorkflowExecution().withAppId(appId).withWorkflowId(workflowId).withStatus(ExecutionStatus.FAILED).build());
    try {
      logMLResource.getLastExecutionNodes(accountId, appId, workflowId);
      fail("Did not throw exception for non existent successful workflow");
    } catch (WingsException e) {
      assertEquals(ErrorCode.APM_CONFIGURATION_ERROR, e.getCode());
      assertEquals(1, e.getParams().size());
      assertEquals("No successful execution exists for the workflow.", e.getParams().get("reason"));
    }
  }

  @Test
  public void testGetLastExecutionNodeWithSuccessfulWorkflow() {
    WorkflowExecution workflowExecution =
        aWorkflowExecution()
            .withAppId(appId)
            .withWorkflowId(workflowId)
            .withStatus(ExecutionStatus.SUCCESS)
            .withServiceExecutionSummaries(Lists.newArrayList(
                anElementExecutionSummary()
                    .withInstanceStatusSummaries(
                        Lists.newArrayList(anInstanceStatusSummary()
                                               .withInstanceElement(anInstanceElement().withHostName("host1").build())
                                               .build(),
                            anInstanceStatusSummary()
                                .withInstanceElement(anInstanceElement().withHostName("host2").build())
                                .build()))
                    .build(),
                anElementExecutionSummary()
                    .withInstanceStatusSummaries(
                        Lists.newArrayList(anInstanceStatusSummary()
                                               .withInstanceElement(anInstanceElement().withHostName("host3").build())
                                               .build(),
                            anInstanceStatusSummary()
                                .withInstanceElement(anInstanceElement().withHostName("host4").build())
                                .build()))
                    .build()))
            .build();
    wingsPersistence.save(workflowExecution);
    RestResponse<Map<String, InstanceElement>> lastExecutionNodes =
        logMLResource.getLastExecutionNodes(accountId, appId, workflowId);
    assertEquals(Sets.newHashSet("host1", "host2", "host3", "host4"), lastExecutionNodes.getResource().keySet());
  }
}
