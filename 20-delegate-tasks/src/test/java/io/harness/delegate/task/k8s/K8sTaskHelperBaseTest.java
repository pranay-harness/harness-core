package io.harness.delegate.task.k8s;

import static io.harness.helm.HelmConstants.HELM_RELEASE_LABEL;
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
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.container.ContainerInfo;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.AbstractExecutable;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1ContainerStatusBuilder;
import io.kubernetes.client.openapi.models.V1LoadBalancerIngress;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1PodStatusBuilder;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1ServicePortBuilder;
import io.kubernetes.client.util.Yaml;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.snowdrop.istio.api.networking.v1alpha3.Subset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

public class K8sTaskHelperBaseTest extends CategoryTest {
  private static final KubernetesConfig KUBERNETES_CONFIG = KubernetesConfig.builder().build();
  private static final String DEFAULT = "default";
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private KubernetesContainerService mockKubernetesContainerService;
  @Mock private TimeLimiter mockTimeLimiter;
  @Mock private LogCallback executionLogCallback;

  @Inject @InjectMocks private K8sTaskHelperBase k8sTaskHelperBase;

  long LONG_TIMEOUT_INTERVAL = 60 * 1000L;

  @Before
  public void setup() throws Exception {
    doAnswer(invocation -> invocation.getArgumentAt(0, Callable.class).call())
        .when(mockTimeLimiter)
        .callWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class), anyBoolean());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPodDetailsWithLabelsFabric8() throws Exception {
    KubernetesConfig config = KubernetesConfig.builder().build();
    Map<String, String> labelsQuery = ImmutableMap.of("release-name", "releaseName");
    List<Pod> existingPods =
        asList(k8sApiMockPodWith("uid-1", ImmutableMap.of("marker", "marker-value"), singletonList("container")),
            k8sApiMockPodWith("uid-2", ImmutableMap.of("release", "releaseName", "color", "green"), emptyList()),
            k8sApiMockPodWith("uid-3", ImmutableMap.of(), asList("container-1", "container-2", "container-3")));

    doReturn(existingPods)
        .when(mockKubernetesContainerService)
        .getRunningPodsWithLabelsFabric8(config, "default", labelsQuery);
    List<K8sPod> pods = k8sTaskHelperBase.getPodDetailsWithLabelsFabric8(
        config, "default", "releaseName", labelsQuery, LONG_TIMEOUT_INTERVAL);

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

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLoadBalancerEndpoint() {
    List<Integer> servicePorts = asList(38493, 80, 443);
    List<KubernetesResource> resources = asList(getServiceResource("ClusterIP"), getServiceResource("LoadBalancer"));

    doReturn(getK8sService("LoadBalancer", servicePorts, "hostname", null))
        .when(mockKubernetesContainerService)
        .getService(KUBERNETES_CONFIG, "LoadBalancer", DEFAULT);

    String endpoint = k8sTaskHelperBase.getLoadBalancerEndpoint(KUBERNETES_CONFIG, resources);
    assertThat(endpoint).isEqualTo("https://hostname/");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLoadBalancerEndpointHttpPort() {
    List<Integer> servicePorts = asList(38493, 80);
    List<KubernetesResource> resources = singletonList(getServiceResource("LoadBalancer"));

    doReturn(getK8sService("LoadBalancer", servicePorts, "hostname", null))
        .when(mockKubernetesContainerService)
        .getService(KUBERNETES_CONFIG, "LoadBalancer", DEFAULT);

    String endpoint = k8sTaskHelperBase.getLoadBalancerEndpoint(KUBERNETES_CONFIG, resources);
    assertThat(endpoint).isEqualTo("http://hostname/");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLoadBalancerEndpointHttpPortWithIP() {
    List<Integer> servicePorts = asList(38493, 80);
    List<KubernetesResource> resources = singletonList(getServiceResource("LoadBalancer"));

    doReturn(getK8sService("LoadBalancer", servicePorts, null, "10.33.33.33"))
        .when(mockKubernetesContainerService)
        .getService(KUBERNETES_CONFIG, "LoadBalancer", DEFAULT);

    String endpoint = k8sTaskHelperBase.getLoadBalancerEndpoint(KUBERNETES_CONFIG, resources);
    assertThat(endpoint).isEqualTo("http://10.33.33.33/");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLoadBalancerEndpointRandomPort() {
    List<Integer> servicePorts = singletonList(38493);
    List<KubernetesResource> resources = singletonList(getServiceResource("LoadBalancer"));

    doReturn(getK8sService("LoadBalancer", servicePorts, "hostname", null))
        .when(mockKubernetesContainerService)
        .getService(KUBERNETES_CONFIG, "LoadBalancer", DEFAULT);

    String endpoint = k8sTaskHelperBase.getLoadBalancerEndpoint(KUBERNETES_CONFIG, resources);
    assertThat(endpoint).isEqualTo("hostname:38493");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLoadBalancerEndpointNoPortsExposed() {
    List<KubernetesResource> resources = singletonList(getServiceResource("LoadBalancer"));

    doReturn(getK8sService("LoadBalancer", emptyList(), null, "33.33.33.33"))
        .when(mockKubernetesContainerService)
        .getService(KUBERNETES_CONFIG, "LoadBalancer", DEFAULT);

    String endpoint = k8sTaskHelperBase.getLoadBalancerEndpoint(KUBERNETES_CONFIG, resources);
    assertThat(endpoint).isEqualTo("33.33.33.33");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetLoadBalancerEndpointServiceNotReady() throws Exception {
    List<KubernetesResource> resources = singletonList(getServiceResource("LoadBalancer"));

    doThrow(new UncheckedTimeoutException())
        .when(mockTimeLimiter)
        .callWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class), anyBoolean());

    String endpoint = k8sTaskHelperBase.getLoadBalancerEndpoint(KUBERNETES_CONFIG, resources);
    assertThat(endpoint).isNull();
  }

  private V1Service getK8sService(String type, List<Integer> ports, String hostname, String ip) {
    V1ServiceBuilder serviceBuilder = new V1ServiceBuilder()
                                          .withNewSpec()
                                          .withPorts(ports.stream()
                                                         .map(port -> new V1ServicePortBuilder().withPort(port).build())
                                                         .collect(Collectors.toList()))
                                          .withType(type)
                                          .endSpec();

    if (hostname != null || ip != null) {
      serviceBuilder.withNewStatus()
          .withNewLoadBalancer()
          .withIngress(new V1LoadBalancerIngress().hostname(hostname).ip(ip))
          .endLoadBalancer()
          .endStatus();
    }

    return serviceBuilder.build();
  }

  private KubernetesResource getServiceResource(String type) {
    return KubernetesResource.builder()
        .resourceId(KubernetesResourceId.builder().name(type).kind(Service.name()).namespace(DEFAULT).build())
        .spec(Yaml.dump(getK8sService(type, emptyList(), null, null)))
        .build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPodDetailsWithLabels() throws Exception {
    KubernetesConfig config = KubernetesConfig.builder().build();
    Map<String, String> labels = ImmutableMap.of("release-name", "releaseName");
    List<V1Pod> existingPods =
        asList(v1Pod(v1Metadata("pod-1", labels), v1PodStatus("pod-1-ip", v1ContainerStatus("web", "nginx"))),
            v1Pod(v1Metadata("pod-2", labels),
                v1PodStatus("pod-2-ip", v1ContainerStatus("app", "todo"), v1ContainerStatus("web", "nginx"))),
            v1Pod(v1Metadata("pod-3", labels), v1PodStatus("pod-3-ip")), v1Pod(v1Metadata("pod-4", labels), null),
            v1Pod(null, null));

    doReturn(existingPods).when(mockKubernetesContainerService).getRunningPodsWithLabels(config, "default", labels);

    List<K8sPod> pods =
        k8sTaskHelperBase.getPodDetailsWithLabels(config, "default", "releaseName", labels, LONG_TIMEOUT_INTERVAL);

    assertThat(pods).hasSize(2);
    K8sPod pod = pods.get(0);
    K8sContainer container = pods.get(0).getContainerList().get(0);
    assertThat(pod.getName()).isEqualTo("pod-1");
    assertThat(pod.getUid()).isEqualTo("pod-1");
    assertThat(pod.getLabels()).isEqualTo(labels);
    assertThat(pod.getContainerList()).hasSize(1);
    assertThat(container.getName()).isEqualTo("web");
    assertThat(container.getImage()).isEqualTo("nginx");

    pod = pods.get(1);
    assertThat(pod.getName()).isEqualTo("pod-2");
    assertThat(pod.getUid()).isEqualTo("pod-2");
    assertThat(pod.getLabels()).isEqualTo(labels);
    assertThat(pod.getContainerList()).hasSize(2);
    container = pods.get(1).getContainerList().get(0);
    assertThat(container.getName()).isEqualTo("app");
    assertThat(container.getImage()).isEqualTo("todo");
    container = pods.get(1).getContainerList().get(1);
    assertThat(container.getName()).isEqualTo("web");
    assertThat(container.getImage()).isEqualTo("nginx");
  }

  private V1ObjectMeta v1Metadata(String name, Map<String, String> labels) {
    return new V1ObjectMetaBuilder().withUid(name).withName(name).withLabels(labels).build();
  }

  private V1ContainerStatus v1ContainerStatus(String name, String image) {
    return new V1ContainerStatusBuilder().withContainerID(name).withName(name).withImage(image).build();
  }

  private V1PodStatus v1PodStatus(String podIP, V1ContainerStatus... containerStatuses) {
    return new V1PodStatusBuilder().withPodIP(podIP).withContainerStatuses(containerStatuses).build();
  }

  private V1Pod v1Pod(V1ObjectMeta metadata, V1PodStatus status) {
    return new V1PodBuilder().withMetadata(metadata).withStatus(status).build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPodDetailsWithTrack() throws Exception {
    KubernetesConfig config = KubernetesConfig.builder().build();
    K8sTaskHelperBase spyK8sTaskHelperBase = spy(K8sTaskHelperBase.class);
    Map<String, String> expectedLabels =
        ImmutableMap.of(HarnessLabels.releaseName, "release", HarnessLabels.track, "canary");
    doReturn(emptyList())
        .when(spyK8sTaskHelperBase)
        .getPodDetailsWithLabels(
            any(KubernetesConfig.class), anyString(), anyString(), anyMapOf(String.class, String.class), anyLong());
    spyK8sTaskHelperBase.getPodDetailsWithTrack(config, "default", "release", "canary", DEFAULT_STEADY_STATE_TIMEOUT);

    verify(spyK8sTaskHelperBase, times(1))
        .getPodDetailsWithLabels(config, "default", "release", expectedLabels, DEFAULT_STEADY_STATE_TIMEOUT);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPodDetailsWithColor() throws Exception {
    KubernetesConfig config = KubernetesConfig.builder().build();
    K8sTaskHelperBase spyK8sTaskHelperBase = spy(K8sTaskHelperBase.class);
    Map<String, String> expectedLabels =
        ImmutableMap.of(HarnessLabels.releaseName, "release", HarnessLabels.color, "blue");
    doReturn(emptyList())
        .when(spyK8sTaskHelperBase)
        .getPodDetailsWithLabels(
            any(KubernetesConfig.class), anyString(), anyString(), anyMapOf(String.class, String.class), anyLong());
    spyK8sTaskHelperBase.getPodDetailsWithColor(config, "default", "release", "blue", DEFAULT_STEADY_STATE_TIMEOUT);

    verify(spyK8sTaskHelperBase, times(1))
        .getPodDetailsWithLabels(config, "default", "release", expectedLabels, DEFAULT_STEADY_STATE_TIMEOUT);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetContainerInfos() throws Exception {
    List<K8sPod> existingPods = singletonList(K8sPod.builder().name("name").podIP("pod-ip").build());
    KubernetesConfig config = KubernetesConfig.builder().build();
    K8sTaskHelperBase spyK8sTaskHelperBase = spy(K8sTaskHelperBase.class);
    Map<String, String> expectedLabels = ImmutableMap.of(HELM_RELEASE_LABEL, "release");
    doReturn(existingPods)
        .when(spyK8sTaskHelperBase)
        .getPodDetailsWithLabels(
            any(KubernetesConfig.class), anyString(), anyString(), anyMapOf(String.class, String.class), anyLong());
    List<ContainerInfo> result =
        spyK8sTaskHelperBase.getContainerInfos(config, "release", "default", DEFAULT_STEADY_STATE_TIMEOUT);

    verify(spyK8sTaskHelperBase, times(1))
        .getPodDetailsWithLabels(config, "default", "release", expectedLabels, DEFAULT_STEADY_STATE_TIMEOUT);
    assertThat(result).hasSize(1);
    ContainerInfo containerInfo = result.get(0);
    assertThat(containerInfo.getPodName()).isEqualTo("name");
    assertThat(containerInfo.getIp()).isEqualTo("pod-ip");
    assertThat(containerInfo.getReleaseName()).isEqualTo("release");
    assertThat(containerInfo.isNewContainer()).isTrue();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetReleaseHistoryFromConfigMapUsingFabric8() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    when(mockKubernetesContainerService.fetchReleaseHistoryFromConfigMapFabric8(any(), any())).thenReturn("secret");
    String releaseHistory = k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(kubernetesConfig, "release", false);
    ArgumentCaptor<String> releaseArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService)
        .fetchReleaseHistoryFromConfigMapFabric8(any(), releaseArgumentCaptor.capture());
    verify(mockKubernetesContainerService, times(0)).fetchReleaseHistoryFromConfigMap(any(), any());

    assertThat(releaseArgumentCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistory).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetReleaseHistoryFromConfigMapUsingK8sClient() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    when(mockKubernetesContainerService.fetchReleaseHistoryFromConfigMap(any(), any())).thenReturn("secret");
    String releaseHistory = k8sTaskHelperBase.getReleaseHistoryDataFromConfigMap(kubernetesConfig, "release", true);
    ArgumentCaptor<String> releaseArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService).fetchReleaseHistoryFromConfigMap(any(), releaseArgumentCaptor.capture());
    verify(mockKubernetesContainerService, times(0)).fetchReleaseHistoryFromConfigMapFabric8(any(), any());

    assertThat(releaseArgumentCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistory).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldSaveReleaseHistoryInConfigMapUsingFabric8() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    doNothing().when(mockKubernetesContainerService).saveReleaseHistoryInConfigMapFabric8(any(), any(), anyString());
    k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, "release", "secret", false);
    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> releaseHistoryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService)
        .saveReleaseHistoryInConfigMapFabric8(any(), releaseNameCaptor.capture(), releaseHistoryCaptor.capture());
    verify(mockKubernetesContainerService, times(0)).saveReleaseHistoryInConfigMap(any(), any(), anyString());

    assertThat(releaseNameCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistoryCaptor.getValue()).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldSaveReleaseHistoryInConfigMapUsingK8sClient() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    when(mockKubernetesContainerService.saveReleaseHistoryInConfigMap(any(), any(), anyString())).thenReturn(null);
    k8sTaskHelperBase.saveReleaseHistoryInConfigMap(kubernetesConfig, "release", "secret", true);
    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> releaseHistoryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService)
        .saveReleaseHistoryInConfigMap(any(), releaseNameCaptor.capture(), releaseHistoryCaptor.capture());
    verify(mockKubernetesContainerService, times(0)).saveReleaseHistoryInConfigMapFabric8(any(), any(), anyString());

    assertThat(releaseNameCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistoryCaptor.getValue()).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldSaveReleaseHistoryUsingFabric8() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    doNothing().when(mockKubernetesContainerService).saveReleaseHistoryFabric8(any(), any(), anyString(), anyBoolean());
    k8sTaskHelperBase.saveReleaseHistory(kubernetesConfig, "release", "secret", true, false);
    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> releaseHistoryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService)
        .saveReleaseHistoryFabric8(any(), releaseNameCaptor.capture(), releaseHistoryCaptor.capture(), anyBoolean());
    verify(mockKubernetesContainerService, times(0)).saveReleaseHistory(any(), any(), anyString(), anyBoolean());

    assertThat(releaseNameCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistoryCaptor.getValue()).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldSaveReleaseHistoryUsingK8sClient() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    doNothing().when(mockKubernetesContainerService).saveReleaseHistory(any(), any(), anyString(), anyBoolean());
    k8sTaskHelperBase.saveReleaseHistory(kubernetesConfig, "release", "secret", true, true);
    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> releaseHistoryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService)
        .saveReleaseHistory(any(), releaseNameCaptor.capture(), releaseHistoryCaptor.capture(), anyBoolean());
    verify(mockKubernetesContainerService, times(0)).saveReleaseHistoryFabric8(any(), any(), anyString(), anyBoolean());

    assertThat(releaseNameCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistoryCaptor.getValue()).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetReleaseHistoryFromSecretUsingFabric8() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    when(mockKubernetesContainerService.fetchReleaseHistoryFromSecretsFabric8(any(), any())).thenReturn("secret");
    String releaseHistory = k8sTaskHelperBase.getReleaseHistoryFromSecret(kubernetesConfig, "release", false);
    ArgumentCaptor<String> releaseArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService)
        .fetchReleaseHistoryFromSecretsFabric8(any(), releaseArgumentCaptor.capture());
    verify(mockKubernetesContainerService, times(0)).fetchReleaseHistoryFromSecrets(any(), any());

    assertThat(releaseArgumentCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistory).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetReleaseHistoryFromSecretUsingK8sClient() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    when(mockKubernetesContainerService.fetchReleaseHistoryFromSecrets(any(), any())).thenReturn("secret");
    String releaseHistory = k8sTaskHelperBase.getReleaseHistoryFromSecret(kubernetesConfig, "release", true);
    ArgumentCaptor<String> releaseArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockKubernetesContainerService).fetchReleaseHistoryFromSecrets(any(), releaseArgumentCaptor.capture());
    verify(mockKubernetesContainerService, times(0)).fetchReleaseHistoryFromSecretsFabric8(any(), any());

    assertThat(releaseArgumentCaptor.getValue()).isEqualTo("release");
    assertThat(releaseHistory).isEqualTo("secret");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyWithCustomChangeCauseAnnotation() throws Exception {
    String deploymentSpec = "apiVersion: apps/v1\n"
        + "kind: Deployment\n"
        + "metadata:\n"
        + "  name: deployment\n"
        + "  annotations:\n"
        + "    ${annotation}\n"
        + "spec:\n"
        + "  template:\n"
        + "    metadata:\n"
        + "      name: deployment\n";
    KubernetesResource resourceWithCustomChangeCause = ManifestHelper.getKubernetesResourceFromSpec(
        deploymentSpec.replace("${annotation}", "kubernetes.io/change-cause: custom value"));
    KubernetesResource resourceWithoutCustomChangeCause =
        ManifestHelper.getKubernetesResourceFromSpec(deploymentSpec.replace("${annotation}", ""));

    testApplyAndCheckRecord(resourceWithCustomChangeCause, false);
    testApplyAndCheckRecord(resourceWithoutCustomChangeCause, true);
  }

  private void testApplyAndCheckRecord(KubernetesResource resource, boolean expectedRecord) throws Exception {
    K8sTaskHelperBase spyK8sTaskHelper = spy(k8sTaskHelperBase);
    K8sDelegateTaskParams k8sDelegateTaskParams = K8sDelegateTaskParams.builder()
                                                      .workingDirectory(".")
                                                      .kubectlPath("kubectl")
                                                      .kubeconfigPath("config-path")
                                                      .build();
    Kubectl client = Kubectl.client("kubectl", "config-path");

    spyK8sTaskHelper.applyManifests(client, singletonList(resource), k8sDelegateTaskParams, executionLogCallback, true);
    ArgumentCaptor<ApplyCommand> captor = ArgumentCaptor.forClass(ApplyCommand.class);
    verify(spyK8sTaskHelper, times(1)).runK8sExecutable(any(), any(), captor.capture());

    String expectedExecutedCommand = "kubectl --kubeconfig=config-path apply --filename=manifests.yaml";
    if (expectedRecord) {
      expectedExecutedCommand += " --record";
    }

    assertThat(captor.getValue().command()).isEqualTo(expectedExecutedCommand);
  }
}
