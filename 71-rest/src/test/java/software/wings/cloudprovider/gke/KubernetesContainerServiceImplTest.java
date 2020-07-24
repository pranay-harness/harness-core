package software.wings.cloudprovider.gke;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.PASSWORD;

import com.google.common.util.concurrent.TimeLimiter;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import io.fabric8.kubernetes.api.model.extensions.DoneableDeployment;
import io.fabric8.kubernetes.client.ExtensionsAPIGroupClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.DoneableDeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.DeployableScalableResource;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by brett on 2/10/17.
 */
public class KubernetesContainerServiceImplTest extends CategoryTest {
  public static final String MASTER_URL = "masterUrl";
  public static final String USERNAME = "username";

  private static final KubernetesConfig KUBERNETES_CONFIG = KubernetesConfig.builder()
                                                                .masterUrl(MASTER_URL)
                                                                .username(USERNAME)
                                                                .password(PASSWORD)
                                                                .namespace("default")
                                                                .build();

  @Mock private KubernetesClient kubernetesClient;
  @Mock private OpenShiftClient openShiftClient;
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
  @Mock private Resource<Service, DoneableService> serviceResource;
  @Mock private MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> pods;

  @Mock
  private MixedOperation<DeploymentConfig, DeploymentConfigList, DoneableDeploymentConfig,
      DeployableScalableResource<DeploymentConfig, DoneableDeploymentConfig>> deploymentConfigsOperation;
  @Mock
  private NonNamespaceOperation<DeploymentConfig, DeploymentConfigList, DoneableDeploymentConfig,
      DeployableScalableResource<DeploymentConfig, DoneableDeploymentConfig>> deploymentConfigs;
  @Mock
  private MixedOperation<Deployment, DeploymentList, DoneableDeployment,
      ScalableResource<Deployment, DoneableDeployment>> deploymentOperations;
  @Mock
  private NonNamespaceOperation<Deployment, DeploymentList, DoneableDeployment,
      ScalableResource<Deployment, DoneableDeployment>> deployments;
  @Mock
  private FilterWatchListDeletable<Deployment, DeploymentList, Boolean, Watch, Watcher<Deployment>>
      deploymentFilteredList;

  @Mock private ExtensionsAPIGroupClient extensionsAPIGroupClient;

  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private TimeLimiter timeLimiter;
  @Mock private Clock clock;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;

  @InjectMocks private KubernetesContainerServiceImpl kubernetesContainerService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  ReplicationController replicationController;
  ReplicationControllerSpec spec;

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG, Collections.emptyList()))
        .thenReturn(kubernetesClient);
    when(kubernetesHelperService.getOpenShiftClient(KUBERNETES_CONFIG, Collections.emptyList()))
        .thenReturn(openShiftClient);

    when(kubernetesClient.services()).thenReturn(services);
    when(kubernetesClient.extensions()).thenReturn(extensionsAPIGroupClient);
    when(kubernetesClient.replicationControllers()).thenReturn(replicationControllers);
    when(replicationControllers.inNamespace("default")).thenReturn(namespacedControllers);
    when(services.inNamespace("default")).thenReturn(namespacedServices);
    when(namespacedServices.createOrReplaceWithNew()).thenReturn(new DoneableService(item -> item));
    when(namespacedControllers.withName(anyString())).thenReturn(scalableReplicationController);
    when(namespacedServices.withName(anyString())).thenReturn(serviceResource);
    when(extensionsAPIGroupClient.deployments()).thenReturn(deploymentOperations);
    when(deploymentOperations.inNamespace("default")).thenReturn(deployments);
    when(deployments.withLabels(any(Map.class))).thenReturn(deploymentFilteredList);
    when(deploymentFilteredList.list()).thenReturn(new DeploymentList());

    replicationController = new ReplicationController();
    spec = new ReplicationControllerSpec();
    spec.setReplicas(8);
    replicationController.setSpec(spec);
    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean()))
        .thenReturn(replicationController);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDeleteController() {
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedControllers).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("ctrl");
    verify(scalableReplicationController).delete();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDeleteService() {
    kubernetesContainerService.deleteService(KUBERNETES_CONFIG, "service");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedServices).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("service");
    verify(serviceResource).delete();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldSetControllerPodCount() {
    List<ContainerInfo> containerInfos = kubernetesContainerService.setControllerPodCount(
        KUBERNETES_CONFIG, "foo", "bar", 0, 3, 10, new ExecutionLogCallback());

    ArgumentCaptor<Integer> args = ArgumentCaptor.forClass(Integer.class);
    verify(scalableReplicationController).scale(args.capture());
    assertThat(args.getValue()).isEqualTo(3);

    assertThat(containerInfos.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetControllerPodCount() throws Exception {
    Optional<Integer> count = kubernetesContainerService.getControllerPodCount(KUBERNETES_CONFIG, "foo");

    assertThat(count.isPresent()).isTrue();
    assertThat(count.get()).isEqualTo(8);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetControllerPodCountUnhandledResource() {
    Service service = new Service();
    try {
      kubernetesContainerService.getControllerPodCount(service);
      fail("Should not reach here.");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Unhandled kubernetes resource type [Service] for getting the pod count");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNPEInGetContainerInfosWhenReady() throws Exception {
    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean())).thenReturn(null);

    try {
      kubernetesContainerService.getContainerInfosWhenReady(
          KUBERNETES_CONFIG, "controllerName", 0, 0, 0, asList(), false, null, false, 0L, "default");
      fail("Should not reach here.");
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage()).isEqualTo("Could not find a controller named controllerName");
    } catch (Exception ex) {
      fail("Should not reach here.");
    }

    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean()))
        .thenReturn(replicationController);
    kubernetesContainerService.getContainerInfosWhenReady(
        KUBERNETES_CONFIG, "controllerName", 0, 0, 0, asList(), false, null, false, 0L, "default");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetPodTemplateSpec() {
    testGetPodSpecForController();
  }

  private void testGetPodSpecForController() {
    DeploymentConfig controller = new DeploymentConfig();
    DeploymentConfigSpec spec = new DeploymentConfigSpec();
    spec.setTemplate(new PodTemplateSpec());
    controller.setSpec(spec);

    PodTemplateSpec podTemplateSpec = kubernetesContainerService.getPodTemplateSpec(controller);
    assertThat(podTemplateSpec).isNotNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetControllers() {
    testGetDeploymentConfig();
  }

  private void testGetDeploymentConfig() {
    Map<String, String> labels = new HashMap<>();
    when(openShiftClient.deploymentConfigs()).thenReturn(deploymentConfigsOperation);
    when(deploymentConfigsOperation.inNamespace("default")).thenReturn(deploymentConfigs);

    kubernetesContainerService.getControllers(KUBERNETES_CONFIG, labels);

    verify(deploymentConfigs, times(1)).withLabels(labels);
  }
}
