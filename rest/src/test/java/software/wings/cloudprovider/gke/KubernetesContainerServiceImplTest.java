package software.wings.cloudprovider.gke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.PASSWORD;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.service.impl.KubernetesHelperService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by brett on 2/10/17.
 */
public class KubernetesContainerServiceImplTest extends WingsBaseTest {
  public static final String MASTER_URL = "masterUrl";
  public static final String USERNAME = "username";

  private static final KubernetesConfig KUBERNETES_CONFIG = KubernetesConfig.builder()
                                                                .masterUrl(MASTER_URL)
                                                                .username(USERNAME)
                                                                .password(PASSWORD)
                                                                .namespace("default")
                                                                .build();

  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private KubernetesClient kubernetesClient;
  @Mock
  private MixedOperation<ReplicationController, ReplicationControllerList, DoneableReplicationController,
      RollableScalableResource<ReplicationController, DoneableReplicationController>> replicationControllers;
  @Mock
  private NonNamespaceOperation<ReplicationController, ReplicationControllerList, DoneableReplicationController,
      RollableScalableResource<ReplicationController, DoneableReplicationController>> namespacedControllers;
  @Mock private MixedOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>> services;
  @Mock
  private NonNamespaceOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>>
      namespacedServices;
  @Mock
  private RollableScalableResource<ReplicationController, DoneableReplicationController> scalableReplicationController;
  @Mock private ReplicationController replicationController;
  @Mock private Resource<Service, DoneableService> serviceResource;
  @Mock private ObjectMeta replicationControllerMetadata;
  @Mock private MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> pods;
  @Mock private NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> namespacedPods;
  @Mock private FilterWatchListDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> podsWithLabels;
  @Mock private PodList podList;
  @Mock private Pod pod;
  @Mock private PodStatus podStatus;
  @Mock private ObjectMeta podMetadata;
  @Mock private ContainerStatus containerStatus;
  @Mock private ReplicationControllerSpec replicationControllerSpec;

  @Inject @InjectMocks private KubernetesContainerService kubernetesContainerService;

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG, Collections.emptyList()))
        .thenReturn(kubernetesClient);
    when(kubernetesClient.replicationControllers()).thenReturn(replicationControllers);
    when(replicationControllers.inNamespace("default")).thenReturn(namespacedControllers);
    when(kubernetesClient.services()).thenReturn(services);
    when(services.inNamespace("default")).thenReturn(namespacedServices);
    when(namespacedServices.createOrReplaceWithNew()).thenReturn(new DoneableService(item -> item));
    when(namespacedControllers.withName(anyString())).thenReturn(scalableReplicationController);
    when(namespacedServices.withName(anyString())).thenReturn(serviceResource);
    when(scalableReplicationController.get()).thenReturn(replicationController);
    when(replicationController.getMetadata()).thenReturn(replicationControllerMetadata);
    when(replicationController.getSpec()).thenReturn(replicationControllerSpec);
    when(replicationControllerSpec.getTemplate()).thenReturn(null);
    when(replicationControllerMetadata.getLabels()).thenReturn(ImmutableMap.of("app", "appname"));
    when(kubernetesClient.pods()).thenReturn(pods);
    when(pods.inNamespace("default")).thenReturn(namespacedPods);
    when(namespacedPods.withLabels(anyMap())).thenReturn(podsWithLabels);
    when(podsWithLabels.list()).thenReturn(podList);
    when(podList.getItems()).thenReturn(ImmutableList.of(pod, pod, pod));
    when(pod.getStatus()).thenReturn(podStatus);
    when(podStatus.getPhase()).thenReturn("Running");
    when(pod.getMetadata()).thenReturn(podMetadata);
    when(podMetadata.getName()).thenReturn("pod-name");
    when(podStatus.getContainerStatuses()).thenReturn(ImmutableList.of(containerStatus));
    when(containerStatus.getContainerID()).thenReturn("docker://0123456789ABCDEF");
  }

  @Test
  public void shouldCreateBackendController() {}

  @Test
  public void shouldCreateFrontendController() {}

  @Test
  public void shouldDeleteController() {
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, Collections.emptyList(), "ctrl");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedControllers, times(2)).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("ctrl");
    verify(scalableReplicationController).delete();
  }

  @Test
  public void shouldCreateFrontendService() {}

  @Test
  public void shouldCreateBackendService() {}

  @Test
  public void shouldDeleteService() {
    kubernetesContainerService.deleteService(KUBERNETES_CONFIG, Collections.emptyList(), "service");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedServices).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("service");
    verify(serviceResource).delete();
  }

  @Test
  @Ignore
  public void shouldSetControllerPodCount() {
    List<ContainerInfo> containerInfos = kubernetesContainerService.setControllerPodCount(
        KUBERNETES_CONFIG, Collections.emptyList(), "foo", "bar", 0, 3, 10, new ExecutionLogCallback());

    ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
    verify(scalableReplicationController).scale(args.capture());
    assertThat(args.getValue()).isEqualTo(3);

    assertThat(containerInfos.size()).isEqualTo(3);
  }

  @Test
  public void shouldGetControllerPodCount() {
    when(scalableReplicationController.get())
        .thenReturn(new ReplicationControllerBuilder().withNewSpec().withReplicas(8).endSpec().build());

    Optional<Integer> count =
        kubernetesContainerService.getControllerPodCount(KUBERNETES_CONFIG, Collections.emptyList(), "foo");

    assertThat(count.isPresent()).isTrue();
    assertThat(count.get()).isEqualTo(8);
  }
}
