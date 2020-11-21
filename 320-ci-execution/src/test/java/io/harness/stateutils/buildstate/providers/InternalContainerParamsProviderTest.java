package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_ARGS;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_CONTAINER_NAME;
import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InternalContainerParamsProviderTest extends CIExecutionTest {
  @Inject InternalContainerParamsProvider internalContainerParamsProvider;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getSetupAddonContainerParams() {
    ConnectorDetails connectorDetails = ConnectorDetails.builder().build();

    CIK8ContainerParams containerParams =
        internalContainerParamsProvider.getSetupAddonContainerParams(connectorDetails, null, "workspace");

    assertThat(containerParams.getName()).isEqualTo(SETUP_ADDON_CONTAINER_NAME);
    assertThat(containerParams.getContainerType()).isEqualTo(CIContainerType.ADD_ON);
    assertThat(containerParams.getArgs()).isEqualTo(Arrays.asList(SETUP_ADDON_ARGS));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getLiteEngineContainerParams() {
    BuildNumberDetails buildNumberDetails = BuildNumberDetails.builder().buildNumber(1L).build();
    K8PodDetails k8PodDetails = K8PodDetails.builder().buildNumberDetails(buildNumberDetails).build();

    ConnectorDetails connectorDetails = ConnectorDetails.builder().build();
    Map<String, ConnectorDetails> publishArtifactConnectorDetailsMap = new HashMap<>();
    String logSecret = "secret";
    String logEndpoint = "http://localhost:8079";
    Map<String, String> logEnvVars = new HashMap<>();
    logEnvVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, logEndpoint);
    logEnvVars.put(LOG_SERVICE_TOKEN_VARIABLE, logSecret);

    Map<String, String> volumeToMountPath = new HashMap<>();

    String serialisedStage = "test";
    String serviceToken = "test";
    Integer stageCpuRequest = 500;
    Integer stageMemoryRequest = 200;

    CIK8ContainerParams containerParams = internalContainerParamsProvider.getLiteEngineContainerParams(connectorDetails,
        publishArtifactConnectorDetailsMap, k8PodDetails, serialisedStage, serviceToken, stageCpuRequest,
        stageMemoryRequest, null, logEnvVars, volumeToMountPath, "/step-exec/workspace");

    Map<String, String> expectedEnv = new HashMap<>();
    expectedEnv.put(LOG_SERVICE_ENDPOINT_VARIABLE, logEndpoint);
    expectedEnv.put(LOG_SERVICE_TOKEN_VARIABLE, logSecret);

    Map<String, String> gotEnv = containerParams.getEnvVars();
    assertThat(gotEnv).containsAllEntriesOf(expectedEnv);
    assertThat(containerParams.getName()).isEqualTo(LITE_ENGINE_CONTAINER_NAME);
    assertThat(containerParams.getContainerType()).isEqualTo(CIContainerType.LITE_ENGINE);
  }
}
