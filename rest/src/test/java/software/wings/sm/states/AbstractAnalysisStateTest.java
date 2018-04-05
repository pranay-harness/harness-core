package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement.PhaseElementBuilder;
import software.wings.api.ServiceElement;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.ContainerInstanceHelper;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by sriram_parthasarathy on 12/7/17.
 */
public class AbstractAnalysisStateTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Mock private ContainerInstanceHelper containerInstanceHelper;
  @Mock private InfrastructureMappingService infraMappingService;
  private final String workflowId = UUID.randomUUID().toString();
  private final String appId = UUID.randomUUID().toString();
  private final String previousWorkflowExecutionId = UUID.randomUUID().toString();

  @Mock ExecutionContext context;

  @Before
  public void setup() {
    initMocks(this);
    when(containerInstanceHelper.isContainerDeployment(anyObject())).thenReturn(false);
    when(infraMappingService.get(anyString(), anyString())).thenReturn(null);
  }

  @Test
  public void testGetLastExecutionNodes() throws NoSuchAlgorithmException, KeyManagementException {
    List<ElementExecutionSummary> elementExecutionSummary = new ArrayList<>();
    for (String service : new String[] {"serviceA", "serviceB"}) {
      List<InstanceStatusSummary> instanceStatusSummaryList = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
        instanceStatusSummaryList.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                          .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                                   .withHostName(service + "-" + i + ".harness.com")
                                                                   .build())
                                          .build());
      }
      elementExecutionSummary.add(
          ElementExecutionSummaryBuilder.anElementExecutionSummary()
              .withContextElement(ServiceElement.Builder.aServiceElement().withUuid(service).build())
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

    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
        .thenReturn(PhaseElementBuilder.aPhaseElement()
                        .withServiceElement(ServiceElement.Builder.aServiceElement().withUuid("serviceA").build())
                        .build());
    when(context.getAppId()).thenReturn(appId);
    when(context.getWorkflowExecutionId()).thenReturn(UUID.randomUUID().toString());

    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    doReturn(workflowId).when(splunkV2State).getWorkflowId(context);
    setInternalState(splunkV2State, "containerInstanceHelper", containerInstanceHelper);
    setInternalState(splunkV2State, "infraMappingService", infraMappingService);
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);
    Set<String> nodes = splunkV2State.getLastExecutionNodes(context);
    assertEquals(5, nodes.size());
    for (int i = 0; i < 5; ++i) {
      assertTrue(nodes.contains("serviceA"
          + "-" + i + ".harness.com"));
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
      assertFalse(nodes.contains("serviceA"
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
      instanceElements.add(InstanceElement.Builder.anInstanceElement()
                               .withHostName("serviceA"
                                   + "-" + i + ".harness.com")
                               .build());
    }
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    CanaryWorkflowStandardParams params = Mockito.mock(CanaryWorkflowStandardParams.class);
    doReturn(instanceElements).when(params).getInstances();

    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);
    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    Set<String> nodes = splunkV2State.getCanaryNewHostNames(context);
    assertEquals(5, nodes.size());
    for (int i = 0; i < 5; ++i) {
      assertTrue(nodes.contains("serviceA"
          + "-" + i + ".harness.com"));
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
    }
    assertEquals(0, nodes.size());
  }
}
