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
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.presentation.Graph;
import io.harness.presentation.visualization.GraphVisualizer;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
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
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.GenericType;

public class GraphVisualizerTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;

  @Inject private GraphVisualizer graphVisualizer;

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
  @Ignore("This test is used internally only")
  public void shouldGenerateImage() throws IOException {
    PlanExecution response =
        executePlan(bearerToken, application.getAccountId(), application.getAppId(), "test-graph-plan");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);

    Graph harnessGraph = requestGraph(response.getUuid());
    assertThat(harnessGraph).isNotNull();
    graphVisualizer.generateImage(harnessGraph, "graph.png");
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
