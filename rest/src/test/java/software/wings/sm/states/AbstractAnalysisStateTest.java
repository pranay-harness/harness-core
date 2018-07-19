package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by sriram_parthasarathy on 12/7/17.
 */
public class AbstractAnalysisStateTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Mock private ContainerInstanceHandler containerInstanceHandler;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private InfrastructureMapping infrastructureMapping;
  private final String workflowId = UUID.randomUUID().toString();
  private final String appId = UUID.randomUUID().toString();
  private final String previousWorkflowExecutionId = UUID.randomUUID().toString();

  @Before
  public void setup() {
    initMocks(this);
    when(containerInstanceHandler.isContainerDeployment(anyObject())).thenReturn(false);
    when(infrastructureMapping.getDeploymentType()).thenReturn(DeploymentType.KUBERNETES.name());
    when(infraMappingService.get(anyString(), anyString())).thenReturn(infrastructureMapping);
  }

  @Test
  public void testGetLastExecutionNodes() throws NoSuchAlgorithmException, KeyManagementException {
    List<ElementExecutionSummary> elementExecutionSummary = new ArrayList<>();
    for (String service : new String[] {"serviceA", "serviceB"}) {
      List<InstanceStatusSummary> instanceStatusSummaryList = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
        instanceStatusSummaryList.add(
            anInstanceStatusSummary()
                .withInstanceElement(anInstanceElement().withHostName(service + "-" + i + ".harness.com").build())
                .build());
      }
      elementExecutionSummary.add(anElementExecutionSummary()
                                      .withContextElement(aServiceElement().withUuid(service).build())
                                      .withInstanceStatusSummaries(instanceStatusSummaryList)
                                      .build());
    }
    WorkflowExecution workflowExecution =
        WorkflowExecutionBuilder.aWorkflowExecution()
            .withAppId(appId)
            .withUuid(previousWorkflowExecutionId)
            .withWorkflowId(workflowId)
            .withStatus(ExecutionStatus.SUCCESS)
            .withServiceExecutionSummaries(elementExecutionSummary)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();

    Workflow workflow = WorkflowBuilder.aWorkflow().withAppId(appId).withUuid(workflowId).build();

    wingsPersistence.save(workflow);
    wingsPersistence.save(workflowExecution);

    ExecutionContext context = spy(new ExecutionContextImpl(new StateExecutionInstance()));
    doReturn(aPhaseElement().withServiceElement(aServiceElement().withUuid("serviceA").build()).build())
        .when(context)
        .getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    doReturn(appId).when(context).getAppId();
    doReturn(UUID.randomUUID().toString()).when(context).getWorkflowExecutionId();

    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    doReturn(workflowId).when(splunkV2State).getWorkflowId(context);
    setInternalState(splunkV2State, "containerInstanceHandler", containerInstanceHandler);
    setInternalState(splunkV2State, "infraMappingService", infraMappingService);
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);
    Map<String, String> nodes = splunkV2State.getLastExecutionNodes(context);
    assertEquals(5, nodes.size());
    for (int i = 0; i < 5; ++i) {
      assertTrue(nodes.keySet().contains("serviceA"
          + "-" + i + ".harness.com"));
      assertEquals(DEFAULT_GROUP_NAME,
          nodes.get("serviceA"
              + "-" + i + ".harness.com"));
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
      assertFalse(nodes.keySet().contains("serviceA"
          + "-" + i));
      nodes.remove("serviceA"
          + "-" + i);
    }
    assertEquals(0, nodes.size());
  }

  @Test
  public void testGetLastExecutionNodesWithPhase() throws NoSuchAlgorithmException, KeyManagementException {
    List<ElementExecutionSummary> elementExecutionSummary = new ArrayList<>();
    for (String service : new String[] {"serviceA", "serviceB"}) {
      List<InstanceStatusSummary> instanceStatusSummaryList = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
        instanceStatusSummaryList.add(
            anInstanceStatusSummary()
                .withInstanceElement(anInstanceElement().withHostName(service + "-" + i + ".harness.com").build())
                .build());
      }
      elementExecutionSummary.add(anElementExecutionSummary()
                                      .withContextElement(aServiceElement().withUuid(service).build())
                                      .withInstanceStatusSummaries(instanceStatusSummaryList)
                                      .build());
    }
    WorkflowExecution workflowExecution =
        WorkflowExecutionBuilder.aWorkflowExecution()
            .withAppId(appId)
            .withUuid(previousWorkflowExecutionId)
            .withWorkflowId(workflowId)
            .withStatus(ExecutionStatus.SUCCESS)
            .withServiceExecutionSummaries(elementExecutionSummary)
            .withBreakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();

    Workflow workflow = WorkflowBuilder.aWorkflow().withAppId(appId).withUuid(workflowId).build();

    wingsPersistence.save(workflow);
    wingsPersistence.save(workflowExecution);

    StateExecutionInstance stateExecutionInstance = mock(StateExecutionInstance.class);

    Map<String, StateExecutionData> stateExecutionDataMap = new HashMap<>();
    StateExecutionData stateExecutionData =
        aPhaseExecutionData()
            .withElementStatusSummary(Lists.newArrayList(
                anElementExecutionSummary()
                    .withContextElement(
                        aPhaseElement().withServiceElement(aServiceElement().withUuid("serviceA").build()).build())
                    .withInstanceStatusSummaries(Lists.newArrayList(
                        anInstanceStatusSummary()
                            .withInstanceElement(anInstanceElement().withHostName("serviceA-0.harness.com").build())
                            .build(),
                        anInstanceStatusSummary()
                            .withInstanceElement(anInstanceElement().withHostName("serviceA-1.harness.com").build())
                            .build()))
                    .build()))
            .build();
    stateExecutionData.setStateType(StateType.PHASE.name());
    stateExecutionDataMap.put(UUID.randomUUID().toString(), stateExecutionData);

    ExecutionContext context = spy(new ExecutionContextImpl(stateExecutionInstance));
    when(stateExecutionInstance.getStateExecutionMap()).thenReturn(stateExecutionDataMap);

    doReturn(aPhaseElement().withServiceElement(aServiceElement().withUuid("serviceA").build()).build())
        .when(context)
        .getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    doReturn(appId).when(context).getAppId();
    doReturn(UUID.randomUUID().toString()).when(context).getWorkflowExecutionId();

    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    doReturn(workflowId).when(splunkV2State).getWorkflowId(context);
    setInternalState(splunkV2State, "containerInstanceHandler", containerInstanceHandler);
    setInternalState(splunkV2State, "infraMappingService", infraMappingService);
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);
    Map<String, String> nodes = splunkV2State.getLastExecutionNodes(context);
    assertEquals(3, nodes.size());
    assertFalse(nodes.keySet().contains("serviceA-0.harness.com"));
    assertFalse(nodes.keySet().contains("serviceA-1.harness.com"));
    for (int i = 2; i < 5; ++i) {
      assertTrue(nodes.keySet().contains("serviceA"
          + "-" + i + ".harness.com"));
      assertEquals(DEFAULT_GROUP_NAME,
          nodes.get("serviceA"
              + "-" + i + ".harness.com"));
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
      assertFalse(nodes.keySet().contains("serviceA"
          + "-" + i));
      nodes.remove("serviceA"
          + "-" + i);
    }
    assertEquals(0, nodes.size());
  }

  @Test
  public void testGetCanaryNewNodes() throws NoSuchAlgorithmException, KeyManagementException {
    List<InstanceElement> instanceElements = new ArrayList<>();
    for (int i = 0; i < 5; ++i) {
      instanceElements.add(anInstanceElement()
                               .withHostName("serviceA"
                                   + "-" + i + ".harness.com")
                               .withWorkloadName("workload-" + i)
                               .build());
    }
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    CanaryWorkflowStandardParams params = Mockito.mock(CanaryWorkflowStandardParams.class);
    doReturn(instanceElements).when(params).getInstances();
    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
        .thenReturn(aPhaseElement().withInfraMappingId(UUID.randomUUID().toString()).build());
    when(context.getAppId()).thenReturn(appId);

    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);
    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));

    setInternalState(splunkV2State, "infraMappingService", infraMappingService);
    Map<String, String> nodes = splunkV2State.getCanaryNewHostNames(context);
    assertEquals(5, nodes.size());
    for (int i = 0; i < 5; ++i) {
      assertTrue(nodes.keySet().contains("serviceA"
          + "-" + i + ".harness.com"));
      assertEquals(DEFAULT_GROUP_NAME,
          nodes.get("serviceA"
              + "-" + i + ".harness.com"));
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
    }
    assertEquals(0, nodes.size());
  }

  @Test
  public void testGetCanaryNewNodesHelm() throws NoSuchAlgorithmException, KeyManagementException {
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping()
                        .withDeploymentType(DeploymentType.HELM.name())
                        .build());
    List<InstanceElement> instanceElements = new ArrayList<>();
    for (int i = 0; i < 5; ++i) {
      instanceElements.add(anInstanceElement()
                               .withHostName("serviceA"
                                   + "-" + i + ".harness.com")
                               .withWorkloadName("workload-" + i)
                               .build());
    }
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    CanaryWorkflowStandardParams params = Mockito.mock(CanaryWorkflowStandardParams.class);
    doReturn(instanceElements).when(params).getInstances();
    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
        .thenReturn(aPhaseElement().withInfraMappingId(UUID.randomUUID().toString()).build());
    when(context.getAppId()).thenReturn(appId);

    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);
    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));

    setInternalState(splunkV2State, "infraMappingService", infraMappingService);
    Map<String, String> nodes = splunkV2State.getCanaryNewHostNames(context);
    assertEquals(5, nodes.size());
    for (int i = 0; i < 5; ++i) {
      assertTrue(nodes.keySet().contains("serviceA"
          + "-" + i + ".harness.com"));
      assertEquals("workload-" + i,
          nodes.get("serviceA"
              + "-" + i + ".harness.com"));
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
    }
    assertEquals(0, nodes.size());
  }
}
