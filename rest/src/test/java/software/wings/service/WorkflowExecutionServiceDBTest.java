package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.CountsByStatuses.Builder.aCountsByStatuses;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.STATE_MACHINE_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.resources.WorkflowResource;
import software.wings.rules.Listeners;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.BarrierState;
import software.wings.waitnotify.NotifyEventListener;

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
  public void shouldListExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    final WorkflowExecutionBuilder workflowExecutionBuilder =
        aWorkflowExecution().withAppId(APP_ID).withEnvId(ENV_ID).withStateMachineId(STATE_MACHINE_ID);

    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(SUCCESS).build());
    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid())
                              .withStatus(ExecutionStatus.ERROR)
                              .withBreakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid())
                              .withStatus(ExecutionStatus.FAILED)
                              .withBreakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(
        workflowExecutionBuilder.withUuid(generateUuid()).withStatus(ExecutionStatus.ABORTED).build());

    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(RUNNING).build());

    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(PAUSED).build());
    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(WAITING).build());

    PageResponse<WorkflowExecution> pageResponse = workflowExecutionService.listExecutions(
        aPageRequest().addFilter(WorkflowExecution.APP_ID_KEY, Operator.EQ, APP_ID).build(), false, true, false, true);
    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.size()).isEqualTo(7);
  }

  @Test
  public void shouldListDeployedNodes() {
    String appId = generateUuid();
    String envId = generateUuid();
    String workflowId = wingsPersistence.save(aWorkflow().withAppId(appId).withEnvId(envId).build());
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

    WorkflowExecution workflowExecution = aWorkflowExecution()
                                              .withAppId(appId)
                                              .withEnvId(envId)
                                              .withStateMachineId(stateMachineId)
                                              .withWorkflowId(workflowId)
                                              .withStatus(ExecutionStatus.SUCCESS)
                                              .withServiceExecutionSummaries(executionSummaries)
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