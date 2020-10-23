package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_ARGS;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_CONTAINER_NAME;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.ConnectorDetails;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class InternalContainerParamsProviderTest extends CIExecutionTest {
  @Inject InternalContainerParamsProvider internalContainerParamsProvider;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getSetupAddonContainerParams() {
    ConnectorDetails connectorDetails = ConnectorDetails.builder().connectorDTO(ConnectorDTO.builder().build()).build();

    CIK8ContainerParams containerParams =
        internalContainerParamsProvider.getSetupAddonContainerParams(connectorDetails);

    assertThat(containerParams.getName()).isEqualTo(SETUP_ADDON_CONTAINER_NAME);
    assertThat(containerParams.getContainerType()).isEqualTo(CIContainerType.ADD_ON);
    assertThat(containerParams.getArgs()).isEqualTo(Arrays.asList(SETUP_ADDON_ARGS));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getLiteEngineContainerParams() {
    BuildNumber buildNumber = BuildNumber.builder().buildNumber(1L).build();
    K8PodDetails k8PodDetails = K8PodDetails.builder().buildNumber(buildNumber).build();

    ConnectorDetails connectorDetails = ConnectorDetails.builder().connectorDTO(ConnectorDTO.builder().build()).build();
    Map<String, ConnectorDetails> publishArtifactConnectorDetailsMap = new HashMap<>();
    String logSecret = "secret";
    String logEndpoint = "http://localhost:8079";
    Map<String, String> logEnvVars = new HashMap<>();
    logEnvVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, logEndpoint);
    logEnvVars.put(LOG_SERVICE_TOKEN_VARIABLE, logSecret);

    String serialisedStage = "test";
    String serviceToken = "test";
    Integer stageCpuRequest = 500;
    Integer stageMemoryRequest = 200;

    CIK8ContainerParams containerParams = internalContainerParamsProvider.getLiteEngineContainerParams(connectorDetails,
        publishArtifactConnectorDetailsMap, k8PodDetails, serialisedStage, serviceToken, stageCpuRequest,
        stageMemoryRequest, null, logEnvVars);

    Map<String, String> expectedEnv = new HashMap<>();
    expectedEnv.put(LOG_SERVICE_ENDPOINT_VARIABLE, logEndpoint);
    expectedEnv.put(LOG_SERVICE_TOKEN_VARIABLE, logSecret);

    Map<String, String> gotEnv = containerParams.getEnvVars();
    assertThat(gotEnv).containsAllEntriesOf(expectedEnv);
    assertThat(containerParams.getName()).isEqualTo(LITE_ENGINE_CONTAINER_NAME);
    assertThat(containerParams.getContainerType()).isEqualTo(CIContainerType.LITE_ENGINE);
  }
}