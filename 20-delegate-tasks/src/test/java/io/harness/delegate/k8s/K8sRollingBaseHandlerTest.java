package io.harness.delegate.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class K8sRollingBaseHandlerTest extends CategoryTest {
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @InjectMocks private K8sRollingBaseHandler handler;

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getPodsMultipleWorkloadsInSameNamespace() throws Exception {
    doReturn(Arrays.asList(buildPod(1), buildPod(2)))
        .when(k8sTaskHelperBase)
        .getPodDetails(kubernetesConfig, "default", "release-name", 1);

    List<KubernetesResource> resources = ImmutableList.of(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("deploy-1").namespace("default").kind("Deployment").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("deploy-2").namespace("default").kind("StatefulSet").build())
            .build());

    final List<K8sPod> pods = handler.getPods(1, resources, kubernetesConfig, "release-name");

    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList())).containsExactly("1", "2");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getPodsMultipleWorkloadsInMutipleNamespace() throws Exception {
    doReturn(Arrays.asList(buildPod(1), buildPod(2)))
        .when(k8sTaskHelperBase)
        .getPodDetails(kubernetesConfig, "default", "release-name", 1);

    doReturn(Arrays.asList(buildPod(3), buildPod(4), buildPod(5)))
        .when(k8sTaskHelperBase)
        .getPodDetails(kubernetesConfig, "harness", "release-name", 1);

    List<KubernetesResource> resources = ImmutableList.of(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("deploy-1").namespace("default").kind("Deployment").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("deploy-2").namespace("default").kind("StatefulSet").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("deploy-3").namespace("harness").kind("StatefulSet").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("deploy-4").namespace("harness").kind("Deployment").build())
            .build());

    final List<K8sPod> pods = handler.getPods(1, resources, kubernetesConfig, "release-name");

    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactly("1", "2", "3", "4", "5");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getPodsIfNoWorkloadGiven() throws Exception {
    assertThat(handler.getPods(1, null, kubernetesConfig, "release-name")).isEmpty();
    assertThat(handler.getPods(1, Collections.emptyList(), kubernetesConfig, "release-name")).isEmpty();
  }

  private K8sPod buildPod(int name) {
    return K8sPod.builder().name(String.valueOf(name)).build();
  }
}