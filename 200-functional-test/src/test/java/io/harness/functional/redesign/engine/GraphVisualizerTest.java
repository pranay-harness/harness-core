package io.harness.functional.redesign.engine;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.harness.ambiance.Ambiance;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.data.Outcome;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.execution.PlanExecution;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.GraphVisualizer;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.pms.execution.Status;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.state.io.StepParameters;
import io.harness.testframework.framework.MockServerExecutor;
import io.harness.testframework.framework.Setup;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.Application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.GenericType;

public class GraphVisualizerTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
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
    PlanExecution response =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
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
    PlanExecution response =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
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
    PlanExecution response =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
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

    return Setup.portal()
        .config(RestAssuredConfig.config()
                    .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> {
                      ObjectMapper mapper = new ObjectMapper();
                      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                      mapper.addMixIn(Outcome.class, OutcomeTestMixin.class);
                      mapper.addMixIn(StepParameters.class, StepParametersTestMixin.class);
                      mapper.addMixIn(Ambiance.class, AmbianceTestMixin.class);
                      return mapper;
                    }))
                    .sslConfig(new SSLConfig().relaxedHTTPSValidation()))
        .auth()
        .oauth2(bearerToken)
        .queryParams(queryParams)
        .contentType(ContentType.JSON)
        .get("/execute2/" + requestUri)
        .as(returnType.getType(), ObjectMapperType.JACKSON_2);
  }

  @JsonDeserialize(using = OutcomeTestDeserializer.class)
  private abstract static class OutcomeTestMixin {}

  private static class OutcomeTestDeserializer extends StdDeserializer<Outcome> {
    OutcomeTestDeserializer() {
      super(Outcome.class);
    }

    @Override
    public Outcome deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      final String httpExecutionDataParam = "httpMethod";
      JsonNode node = p.getCodec().readTree(p);
      if (node.hasNonNull(httpExecutionDataParam)) {
        return HttpStateExecutionData.builder()
            .status(ExecutionStatus.valueOf(node.get("status").asText()))
            .httpMethod(node.get("httpMethod").asText())
            .build();
      }
      return null;
    }
  }

  @JsonDeserialize(using = StepParametersTestDeserializer.class)
  private abstract static class StepParametersTestMixin {}

  private static class StepParametersTestDeserializer extends StdDeserializer<StepParameters> {
    StepParametersTestDeserializer() {
      super(StepParameters.class);
    }

    @Override
    public StepParameters deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      return new StepParameters() {};
    }
  }

  @JsonDeserialize(using = AmbianceTestDeserializer.class)
  private abstract static class AmbianceTestMixin {}

  private static class AmbianceTestDeserializer extends StdDeserializer<Ambiance> {
    AmbianceTestDeserializer() {
      super(Ambiance.class);
    }

    @Override
    public Ambiance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      return new Ambiance(new HashMap<>(), new ArrayList<>(), "");
    }
  }
}