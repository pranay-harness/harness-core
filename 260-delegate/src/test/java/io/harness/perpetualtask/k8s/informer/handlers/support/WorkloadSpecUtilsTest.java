/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.informer.handlers.support;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WorkloadSpecUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldMakeContainerSpecsIfNull() throws Exception {
    assertThat(WorkloadSpecUtils.makeContainerSpecs(null)).isEmpty();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldMakeContainerSpecsIfEmpty() throws Exception {
    assertThat(WorkloadSpecUtils.makeContainerSpecs(emptyList())).isEmpty();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldMakeContainerSpecsIfNoResourceRequirements() throws Exception {
    assertThat(
        WorkloadSpecUtils.makeContainerSpecs(ImmutableList.of(new V1ContainerBuilder().withName("container-1").build(),
            new V1ContainerBuilder().withName("container-2").build())))
        .isEqualTo(ImmutableList.of(K8sWorkloadSpec.ContainerSpec.newBuilder().setName("container-1").build(),
            K8sWorkloadSpec.ContainerSpec.newBuilder().setName("container-2").build()));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldMakeContainerSpecsIfSpecified() throws Exception {
    assertThat(
        WorkloadSpecUtils.makeContainerSpecs(ImmutableList.of(new V1ContainerBuilder()
                                                                  .withName("container-1")
                                                                  .withNewResources()
                                                                  .addToRequests("memory", Quantity.fromString("2Gi"))
                                                                  .endResources()
                                                                  .build(),
            new V1ContainerBuilder()
                .withName("container-2")
                .withNewResources()
                .addToRequests("cpu", Quantity.fromString("750m"))
                .addToLimits("cpu", Quantity.fromString("1500m"))
                .addToRequests("memory", Quantity.fromString("1Gi"))
                .addToLimits("memory", Quantity.fromString("2Gi"))
                .endResources()
                .build())))
        .isEqualTo(ImmutableList.of(
            K8sWorkloadSpec.ContainerSpec.newBuilder().setName("container-1").putRequests("memory", "2Gi").build(),
            K8sWorkloadSpec.ContainerSpec.newBuilder()
                .setName("container-2")
                .putRequests("cpu", "750m")
                .putLimits("cpu", "1500m")
                .putRequests("memory", "1Gi")
                .putLimits("memory", "2Gi")
                .build()));
  }
}
