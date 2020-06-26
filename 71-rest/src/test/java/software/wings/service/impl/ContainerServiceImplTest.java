package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.stream.Collectors;

public class ContainerServiceImplTest extends WingsBaseTest {
  @Mock private KubernetesContainerService kubernetesContainerService;
  @InjectMocks private ContainerServiceImpl containerService;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getContainerInfos() {
    ContainerServiceParams containerServiceParams =
        buildContainerSvcParams(null, KubernetesClusterConfig.builder().build());
    doReturn(asList(buildPod("p-1", "i-1"), buildPod("p-2", "i-2")))
        .when(kubernetesContainerService)
        .getRunningPodsWithLabels(
            any(KubernetesConfig.class), anyList(), eq("default"), eq(ImmutableMap.of("release", "release-name")));

    final List<ContainerInfo> containerInfos = containerService.getContainerInfos(containerServiceParams);

    assertThat(containerInfos.stream().map(ContainerInfo::getClusterName).collect(Collectors.toList()))
        .containsExactly("test", "test");
    assertThat(
        containerInfos.stream().map(v -> ((KubernetesContainerInfo) v).getPodName()).collect(Collectors.toList()))
        .containsExactly("p-1", "p-2");
    assertThat(containerInfos.stream().map(v -> ((KubernetesContainerInfo) v).getIp()).collect(Collectors.toList()))
        .containsExactly("i-1", "i-2");
    assertThat(
        containerInfos.stream().map(v -> ((KubernetesContainerInfo) v).getReleaseName()).collect(Collectors.toList()))
        .containsExactly("release-name", "release-name");
    assertThat(
        containerInfos.stream().map(v -> ((KubernetesContainerInfo) v).getNamespace()).collect(Collectors.toList()))
        .containsExactly("default", "default");
  }

  private ContainerServiceParams buildContainerSvcParams(String containerSvcName, SettingValue value) {
    return ContainerServiceParams.builder()
        .containerServiceName(containerSvcName)
        .settingAttribute(SettingAttribute.Builder.aSettingAttribute().withValue(value).build())
        .releaseName("release-name")
        .clusterName("test")
        .namespace("default")
        .build();
  }

  private Pod buildPod(String name, String ip) {
    Pod pod = new Pod();
    ObjectMeta meta = new ObjectMeta();
    meta.setName(name);
    PodStatus status = new PodStatus();
    status.setPodIP(ip);
    pod.setMetadata(meta);
    pod.setStatus(status);
    return pod;
  }
}