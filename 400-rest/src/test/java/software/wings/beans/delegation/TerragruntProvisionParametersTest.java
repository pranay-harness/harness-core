/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.delegation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.beans.GitConfig;
import software.wings.beans.KmsConfig;

import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class TerragruntProvisionParametersTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.TATHAGAT)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilities() {
    testWithGitConfig();
    testWithoutGitConfig();
    testWithSecretManagerConfig();
  }
  private void testWithoutGitConfig() {
    assertThat(TerragruntProvisionParameters.builder()
                   .build()
                   .fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.PROCESS_EXECUTOR, CapabilityType.PROCESS_EXECUTOR);
  }

  private void testWithGitConfig() {
    TerragruntProvisionParameters parameters =
        TerragruntProvisionParameters.builder().sourceRepo(GitConfig.builder().build()).build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(
            CapabilityType.GIT_CONNECTION, CapabilityType.PROCESS_EXECUTOR, CapabilityType.PROCESS_EXECUTOR);
  }

  private void testWithSecretManagerConfig() {
    TerragruntProvisionParameters parameters = TerragruntProvisionParameters.builder()
                                                   .sourceRepo(GitConfig.builder().build())
                                                   .secretManagerConfig(KmsConfig.builder().build())
                                                   .build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.GIT_CONNECTION, CapabilityType.PROCESS_EXECUTOR, CapabilityType.HTTP,
            CapabilityType.PROCESS_EXECUTOR);
  }
}
