package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.k8s.K8sTestHelper.service;
import static io.harness.rule.OwnerRule.ABOSII;

import static software.wings.beans.LogColor.Blue;
import static software.wings.beans.LogColor.Green;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1ServiceSpecBuilder;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class K8sBGBaseHandlerTest extends CategoryTest {
  @Mock K8sTaskHelperBase k8sTaskHelperBase;
  @Mock KubernetesContainerService kubernetesContainerService;

  @InjectMocks K8sBGBaseHandler k8sBGBaseHandler;

  @Mock LogCallback logCallback;
  @Mock Kubectl kubectl;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLogColor() {
    assertThat(k8sBGBaseHandler.getLogColor(HarnessLabelValues.colorBlue)).isEqualTo(Blue);

    assertThat(k8sBGBaseHandler.getLogColor(HarnessLabelValues.colorGreen)).isEqualTo(Green);

    assertThat(k8sBGBaseHandler.getLogColor("unhandled")).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetInverseColor() {
    assertThat(k8sBGBaseHandler.getInverseColor(HarnessLabelValues.colorBlue)).isEqualTo(HarnessLabelValues.colorGreen);

    assertThat(k8sBGBaseHandler.getInverseColor(HarnessLabelValues.colorGreen)).isEqualTo(HarnessLabelValues.colorBlue);

    assertThat(k8sBGBaseHandler.getInverseColor("unhandled")).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testWrapUp() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    k8sBGBaseHandler.wrapUp(delegateTaskParams, logCallback, kubectl);

    verify(k8sTaskHelperBase).describe(kubectl, delegateTaskParams, logCallback);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPrimaryColor() throws Exception {
    V1Service serviceInCluster =
        new V1ServiceBuilder()
            .withSpec(new V1ServiceSpecBuilder()
                          .addToSelector(ImmutableMap.of(HarnessLabels.color, HarnessLabelValues.colorBlue))
                          .build())
            .build();
    testGetPrimaryColor(serviceInCluster, HarnessLabelValues.colorBlue);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPrimaryColorMissingSpec() throws Exception {
    V1Service serviceInCluster = new V1ServiceBuilder().withSpec(null).build();

    testGetPrimaryColor(serviceInCluster, null);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPrimaryColorMissingSelector() throws Exception {
    V1Service serviceInCluster =
        new V1ServiceBuilder().withSpec(new V1ServiceSpecBuilder().withSelector(null).build()).build();

    testGetPrimaryColor(serviceInCluster, null);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPrimaryColorMissingService() throws Exception {
    testGetPrimaryColor(null, HarnessLabelValues.colorDefault);
  }

  private void testGetPrimaryColor(V1Service serviceInCluster, String expectedColor) throws Exception {
    KubernetesResource service = service();
    KubernetesConfig config = KubernetesConfig.builder().build();

    doReturn(serviceInCluster).when(kubernetesContainerService).getService(config, "my-service");

    String result = k8sBGBaseHandler.getPrimaryColor(service, config, logCallback);
    assertThat(result).isEqualTo(expectedColor);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetAllPods() throws Exception {
    final K8sPod stagePod = K8sPod.builder().name("stage-pod").build();
    final K8sPod primaryPod = K8sPod.builder().name("primary-pod").build();
    final List<K8sPod> stagePods = Collections.singletonList(stagePod);
    final List<K8sPod> primaryPods = Collections.singletonList(primaryPod);
    final KubernetesConfig config = KubernetesConfig.builder().build();
    final KubernetesResource managedWorkload =
        KubernetesResource.builder().resourceId(KubernetesResourceId.builder().namespace("default").build()).build();

    doReturn(stagePods).when(k8sTaskHelperBase).getPodDetailsWithColor(config, "default", "release", "stage", 1000);
    doReturn(primaryPods).when(k8sTaskHelperBase).getPodDetailsWithColor(config, "default", "release", "primary", 1000);

    List<K8sPod> pods = k8sBGBaseHandler.getAllPods(1000, config, managedWorkload, "primary", "stage", "release");

    assertThat(pods).containsExactlyInAnyOrder(stagePod, primaryPod);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCleanupForBlueGreen() throws Exception {
    KubernetesResourceId stage = KubernetesResourceId.builder().name("deployment-blue").build();
    KubernetesResourceId versioned = KubernetesResourceId.builder().name("config-1").versioned(true).build();
    KubernetesResourceId primary = KubernetesResourceId.builder().name("deployment-green").build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.createNewRelease(asList(stage, versioned));
    releaseHistory.setReleaseStatus(Release.Status.Succeeded);
    releaseHistory.getLatestRelease().setManagedWorkload(stage);
    releaseHistory.setReleaseNumber(0);

    releaseHistory.createNewRelease(Collections.singletonList(stage));
    releaseHistory.setReleaseStatus(Release.Status.Failed);
    releaseHistory.getLatestRelease().setManagedWorkload(stage);
    releaseHistory.setReleaseNumber(1);

    releaseHistory.createNewRelease(asList(primary, versioned));
    releaseHistory.setReleaseStatus(Release.Status.Succeeded);
    releaseHistory.getLatestRelease().setManagedWorkload(primary);
    releaseHistory.setReleaseNumber(2);

    releaseHistory.createNewRelease(asList(stage, versioned));
    releaseHistory.setReleaseStatus(Release.Status.Succeeded);
    releaseHistory.getLatestRelease().setManagedWorkload(stage);
    releaseHistory.setReleaseNumber(3);

    k8sBGBaseHandler.cleanupForBlueGreen(
        delegateTaskParams, releaseHistory, logCallback, "green", "blue", releaseHistory.getLatestRelease(), kubectl);

    // Should remove all stage release expect the current one and keep primary
    assertThat(releaseHistory.getReleases()).hasSize(2);
    assertThat(releaseHistory.getRelease(2)).isNotNull();
    assertThat(releaseHistory.getRelease(3)).isNotNull();

    // Should delete resource single time since the latest and primary releases shouldn't be cleaned
    verify(k8sTaskHelperBase).delete(kubectl, delegateTaskParams, asList(versioned), logCallback, true);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCleanupForBlueGreenSameColor() throws Exception {
    ReleaseHistory releaseHistory = mock(ReleaseHistory.class);

    k8sBGBaseHandler.cleanupForBlueGreen(K8sDelegateTaskParams.builder().build(), releaseHistory, logCallback, "blue",
        "blue", Release.builder().build(), kubectl);

    // Do nothing if colors are the same
    verifyNoMoreInteractions(releaseHistory);
  }
}