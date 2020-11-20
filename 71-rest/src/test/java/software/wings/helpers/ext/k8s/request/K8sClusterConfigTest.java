package software.wings.helpers.ext.k8s.request;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.validation.capabilities.ClusterMasterUrlValidationCapability;
import software.wings.service.impl.ContainerServiceParams;

public class K8sClusterConfigTest extends WingsBaseTest {
  private static final String MASTER_URL = "http://a.b.c";

  @Test
  @Owner(developers = {OwnerRule.YOGESH, OwnerRule.ACASIAN})
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilities() {
    testGcpConfig();
    testAzureConfig();
    testKubernetesConfig();
    testKubernetesConfigWithDelegate();
    testContainerServiceParamsNoMasterUrl("");
    testContainerServiceParamsNoMasterUrl(null);
    testContainerServiceParamsWithMasterUrl();
  }

  private void testKubernetesConfigWithDelegate() {
    K8sClusterConfig clusterConfig =
        K8sClusterConfig.builder()
            .cloudProvider(
                KubernetesClusterConfig.builder().delegateName("my-delegate").useKubernetesDelegate(true).build())
            .build();

    assertThat(clusterConfig.fetchRequiredExecutionCapabilities())
        .containsExactlyInAnyOrder(
            SystemEnvCheckerCapability.builder().systemPropertyName("DELEGATE_NAME").comparate("my-delegate").build());
  }

  private void testKubernetesConfig() {
    K8sClusterConfig clusterConfig = K8sClusterConfig.builder()
                                         .cloudProvider(KubernetesClusterConfig.builder().masterUrl(MASTER_URL).build())
                                         .build();

    assertThat(clusterConfig.fetchRequiredExecutionCapabilities())
        .containsExactlyInAnyOrder(
            HttpConnectionExecutionCapability.builder().host("a.b.c").port(-1).scheme("http").build());
  }

  private void testContainerServiceParamsNoMasterUrl(String masterUrl) {
    KubernetesClusterConfig clusterConfig = KubernetesClusterConfig.builder().masterUrl("http://localhost").build();
    ContainerServiceParams params =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute().withValue(clusterConfig).build())
            .masterUrl(masterUrl)
            .build();

    assertThat(params.fetchRequiredExecutionCapabilities())
        .containsExactlyInAnyOrder(
            ClusterMasterUrlValidationCapability.builder().containerServiceParams(params).build());
  }

  private void testContainerServiceParamsWithMasterUrl() {
    KubernetesClusterConfig clusterConfig = KubernetesClusterConfig.builder().masterUrl("http://localhost").build();
    ContainerServiceParams params =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute().withValue(clusterConfig).build())
            .masterUrl("http://masterUrl")
            .build();

    assertThat(params.fetchRequiredExecutionCapabilities())
        .containsExactlyInAnyOrder(
            HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability("http://masterUrl"));
  }

  private void testGcpConfig() {
    K8sClusterConfig clusterConfig =
        K8sClusterConfig.builder().masterUrl(MASTER_URL).cloudProvider(GcpConfig.builder().build()).build();

    assertThat(clusterConfig.fetchRequiredExecutionCapabilities())
        .containsExactlyInAnyOrder(
            HttpConnectionExecutionCapability.builder().host("a.b.c").port(-1).scheme("http").build());
  }

  private void testAzureConfig() {
    K8sClusterConfig clusterConfig =
        K8sClusterConfig.builder().masterUrl(MASTER_URL).cloudProvider(AzureConfig.builder().build()).build();

    assertThat(clusterConfig.fetchRequiredExecutionCapabilities())
        .containsExactlyInAnyOrder(
            HttpConnectionExecutionCapability.builder().host("a.b.c").port(-1).scheme("http").build());
  }
}
