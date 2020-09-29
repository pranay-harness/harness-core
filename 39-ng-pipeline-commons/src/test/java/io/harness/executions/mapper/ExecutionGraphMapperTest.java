package io.harness.executions.mapper;

import static io.harness.cdng.pipeline.executions.ExecutionStatus.FAILED;
import static io.harness.cdng.pipeline.executions.ExecutionStatus.RUNNING;
import static io.harness.cdng.pipeline.executions.ExecutionStatus.SUCCESS;
import static io.harness.cdng.pipeline.executions.ExecutionStatus.WAITING;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.category.element.UnitTests;
import io.harness.dto.OrchestrationGraph;
import io.harness.execution.status.Status;
import io.harness.executions.beans.ExecutionGraph;
import io.harness.executions.beans.ExecutionNode;
import io.harness.executions.beans.ExecutionNodeAdjacencyList;
import io.harness.rule.Owner;
import lombok.NonNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExecutionGraphMapperTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testToExecutionGraph() {
    OrchestrationGraph orchestrationGraph = prepareOrchestrationGraph();
    ExecutionGraph executionGraph = prepareExpectedExecutionGraph();

    @NonNull ExecutionGraph actualExecutionGraph = ExecutionGraphMapper.toExecutionGraph(orchestrationGraph);
    assertThat(actualExecutionGraph).isEqualTo(executionGraph);
  }

  private ExecutionGraph prepareExpectedExecutionGraph() {
    ExecutionNode executionNode1 = ExecutionNode.builder().name("node1").uuid("id1").status(RUNNING).build();
    ExecutionNode executionNode2 = ExecutionNode.builder().name("node2").uuid("id2").status(FAILED).build();
    ExecutionNode executionNode3 = ExecutionNode.builder().name("node3").uuid("id3").status(SUCCESS).build();
    ExecutionNode executionNode4 = ExecutionNode.builder().name("node4").uuid("id4").status(WAITING).build();

    Map<String, ExecutionNode> executionNodeMap = new HashMap<>();
    executionNodeMap.put("id1", executionNode1);
    executionNodeMap.put("id2", executionNode2);
    executionNodeMap.put("id3", executionNode3);
    executionNodeMap.put("id4", executionNode4);

    ExecutionNodeAdjacencyList executionNodeAdjacencyList =
        ExecutionNodeAdjacencyList.builder().children(Arrays.asList("id2", "id3")).next("id4").build();
    Map<String, ExecutionNodeAdjacencyList> adjacencyListMap = new HashMap<>();
    adjacencyListMap.put("id1", executionNodeAdjacencyList);

    return ExecutionGraph.builder()
        .rootNodeId("id1")
        .nodeMap(executionNodeMap)
        .nodeAdjacencyListMap(adjacencyListMap)
        .build();
  }

  private OrchestrationGraph prepareOrchestrationGraph() {
    GraphVertex node1 = GraphVertex.builder().name("node1").status(Status.RUNNING).uuid("id1").build();
    GraphVertex node2 = GraphVertex.builder().name("node2").status(Status.FAILED).uuid("id2").build();
    GraphVertex node3 = GraphVertex.builder().name("node3").status(Status.SUCCEEDED).uuid("id3").build();
    GraphVertex node4 = GraphVertex.builder().name("node4").status(Status.TIMED_WAITING).uuid("id4").build();

    Map<String, GraphVertex> graphVertexMap = new HashMap<>();
    graphVertexMap.put("id1", node1);
    graphVertexMap.put("id2", node2);
    graphVertexMap.put("id3", node3);
    graphVertexMap.put("id4", node4);

    EdgeList edgeList =
        EdgeList.builder().edges(Arrays.asList("id2", "id3")).nextIds(Collections.singletonList("id4")).build();
    Map<String, EdgeList> edgeListMap = new HashMap<>();
    edgeListMap.put("id1", edgeList);

    OrchestrationAdjacencyList orchestrationAdjacencyList =
        OrchestrationAdjacencyList.builder().graphVertexMap(graphVertexMap).adjacencyList(edgeListMap).build();

    return OrchestrationGraph.builder()
        .rootNodeIds(Collections.singletonList("id1"))
        .adjacencyList(orchestrationAdjacencyList)
        .build();
  }
}