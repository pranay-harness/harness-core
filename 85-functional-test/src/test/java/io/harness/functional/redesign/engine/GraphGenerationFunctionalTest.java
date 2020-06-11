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
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.data.Outcome;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.presentation.Graph;
import io.harness.presentation.GraphVertex;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.state.core.dummy.DummyStep;
import io.harness.state.core.fork.ForkStep;
import io.harness.state.core.section.SectionStep;
import io.harness.testframework.framework.Setup;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.Application;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.GenericType;

/**
 * Functional Tests for {@link io.harness.engine.services.GraphGenerationService}
 */
public class GraphGenerationFunctionalTest extends AbstractFunctionalTest {
  private static final String BASIC_HTTP_STEP_TYPE = "BASIC_HTTP";

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;

  OwnerManager.Owners owners;
  Application application;

  final Randomizer.Seed seed = new Randomizer.Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  //@Ignore("Alternative to JsonTypeInfo on Outcomes/StepParameter  needs to be seen")
  public void shouldGenerateGraph() {
    PlanExecution planExecutionResponse =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
    assertThat(planExecutionResponse.getStatus()).isEqualTo(Status.SUCCEEDED);

    Graph response = requestGraph(planExecutionResponse.getUuid());
    assertThat(response).isNotNull();

    // start dummy node
    GraphVertex dummyNode = response.getGraphVertex();
    assertThat(dummyNode).isNotNull();
    assertThat(dummyNode.getSubgraph()).isNull();
    assertThat(dummyNode.getNext()).isNotNull();
    assertThat(dummyNode.getStepType()).isEqualTo(DummyStep.STEP_TYPE.getType());
    assertThat(dummyNode.getOutcomes()).isEmpty();

    // fork node
    GraphVertex fork1 = response.getGraphVertex().getNext();
    assertThat(fork1).isNotNull();
    assertThat(fork1.getStepType()).isEqualTo(ForkStep.STEP_TYPE.getType());
    assertThat(fork1.getSubgraph()).isNotNull();
    assertThat(fork1.getSubgraph().getMode()).isEqualTo(ExecutionMode.CHILDREN);
    assertThat(fork1.getSubgraph().getVertices().size()).isEqualTo(2);
    assertThat(fork1.getNext()).isNotNull();
    assertThat(fork1.getOutcomes()).isEmpty();

    // section 1 node
    GraphVertex section1 = fork1.getSubgraph()
                               .getVertices()
                               .stream()
                               .filter(vertex -> vertex.getName().equals("section1"))
                               .findFirst()
                               .orElse(null);
    assertThat(section1).isNotNull();
    assertThat(section1.getSubgraph()).isNotNull();
    assertThat(section1.getSubgraph().getVertices().size()).isEqualTo(1);
    assertThat(section1.getStepType()).isEqualTo(SectionStep.STEP_TYPE.getType());
    assertThat(section1.getOutcomes()).isEmpty();

    // section 2 node
    GraphVertex section2 = fork1.getSubgraph()
                               .getVertices()
                               .stream()
                               .filter(vertex -> vertex.getName().equals("section2"))
                               .findFirst()
                               .orElse(null);
    assertThat(section2).isNotNull();
    assertThat(section2.getSubgraph()).isNotNull();
    assertThat(section2.getSubgraph().getVertices().size()).isEqualTo(1);
    assertThat(section2.getStepType()).isEqualTo(SectionStep.STEP_TYPE.getType());
    assertThat(section2.getOutcomes()).isEmpty();

    // section 1 child node (fork2)
    GraphVertex section1Child = section1.getSubgraph().getVertices().get(0);
    assertThat(section1Child).isNotNull();
    assertThat(section1Child.getStepType()).isEqualTo(ForkStep.STEP_TYPE.getType());
    assertThat(section1Child.getSubgraph()).isNotNull();
    assertThat(section1Child.getSubgraph().getMode()).isEqualTo(ExecutionMode.CHILDREN);
    assertThat(section1Child.getSubgraph().getVertices().size()).isEqualTo(2);
    assertThat(section1Child.getOutcomes()).isEmpty();

    // section 1 child subgraph fork node (http1)
    GraphVertex section1ChildHttp1 = section1Child.getSubgraph()
                                         .getVertices()
                                         .stream()
                                         .filter(vertex -> vertex.getName().equals("http1"))
                                         .findFirst()
                                         .orElse(null);
    assertThat(section1ChildHttp1).isNotNull();
    assertThat(section1ChildHttp1.getNext()).isNull();
    assertThat(section1ChildHttp1.getSubgraph()).isNull();
    assertThat(section1ChildHttp1.getStepType()).isEqualTo(BASIC_HTTP_STEP_TYPE);
    assertThat(section1ChildHttp1.getOutcomes().size()).isEqualTo(1);
    assertThat(section1ChildHttp1.getOutcomes().get(0)).isInstanceOf(HttpStateExecutionData.class);
    assertThat(((HttpStateExecutionData) section1ChildHttp1.getOutcomes().get(0)).getStatus())
        .isEqualTo(ExecutionStatus.SUCCESS);

    // section 1 child subgraph fork node (http2)
    GraphVertex section1ChildHttp2 = section1Child.getSubgraph()
                                         .getVertices()
                                         .stream()
                                         .filter(vertex -> vertex.getName().equals("http2"))
                                         .findFirst()
                                         .orElse(null);
    assertThat(section1ChildHttp2).isNotNull();
    assertThat(section1ChildHttp2.getNext()).isNull();
    assertThat(section1ChildHttp2.getSubgraph()).isNull();
    assertThat(section1ChildHttp2.getStepType()).isEqualTo(BASIC_HTTP_STEP_TYPE);
    assertThat(section1ChildHttp2.getOutcomes().size()).isEqualTo(1);
    assertThat(section1ChildHttp2.getOutcomes().get(0)).isInstanceOf(HttpStateExecutionData.class);
    assertThat(((HttpStateExecutionData) section1ChildHttp2.getOutcomes().get(0)).getStatus())
        .isEqualTo(ExecutionStatus.SUCCESS);

    // section 2 child node (http switch)
    GraphVertex section2Child = section2.getSubgraph().getVertices().get(0);
    assertThat(section2Child).isNotNull();
    assertThat(section2Child.getStepType()).isEqualTo(BASIC_HTTP_STEP_TYPE);
    assertThat(section2Child.getNext()).isNotNull();
    assertThat(section2Child.getSubgraph()).isNull();
    assertThat(section2Child.getOutcomes().size()).isEqualTo(1);
    assertThat(section2Child.getOutcomes().get(0)).isInstanceOf(HttpStateExecutionData.class);
    assertThat(((HttpStateExecutionData) section2Child.getOutcomes().get(0)).getStatus())
        .isEqualTo(ExecutionStatus.SUCCESS);

    // section 2 child next node (http)
    GraphVertex section2ChildNextNode = section2Child.getNext();
    assertThat(section2ChildNextNode).isNotNull();
    assertThat(section2ChildNextNode.getNext()).isNull();
    assertThat(section2ChildNextNode.getSubgraph()).isNull();
    assertThat(section2ChildNextNode.getStepType()).isEqualTo(DummyStep.STEP_TYPE.getType());
    assertThat(section2ChildNextNode.getOutcomes()).isEmpty();

    // final dummy node
    GraphVertex finalDummyNode = fork1.getNext();
    assertThat(finalDummyNode).isNotNull();
    assertThat(finalDummyNode.getSubgraph()).isNull();
    assertThat(finalDummyNode.getNext()).isNull();
    assertThat(finalDummyNode.getStepType()).isEqualTo(DummyStep.STEP_TYPE.getType());
    assertThat(finalDummyNode.getOutcomes()).isEmpty();
  }

  private Graph requestGraph(String planExecutionId) {
    GenericType<RestResponse<Graph>> returnType = new GenericType<RestResponse<Graph>>() {};

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("accountId", application.getAccountId());
    queryParams.put("appId", application.getAppId());
    queryParams.put("planExecutionId", planExecutionId);

    RestResponse<Graph> response =
        Setup.portal()
            .config(RestAssuredConfig.config()
                        .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> {
                          ObjectMapper mapper = new ObjectMapper();
                          mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                          mapper.addMixIn(Outcome.class, OutcomeTestMixin.class);
                          return mapper;
                        }))
                        .sslConfig(new SSLConfig().relaxedHTTPSValidation()))
            .auth()
            .oauth2(bearerToken)
            .queryParams(queryParams)
            .contentType(ContentType.JSON)
            .get("/execute2/get-graph")
            .as(returnType.getType(), ObjectMapperType.JACKSON_2);

    return response.getResource();
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
}
