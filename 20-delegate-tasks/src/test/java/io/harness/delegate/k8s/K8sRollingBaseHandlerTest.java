package io.harness.delegate.k8s;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class K8sRollingBaseHandlerTest extends CategoryTest {
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @InjectMocks private K8sRollingBaseHandler k8sRollingBaseHandler;

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

    final List<K8sPod> pods = k8sRollingBaseHandler.getPods(1, resources, kubernetesConfig, "release-name");

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

    final List<K8sPod> pods = k8sRollingBaseHandler.getPods(1, resources, kubernetesConfig, "release-name");

    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactly("1", "2", "3", "4", "5");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getPodsIfNoWorkloadGiven() throws Exception {
    assertThat(k8sRollingBaseHandler.getPods(1, null, kubernetesConfig, "release-name")).isEmpty();
    assertThat(k8sRollingBaseHandler.getPods(1, Collections.emptyList(), kubernetesConfig, "release-name")).isEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTagNewPods() {
    assertThat(k8sRollingBaseHandler.tagNewPods(emptyList(), emptyList())).isEmpty();

    List<K8sPod> pods = k8sRollingBaseHandler.tagNewPods(
        asList(podWithName("pod-1"), podWithName("pod-2")), asList(podWithName("old-pod-1"), podWithName("old-pod-2")));
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(2);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");

    pods = k8sRollingBaseHandler.tagNewPods(
        asList(podWithName("pod-1"), podWithName("pod-2")), asList(podWithName("pod-1")));
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(1);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");

    pods = k8sRollingBaseHandler.tagNewPods(asList(podWithName("pod-1"), podWithName("pod-2")), emptyList());
    assertThat(pods).hasSize(2);
    assertThat(pods.stream().filter(K8sPod::isNewPod).count()).isEqualTo(2);
    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
  }

  private K8sPod podWithName(String name) {
    return K8sPod.builder().name(name).build();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testUpdateDeploymentConfigRevision() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    Release.KubernetesResourceIdRevision resourceIdMock = Mockito.mock(Release.KubernetesResourceIdRevision.class);
    Release release = Release.builder().managedWorkloads(asList(resourceIdMock)).build();

    when(k8sTaskHelperBase.getLatestRevision(any(), any(), any())).thenReturn("2");
    when(resourceIdMock.getWorkload())
        .thenReturn(KubernetesResourceId.builder().kind(Kind.DeploymentConfig.name()).build());

    k8sRollingBaseHandler.updateDeploymentConfigRevision(delegateTaskParams, release, null);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(resourceIdMock).setRevision(captor.capture());
    assertThat(captor.getValue()).isEqualTo("2");

    when(resourceIdMock.getWorkload()).thenReturn(KubernetesResourceId.builder().kind(Kind.Deployment.name()).build());
    verify(resourceIdMock, times(1)).setRevision(anyString());
  }

  private K8sPod buildPod(int name) {
    return K8sPod.builder().name(String.valueOf(name)).build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPods() throws Exception {
    KubernetesResourceId sample = KubernetesResourceId.builder().namespace("default").build();
    List<KubernetesResource> managedWorkload = Arrays.asList(KubernetesResource.builder().resourceId(sample).build(),
        KubernetesResource.builder().resourceId(sample).build());
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

    k8sRollingBaseHandler.getPods(3000L, managedWorkload, kubernetesConfig, "releaseName");

    verify(k8sTaskHelperBase, times(1)).getPodDetails(kubernetesConfig, "default", "releaseName", 3000L);
  }
}
