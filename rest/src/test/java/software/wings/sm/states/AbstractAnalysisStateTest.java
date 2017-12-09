package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.integration.BaseIntegrationTest.setup;

import org.joor.Reflect;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
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
import software.wings.rules.RealMongo;
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
import javax.inject.Inject;

/**
 * Created by sriram_parthasarathy on 12/7/17.
 */
public class AbstractAnalysisStateTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  private final String workflowId = UUID.randomUUID().toString();
  private final String appId = UUID.randomUUID().toString();
  private final String previousWorkflowExecutionId = UUID.randomUUID().toString();

  @Mock ExecutionContext context;

  @Test
  @RealMongo
  public void getLastExecutionNodes() throws NoSuchAlgorithmException, KeyManagementException {
    List<ElementExecutionSummary> elementExecutionSummary = new ArrayList<>();
    for (String service : new String[] {"serviceA", "serviceB"}) {
      List<InstanceStatusSummary> instanceStatusSummaryList = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
        instanceStatusSummaryList.add(
            InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                .withInstanceElement(
                    InstanceElement.Builder.anInstanceElement().withHostName(service + "-" + i).build())
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
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);
    Set<String> nodes = splunkV2State.getLastExecutionNodes(context);
    for (String host : nodes) {
      assertTrue(host.startsWith("serviceA-"));
      assertEquals(nodes.size(), 5);
    }
  }
}
