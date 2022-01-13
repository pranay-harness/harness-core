/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.helm.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(CDP)
public class HelmCommandRequestTest extends WingsBaseTest {
  @Mock private ContainerServiceParams containerServiceParams;

  @Before
  public void setUp() throws Exception {
    doReturn(asList(HttpConnectionExecutionCapability.builder().build()))
        .when(containerServiceParams)
        .fetchRequiredExecutionCapabilities(null);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilities() {
    testWithDelegateSelectors();
    testWithoutContainerParams();
    testWithoutContainerParams_ffOff();
    testWithoutGitConfig();
    testWithContainerParams();
  }

  private void testWithDelegateSelectors() {
    HelmInstallCommandRequest installCommandRequest =
        HelmInstallCommandRequest.builder()
            .sourceRepoConfig(
                K8sDelegateManifestConfig.builder()
                    .helmChartConfigParams(
                        HelmChartConfigParams.builder()
                            .connectorConfig(GcpConfig.builder()
                                                 .delegateSelectors(Collections.singletonList("gcp-delegate"))
                                                 .build())
                            .build())
                    .build())
            .build();
    // delegate selector in gcp config
    List<ExecutionCapability> executionCapabilities = installCommandRequest.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).hasSize(2);
    assertThat(executionCapabilities.stream().map(ExecutionCapability::getCapabilityType).collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.SELECTORS);

    // delegate selector in aws config
    installCommandRequest.setRepoConfig(
        K8sDelegateManifestConfig.builder()
            .helmChartConfigParams(HelmChartConfigParams.builder()
                                       .connectorConfig(AwsConfig.builder().tag("aws-delegagte").build())
                                       .build())
            .build());
    executionCapabilities = installCommandRequest.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).hasSize(2);
    assertThat(executionCapabilities.stream().map(ExecutionCapability::getCapabilityType).collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.SELECTORS);
  }

  private void testWithoutGitConfig() {
    HelmRollbackCommandRequest rollbackCommandRequest =
        HelmRollbackCommandRequest.builder().containerServiceParams(containerServiceParams).build();
    assertThat(rollbackCommandRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.HTTP);
  }

  private void testWithContainerParams() {
    HelmInstallCommandRequest installCommandRequest = HelmInstallCommandRequest.builder()
                                                          .gitConfig(GitConfig.builder().repoUrl("https://abc").build())
                                                          .containerServiceParams(containerServiceParams)
                                                          .isGitHostConnectivityCheck(true)
                                                          .build();
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.HTTP, CapabilityType.HTTP);

    installCommandRequest.setGitHostConnectivityCheck(false);
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.GIT_CONNECTION, CapabilityType.HTTP);
  }

  private void testWithoutContainerParams() {
    HelmInstallCommandRequest installCommandRequest = HelmInstallCommandRequest.builder()
                                                          .gitConfig(GitConfig.builder().repoUrl("https://abc").build())
                                                          .mergeCapabilities(true)
                                                          .isGitHostConnectivityCheck(true)
                                                          .build();
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_INSTALL, CapabilityType.HTTP);

    installCommandRequest.setGitHostConnectivityCheck(false);
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_INSTALL, CapabilityType.GIT_CONNECTION);
  }

  private void testWithoutContainerParams_ffOff() {
    HelmInstallCommandRequest installCommandRequest = HelmInstallCommandRequest.builder()
                                                          .gitConfig(GitConfig.builder().repoUrl("https://abc").build())
                                                          .isGitHostConnectivityCheck(true)
                                                          .build();
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.HTTP);

    installCommandRequest.setGitHostConnectivityCheck(false);
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.GIT_CONNECTION);
  }
}
