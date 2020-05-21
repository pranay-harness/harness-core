package software.wings.helpers.ext.container;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.SETTING_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.intfc.SettingsService;

public class ContainerDeploymentManagerHelperTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @InjectMocks @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetK8sClusterConfig() {
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(SETTING_NAME)
                                            .withUuid(SETTING_ID)
                                            .withAccountId(ACCOUNT_ID)
                                            .withValue(KubernetesClusterConfig.builder().build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    DirectKubernetesInfrastructureMapping infraMapping = aDirectKubernetesInfrastructureMapping()
                                                             .withNamespace("default")
                                                             .withComputeProviderSettingId(SETTING_ID)
                                                             .build();

    K8sClusterConfig k8sClusterConfig = containerDeploymentManagerHelper.getK8sClusterConfig(infraMapping);
    assertThat(k8sClusterConfig).isNotNull();
    assertThat(k8sClusterConfig.getCloudProviderName()).isEqualTo(SETTING_NAME);
    assertThat(k8sClusterConfig.getClusterName()).isNull();
    assertThat(k8sClusterConfig.getNamespace()).isEqualTo("default");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void buildInstanceElement() {
    final ServiceTemplateElement serviceTemplateElement =
        ServiceTemplateElement.Builder.aServiceTemplateElement().build();
    final InstanceElement instanceElement =
        containerDeploymentManagerHelper.buildInstanceElement(serviceTemplateElement,
            ContainerInfo.builder()
                .workloadName("workload")
                .ip("ip")
                .podName("podname")
                .hostName("hostname")
                .newContainer(true)
                .containerId("containerId")
                .build());
    assertThat(instanceElement.getServiceTemplateElement()).isEqualTo(serviceTemplateElement);
    assertThat(instanceElement.getWorkloadName()).isEqualTo("workload");
    assertThat(instanceElement.getDockerId()).isEqualTo("containerId");
    assertThat(instanceElement.isNewInstance()).isTrue();
    assertThat(instanceElement.getPodName()).isEqualTo("podname");
    assertThat(instanceElement.getHostName()).isEqualTo("hostname");
    assertThat(instanceElement.getHost().getIp()).isEqualTo("ip");
  }
}
