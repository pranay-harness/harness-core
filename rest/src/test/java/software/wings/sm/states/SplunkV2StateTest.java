package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.rules.RealMongo;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/9/17.
 */
public class SplunkV2StateTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String delegateTaskId;
  @Mock private ExecutionContext executionContext;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private AnalysisService analysisService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private MainConfiguration configuration;
  private SplunkV2State splunkState;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    delegateTaskId = UUID.randomUUID().toString();

    wingsPersistence.save(Application.Builder.anApplication().withUuid(appId).withAccountId(accountId).build());
    wingsPersistence.save(WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution()
                              .withAppId(appId)
                              .withWorkflowId(workflowId)
                              .build());
    configuration.getPortal().setJwtExternalServiceSecret(accountId);
    MockitoAnnotations.initMocks(this);
    when(executionContext.getAppId()).thenReturn(appId);
    when(executionContext.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
    when(executionContext.getStateExecutionInstanceId()).thenReturn(stateExecutionId);
    splunkState = new SplunkV2State("SplunkState");
    splunkState.setQuery("exception");
    splunkState.setTimeDuration("15");
    setInternalState(splunkState, "appService", appService);
    setInternalState(splunkState, "configuration", configuration);
    setInternalState(splunkState, "analysisService", analysisService);
  }

  @Test
  public void testDefaultComparsionStrategy() {
    SplunkV2State splunkState = new SplunkV2State("SplunkState");
    assertEquals(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS, splunkState.getComparisonStrategy());
  }

  @Test
  @RealMongo
  public void noTestNodes() {
    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.emptySet()).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.FAILED, response.getExecutionStatus());
    assertEquals("Could not find test nodes to compare the data", response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.LOW, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  @RealMongo
  public void noControlNodesCompareWithCurrent() {
    splunkState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.singleton("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.emptySet()).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.SUCCESS, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline data (First time deployment).", response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.LOW, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }

  @Test
  @RealMongo
  public void compareWithCurrentSameTestAndControlNodes() {
    splunkState.setComparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name());
    SplunkV2State spyState = spy(splunkState);
    doReturn(Collections.singleton("some-host")).when(spyState).getCanaryNewHostNames(executionContext);
    doReturn(Collections.singleton("some-host")).when(spyState).getLastExecutionNodes(executionContext);
    doReturn(workflowId).when(spyState).getWorkflowId(executionContext);
    doReturn(serviceId).when(spyState).getPhaseServiceId(executionContext);

    ExecutionResponse response = spyState.execute(executionContext);
    assertEquals(ExecutionStatus.FAILED, response.getExecutionStatus());
    assertEquals("Skipping analysis due to lack of baseline data (Minimum two phases are required).",
        response.getErrorMessage());

    LogMLAnalysisSummary analysisSummary =
        analysisService.getAnalysisSummary(stateExecutionId, appId, StateType.SPLUNKV2);
    assertEquals(RiskLevel.LOW, analysisSummary.getRiskLevel());
    assertEquals(splunkState.getQuery(), analysisSummary.getQuery());
    assertEquals(response.getErrorMessage(), analysisSummary.getAnalysisSummaryMessage());
    assertTrue(analysisSummary.getControlClusters().isEmpty());
    assertTrue(analysisSummary.getTestClusters().isEmpty());
    assertTrue(analysisSummary.getUnknownClusters().isEmpty());
  }
}
