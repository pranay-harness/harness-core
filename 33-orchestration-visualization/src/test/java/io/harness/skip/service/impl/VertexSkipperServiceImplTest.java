package io.harness.skip.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.skip.SkipType.NOOP;
import static io.harness.skip.SkipType.SKIP_NODE;
import static io.harness.skip.SkipType.SKIP_TREE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.OrchestrationVisualizationTest;
import io.harness.beans.EdgeList;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.category.element.UnitTests;
import io.harness.dto.OrchestrationGraph;
import io.harness.execution.status.Status;
import io.harness.rule.Owner;
import io.harness.skip.service.VertexSkipperService;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VertexSkipperServiceImplTest extends OrchestrationVisualizationTest {
  private static final String PLAN_EXECUTION_ID = generateUuid();

  @Inject VertexSkipperService vertexSkipperService;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldSkipVertices() {
    GraphVertex previous = GraphVertex.builder()
                               .uuid(generateUuid())
                               .name("previous")
                               .skipType(NOOP)
                               .startTs(System.currentTimeMillis())
                               .build();
    GraphVertex current = GraphVertex.builder()
                              .uuid(generateUuid())
                              .name("current")
                              .skipType(SKIP_TREE)
                              .startTs(System.currentTimeMillis())
                              .build();
    GraphVertex next = GraphVertex.builder()
                           .uuid(generateUuid())
                           .name("next")
                           .skipType(SKIP_NODE)
                           .startTs(System.currentTimeMillis())
                           .build();
    GraphVertex currentChild1 = GraphVertex.builder()
                                    .uuid(generateUuid())
                                    .name("currentChild1")
                                    .skipType(NOOP)
                                    .startTs(System.currentTimeMillis())
                                    .build();
    GraphVertex currentChild2 = GraphVertex.builder()
                                    .uuid(generateUuid())
                                    .name("currentChild2")
                                    .skipType(NOOP)
                                    .startTs(System.currentTimeMillis())
                                    .build();
    List<GraphVertex> graphVertices = Lists.newArrayList(previous, current, next, currentChild1, currentChild2);

    Map<String, GraphVertex> graphVertexMap =
        graphVertices.stream().collect(Collectors.toMap(GraphVertex::getUuid, Function.identity()));
    Map<String, EdgeList> adjacencyList = new HashMap<>();
    adjacencyList.put(previous.getUuid(),
        EdgeList.builder()
            .parentId(null)
            .prevIds(new ArrayList<>())
            .nextIds(Lists.newArrayList(current.getUuid()))
            .edges(new ArrayList<>())
            .build());
    adjacencyList.put(current.getUuid(),
        EdgeList.builder()
            .parentId(null)
            .prevIds(Lists.newArrayList(previous.getUuid()))
            .nextIds(Lists.newArrayList(next.getUuid()))
            .edges(Lists.newArrayList(currentChild1.getUuid(), currentChild2.getUuid()))
            .build());
    adjacencyList.put(next.getUuid(),
        EdgeList.builder()
            .parentId(null)
            .prevIds(Lists.newArrayList(current.getUuid()))
            .nextIds(new ArrayList<>())
            .edges(new ArrayList<>())
            .build());
    adjacencyList.put(currentChild1.getUuid(),
        EdgeList.builder()
            .parentId(current.getUuid())
            .prevIds(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .edges(new ArrayList<>())
            .build());
    adjacencyList.put(currentChild2.getUuid(),
        EdgeList.builder()
            .parentId(current.getUuid())
            .prevIds(new ArrayList<>())
            .nextIds(new ArrayList<>())
            .edges(new ArrayList<>())
            .build());

    OrchestrationGraph orchestrationGraph = OrchestrationGraph.builder()
                                                .startTs(System.currentTimeMillis())
                                                .endTs(System.currentTimeMillis())
                                                .status(Status.SUCCEEDED)
                                                .rootNodeIds(Collections.singletonList("someId"))
                                                .planExecutionId(PLAN_EXECUTION_ID)
                                                .adjacencyList(OrchestrationAdjacencyList.builder()
                                                                   .graphVertexMap(graphVertexMap)
                                                                   .adjacencyList(adjacencyList)
                                                                   .build())
                                                .build();

    vertexSkipperService.removeSkippedVertices(orchestrationGraph);

    OrchestrationAdjacencyList updatedOrchestrationAdjacencyList = orchestrationGraph.getAdjacencyList();
    assertThat(updatedOrchestrationAdjacencyList).isNotNull();

    Map<String, GraphVertex> updatedGraphVertexMap = updatedOrchestrationAdjacencyList.getGraphVertexMap();
    assertThat(updatedGraphVertexMap).isNotEmpty();
    assertThat(updatedGraphVertexMap.values()).containsExactlyInAnyOrder(previous);

    Map<String, EdgeList> updatedAdjacencyList = updatedOrchestrationAdjacencyList.getAdjacencyList();
    assertThat(updatedAdjacencyList).isNotEmpty();
    assertThat(updatedAdjacencyList.keySet()).containsExactlyInAnyOrder(previous.getUuid());

    assertThat(updatedAdjacencyList.get(previous.getUuid()).getNextIds()).isEmpty();
    assertThat(updatedAdjacencyList.get(previous.getUuid()).getEdges()).isEmpty();
    assertThat(updatedAdjacencyList.get(previous.getUuid()).getParentId()).isNull();
    assertThat(updatedAdjacencyList.get(previous.getUuid()).getPrevIds()).isEmpty();
  }
}
