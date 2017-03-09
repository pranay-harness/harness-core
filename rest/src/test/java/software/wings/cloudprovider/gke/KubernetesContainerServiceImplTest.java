package software.wings.cloudprovider.gke;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.kubernetes.client.dsl.ClientNonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.kubernetes.client.dsl.ClientRollableScallableResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.service.impl.KubernetesHelperService;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.KubernetesConfig.Builder.aKubernetesConfig;
import static software.wings.utils.WingsTestConstants.PASSWORD;

/**
 * Created by brett on 2/10/17.
 */
public class KubernetesContainerServiceImplTest extends WingsBaseTest {
  public static final String MASTER_URL = "masterUrl";
  public static final String USERNAME = "username";

  private static final KubernetesConfig KUBERNETES_CONFIG =
      aKubernetesConfig().withMasterUrl(MASTER_URL).withUsername(USERNAME).withPassword(PASSWORD).build();

  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private KubernetesClient kubernetesClient;
  @Mock
  private ClientMixedOperation<ReplicationController, ReplicationControllerList, DoneableReplicationController,
      ClientRollableScallableResource<ReplicationController, DoneableReplicationController>> replicationControllers;
  @Mock
  private ClientNonNamespaceOperation<ReplicationController, ReplicationControllerList, DoneableReplicationController,
      ClientRollableScallableResource<ReplicationController, DoneableReplicationController>> defaultNamespace;
  @Mock
  private ClientMixedOperation<Service, ServiceList, DoneableService, ClientResource<Service, DoneableService>>
      services;
  @Mock
  private ClientRollableScallableResource<ReplicationController, DoneableReplicationController>
      scalableReplicationController;
  @Mock private ClientResource<Service, DoneableService> serviceResource;

  @Inject @InjectMocks private KubernetesContainerService kubernetesContainerService;

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG)).thenReturn(kubernetesClient);
    when(kubernetesClient.replicationControllers()).thenReturn(replicationControllers);
    when(replicationControllers.inNamespace("default")).thenReturn(defaultNamespace);
    when(kubernetesClient.services()).thenReturn(services);
    when(services.createOrReplaceWithNew()).thenReturn(new DoneableService(item -> item));
    when(replicationControllers.withName(anyString())).thenReturn(scalableReplicationController);
    when(services.withName(anyString())).thenReturn(serviceResource);
  }

  @Test
  public void shouldCreateBackendController() {}

  @Test
  public void shouldCreateFrontendController() {}

  @Test
  public void shouldDeleteController() {
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(replicationControllers).withName(args.capture());
    assertThat(args.getValue().equals("ctrl"));
    verify(scalableReplicationController).delete();
  }

  @Test
  public void shouldCreateFrontendService() {
    Service service = kubernetesContainerService.createService(KUBERNETES_CONFIG,
        ImmutableMap.<String, String>builder()
            .put("name", "srvc")
            .put("appName", "unit-test")
            .put("tier", "frontend")
            .put("type", "LoadBalancer")
            .put("port", "80")
            .put("targetPort", "8080")
            .build());

    assertThat(service.getMetadata().getName()).isEqualTo("srvc");
    assertThat(service.getMetadata().getLabels().get("app")).isEqualTo("unit-test");
    assertThat(service.getMetadata().getLabels().get("tier")).isEqualTo("frontend");
    assertThat(service.getSpec().getType()).isEqualTo("LoadBalancer");
    assertThat(service.getSpec().getPorts().get(0).getPort()).isEqualTo(80);
    assertThat(service.getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
    assertThat(service.getSpec().getSelector().get("app")).isEqualTo("unit-test");
    assertThat(service.getSpec().getSelector().get("tier")).isEqualTo("frontend");
  }

  @Test
  public void shouldCreateBackendService() {
    Service service = kubernetesContainerService.createService(KUBERNETES_CONFIG,
        ImmutableMap.<String, String>builder()
            .put("name", "srvc")
            .put("appName", "unit-test")
            .put("tier", "backend")
            .put("port", "80")
            .put("targetPort", "8080")
            .build());

    assertThat(service.getMetadata().getName()).isEqualTo("srvc");
    assertThat(service.getMetadata().getLabels().get("app")).isEqualTo("unit-test");
    assertThat(service.getMetadata().getLabels().get("tier")).isEqualTo("backend");
    assertThat(service.getSpec().getPorts().get(0).getPort()).isEqualTo(80);
    assertThat(service.getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
    assertThat(service.getSpec().getSelector().get("app")).isEqualTo("unit-test");
    assertThat(service.getSpec().getSelector().get("tier")).isEqualTo("backend");
  }

  @Test
  public void shouldDeleteService() {
    kubernetesContainerService.deleteService(KUBERNETES_CONFIG, "service");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(services).withName(args.capture());
    assertThat(args.getValue().equals("service"));
    verify(serviceResource).delete();
  }

  @Test
  public void shouldSetControllerPodCount() {
    kubernetesContainerService.setControllerPodCount(KUBERNETES_CONFIG, "foo", 10);

    ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
    verify(scalableReplicationController).scale(args.capture());
    assertThat(args.getValue()).isEqualTo(10);
  }

  @Test
  public void shouldGetControllerPodCount() {
    when(scalableReplicationController.get())
        .thenReturn(new ReplicationControllerBuilder().withNewSpec().withReplicas(8).endSpec().build());

    int count = kubernetesContainerService.getControllerPodCount(KUBERNETES_CONFIG, "foo");

    assertThat(count).isEqualTo(8);
  }
}
