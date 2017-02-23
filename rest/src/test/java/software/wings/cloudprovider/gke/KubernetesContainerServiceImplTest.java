package software.wings.cloudprovider.gke;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.ReplicationController;
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
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.KubernetesHelperService;

import javax.inject.Inject;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.KubernetesConfig.Builder.aKubernetesConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.PASSWORD;

/**
 * Created by brett on 2/10/17.
 */
public class KubernetesContainerServiceImplTest extends WingsBaseTest {
  public static final String API_SERVER_URL = "apiServerUrl";
  public static final String USERNAME = "username";

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

  @Inject @InjectMocks private KubernetesContainerService kubernetesContainerService;

  private SettingAttribute connectorConfig = aSettingAttribute()
                                                 .withValue(aKubernetesConfig()
                                                                .withApiServerUrl(API_SERVER_URL)
                                                                .withUsername(USERNAME)
                                                                .withPassword(PASSWORD)
                                                                .build())
                                                 .build();

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getKubernetesClient(connectorConfig)).thenReturn(kubernetesClient);
    when(kubernetesClient.replicationControllers()).thenReturn(replicationControllers);
    when(replicationControllers.inNamespace("default")).thenReturn(defaultNamespace);
    when(kubernetesClient.services()).thenReturn(services);
    when(services.createOrReplaceWithNew()).thenReturn(new DoneableService(item -> item));
    when(replicationControllers.withName(anyString())).thenReturn(scalableReplicationController);
  }

  @Test
  public void shouldCreateController() {
    kubernetesContainerService.createController(connectorConfig,
        ImmutableMap.of("name", "ctrl", "appName", "unit-test", "tier", "backend", "count", "4", "port", "9999"));

    ArgumentCaptor<ReplicationController> args = ArgumentCaptor.forClass(ReplicationController.class);
    verify(defaultNamespace).createOrReplace(args.capture());
    ReplicationController rc = args.getValue();
    assertThat(rc.getMetadata().getName()).isEqualTo("ctrl");
    assertThat(rc.getMetadata().getLabels().get("app")).isEqualTo("unit-test");
    assertThat(rc.getMetadata().getLabels().get("tier")).isEqualTo("backend");
    assertThat(rc.getSpec().getReplicas()).isEqualTo(4);
    assertThat(rc.getSpec().getTemplate().getMetadata().getLabels().get("app")).isEqualTo("unit-test");
    assertThat(rc.getSpec().getTemplate().getMetadata().getLabels().get("tier")).isEqualTo("backend");
    assertThat(rc.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts().get(0).getContainerPort())
        .isEqualTo(9999);
  }

  @Test
  public void shouldCreateService() {
    Service service = kubernetesContainerService.createService(connectorConfig,
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
  public void shouldSetControllerPodCount() {
    kubernetesContainerService.setControllerPodCount(connectorConfig, "foo", 10);

    ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
    verify(scalableReplicationController).scale(args.capture());
    assertThat(args.getValue()).isEqualTo(10);
  }
}
