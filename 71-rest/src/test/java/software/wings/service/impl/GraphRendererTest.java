package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.ABORTED;
import static io.harness.beans.ExecutionStatus.DISCONTINUING;
import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.NEW;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.PAUSING;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RESUMED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SCHEDULED;
import static io.harness.beans.ExecutionStatus.STARTING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.REPEAT;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.sm.StateExecutionInstance;

import java.util.List;
import java.util.Map;

@Slf4j
public class GraphRendererTest extends WingsBaseTest {
  @Inject GraphRenderer graphRenderer;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testIsSubWorkflow() {
    assertThat(GraphRenderer.isSubWorkflow(null)).isFalse();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().build())).isFalse();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().stateType("SUB_WORKFLOW").build())).isTrue();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().stateType("PHASE").build())).isTrue();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().stateType("PHASE_STEP").build())).isTrue();
    assertThat(GraphRenderer.isSubWorkflow(aStateExecutionInstance().stateType("COMMAND").build())).isFalse();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSanity() {
    List<StateExecutionInstance> stateExecutionInstances = asList(aStateExecutionInstance()
                                                                      .displayName("origin")
                                                                      .uuid(generateUuid())
                                                                      .stateType("PHASE")
                                                                      .status(SUCCESS)
                                                                      .build());
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    final GraphNode node = graphRenderer.generateHierarchyNode(stateExecutionInstanceMap);
    assertThat(node).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGenerateHierarchyNode() {
    final StateExecutionInstance parent = aStateExecutionInstance()
                                              .displayName("Deploy Service")
                                              .uuid("deploy")
                                              .stateType(PHASE.name())
                                              .contextTransition(true)
                                              .status(SUCCESS)
                                              .build();

    final StateExecutionInstance repeat = aStateExecutionInstance()
                                              .displayName("Repeat deploy on hosts")
                                              .uuid("repeat")
                                              .stateType(REPEAT.name())
                                              .contextTransition(true)
                                              .status(SUCCESS)
                                              .parentInstanceId(parent.getUuid())
                                              .build();

    final StateExecutionInstance host1 = aStateExecutionInstance()
                                             .displayName("install on host1")
                                             .uuid("host1")
                                             .stateType(COMMAND.name())
                                             .contextElement(aHostElement().hostName("host1").build())
                                             .contextTransition(true)
                                             .status(SUCCESS)
                                             .parentInstanceId(repeat.getUuid())
                                             .build();

    final StateExecutionInstance host2 = aStateExecutionInstance()
                                             .displayName("install on host 2")
                                             .uuid("host2")
                                             .stateType(COMMAND.name())
                                             .contextElement(aHostElement().hostName("host2").build())
                                             .contextTransition(true)
                                             .status(SUCCESS)
                                             .parentInstanceId(repeat.getUuid())
                                             .build();

    List<StateExecutionInstance> stateExecutionInstances = asList(parent, repeat, host1, host2);
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    final GraphNode node = graphRenderer.generateHierarchyNode(stateExecutionInstanceMap);
    assertThat(node).isNotNull();
    assertThat(node.getName()).isEqualTo(parent.getDisplayName());

    final GraphGroup deployGroup = node.getGroup();
    assertThat(deployGroup).isNotNull();

    final List<GraphNode> deployChildElements = deployGroup.getElements();
    assertThat(deployChildElements.size()).isEqualTo(1);
    assertThat(deployChildElements.get(0).getName()).isEqualTo(repeat.getDisplayName());

    final GraphGroup repeatGroup = deployChildElements.get(0).getGroup();
    assertThat(repeatGroup).isNotNull();

    final List<GraphNode> repeatChildElements = repeatGroup.getElements();
    assertThat(repeatChildElements.size()).isEqualTo(2);
    assertThat(repeatChildElements.get(0).getName()).isEqualTo("host1");
    assertThat(repeatChildElements.get(1).getName()).isEqualTo("host2");

    assertThat(repeatChildElements.get(0).getNext().getName()).isEqualTo(host1.getDisplayName());
    assertThat(repeatChildElements.get(1).getNext().getName()).isEqualTo(host2.getDisplayName());
  }

  private GraphNode getInfrastructureNode() {
    final StateExecutionInstance infrastructure = aStateExecutionInstance()
                                                      .displayName(WorkflowServiceHelper.INFRASTRUCTURE_NODE_NAME)
                                                      .uuid("infrastructure")
                                                      .stateType(PHASE_STEP.name())
                                                      .contextTransition(true)
                                                      .status(SUCCESS)
                                                      .build();

    final StateExecutionInstance element = aStateExecutionInstance()
                                               .displayName("first")
                                               .uuid("first")
                                               .stateType(COMMAND.name())
                                               .contextTransition(true)
                                               .status(SUCCESS)
                                               .parentInstanceId(infrastructure.getUuid())
                                               .build();

    List<StateExecutionInstance> stateExecutionInstances = asList(infrastructure, element);
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    return graphRenderer.generateHierarchyNode(stateExecutionInstanceMap);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testConvertToNode() {
    final StateExecutionInstance instance = aStateExecutionInstance()
                                                .displayName("state name")
                                                .uuid("uuid")
                                                .stateType(PHASE_STEP.name())
                                                .contextTransition(true)
                                                .status(SUCCESS)
                                                .build();

    instance.setStateParams(ImmutableMap.of("key", "value"));

    GraphNode node = graphRenderer.convertToNode(instance);

    assertThat(node.getId()).isEqualTo(instance.getUuid());
    assertThat(node.getName()).isEqualTo(instance.getDisplayName());
    assertThat(node.getType()).isEqualTo(instance.getStateType());
    assertThat(node.isRollback()).isEqualTo(instance.isRollback());
    assertThat(node.getStatus()).isEqualTo(instance.getStatus().name());
    assertThat(node.getProperties()).isEqualTo(instance.getStateParams());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testAggregateStatus() {
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, NEW, NEW))).isEqualTo(NEW);
    assertThat(GraphRenderer.aggregateStatus(asList(STARTING, STARTING, STARTING))).isEqualTo(STARTING);
    assertThat(GraphRenderer.aggregateStatus(asList(RUNNING, RUNNING, RUNNING))).isEqualTo(RUNNING);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, SUCCESS, SUCCESS))).isEqualTo(SUCCESS);
    assertThat(GraphRenderer.aggregateStatus(asList(DISCONTINUING, DISCONTINUING, DISCONTINUING)))
        .isEqualTo(DISCONTINUING);
    assertThat(GraphRenderer.aggregateStatus(asList(ABORTED, ABORTED, ABORTED))).isEqualTo(ABORTED);
    assertThat(GraphRenderer.aggregateStatus(asList(FAILED, FAILED, FAILED))).isEqualTo(FAILED);
    assertThat(GraphRenderer.aggregateStatus(asList(QUEUED, QUEUED, QUEUED))).isEqualTo(QUEUED);
    assertThat(GraphRenderer.aggregateStatus(asList(SCHEDULED, SCHEDULED, SCHEDULED))).isEqualTo(SCHEDULED);
    assertThat(GraphRenderer.aggregateStatus(asList(ERROR, ERROR, ERROR))).isEqualTo(ERROR);
    assertThat(GraphRenderer.aggregateStatus(asList(WAITING, WAITING, WAITING))).isEqualTo(WAITING);
    assertThat(GraphRenderer.aggregateStatus(asList(PAUSING, PAUSING, PAUSING))).isEqualTo(PAUSING);
    assertThat(GraphRenderer.aggregateStatus(asList(PAUSED, PAUSED, PAUSED))).isEqualTo(PAUSED);
    assertThat(GraphRenderer.aggregateStatus(asList(RESUMED, RESUMED, RESUMED))).isEqualTo(RESUMED);

    assertThat(GraphRenderer.aggregateStatus(asList(NEW, STARTING, RUNNING, SUCCESS, DISCONTINUING, ABORTED, FAILED,
                   QUEUED, SCHEDULED, ERROR, WAITING, PAUSING, PAUSED, RESUMED)))
        .isEqualTo(WAITING);
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, STARTING, RUNNING, SUCCESS, DISCONTINUING, ABORTED, FAILED,
                   QUEUED, SCHEDULED, ERROR, PAUSING, PAUSED, RESUMED)))
        .isEqualTo(PAUSED);
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, STARTING, RUNNING, SUCCESS, DISCONTINUING, ABORTED, FAILED,
                   QUEUED, SCHEDULED, ERROR, PAUSING, RESUMED)))
        .isEqualTo(PAUSING);
    assertThat(GraphRenderer.aggregateStatus(asList(
                   NEW, STARTING, RUNNING, SUCCESS, DISCONTINUING, ABORTED, FAILED, QUEUED, SCHEDULED, ERROR, RESUMED)))
        .isEqualTo(RUNNING);
    assertThat(GraphRenderer.aggregateStatus(
                   asList(NEW, STARTING, SUCCESS, DISCONTINUING, ABORTED, FAILED, QUEUED, SCHEDULED, ERROR, RESUMED)))
        .isIn(STARTING, DISCONTINUING, QUEUED, SCHEDULED, RESUMED);
    assertThat(GraphRenderer.aggregateStatus(asList(NEW, SUCCESS, ABORTED, FAILED, ERROR))).isEqualTo(NEW);

    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, ABORTED, FAILED, ERROR))).isEqualTo(ABORTED);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, FAILED, ERROR))).isEqualTo(ERROR);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS, FAILED))).isEqualTo(FAILED);
    assertThat(GraphRenderer.aggregateStatus(asList(SUCCESS))).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
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
