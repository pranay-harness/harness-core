package io.harness.delegate.task.k8s;

import static io.harness.k8s.model.Kind.ConfigMap;
import static io.harness.k8s.model.Kind.Deployment;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.k8s.model.Kind.Service;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.state.StateConstants.DEFAULT_STEADY_STATE_TIMEOUT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;
import me.snowdrop.istio.api.networking.v1alpha3.Subset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class K8sTaskHelperBaseTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private KubernetesContainerService mockKubernetesContainerService;
  @Mock private TimeLimiter mockTimeLimiter;
  @Inject @InjectMocks private K8sTaskHelperBase k8sTaskHelperBase;

  long LONG_TIMEOUT_INTERVAL = 60 * 1000L;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPodDetailsWithLabels() throws Exception {
    KubernetesConfig config = KubernetesConfig.builder().build();
    Map<String, String> labelsQuery = ImmutableMap.of("release-name", "releaseName");
    List<Pod> existingPods =
        asList(k8sApiMockPodWith("uid-1", ImmutableMap.of("marker", "marker-value"), singletonList("container")),
            k8sApiMockPodWith("uid-2", ImmutableMap.of("release", "releaseName", "color", "green"), emptyList()),
            k8sApiMockPodWith("uid-3", ImmutableMap.of(), asList("container-1", "container-2", "container-3")));

    doReturn(existingPods)
        .when(mockKubernetesContainerService)
        .getRunningPodsWithLabels(config, "default", labelsQuery);
    doAnswer(invocation -> invocation.getArgumentAt(0, Callable.class).call())
        .when(mockTimeLimiter)
        .callWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class), anyBoolean());
    List<K8sPod> pods =
        k8sTaskHelperBase.getPodDetailsWithLabels(config, "default", "releaseName", labelsQuery, LONG_TIMEOUT_INTERVAL);

    assertThat(pods).isNotEmpty();
    assertThat(pods).hasSize(3);
    assertThat(pods.get(0).getUid()).isEqualTo("uid-1");
    assertThatK8sPodHas(pods.get(0), "uid-1", ImmutableMap.of("marker", "marker-value"), singletonList("container"));
    assertThatK8sPodHas(pods.get(1), "uid-2", ImmutableMap.of("release", "releaseName", "color", "green"), emptyList());
    assertThatK8sPodHas(pods.get(2), "uid-3", ImmutableMap.of(), asList("container-1", "container-2", "container-3"));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetEmptyPodListWhenReleaseLabelIsMissing() throws Exception {
    KubernetesConfig config = KubernetesConfig.builder().build();

    List<K8sPod> podsWithReleaseNameNull =
        k8sTaskHelperBase.getPodDetails(config, "default", null, LONG_TIMEOUT_INTERVAL);
    assertThat(podsWithReleaseNameNull).isEmpty();

    List<K8sPod> podsWithReleaseNameEmpty =
        k8sTaskHelperBase.getPodDetails(config, "default", "", LONG_TIMEOUT_INTERVAL);
    assertThat(podsWithReleaseNameEmpty).isEmpty();
  }

  private Pod k8sApiMockPodWith(String uid, Map<String, String> labels, List<String> containerIds) {
    return new PodBuilder()
        .withMetadata(new ObjectMetaBuilder()
                          .withUid(uid)
                          .withName(uid + "-name")
                          .withNamespace("default")
                          .withLabels(labels)
                          .build())
        .withStatus(new PodStatusBuilder()
                        .withContainerStatuses(containerIds.stream()
                                                   .map(id
                                                       -> new ContainerStatusBuilder()
                                                              .withContainerID(id)
                                                              .withName(id + "-name")
                                                              .withImage("example:0.0.1")
                                                              .build())
                                                   .collect(Collectors.toList()))
                        .build())
        .build();
  }

  private void assertThatK8sPodHas(K8sPod pod, String uid, Map<String, String> labels, List<String> containerIds) {
    assertThat(pod.getUid()).isEqualTo(uid);
    assertThat(pod.getName()).isEqualTo(uid + "-name");
    assertThat(pod.getLabels()).isEqualTo(labels);
    assertThat(pod.getContainerList()).hasSize(containerIds.size());
    IntStream.range(0, containerIds.size()).forEach(idx -> {
      K8sContainer container = pod.getContainerList().get(idx);
      String expectedContainerId = containerIds.get(idx);
      assertThat(container.getContainerId()).isEqualTo(expectedContainerId);
      assertThat(container.getName()).isEqualTo(expectedContainerId + "-name");
      assertThat(container.getImage()).isEqualTo("example:0.0.1");
    });
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGenerateSubsetsForDestinationRule() {
    List<String> subsetNames = new ArrayList<>();
    subsetNames.add(HarnessLabelValues.trackCanary);
    subsetNames.add(HarnessLabelValues.trackStable);
    subsetNames.add(HarnessLabelValues.colorBlue);
    subsetNames.add(HarnessLabelValues.colorGreen);

    final List<Subset> result = k8sTaskHelperBase.generateSubsetsForDestinationRule(subsetNames);

    assertThat(result.size()).isEqualTo(4);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetTimeoutMillisFromMinutes() throws Exception {
    int randomPositiveInt = new Random().nextInt(1000) + 1;
    assertThat(K8sTaskHelperBase.getTimeoutMillisFromMinutes(-randomPositiveInt))
        .isEqualTo(DEFAULT_STEADY_STATE_TIMEOUT * 60 * 1000L);
    assertThat(K8sTaskHelperBase.getTimeoutMillisFromMinutes(null))
        .isEqualTo(DEFAULT_STEADY_STATE_TIMEOUT * 60 * 1000L);
    assertThat(K8sTaskHelperBase.getTimeoutMillisFromMinutes(0)).isEqualTo(DEFAULT_STEADY_STATE_TIMEOUT * 60 * 1000L);
    assertThat(K8sTaskHelperBase.getTimeoutMillisFromMinutes(1)).isEqualTo(60 * 1000L);
    assertThat(K8sTaskHelperBase.getTimeoutMillisFromMinutes(randomPositiveInt))
        .isEqualTo(randomPositiveInt * 60 * 1000L);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetEmptyLogOutputStream() throws Exception {
    assertThat(K8sTaskHelperBase.getEmptyLogOutputStream()).isInstanceOf(LogOutputStream.class);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testArrangeResourceIdsInDeletionOrder() {
    List<KubernetesResourceId> kubernetesResourceIdList = getKubernetesResourceIdList();
    kubernetesResourceIdList = k8sTaskHelperBase.arrangeResourceIdsInDeletionOrder(kubernetesResourceIdList);

    assertThat(kubernetesResourceIdList.size()).isEqualTo(4);
    assertThat(kubernetesResourceIdList.get(0).getKind()).isEqualTo(Deployment.name());
    assertThat(kubernetesResourceIdList.get(1).getKind()).isEqualTo(Service.name());
    assertThat(kubernetesResourceIdList.get(2).getKind()).isEqualTo(ConfigMap.name());
    assertThat(kubernetesResourceIdList.get(3).getKind()).isEqualTo(Namespace.name());
  }

  private List<KubernetesResourceId> getKubernetesResourceIdList() {
    List<KubernetesResourceId> kubernetesResourceIds = new ArrayList<>();
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(Namespace.name()).name("n1").namespace("default").build());
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(Deployment.name()).name("d1").namespace("default").build());
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(ConfigMap.name()).name("c1").namespace("default").build());
    kubernetesResourceIds.add(
        KubernetesResourceId.builder().kind(Service.name()).name("s1").namespace("default").build());
    return kubernetesResourceIds;
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testCreateLogInfoOutputStream() throws Exception {
    List<KubernetesResourceId> resourceIds =
        ImmutableList.of(KubernetesResourceId.builder().name("app1").namespace("default").build());

    final String eventInfoFormat = "%-7s: %-4s   %s";
    LogCallback executionLogCallback = spy(getLogCallback());
    LogOutputStream logOutputStream =
        k8sTaskHelperBase.createFilteredInfoLogOutputStream(resourceIds, executionLogCallback, eventInfoFormat);
    byte[] message = "Starting app1 in default namespace\r\n".getBytes();
    logOutputStream.write(message);

    verify(executionLogCallback, times(1)).saveExecutionLog("Event  : app1   Starting app1 in default namespace", INFO);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testCreateLogErrorOutputStream() throws Exception {
    LogCallback executionLogCallback = spy(getLogCallback());
    LogOutputStream logOutputStream = k8sTaskHelperBase.createErrorLogOutputStream(executionLogCallback);
    byte[] message = "Failed to start app1 in default namespace\r\n".getBytes();
    logOutputStream.write(message);

    verify(executionLogCallback, times(1))
        .saveExecutionLog("Event  : Failed to start app1 in default namespace", ERROR);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testCreateStatusInfoLogOutputStream() throws Exception {
    LogCallback executionLogCallback = spy(getLogCallback());
    LogOutputStream logOutputStream =
        k8sTaskHelperBase.createStatusInfoLogOutputStream(executionLogCallback, "app1", "%n%-7s: %-4s   %s");
    byte[] message = "Deployed\r\n".getBytes();
    logOutputStream.write(message);

    verify(executionLogCallback, times(1)).saveExecutionLog("\nStatus : app1   Deployed", INFO);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testCreateStatusErrorLogOutputStream() throws Exception {
    LogCallback executionLogCallback = spy(getLogCallback());
    LogOutputStream logOutputStream =
        k8sTaskHelperBase.createStatusErrorLogOutputStream(executionLogCallback, "app1", "%n%-7s: %-4s   %s");
    byte[] message = "Failed\r\n".getBytes();
    logOutputStream.write(message);

    verify(executionLogCallback, times(1)).saveExecutionLog("\nStatus : app1   Failed", ERROR);
  }

  private LogCallback getLogCallback() {
    return new LogCallback() {
      @Override
      public void saveExecutionLog(String line) {}

      @Override
      public void saveExecutionLog(String line, LogLevel logLevel) {}

      @Override
      public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {}
    };
  }
}
