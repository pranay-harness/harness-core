package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ADDON_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_NAME;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.ADDON_CONTAINER;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.LITE_ENGINE_CONTAINER;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8ContainerParams.CIK8ContainerParamsBuilder;

public class InternalContainerParamsProviderTest extends CIExecutionTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldGetContainerParams() {
    BuildNumber buildNumber = BuildNumber.builder().buildNumber(1L).build();
    K8PodDetails k8PodDetails = K8PodDetails.builder().buildNumber(buildNumber).build();
    CIK8ContainerParams containerParams =
        InternalContainerParamsProvider.getContainerParams(ADDON_CONTAINER, k8PodDetails).build();
    assertThat(containerParams).isNotNull();
    assertThat(containerParams.getName()).isEqualTo(ADDON_CONTAINER_NAME);

    containerParams = InternalContainerParamsProvider.getContainerParams(LITE_ENGINE_CONTAINER, k8PodDetails).build();
    assertThat(containerParams).isNotNull();
    assertThat(containerParams.getName()).isEqualTo(LITE_ENGINE_CONTAINER_NAME);

    CIK8ContainerParamsBuilder containerParamsBuilder =
        InternalContainerParamsProvider.getContainerParams(null, k8PodDetails);
    assertThat(containerParamsBuilder).isNull();
  }
}