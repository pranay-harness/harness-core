package io.harness.functional.redesign.engine;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EdgeList;
import io.harness.category.element.FunctionalTests;
import io.harness.dto.GraphVertexDTO;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.execution.PlanExecution;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.redesign.OrchestrationEngineTestSetupHelper;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.MockServerExecutor;

import software.wings.beans.Application;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.core.GenericType;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Functional Tests for {@link io.harness.service.GraphGenerationService}
 */
public class GraphGenerationFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private OrchestrationEngineTestSetupHelper engineTestSetupHelper;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private MockServerExecutor mockServerExecutor;

  OwnerManager.Owners owners;
  Application application;

  final Randomizer.Seed seed = new Randomizer.Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    mockServerExecutor.ensureMockServer(AbstractFunctionalTest.class);
  }

  @Test
  @Owner(developers = ALEXEI, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldGenerateOrchestrationGraph() {
    PlanExecution planExecutionResponse = engineTestSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
    assertThat(planExecutionResponse.getStatus()).isEqualTo(Status.SUCCEEDED);

    OrchestrationGraphDTO response =
        requestOrchestrationGraph(null, planExecutionResponse.getUuid(), "get-orchestration-graph");
    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldGeneratePartialOrchestrationGraph() {
    PlanExecution planExecutionResponse = engineTestSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
    assertThat(planExecutionResponse.getStatus()).isEqualTo(Status.SUCCEEDED);

    OrchestrationGraphDTO response =
        requestOrchestrationGraph(null, planExecutionResponse.getUuid(), "get-orchestration-graph");
    assertThat(response).isNotNull();

    GraphVertexDTO forkVertex = response.getAdjacencyList()
                                    .getGraphVertexMap()
                                    .values()
                                    .stream()
                                    .filter(graphVertex -> graphVertex.getName().equals("fork2"))
                                    .findFirst()
                                    .orElse(null);
    assertThat(forkVertex).isNotNull();

    OrchestrationGraphDTO partialOrchestrationResponse = requestOrchestrationGraph(
        forkVertex.getPlanNodeId(), planExecutionResponse.getUuid(), "get-partial-orchestration-graph");
    assertThat(partialOrchestrationResponse).isNotNull();
    assertThat(partialOrchestrationResponse.getAdjacencyList().getGraphVertexMap().size()).isEqualTo(3);
    assertThat(partialOrchestrationResponse.getAdjacencyList()
                   .getGraphVertexMap()
                   .values()
                   .stream()
                   .map(GraphVertexDTO::getName)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("fork2", "http1", "http2");
    assertThat(partialOrchestrationResponse.getAdjacencyList().getAdjacencyMap().size()).isEqualTo(3);
    assertThat(
        partialOrchestrationResponse.getAdjacencyList().getAdjacencyMap().get(forkVertex.getUuid()).getEdges().size())
        .isEqualTo(2);
    assertThat(partialOrchestrationResponse.getAdjacencyList().getAdjacencyMap().get(forkVertex.getUuid()).getEdges())
        .containsExactlyInAnyOrderElementsOf(partialOrchestrationResponse.getAdjacencyList()
                                                 .getGraphVertexMap()
                                                 .values()
                                                 .stream()
                                                 .filter(vertex -> vertex.getName().startsWith("http"))
                                                 .map(GraphVertexDTO::getUuid)
                                                 .collect(Collectors.toList()));
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldGenerateOrchestrationGraphWithBarriers() {
    List<String> nodeNames = Lists.newArrayList("Dummy Node 2", "barrier3", "Dummy Node 1", "barrier1", "Dummy Node 3",
        "Dummy Node 4", "Wait Node", "barrier2", "Fork Node");
    PlanExecution planExecutionResponse = engineTestSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "multiple-barriers");
    assertThat(planExecutionResponse.getStatus()).isEqualTo(Status.SUCCEEDED);

    OrchestrationGraphDTO response =
        requestOrchestrationGraph(null, planExecutionResponse.getUuid(), "get-orchestration-graph-v2");
    assertThat(response).isNotNull();

    assertThat(response.getPlanExecutionId()).isEqualTo(planExecutionResponse.getUuid());
    assertThat(response.getRootNodeIds()).isNotEmpty();
    assertThat(response.getStartTs()).isNotNull();
    assertThat(response.getEndTs()).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);

    Map<String, GraphVertexDTO> graphVertexMap = response.getAdjacencyList().getGraphVertexMap();
    Map<String, EdgeList> adjacencyList = response.getAdjacencyList().getAdjacencyMap();
    assertThat(graphVertexMap.size()).isEqualTo(9);
    assertThat(adjacencyList.size()).isEqualTo(9);

    assertThat(
        graphVertexMap.values().stream().map(GraphVertexDTO::getStatus).allMatch(status -> Status.SUCCEEDED == status))
        .isTrue();
    assertThat(graphVertexMap.values().stream().map(GraphVertexDTO::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrderElementsOf(nodeNames);

    Map<String, String> nameVertexMap = graphVertexMap.entrySet().stream().collect(
        Collectors.toMap(entry -> entry.getValue().getName(), entry -> entry.getValue().getUuid()));

    assertThat(adjacencyList.get(nameVertexMap.get("Fork Node")).getEdges().size()).isEqualTo(2);
    assertThat(adjacencyList.get(nameVertexMap.get("Fork Node")).getNextIds()).isEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 2")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 2")).getNextIds()).isNotEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 1")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 1")).getNextIds()).isNotEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("Wait Node")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("Wait Node")).getNextIds()).isNotEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("barrier1")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("barrier1")).getNextIds()).isNotEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("barrier2")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("barrier2")).getNextIds()).isNotEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("barrier3")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("barrier3")).getNextIds()).isNotEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 3")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 3")).getNextIds()).isEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 4")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("Dummy Node 4")).getNextIds()).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldGenerateOrchestrationGraphWithSkippedNodes() {
    List<String> nodeNames =
        Lists.newArrayList("dummy-start", "fork1", "section1", "section2", "dummy2", "dummy-final");
    PlanExecution planExecutionResponse =
        engineTestSetupHelper.executePlan(bearerToken, application.getAccountId(), application.getAppId(), "skip-node");
    assertThat(planExecutionResponse.getStatus()).isEqualTo(Status.SUCCEEDED);

    OrchestrationGraphDTO response =
        requestOrchestrationGraph(null, planExecutionResponse.getUuid(), "get-orchestration-graph-v2");
    assertThat(response).isNotNull();

    assertThat(response.getPlanExecutionId()).isEqualTo(planExecutionResponse.getUuid());
    assertThat(response.getRootNodeIds()).isNotEmpty();
    assertThat(response.getStartTs()).isNotNull();
    assertThat(response.getEndTs()).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);

    Map<String, GraphVertexDTO> graphVertexMap = response.getAdjacencyList().getGraphVertexMap();
    Map<String, EdgeList> adjacencyList = response.getAdjacencyList().getAdjacencyMap();
    assertThat(graphVertexMap.size()).isEqualTo(6);
    assertThat(adjacencyList.size()).isEqualTo(6);

    assertThat(
        graphVertexMap.values().stream().map(GraphVertexDTO::getStatus).allMatch(status -> Status.SUCCEEDED == status))
        .isTrue();
    assertThat(graphVertexMap.values().stream().map(GraphVertexDTO::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrderElementsOf(nodeNames);

    Map<String, String> nameVertexMap = graphVertexMap.entrySet().stream().collect(
        Collectors.toMap(entry -> entry.getValue().getName(), entry -> entry.getValue().getUuid()));

    assertThat(adjacencyList.get(nameVertexMap.get("dummy-start")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("dummy-start")).getNextIds()).isNotEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("dummy-start")).getNextIds())
        .containsExactlyInAnyOrder(nameVertexMap.get("fork1"));

    assertThat(adjacencyList.get(nameVertexMap.get("fork1")).getEdges()).isNotEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("fork1")).getEdges())
        .containsExactlyInAnyOrder(nameVertexMap.get("section1"), nameVertexMap.get("section2"));
    assertThat(adjacencyList.get(nameVertexMap.get("fork1")).getNextIds()).isNotEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("fork1")).getNextIds())
        .containsExactlyInAnyOrder(nameVertexMap.get("dummy-final"));

    assertThat(adjacencyList.get(nameVertexMap.get("section1")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("section1")).getNextIds()).isEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("section2")).getEdges()).isNotEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("section2")).getEdges())
        .containsExactlyInAnyOrder(nameVertexMap.get("dummy2"));
    assertThat(adjacencyList.get(nameVertexMap.get("section2")).getNextIds()).isEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("dummy2")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("dummy2")).getNextIds()).isEmpty();

    assertThat(adjacencyList.get(nameVertexMap.get("dummy-final")).getEdges()).isEmpty();
    assertThat(adjacencyList.get(nameVertexMap.get("dummy-final")).getNextIds()).isEmpty();
  }

  private OrchestrationGraphDTO requestOrchestrationGraph(
      String startingNodeId, String planExecutionId, String requestUri) {
    GenericType<RestResponse<OrchestrationGraphDTO>> returnType =
        new GenericType<RestResponse<OrchestrationGraphDTO>>() {};

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("planExecutionId", planExecutionId);

    if (startingNodeId != null) {
      queryParams.put("startingSetupNodeId", startingNodeId);
    }

    Awaitility.await().atMost(Duration.FIVE_MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      RestResponse<OrchestrationGraphDTO> response = internalRequest(returnType, queryParams, requestUri);
      return response.getResource().getEndTs() != null
          && StatusUtils.finalStatuses().contains(response.getResource().getStatus());
    });

    RestResponse<OrchestrationGraphDTO> response = internalRequest(returnType, queryParams, requestUri);

    return response.getResource();
  }

  private <T> RestResponse<T> internalRequest(
      GenericType<RestResponse<T>> returnType, Map<String, String> queryParams, String requestUri) {
    queryParams.put("accountId", application.getAccountId());
    queryParams.put("appId", application.getAppId());

    return engineTestSetupHelper.getPortalRequestSpecification(bearerToken)
        .queryParams(queryParams)
        .contentType(ContentType.JSON)
        .get("/execute2/" + requestUri)
        .as(returnType.getType(), ObjectMapperType.JACKSON_2);
  }
}
