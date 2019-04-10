package software.wings.service;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.CountsByStatuses.Builder.aCountsByStatuses;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.STATE_MACHINE_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.waiter.NotifyEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.resources.WorkflowResource;
import software.wings.rules.Listeners;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.BarrierState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class workflowExecutionServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowExecutionServiceDBTest extends WingsBaseTest {
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Inject private WorkflowResource workflowResource;
  @Inject private HostService hostService;

  @Before
  public void init() {
    wingsPersistence.save(aStateMachine()
                              .withAppId(APP_ID)
                              .withUuid(STATE_MACHINE_ID)
                              .withInitialStateName("foo")
                              .addState(new BarrierState("foo"))
                              .build());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    final WorkflowExecutionBuilder workflowExecutionBuilder = WorkflowExecution.builder().appId(APP_ID).envId(ENV_ID);

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(SUCCESS).build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid())
                              .status(ExecutionStatus.ERROR)
                              .breakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid())
                              .status(ExecutionStatus.FAILED)
                              .breakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(ExecutionStatus.ABORTED).build());

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(RUNNING).build());

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(PAUSED).build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(WAITING).build());

    PageResponse<WorkflowExecution> pageResponse = workflowExecutionService.listExecutions(
        aPageRequest().addFilter(WorkflowExecutionKeys.appId, Operator.EQ, APP_ID).build(), false, true, false, true);
    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.size()).isEqualTo(7);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListDeployedNodes() {
    String appId = generateUuid();
    String envId = generateUuid();
    String workflowId = wingsPersistence.save(aWorkflow().appId(appId).envId(envId).build());
    int numOfExecutionSummaries = 2;
    int numOfHosts = 3;
    List<ElementExecutionSummary> executionSummaries = new ArrayList<>();
    Map<String, HostElement> hostElements = new HashMap<>();
    for (int i = 0; i < numOfExecutionSummaries; i++) {
      List<InstanceStatusSummary> instanceElements = new ArrayList<>();
      for (int j = 0; j < numOfHosts; j++) {
        Instance ec2Instance = new Instance();
        ec2Instance.setPrivateDnsName(generateUuid());
        ec2Instance.setPublicDnsName(generateUuid());

        Host host =
            aHost().withEc2Instance(ec2Instance).withAppId(appId).withEnvId(envId).withHostName(generateUuid()).build();
        String hostId = wingsPersistence.save(host);
        HostElement hostElement = aHostElement().withHostName(generateUuid()).withUuid(hostId).build();
        instanceElements.add(
            anInstanceStatusSummary().withInstanceElement(anInstanceElement().withHost(hostElement).build()).build());
        hostElements.put(hostId, hostElement);
      }
      executionSummaries.add(anElementExecutionSummary().withInstanceStatusSummaries(instanceElements).build());
    }
    StateMachine stateMachine = new StateMachine();
    stateMachine.setInitialStateName("some-state");
    stateMachine.setStates(Lists.newArrayList(new ApprovalState(stateMachine.getInitialStateName())));
    stateMachine.setAppId(appId);
    String stateMachineId = wingsPersistence.save(stateMachine);

    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .appId(appId)
                                              .envId(envId)
                                              .stateMachine(stateMachine)
                                              .workflowId(workflowId)
                                              .status(ExecutionStatus.SUCCESS)
                                              .serviceExecutionSummaries(executionSummaries)
                                              .build();
    wingsPersistence.save(workflowExecution);
    List<InstanceElement> deployedNodes = workflowResource.getDeployedNodes(appId, workflowId).getResource();
    assertEquals(numOfExecutionSummaries * numOfHosts, deployedNodes.size());
    deployedNodes.forEach(deployedNode -> {
      assertTrue(hostElements.containsKey(deployedNode.getHost().getUuid()));
      HostElement hostElement = hostElements.get(deployedNode.getHost().getUuid());
      assertEquals(hostElement.getHostName(), deployedNode.getHost().getHostName());
      assertEquals(hostService.get(appId, envId, hostElement.getUuid()).getEc2Instance(),
          deployedNode.getHost().getEc2Instance());
    });
  }
}