package software.wings.helpers.ext.helm.request;

import static io.harness.rule.OwnerRule.ANSHUL;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HelmValuesFetchTaskParametersTest {
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilities() {
    HelmChartConfigParams helmChartConfigParams =
        HelmChartConfigParams.builder().helmRepoConfig(AmazonS3HelmRepoConfig.builder().build()).build();

    HelmValuesFetchTaskParameters taskParameters =
        HelmValuesFetchTaskParameters.builder().helmChartConfigTaskParams(helmChartConfigParams).build();
    List<ExecutionCapability> capabilities = taskParameters.fetchRequiredExecutionCapabilities();
    assertThat(capabilities.size()).isEqualTo(3);
    assertThat(capabilities)
        .extracting(ExecutionCapability::getCapabilityType)
        .containsExactly(CapabilityType.HELM_INSTALL, CapabilityType.HTTP, CapabilityType.CHART_MUSEUM);

    helmChartConfigParams.setHelmRepoConfig(GCSHelmRepoConfig.builder().build());
    capabilities = taskParameters.fetchRequiredExecutionCapabilities();
    assertThat(capabilities.size()).isEqualTo(2);
    assertThat(capabilities)
        .extracting(ExecutionCapability::getCapabilityType)
        .containsExactly(CapabilityType.HELM_INSTALL, CapabilityType.CHART_MUSEUM);

    helmChartConfigParams.setHelmRepoConfig(
        HttpHelmRepoConfig.builder().chartRepoUrl("http://www.example.com").build());
    capabilities = taskParameters.fetchRequiredExecutionCapabilities();
    assertThat(capabilities.size()).isEqualTo(2);
    assertThat(capabilities)
        .extracting(ExecutionCapability::getCapabilityType)
        .containsExactly(CapabilityType.HELM_INSTALL, CapabilityType.HTTP);

    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder()
            .settingAttribute(aSettingAttribute()
                                  .withValue(KubernetesClusterConfig.builder()
                                                 .useKubernetesDelegate(true)
                                                 .delegateName("delegateName")
                                                 .build())
                                  .build())
            .build();

    taskParameters.setBindTaskFeatureSet(true);
    taskParameters.setContainerServiceParams(containerServiceParams);
    capabilities = taskParameters.fetchRequiredExecutionCapabilities();
    assertThat(capabilities.size()).isEqualTo(3);
    assertThat(capabilities)
        .extracting(ExecutionCapability::getCapabilityType)
        .containsExactly(CapabilityType.HELM_INSTALL, CapabilityType.HTTP, CapabilityType.SYSTEM_ENV);

    helmChartConfigParams.setHelmRepoConfig(null);
    capabilities = taskParameters.fetchRequiredExecutionCapabilities();
    assertThat(capabilities.size()).isEqualTo(2);
    assertThat(capabilities)
        .extracting(ExecutionCapability::getCapabilityType)
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.SYSTEM_ENV);
  }
}
