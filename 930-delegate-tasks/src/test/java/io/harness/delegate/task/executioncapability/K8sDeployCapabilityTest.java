/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.GcpK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(CDP)
public class K8sDeployCapabilityTest extends CategoryTest {
  private static final String SOME_URL = "https://url.com/owner/repo.git";

  @Mock private ExpressionEvaluator expressionEvaluator;

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddDirectK8sCapabilityInheritFromDelegate() {
    Set<String> delegateSelectors = ImmutableSet.of("delegate1");
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
                                    .build())
                    .delegateSelectors(delegateSelectors)
                    .build())
            .build();
    K8sRollingDeployRequest rollingRequest =
        K8sRollingDeployRequest.builder()
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(
                K8sManifestDelegateConfig.builder()
                    .storeDelegateConfig(
                        GitStoreDelegateConfig.builder()
                            .gitConfigDTO(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url(SOME_URL).build())
                            .build())
                    .build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(SelectorCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(GitConnectionNGCapability.class);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddDirectK8sCapabilityManualCredentials() {
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(
                        KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder()
                                        .auth(KubernetesAuthDTO.builder()
                                                  .authType(KubernetesAuthType.USER_PASSWORD)
                                                  .credentials(
                                                      KubernetesUserNamePasswordDTO.builder()
                                                          .username("test")
                                                          .passwordRef(SecretRefData.builder()
                                                                           .decryptedValue("password".toCharArray())
                                                                           .build())
                                                          .build())
                                                  .build())
                                        .build())
                            .build())
                    .build())
            .build();
    K8sRollingDeployRequest rollingRequest =
        K8sRollingDeployRequest.builder()
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(
                K8sManifestDelegateConfig.builder()
                    .storeDelegateConfig(
                        GitStoreDelegateConfig.builder()
                            .gitConfigDTO(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url(SOME_URL).build())
                            .build())
                    .build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(GitConnectionNGCapability.class);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddGcpK8sCapabilityInheritFromDelegate() {
    Set<String> delegateSelectors = ImmutableSet.of("delegate1");
    GcpK8sInfraDelegateConfig k8sInfraDelegateConfig =
        GcpK8sInfraDelegateConfig.builder()
            .gcpConnectorDTO(GcpConnectorDTO.builder()
                                 .credential(GcpConnectorCredentialDTO.builder()
                                                 .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                                 .build())
                                 .delegateSelectors(delegateSelectors)
                                 .build())
            .build();
    K8sRollingDeployRequest rollingRequest =
        K8sRollingDeployRequest.builder()
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(
                K8sManifestDelegateConfig.builder()
                    .storeDelegateConfig(
                        GitStoreDelegateConfig.builder()
                            .gitConfigDTO(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url(SOME_URL).build())
                            .build())
                    .build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(3);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(SelectorCapability.class);
    assertThat(executionCapabilities.get(2)).isInstanceOf(GitConnectionNGCapability.class);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddGcpK8sCapabilityManualCredentials() {
    GcpK8sInfraDelegateConfig k8sInfraDelegateConfig =
        GcpK8sInfraDelegateConfig.builder()
            .gcpConnectorDTO(
                GcpConnectorDTO.builder()
                    .credential(
                        GcpConnectorCredentialDTO.builder()
                            .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                            .config(GcpManualDetailsDTO.builder()
                                        .secretKeyRef(
                                            SecretRefData.builder().decryptedValue("gcp-key".toCharArray()).build())
                                        .build())
                            .build())
                    .build())
            .build();
    K8sRollingDeployRequest rollingRequest =
        K8sRollingDeployRequest.builder()
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(
                K8sManifestDelegateConfig.builder()
                    .storeDelegateConfig(
                        GitStoreDelegateConfig.builder()
                            .gitConfigDTO(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url(SOME_URL).build())
                            .build())
                    .build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(GitConnectionNGCapability.class);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddHttpHelmCapability() {
    Set<String> k8sDelegateSelectors = ImmutableSet.of("k8s-delegate1");
    GcpK8sInfraDelegateConfig k8sInfraDelegateConfig =
        GcpK8sInfraDelegateConfig.builder()
            .gcpConnectorDTO(GcpConnectorDTO.builder()
                                 .credential(GcpConnectorCredentialDTO.builder()
                                                 .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                                 .build())
                                 .delegateSelectors(k8sDelegateSelectors)
                                 .build())
            .build();

    Set<String> httpHelmDelegateSelectors = ImmutableSet.of("helm-delegate1");
    HttpHelmConnectorDTO helmConnectorDTO =
        HttpHelmConnectorDTO.builder().helmRepoUrl("localhost").delegateSelectors(httpHelmDelegateSelectors).build();
    HttpHelmStoreDelegateConfig helmStoreDelegateConfig = HttpHelmStoreDelegateConfig.builder()
                                                              .httpHelmConnector(helmConnectorDTO)
                                                              .encryptedDataDetails(Collections.emptyList())
                                                              .build();
    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig = HelmChartManifestDelegateConfig.builder()
                                                                          .helmVersion(HelmVersion.V3)
                                                                          .storeDelegateConfig(helmStoreDelegateConfig)
                                                                          .build();

    K8sRollingDeployRequest rollingRequest = K8sRollingDeployRequest.builder()
                                                 .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                 .manifestDelegateConfig(helmChartManifestDelegateConfig)
                                                 .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(5);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(SelectorCapability.class);
    SelectorCapability k8sSelectorCapability = (SelectorCapability) executionCapabilities.get(1);
    assertThat(k8sSelectorCapability.getSelectors()).isEqualTo(k8sDelegateSelectors);
    assertThat(executionCapabilities.get(2)).isInstanceOf(HelmInstallationCapability.class);
    assertThat(executionCapabilities.get(3)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(4)).isInstanceOf(SelectorCapability.class);
    SelectorCapability httpHelmSelectorCapability = (SelectorCapability) executionCapabilities.get(4);
    assertThat(httpHelmSelectorCapability.getSelectors()).isEqualTo(httpHelmDelegateSelectors);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddS3HelmCapability() {
    Set<String> k8sDelegateSelectors = ImmutableSet.of("k8s-delegate1");
    GcpK8sInfraDelegateConfig k8sInfraDelegateConfig =
        GcpK8sInfraDelegateConfig.builder()
            .gcpConnectorDTO(GcpConnectorDTO.builder()
                                 .credential(GcpConnectorCredentialDTO.builder()
                                                 .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                                 .build())
                                 .delegateSelectors(k8sDelegateSelectors)
                                 .build())
            .build();

    Set<String> s3HelmDelegateSelectors = ImmutableSet.of("helm-delegate1");
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).build())
            .delegateSelectors(s3HelmDelegateSelectors)
            .build();
    S3HelmStoreDelegateConfig helmStoreDelegateConfig = S3HelmStoreDelegateConfig.builder()
                                                            .awsConnector(awsConnectorDTO)
                                                            .encryptedDataDetails(Collections.emptyList())
                                                            .build();
    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig = HelmChartManifestDelegateConfig.builder()
                                                                          .helmVersion(HelmVersion.V3)
                                                                          .storeDelegateConfig(helmStoreDelegateConfig)
                                                                          .build();

    K8sRollingDeployRequest rollingRequest = K8sRollingDeployRequest.builder()
                                                 .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                 .manifestDelegateConfig(helmChartManifestDelegateConfig)
                                                 .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(4);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(SelectorCapability.class);
    SelectorCapability k8sSelectorCapability = (SelectorCapability) executionCapabilities.get(1);
    assertThat(k8sSelectorCapability.getSelectors()).isEqualTo(k8sDelegateSelectors);
    assertThat(executionCapabilities.get(2)).isInstanceOf(HelmInstallationCapability.class);
    assertThat(executionCapabilities.get(3)).isInstanceOf(SelectorCapability.class);
    SelectorCapability httpHelmSelectorCapability = (SelectorCapability) executionCapabilities.get(3);
    assertThat(httpHelmSelectorCapability.getSelectors()).isEqualTo(s3HelmDelegateSelectors);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddS3HelmManualCapability() {
    Set<String> k8sDelegateSelectors = ImmutableSet.of("k8s-delegate1");
    GcpK8sInfraDelegateConfig k8sInfraDelegateConfig =
        GcpK8sInfraDelegateConfig.builder()
            .gcpConnectorDTO(GcpConnectorDTO.builder()
                                 .credential(GcpConnectorCredentialDTO.builder()
                                                 .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                                 .build())
                                 .delegateSelectors(k8sDelegateSelectors)
                                 .build())
            .build();

    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(
                AwsCredentialDTO.builder()
                    .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                    .config(AwsManualConfigSpecDTO.builder()
                                .accessKey("test")
                                .secretKeyRef(SecretRefData.builder().decryptedValue("secret".toCharArray()).build())
                                .build())
                    .build())
            .build();
    S3HelmStoreDelegateConfig helmStoreDelegateConfig = S3HelmStoreDelegateConfig.builder()
                                                            .awsConnector(awsConnectorDTO)
                                                            .encryptedDataDetails(Collections.emptyList())
                                                            .build();
    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig = HelmChartManifestDelegateConfig.builder()
                                                                          .helmVersion(HelmVersion.V3)
                                                                          .storeDelegateConfig(helmStoreDelegateConfig)
                                                                          .build();

    K8sRollingDeployRequest rollingRequest = K8sRollingDeployRequest.builder()
                                                 .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                 .manifestDelegateConfig(helmChartManifestDelegateConfig)
                                                 .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(4);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(SelectorCapability.class);
    SelectorCapability k8sSelectorCapability = (SelectorCapability) executionCapabilities.get(1);
    assertThat(k8sSelectorCapability.getSelectors()).isEqualTo(k8sDelegateSelectors);
    assertThat(executionCapabilities.get(2)).isInstanceOf(HelmInstallationCapability.class);
    assertThat(executionCapabilities.get(3)).isInstanceOf(HttpConnectionExecutionCapability.class);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddGcsHelmManualCapability() {
    Set<String> k8sDelegateSelectors = ImmutableSet.of("k8s-delegate1");
    GcpK8sInfraDelegateConfig k8sInfraDelegateConfig =
        GcpK8sInfraDelegateConfig.builder()
            .gcpConnectorDTO(GcpConnectorDTO.builder()
                                 .credential(GcpConnectorCredentialDTO.builder()
                                                 .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                                 .build())
                                 .delegateSelectors(k8sDelegateSelectors)
                                 .build())
            .build();

    GcpConnectorDTO gcpConnectorDTO =
        GcpConnectorDTO.builder()
            .credential(
                GcpConnectorCredentialDTO.builder()
                    .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                    .config(GcpManualDetailsDTO.builder()
                                .secretKeyRef(SecretRefData.builder().decryptedValue("gcp-key".toCharArray()).build())
                                .build())
                    .build())
            .build();
    GcsHelmStoreDelegateConfig helmStoreDelegateConfig = GcsHelmStoreDelegateConfig.builder()
                                                             .gcpConnector(gcpConnectorDTO)
                                                             .encryptedDataDetails(Collections.emptyList())
                                                             .build();
    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig = HelmChartManifestDelegateConfig.builder()
                                                                          .helmVersion(HelmVersion.V3)
                                                                          .storeDelegateConfig(helmStoreDelegateConfig)
                                                                          .build();

    K8sRollingDeployRequest rollingRequest = K8sRollingDeployRequest.builder()
                                                 .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                 .manifestDelegateConfig(helmChartManifestDelegateConfig)
                                                 .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(4);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(SelectorCapability.class);
    SelectorCapability k8sSelectorCapability = (SelectorCapability) executionCapabilities.get(1);
    assertThat(k8sSelectorCapability.getSelectors()).isEqualTo(k8sDelegateSelectors);
    assertThat(executionCapabilities.get(2)).isInstanceOf(HelmInstallationCapability.class);
    assertThat(executionCapabilities.get(3)).isInstanceOf(HttpConnectionExecutionCapability.class);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddGcsHelmCapability() {
    Set<String> k8sDelegateSelectors = ImmutableSet.of("k8s-delegate1");
    GcpK8sInfraDelegateConfig k8sInfraDelegateConfig =
        GcpK8sInfraDelegateConfig.builder()
            .gcpConnectorDTO(GcpConnectorDTO.builder()
                                 .credential(GcpConnectorCredentialDTO.builder()
                                                 .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                                 .build())
                                 .delegateSelectors(k8sDelegateSelectors)
                                 .build())
            .build();

    Set<String> gcsHelmDelegateSelectors = ImmutableSet.of("helm-delegate1");
    GcpConnectorDTO gcpConnectorDTO =
        GcpConnectorDTO.builder()
            .credential(
                GcpConnectorCredentialDTO.builder().gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE).build())
            .delegateSelectors(gcsHelmDelegateSelectors)
            .build();
    GcsHelmStoreDelegateConfig helmStoreDelegateConfig = GcsHelmStoreDelegateConfig.builder()
                                                             .gcpConnector(gcpConnectorDTO)
                                                             .encryptedDataDetails(Collections.emptyList())
                                                             .build();
    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig = HelmChartManifestDelegateConfig.builder()
                                                                          .helmVersion(HelmVersion.V3)
                                                                          .storeDelegateConfig(helmStoreDelegateConfig)
                                                                          .build();

    K8sRollingDeployRequest rollingRequest = K8sRollingDeployRequest.builder()
                                                 .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                                 .manifestDelegateConfig(helmChartManifestDelegateConfig)
                                                 .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(5);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(1)).isInstanceOf(SelectorCapability.class);
    SelectorCapability k8sSelectorCapability = (SelectorCapability) executionCapabilities.get(1);
    assertThat(k8sSelectorCapability.getSelectors()).isEqualTo(k8sDelegateSelectors);
    assertThat(executionCapabilities.get(2)).isInstanceOf(HelmInstallationCapability.class);
    assertThat(executionCapabilities.get(3)).isInstanceOf(HttpConnectionExecutionCapability.class);
    assertThat(executionCapabilities.get(4)).isInstanceOf(SelectorCapability.class);
    SelectorCapability httpHelmSelectorCapability = (SelectorCapability) executionCapabilities.get(4);
    assertThat(httpHelmSelectorCapability.getSelectors()).isEqualTo(gcsHelmDelegateSelectors);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldAddKustomizeCapability() {
    String pluginPath = "/bin/kustomize/plugin";
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
                                    .build())
                    .build())
            .build();
    K8sRollingDeployRequest rollingRequest =
        K8sRollingDeployRequest.builder()
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(
                KustomizeManifestDelegateConfig.builder()
                    .storeDelegateConfig(
                        GitStoreDelegateConfig.builder()
                            .gitConfigDTO(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url(SOME_URL).build())
                            .build())
                    .pluginPath(pluginPath)
                    .build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(2);
    assertThat(executionCapabilities.get(0)).isInstanceOf(KustomizeCapability.class);
    KustomizeCapability kustomizeCapability = (KustomizeCapability) executionCapabilities.get(0);
    assertThat(kustomizeCapability.getPluginRootDir()).isEqualTo(pluginPath);
    assertThat(executionCapabilities.get(1)).isInstanceOf(GitConnectionNGCapability.class);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void shouldSkipKustomizeCapabilityIfPluginPathIsMissing() {
    String pluginPath = "";
    DirectK8sInfraDelegateConfig k8sInfraDelegateConfig =
        DirectK8sInfraDelegateConfig.builder()
            .kubernetesClusterConfigDTO(
                KubernetesClusterConfigDTO.builder()
                    .credential(KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
                                    .build())
                    .build())
            .build();
    K8sRollingDeployRequest rollingRequest =
        K8sRollingDeployRequest.builder()
            .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
            .manifestDelegateConfig(
                KustomizeManifestDelegateConfig.builder()
                    .storeDelegateConfig(
                        GitStoreDelegateConfig.builder()
                            .gitConfigDTO(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).url(SOME_URL).build())
                            .build())
                    .pluginPath(pluginPath)
                    .build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.size()).isEqualTo(1);
    assertThat(executionCapabilities.get(0)).isInstanceOf(GitConnectionNGCapability.class);
  }
}
