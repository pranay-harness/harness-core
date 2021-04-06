package io.harness.delegate.task.executioncapability;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.expression.ExpressionEvaluator;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KustomizeCapabilityCheck.class})
@OwnedBy(CDP)
public class KustomizeCapabilityCheckTest extends CategoryTest {
  @Mock private KustomizeCapability capability;
  @Mock private ExpressionEvaluator expressionEvaluator;
  @InjectMocks private KustomizeCapabilityCheck capabilityCheck;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void pluginsExist() {
    PowerMockito.mockStatic(KustomizeCapabilityCheck.class);
    when(KustomizeCapabilityCheck.doesKustomizePluginDirExist(any())).thenReturn(true);
    assertThat(capabilityCheck.performCapabilityCheck(capability))
        .isEqualTo(CapabilityResponse.builder().validated(true).delegateCapability(capability).build());
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void pluginsDoNotExist() {
    PowerMockito.mockStatic(KustomizeCapabilityCheck.class);
    when(KustomizeCapabilityCheck.doesKustomizePluginDirExist(any())).thenReturn(false);
    assertThat(capabilityCheck.performCapabilityCheck(capability))
        .isEqualTo(CapabilityResponse.builder().validated(false).delegateCapability(capability).build());
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
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().pluginPath(pluginPath).build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isNotEmpty();
    assertThat(executionCapabilities.get(0)).isInstanceOf(KustomizeCapability.class);
    KustomizeCapability kustomizeCapability = (KustomizeCapability) executionCapabilities.get(0);
    assertThat(kustomizeCapability.getPluginRootDir()).isEqualTo(pluginPath);
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
            .manifestDelegateConfig(KustomizeManifestDelegateConfig.builder().pluginPath(pluginPath).build())
            .build();

    List<ExecutionCapability> executionCapabilities =
        rollingRequest.fetchRequiredExecutionCapabilities(expressionEvaluator);
    assertThat(executionCapabilities).isEmpty();
  }
}
