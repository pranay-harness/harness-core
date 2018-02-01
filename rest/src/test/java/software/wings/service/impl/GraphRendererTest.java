package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.REPEAT;

import com.google.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Graph.Group;
import software.wings.beans.Graph.Node;
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
                                                                      .withUuid(getUuid())
                                                                      .withStateType("PHASE")
                                                                      .withStatus(SUCCESS)
                                                                      .build());
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    final Node node = graphRenderer.generateHierarchyNode(stateExecutionInstanceMap, "origin");
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

    final Node node = graphRenderer.generateHierarchyNode(stateExecutionInstanceMap, parent.getStateName());
    assertThat(node).isNotNull();
    assertThat(node.getName()).isEqualTo(parent.getStateName());

    final Group deployGroup = node.getGroup();
    assertThat(deployGroup).isNotNull();

    final List<Node> deployChildElements = deployGroup.getElements();
    assertThat(deployChildElements.size()).isEqualTo(1);
    assertThat(deployChildElements.get(0).getName()).isEqualTo(repeat.getStateName());

    final Group repeatGroup = deployChildElements.get(0).getGroup();
    assertThat(repeatGroup).isNotNull();

    final List<Node> repeatChildElements = repeatGroup.getElements();
    assertThat(repeatChildElements.size()).isEqualTo(2);
    assertThat(repeatChildElements.get(0).getName()).isEqualTo("host1");
    assertThat(repeatChildElements.get(1).getName()).isEqualTo("host2");

    assertThat(repeatChildElements.get(0).getNext().getName()).isEqualTo(host1.getStateName());
    assertThat(repeatChildElements.get(1).getNext().getName()).isEqualTo(host2.getStateName());
  }
}
