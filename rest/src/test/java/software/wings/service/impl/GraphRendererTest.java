package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.ABORTING;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.PAUSING;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RESUMED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SCHEDULED;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.REPEAT;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;
import software.wings.common.Constants;
import software.wings.sm.StateExecutionInstance;

import java.util.List;
import java.util.Map;

public class GraphRendererTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(GraphRendererTest.class);

  @Inject GraphRenderer graphRenderer;

  @Test
  public void testIsSubWorkflow() {
    assertThat(GraphRenderer.isSubWorkflow(null)).isFalse();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().build())).isFalse();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().withStateType("SUB_WORKFLOW").build())).isTrue();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().withStateType("PHASE").build())).isTrue();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().withStateType("PHASE_STEP").build())).isTrue();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().withStateType("COMMAND").build())).isFalse();
  }

  @Test
  public void testSanity() {
    List<StateExecutionInstance> stateExecutionInstances = asList(aStateExecutionInstance()
                                                                      .withStateName("origin")
                                                                      .withUuid(generateUuid())
                                                                      .withStateType("PHASE")
                                                                      .withStatus(SUCCESS)
                                                                      .build());
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    final GraphNode node = graphRenderer.generateHierarchyNode(stateExecutionInstanceMap, "origin", emptySet());
    assertThat(node).isNotNull();
  }

  @Test
  public void testGenerateHierarchyNode() {
    final StateExecutionInstance parent = aStateExecutionInstance()
                                              .withStateName("Deploy Service")
                                              .withUuid("deploy")
                                              .withStateType(PHASE.name())
                                              .withContextTransition(true)
                                              .withStatus(SUCCESS)
                                              .build();

    final StateExecutionInstance repeat = aStateExecutionInstance()
                                              .withStateName("Repeat deploy on hosts")
                                              .withUuid("repeat")
                                              .withStateType(REPEAT.name())
                                              .withContextTransition(true)
                                              .withStatus(SUCCESS)
                                              .withParentInstanceId(parent.getUuid())
                                              .build();

    final StateExecutionInstance host1 = aStateExecutionInstance()
                                             .withStateName("install on host1")
                                             .withUuid("host1")
                                             .withStateType(COMMAND.name())
                                             .withContextElement(aHostElement().withHostName("host1").build())
                                             .withContextTransition(true)
                                             .withStatus(SUCCESS)
                                             .withParentInstanceId(repeat.getUuid())
                                             .build();

    final StateExecutionInstance host2 = aStateExecutionInstance()
                                             .withStateName("install on host 2")
                                             .withUuid("host2")
                                             .withStateType(COMMAND.name())
                                             .withContextElement(aHostElement().withHostName("host2").build())
                                             .withContextTransition(true)
                                             .withStatus(SUCCESS)
                                             .withParentInstanceId(repeat.getUuid())
                                             .build();

    List<StateExecutionInstance> stateExecutionInstances = asList(parent, repeat, host1, host2);
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    final GraphNode node =
        graphRenderer.generateHierarchyNode(stateExecutionInstanceMap, parent.getStateName(), emptySet());
    assertThat(node).isNotNull();
    assertThat(node.getName()).isEqualTo(parent.getStateName());

    final GraphGroup deployGroup = node.getGroup();
    assertThat(deployGroup).isNotNull();

    final List<GraphNode> deployChildElements = deployGroup.getElements();
    assertThat(deployChildElements.size()).isEqualTo(1);
    assertThat(deployChildElements.get(0).getName()).isEqualTo(repeat.getStateName());

    final GraphGroup repeatGroup = deployChildElements.get(0).getGroup();
    assertThat(repeatGroup).isNotNull();

    final List<GraphNode> repeatChildElements = repeatGroup.getElements();
    assertThat(repeatChildElements.size()).isEqualTo(2);
    assertThat(repeatChildElements.get(0).getName()).isEqualTo("host1");
    assertThat(repeatChildElements.get(1).getName()).isEqualTo("host2");

    assertThat(repeatChildElements.get(0).getNext().getName()).isEqualTo(host1.getStateName());
    assertThat(repeatChildElements.get(1).getNext().getName()).isEqualTo(host2.getStateName());
  }

  private GraphNode getProvisionNode() {
    final StateExecutionInstance provision = aStateExecutionInstance()
                                                 .withStateName(Constants.PROVISION_NODE_NAME)
                                                 .withUuid("provision")
                                                 .withStateType(PHASE_STEP.name())
                                                 .withContextTransition(true)
                                                 .withStatus(SUCCESS)
                                                 .build();

    final StateExecutionInstance element = aStateExecutionInstance()
                                               .withStateName("first")
                                               .withUuid("first")
                                               .withStateType(COMMAND.name())
                                               .withContextTransition(true)
                                               .withStatus(SUCCESS)
                                               .withParentInstanceId(provision.getUuid())
                                               .build();

    List<StateExecutionInstance> stateExecutionInstances = asList(provision, element);
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    return graphRenderer.generateHierarchyNode(stateExecutionInstanceMap, provision.getStateName(), emptySet());
  }

  @Test
  public void testIsProvisionNode() {
    assertThat(GraphRenderer.isProvisionNode(getProvisionNode())).isTrue();
  }

  @Test
  public void testAdjustProvisionNode() {
    GraphRenderer.adjustProvisionNode((GraphNode) null);
    GraphRenderer.adjustProvisionNode((GraphGroup) null);

    {
      GraphNode node = aGraphNode().build();
      GraphRenderer.adjustProvisionNode(node);
    }

    {
      GraphNode provisionNode = getProvisionNode();
      final GraphNode element = provisionNode.getGroup().getElements().get(0);

      GraphNode node = aGraphNode().build();
      node.setNext(provisionNode);

      GraphRenderer.adjustProvisionNode(node);
      assertThat(node.getNext()).isEqualTo(element);
    }

    {
      GraphNode provisionNode = getProvisionNode();
      GraphNode next = aGraphNode().withName("next").build();
      provisionNode.setNext(next);

      final GraphNode element = provisionNode.getGroup().getElements().get(0);

      final GraphGroup group = new GraphGroup();
      group.setElements(asList(provisionNode));

      GraphRenderer.adjustProvisionNode(group);
      assertThat(group.getElements().get(0)).isEqualTo(element);
      assertThat(element.getNext()).isEqualTo(next);
    }
  }

  @Test
  public void testConvertToNode() {
    final StateExecutionInstance instance = aStateExecutionInstance()
                                                .withStateName("state name")
                                                .withUuid("uuid")
                                                .withStateType(PHASE_STEP.name())
                                                .withContextTransition(true)
                                                .withStatus(SUCCESS)
                                                .build();

    instance.setStateParams(ImmutableMap.of("key", "value"));

    GraphNode node = graphRenderer.convertToNode(instance);

    assertThat(node.getId()).isEqualTo(instance.getUuid());
    assertThat(node.getName()).isEqualTo(instance.getStateName());
    assertThat(node.getType()).isEqualTo(instance.getStateType());
    assertThat(node.isRollback()).isEqualTo(instance.isRollback());
    assertThat(node.getStatus()).isEqualTo(instance.getStatus().name());
    assertThat(node.getProperties()).isEqualTo(instance.getStateParams());
  }

  @Test
  public void testAggregateStatus() {
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, NEW, NEW))).isEqualTo(NEW);
    assertThat(GraphRenderer.aggregateStatus(asList(STARTING, STARTING, STARTING))).isEqualTo(STARTING);
    assertThat(GraphRenderer.aggregateStatus(asList(RUNNING, RUNNING, RUNNING))).isEqualTo(RUNNING);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, SUCCESS, SUCCESS))).isEqualTo(SUCCESS);
    assertThat(GraphRenderer.aggregateStatus(asList(ABORTING, ABORTING, ABORTING))).isEqualTo(ABORTING);
    assertThat(GraphRenderer.aggregateStatus(asList(ABORTED, ABORTED, ABORTED))).isEqualTo(ABORTED);
    assertThat(GraphRenderer.aggregateStatus(asList(FAILED, FAILED, FAILED))).isEqualTo(FAILED);
    assertThat(GraphRenderer.aggregateStatus(asList(QUEUED, QUEUED, QUEUED))).isEqualTo(QUEUED);
    assertThat(GraphRenderer.aggregateStatus(asList(SCHEDULED, SCHEDULED, SCHEDULED))).isEqualTo(SCHEDULED);
    assertThat(GraphRenderer.aggregateStatus(asList(ERROR, ERROR, ERROR))).isEqualTo(ERROR);
    assertThat(GraphRenderer.aggregateStatus(asList(WAITING, WAITING, WAITING))).isEqualTo(WAITING);
    assertThat(GraphRenderer.aggregateStatus(asList(PAUSING, PAUSING, PAUSING))).isEqualTo(PAUSING);
    assertThat(GraphRenderer.aggregateStatus(asList(PAUSED, PAUSED, PAUSED))).isEqualTo(PAUSED);
    assertThat(GraphRenderer.aggregateStatus(asList(RESUMED, RESUMED, RESUMED))).isEqualTo(RESUMED);

    assertThat(GraphRenderer.aggregateStatus(asList(NEW, STARTING, RUNNING, SUCCESS, ABORTING, ABORTED, FAILED, QUEUED,
                   SCHEDULED, ERROR, WAITING, PAUSING, PAUSED, RESUMED)))
        .isEqualTo(WAITING);
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, STARTING, RUNNING, SUCCESS, ABORTING, ABORTED, FAILED, QUEUED,
                   SCHEDULED, ERROR, PAUSING, PAUSED, RESUMED)))
        .isEqualTo(PAUSED);
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, STARTING, RUNNING, SUCCESS, ABORTING, ABORTED, FAILED, QUEUED,
                   SCHEDULED, ERROR, PAUSING, RESUMED)))
        .isEqualTo(PAUSING);
    assertThat(GraphRenderer.aggregateStatus(asList(
                   NEW, STARTING, RUNNING, SUCCESS, ABORTING, ABORTED, FAILED, QUEUED, SCHEDULED, ERROR, RESUMED)))
        .isEqualTo(RUNNING);
    assertThat(GraphRenderer.aggregateStatus(
                   asList(NEW, STARTING, SUCCESS, ABORTING, ABORTED, FAILED, QUEUED, SCHEDULED, ERROR, RESUMED)))
        .isIn(STARTING, ABORTING, QUEUED, SCHEDULED, RESUMED);
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, SUCCESS, ABORTED, FAILED, ERROR))).isEqualTo(NEW);

    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, ABORTED, FAILED, ERROR))).isEqualTo(ABORTED);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, FAILED, ERROR))).isEqualTo(ERROR);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, FAILED))).isEqualTo(FAILED);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS))).isEqualTo(SUCCESS);
  }

  @Test
  public void testAggregateNodeName() {
    assertThat(GraphRenderer.aggregateNodeName(true, 0, true)).isEqualTo("instances");
    assertThat(GraphRenderer.aggregateNodeName(false, 0, true)).isEqualTo("instances");
    assertThat(GraphRenderer.aggregateNodeName(true, 0, false)).isEqualTo("instances");
    assertThat(GraphRenderer.aggregateNodeName(false, 0, false)).isEqualTo("instances");

    assertThat(GraphRenderer.aggregateNodeName(true, 5, true)).isEqualTo("5 instances");
    assertThat(GraphRenderer.aggregateNodeName(false, 5, true)).isEqualTo("5 more instances");
    assertThat(GraphRenderer.aggregateNodeName(true, 5, false)).isEqualTo("5 instances");
    assertThat(GraphRenderer.aggregateNodeName(false, 5, false)).isEqualTo("5 instances");
  }
}
