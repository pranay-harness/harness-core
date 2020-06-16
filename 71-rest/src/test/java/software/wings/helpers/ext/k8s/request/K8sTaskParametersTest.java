package software.wings.helpers.ext.k8s.request;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class K8sTaskParametersTest extends WingsBaseTest {
  @Mock private K8sClusterConfig clusterConfig;

  @Before
  public void setUp() throws Exception {
    doReturn(Arrays.asList(HttpConnectionExecutionCapability.builder().build()))
        .when(clusterConfig)
        .fetchRequiredExecutionCapabilities();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void fetchClusterConfigCapabilities() {
    assertThat(K8sApplyTaskParameters.builder()
                   .k8sClusterConfig(clusterConfig)
                   .build()
                   .fetchRequiredExecutionCapabilities()
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HTTP);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilitiesKustomize() {
    assertThat(hasKustomizeCapability(K8sScaleTaskParameters.builder()
                                          .k8sClusterConfig(clusterConfig)
                                          .build()
                                          .fetchRequiredExecutionCapabilities()))
        .isFalse();
    assertThat(hasKustomizeCapability(k8sTaskParamsWithNoKustomizeConfig().fetchRequiredExecutionCapabilities()))
        .isFalse();
    assertThat(hasKustomizeCapability(k8sTaskParamsWithKustomizePluginPath(null).fetchRequiredExecutionCapabilities()))
        .isTrue();
    assertThat(hasKustomizeCapability(k8sTaskParamsWithKustomizePluginPath(EMPTY).fetchRequiredExecutionCapabilities()))
        .isTrue();
    assertThat(hasKustomizeCapability(k8sTaskParamsWithKustomizePluginPath("foo").fetchRequiredExecutionCapabilities()))
        .isTrue();
  }

  private boolean hasKustomizeCapability(List<ExecutionCapability> capabilityList) {
    return capabilityList.stream()
        .map(ExecutionCapability::getCapabilityType)
        .collect(Collectors.toSet())
        .contains(CapabilityType.KUSTOMIZE);
  }

  private K8sTaskParameters k8sTaskParamsWithNoKustomizeConfig() {
    return K8sRollingDeployTaskParameters.builder()
        .k8sDelegateManifestConfig(K8sDelegateManifestConfig.builder().build())
        .k8sClusterConfig(clusterConfig)
        .build();
  }

  private K8sTaskParameters k8sTaskParamsWithKustomizePluginPath(String kustomizePluginPath) {
    return K8sApplyTaskParameters.builder()
        .k8sClusterConfig(clusterConfig)
        .k8sDelegateManifestConfig(
            K8sDelegateManifestConfig.builder()
                .kustomizeConfig(KustomizeConfig.builder().pluginRootDir(kustomizePluginPath).build())
                .build())
        .build();
  }
}