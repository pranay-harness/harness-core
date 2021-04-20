package io.harness.pms.preflight.handler;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNUtils;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class AsyncPreFlightHandlerTest extends CategoryTest {
  Map<String, Object> fqnToObjectMapMergedYaml = new HashMap<>();
  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "failure-strategy.yaml";
    String yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    Map<FQN, Object> fqnObjectMap = FQNUtils.generateFQNMap(YamlUtils.readTree(yaml).getNode().getCurrJsonNode());
    fqnObjectMap.keySet().forEach(fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testNonExistentConnectorsInPipeline() {
    AsyncPreFlightHandler asyncPreFlightHandler =
        AsyncPreFlightHandler.builder().fqnToObjectMapMergedYaml(fqnToObjectMapMergedYaml).build();

    Map<String, String> connectorIdentifierToFqn = new HashMap<>();
    connectorIdentifierToFqn.put(
        "my_git_connector", "pipeline.stages.qaStage.serviceConfig.manifests.baseValues.my_git_connector");

    List<ConnectorResponseDTO> connectorResponses = new ArrayList<>();
    List<ConnectorCheckResponse> connectorCheckResponse =
        asyncPreFlightHandler.getConnectorCheckResponse(connectorResponses, connectorIdentifierToFqn);
    assertThat(connectorCheckResponse).isNotEmpty();
    ConnectorCheckResponse response = connectorCheckResponse.get(0);
    assertThat(response.getStatus()).isEqualTo(PreFlightStatus.FAILURE);
    assertThat(response.getConnectorIdentifier()).isEqualTo("my_git_connector");
    assertThat(response.getFqn())
        .isEqualTo("pipeline.stages.qaStage.serviceConfig.manifests.baseValues.my_git_connector");
    assertThat(response.getStageIdentifier()).isEqualTo("qaStage");
    assertThat(response.getStageName()).isEqualTo("qa stage");
    assertThat(response.getErrorInfo()).isNotNull();
  }
}