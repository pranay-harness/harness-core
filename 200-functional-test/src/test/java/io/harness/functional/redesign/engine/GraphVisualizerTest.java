package io.harness.functional.redesign.engine;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.execution.PlanExecution;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.redesign.OrchestrationEngineTestSetupHelper;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.GraphVisualizer;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.pms.execution.Status;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.MockServerExecutor;

import software.wings.beans.Application;

import com.google.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GraphVisualizerTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private OrchestrationEngineTestSetupHelper engineTestSetupHelper;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private MockServerExecutor mockServerExecutor;

  @Inject private GraphVisualizer graphVisualizer;

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
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  @Ignore("This test is used internally only")
  public void shouldGenerateImageFromAdjacencyList() throws IOException {
    PlanExecution response = engineTestSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);

    OrchestrationGraphDTO harnessGraph = requestOrchestrationGraph(response.getUuid());
    assertThat(harnessGraph).isNotNull();
    graphVisualizer.generateImage(harnessGraph, "orchestration-graph.png");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  @Ignore("This test is used internally only")
  public void shouldTestBreadthFirstTraversal() {
    PlanExecution response = engineTestSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);

    OrchestrationGraphDTO graph = requestOrchestrationGraph(response.getUuid());
    assertThat(graph).isNotNull();
    graphVisualizer.breadthFirstTraversal(graph);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  @Ignore("This test is used internally only")
  public void shouldTestDepthFirstTraversal() {
    PlanExecution response = engineTestSetupHelper.executePlan(
        bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);

    OrchestrationGraphDTO graph = requestOrchestrationGraph(response.getUuid());
    assertThat(graph).isNotNull();
    graphVisualizer.depthFirstTraversal(graph);
  }

  private OrchestrationGraphDTO requestOrchestrationGraph(String planExecutionId) {
    GenericType<RestResponse<OrchestrationGraphDTO>> returnType =
        new GenericType<RestResponse<OrchestrationGraphDTO>>() {};

    RestResponse<OrchestrationGraphDTO> response =
        internalRequest((GenericType) returnType, planExecutionId, "get-orchestration-graph");

    return response.getResource();
  }

  private RestResponse<?> internalRequest(
      GenericType<RestResponse<?>> returnType, String planExecutionId, String requestUri) {
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("accountId", application.getAccountId());
    queryParams.put("appId", application.getAppId());
    queryParams.put("planExecutionId", planExecutionId);

    return engineTestSetupHelper.getPortalRequestSpecification(bearerToken)
        .queryParams(queryParams)
        .contentType(ContentType.JSON)
        .get("/execute2/" + requestUri)
        .as(returnType.getType(), ObjectMapperType.JACKSON_2);
  }
}
