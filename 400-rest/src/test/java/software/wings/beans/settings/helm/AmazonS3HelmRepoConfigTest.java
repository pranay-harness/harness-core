/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.settings.helm;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AmazonS3HelmRepoConfigTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilities() {
    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig =
        AmazonS3HelmRepoConfig.builder().bucketName("default").region("us-east-1").connectorId("default").build();
    List<ExecutionCapability> executionCapabilityList = amazonS3HelmRepoConfig.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilityList).isNotEmpty();
    assertThat(executionCapabilityList).hasSize(3);
    assertThat(
        executionCapabilityList.stream().map(ExecutionCapability::getCapabilityType).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP, CapabilityType.HELM_INSTALL, CapabilityType.CHART_MUSEUM);
  }
}
