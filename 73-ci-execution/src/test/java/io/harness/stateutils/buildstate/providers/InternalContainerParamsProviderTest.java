package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_ARGS;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_CONTAINER_NAME;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;

import java.util.Arrays;

public class InternalContainerParamsProviderTest extends CIExecutionTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getSetupAddonContainerParams() {
    CIK8ContainerParams containerParams = InternalContainerParamsProvider.getSetupAddonContainerParams().build();
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
    String serialisedStage = "test";
    String serviceToken = "test";
    Integer stageCpuRequest = 500;
    Integer stageMemoryRequest = 200;

    CIK8ContainerParams containerParams = InternalContainerParamsProvider
                                              .getLiteEngineContainerParams(k8PodDetails, serialisedStage, serviceToken,
                                                  stageCpuRequest, stageMemoryRequest)
                                              .build();

    assertThat(containerParams.getName()).isEqualTo(LITE_ENGINE_CONTAINER_NAME);
    assertThat(containerParams.getContainerType()).isEqualTo(CIContainerType.LITE_ENGINE);
  }
}