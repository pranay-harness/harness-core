package software.wings.cloudprovider.gke;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.PASSWORD;

import com.google.common.util.concurrent.TimeLimiter;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.api.model.DoneableHorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.DoneableNamespace;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetList;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.DoneableDaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DoneableDeployment;
import io.fabric8.kubernetes.api.model.extensions.DoneableStatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetList;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetSpec;
import io.fabric8.kubernetes.client.AppsAPIGroupClient;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
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
      RollableScalableResource<ReplicationController, DoneableReplicationController>> namespacedReplicationControllers;

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

  // Deployments
  private MixedOperation<Deployment, DeploymentList, DoneableDeployment,
      ScalableResource<Deployment, DoneableDeployment>> deploymentOperations;
  @Mock
  private NonNamespaceOperation<Deployment, DeploymentList, DoneableDeployment,
      ScalableResource<Deployment, DoneableDeployment>> namespacedDeployments;
  @Mock private ScalableResource<Deployment, DoneableDeployment> scalableDeployment;
  @Mock
  private FilterWatchListDeletable<Deployment, DeploymentList, Boolean, Watch, Watcher<Deployment>>
      deploymentFilteredList;

  // Statefulsets
  @Mock
  private MixedOperation<StatefulSet, StatefulSetList, DoneableStatefulSet,
      RollableScalableResource<StatefulSet, DoneableStatefulSet>> statefulSetOperations;
  @Mock
  private NonNamespaceOperation<StatefulSet, StatefulSetList, DoneableStatefulSet,
      RollableScalableResource<StatefulSet, DoneableStatefulSet>> namespacedStatefulsets;
  @Mock private RollableScalableResource<StatefulSet, DoneableStatefulSet> statefulSetResource;

  // DaemonSet
  @Mock
  private MixedOperation<DaemonSet, DaemonSetList, DoneableDaemonSet, Resource<DaemonSet, DoneableDaemonSet>>
      daemonSetOperations;
  @Mock
  private NonNamespaceOperation<DaemonSet, DaemonSetList, DoneableDaemonSet, Resource<DaemonSet, DoneableDaemonSet>>
      namespacedDaemonSet;
  @Mock private Resource<DaemonSet, DoneableDaemonSet> daemonSetResource;

  // Namespaces
  @Mock
  private NonNamespaceOperation<Namespace, NamespaceList, DoneableNamespace, Resource<Namespace, DoneableNamespace>>
      namespacedNamespaces;
  @Mock private Resource<Namespace, DoneableNamespace> namespaceResource;

  // Secrets
  @Mock private MixedOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> secretOperations;
  @Mock
  private NonNamespaceOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> namespacedSecrets;
  @Mock private Resource<Secret, DoneableSecret> secretResource;

  // ConfigMaps
  @Mock
  private MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>
      configMapOperations;
  @Mock
  private NonNamespaceOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>
      namespacedConfigMaps;
  @Mock private Resource<ConfigMap, DoneableConfigMap> configMapResource;

  // HPA
  @Mock
  private NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
      Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>> namespacedHpa;
  @Mock private Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler> horizontalPodAutoscalerResource;

  @Mock private ExtensionsAPIGroupClient extensionsAPIGroupClient;
  @Mock private AppsAPIGroupClient appsAPIGroupClient;

  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private TimeLimiter timeLimiter;
  @Mock private Clock clock;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;

  @InjectMocks private KubernetesContainerServiceImpl kubernetesContainerService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  ReplicationController replicationController;
  ReplicationControllerSpec spec;

  Deployment deployment;
  DeploymentSpec deploymentSpec;

  StatefulSet statefulSet;
  StatefulSetSpec statefulSetSpec;

  DaemonSet daemonSet;
  DaemonSetSpec daemonSetSpec;

  Namespace namespace;

  Secret secret;
  ConfigMap configMap;
  Service service;
  HorizontalPodAutoscaler horizontalPodAutoscaler;

  @Before
  public void setUp() throws Exception {
    when(kubernetesHelperService.getKubernetesClient(KUBERNETES_CONFIG)).thenReturn(kubernetesClient);
    when(kubernetesHelperService.getOpenShiftClient(KUBERNETES_CONFIG)).thenReturn(openShiftClient);

    when(kubernetesClient.services()).thenReturn(services);
    when(kubernetesClient.extensions()).thenReturn(extensionsAPIGroupClient);
    when(kubernetesClient.apps()).thenReturn(appsAPIGroupClient);
    when(kubernetesClient.namespaces()).thenReturn(namespacedNamespaces);

    when(kubernetesClient.replicationControllers()).thenReturn(replicationControllers);
    when(replicationControllers.inNamespace("default")).thenReturn(namespacedReplicationControllers);
    when(services.inNamespace(anyString())).thenReturn(namespacedServices);
    when(namespacedServices.createOrReplaceWithNew()).thenReturn(new DoneableService(item -> item));
    when(namespacedReplicationControllers.withName(anyString())).thenReturn(scalableReplicationController);
    when(namespacedServices.withName(anyString())).thenReturn(serviceResource);

    when(extensionsAPIGroupClient.deployments()).thenReturn(deploymentOperations);
    when(deploymentOperations.inNamespace("default")).thenReturn(namespacedDeployments);
    when(namespacedDeployments.withName(anyString())).thenReturn(scalableDeployment);
    when(namespacedDeployments.withLabels(any(Map.class))).thenReturn(deploymentFilteredList);
    when(deploymentFilteredList.list()).thenReturn(new DeploymentList());

    when(appsAPIGroupClient.statefulSets()).thenReturn(statefulSetOperations);
    when(statefulSetOperations.inNamespace("default")).thenReturn(namespacedStatefulsets);
    when(namespacedStatefulsets.withName(anyString())).thenReturn(statefulSetResource);

    when(extensionsAPIGroupClient.daemonSets()).thenReturn(daemonSetOperations);
    when(daemonSetOperations.inNamespace("default")).thenReturn(namespacedDaemonSet);
    when(namespacedDaemonSet.withName(anyString())).thenReturn(daemonSetResource);

    when(kubernetesClient.namespaces()).thenReturn(namespacedNamespaces);
    when(namespacedNamespaces.withName(anyString())).thenReturn(namespaceResource);

    when(kubernetesClient.secrets()).thenReturn(secretOperations);
    when(secretOperations.inNamespace(anyString())).thenReturn(namespacedSecrets);
    when(namespacedSecrets.withName(anyString())).thenReturn(secretResource);

    when(kubernetesClient.configMaps()).thenReturn(configMapOperations);
    when(configMapOperations.inNamespace(anyString())).thenReturn(namespacedConfigMaps);
    when(namespacedConfigMaps.withName(anyString())).thenReturn(configMapResource);

    when(kubernetesHelperService.hpaOperations(KUBERNETES_CONFIG)).thenReturn(namespacedHpa);
    when(namespacedHpa.withName(anyString())).thenReturn(horizontalPodAutoscalerResource);

    replicationController = new ReplicationController();
    spec = new ReplicationControllerSpec();
    spec.setReplicas(8);
    replicationController.setSpec(spec);
    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean()))
        .thenReturn(replicationController);

    deployment = new Deployment();
    deploymentSpec = new DeploymentSpec();
    deploymentSpec.setReplicas(2);
    deployment.setSpec(deploymentSpec);

    statefulSet = new StatefulSet();
    statefulSetSpec = new StatefulSetSpec();
    statefulSet.setSpec(statefulSetSpec);

    daemonSet = new DaemonSet();
    daemonSetSpec = new DaemonSetSpec();
    daemonSet.setSpec(daemonSetSpec);

    namespace = new Namespace();
    service = new Service();
    configMap = new ConfigMap();
    horizontalPodAutoscaler = new HorizontalPodAutoscaler();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDeleteController() throws Exception {
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedReplicationControllers).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("ctrl");
    verify(scalableReplicationController).delete();

    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean())).thenReturn(deployment);
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");
    verify(scalableDeployment).delete();

    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean())).thenReturn(statefulSet);
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");
    verify(statefulSetResource).delete();

    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean())).thenReturn(daemonSet);
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");
    verify(daemonSetResource).delete();
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

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCreateOrReplaceController() throws Exception {
    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName("controller");
    replicationController.setMetadata(objectMeta);
    kubernetesContainerService.createOrReplaceController(KUBERNETES_CONFIG, replicationController);
    verify(namespacedReplicationControllers).createOrReplace(replicationController);

    deployment.setMetadata(objectMeta);
    kubernetesContainerService.createOrReplaceController(KUBERNETES_CONFIG, deployment);
    verify(namespacedDeployments).createOrReplace(deployment);

    statefulSet.setMetadata(objectMeta);
    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean())).thenReturn(statefulSet);
    kubernetesContainerService.createOrReplaceController(KUBERNETES_CONFIG, statefulSet);
    verify(namespacedStatefulsets.withName(anyString())).patch(statefulSet);

    when(timeLimiter.callWithTimeout(any(), anyLong(), isA(TimeUnit.class), anyBoolean())).thenReturn(null);
    kubernetesContainerService.createOrReplaceController(KUBERNETES_CONFIG, statefulSet);
    verify(namespacedStatefulsets).create(statefulSet);

    daemonSet.setMetadata(objectMeta);
    kubernetesContainerService.createOrReplaceController(KUBERNETES_CONFIG, daemonSet);
    verify(namespacedDaemonSet).createOrReplace(daemonSet);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCreateNamespaceIfNotExist() {
    when(namespaceResource.get()).thenReturn(null);
    kubernetesContainerService.createNamespaceIfNotExist(KUBERNETES_CONFIG);
    verify(namespacedNamespaces).create(any(Namespace.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSecrets() {
    Secret returnedSecret = kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "");
    assertThat(returnedSecret).isNull();

    when(secretResource.get()).thenReturn(secret);
    kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "secret");
    verify(secretResource).get();

    kubernetesContainerService.deleteSecret(KUBERNETES_CONFIG, "secret");
    verify(secretResource).delete();

    kubernetesContainerService.createOrReplaceSecret(KUBERNETES_CONFIG, secret);
    verify(namespacedSecrets).createOrReplace(secret);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testConfigMap() {
    when(configMapResource.get()).thenReturn(configMap);
    kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "cm");
    verify(configMapResource).get();

    kubernetesContainerService.deleteConfigMap(KUBERNETES_CONFIG, "cm-delete");
    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedConfigMaps, times(2)).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("cm-delete");
    verify(configMapResource).delete();

    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName("cm");
    replicationController.setMetadata(objectMeta);
    configMap.setMetadata(objectMeta);
    kubernetesContainerService.createOrReplaceConfigMap(KUBERNETES_CONFIG, configMap);
    verify(namespacedConfigMaps).createOrReplace(configMap);

    when(kubernetesClient.configMaps()).thenThrow(new InvalidRequestException("test"));
    ConfigMap returnedConfigMap = kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "");
    assertThat(returnedConfigMap).isNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetService() {
    kubernetesContainerService.getService(KUBERNETES_CONFIG, "service");

    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedServices).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("service");
    verify(serviceResource).get();

    kubernetesContainerService.getService(KUBERNETES_CONFIG, "service", "testNamespace");
    verify(services, times(2)).inNamespace(args.capture());
    assertThat(args.getValue()).isEqualTo("testNamespace");
    verify(serviceResource, times(2)).get();

    kubernetesContainerService.createOrReplaceService(KUBERNETES_CONFIG, service);
    verify(namespacedServices).createOrReplace(service);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetAutoscaler() {
    when(horizontalPodAutoscalerResource.get()).thenReturn(horizontalPodAutoscaler);
    HorizontalPodAutoscaler autoscaler =
        kubernetesContainerService.getAutoscaler(KUBERNETES_CONFIG, "autoscalar", "v1");
    assertThat(autoscaler).isEqualTo(horizontalPodAutoscaler);
    verify(horizontalPodAutoscalerResource).get();
    verify(kubernetesHelperService).hpaOperations(KUBERNETES_CONFIG);
    verify(kubernetesHelperService, never()).hpaOperationsForCustomMetricHPA(KUBERNETES_CONFIG, "v1alpha1");

    when(kubernetesHelperService.hpaOperationsForCustomMetricHPA(KUBERNETES_CONFIG, "v1alpha1"))
        .thenReturn(namespacedHpa);
    autoscaler = kubernetesContainerService.getAutoscaler(KUBERNETES_CONFIG, "autoscalar", "v1alpha1");
    assertThat(autoscaler).isEqualTo(horizontalPodAutoscaler);
    verify(kubernetesHelperService).hpaOperations(KUBERNETES_CONFIG);
    verify(kubernetesHelperService).hpaOperationsForCustomMetricHPA(KUBERNETES_CONFIG, "v1alpha1");
    verify(horizontalPodAutoscalerResource, times(2)).get();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCreateOrReplaceAutoscaler() {
    String autoscalerYaml = "apiVersion: autoscaling/v2beta1\n"
        + "kind: HorizontalPodAutoscaler\n"
        + "metadata:\n"
        + "  name: hpa-name\n"
        + "spec:\n"
        + "  minReplicas: 2\n";

    when(kubernetesHelperService.trimVersion("autoscaling/v2beta1")).thenReturn("v2beta1");
    when(kubernetesHelperService.hpaOperationsForCustomMetricHPA(KUBERNETES_CONFIG, "v2beta1"))
        .thenReturn(namespacedHpa);
    kubernetesContainerService.createOrReplaceAutoscaler(KUBERNETES_CONFIG, autoscalerYaml);
    verify(kubernetesHelperService).hpaOperationsForCustomMetricHPA(KUBERNETES_CONFIG, "v2beta1");
    verify(namespacedHpa).createOrReplace(any(HorizontalPodAutoscaler.class));

    autoscalerYaml = "apiVersion: autoscaling/v1\n"
        + "kind: HorizontalPodAutoscaler\n"
        + "metadata:\n"
        + "  name: hpa-name\n"
        + "spec:\n"
        + "  minReplicas: 2\n";

    when(kubernetesHelperService.trimVersion("autoscaling/v1")).thenReturn("v1");
    when(kubernetesHelperService.hpaOperations(KUBERNETES_CONFIG)).thenReturn(namespacedHpa);
    kubernetesContainerService.createOrReplaceAutoscaler(KUBERNETES_CONFIG, autoscalerYaml);
    verify(kubernetesHelperService).hpaOperations(KUBERNETES_CONFIG);
    verify(namespacedHpa, times(2)).createOrReplace(any(HorizontalPodAutoscaler.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDeleteAutoscaler() {
    kubernetesContainerService.deleteAutoscaler(KUBERNETES_CONFIG, "hpa");
    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedHpa).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("hpa");
    verify(horizontalPodAutoscalerResource).delete();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetActiveServiceCounts() {
    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName("controller");
    deployment.setMetadata(objectMeta);

    DeploymentList deploymentList = new DeploymentList();
    deploymentList.setItems(asList(deployment));

    when(namespacedDeployments.list()).thenReturn(deploymentList);
    LinkedHashMap<String, Integer> activeServiceCounts =
        kubernetesContainerService.getActiveServiceCounts(KUBERNETES_CONFIG, "controller-1");
    assertThat(activeServiceCounts.get("controller")).isEqualTo(2);

    activeServiceCounts = kubernetesContainerService.getActiveServiceCounts(KUBERNETES_CONFIG, "ctlr-2");
    assertThat(activeServiceCounts).isEmpty();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetActiveServiceCountsWithLabels() {
    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName("controller");
    deployment.setMetadata(objectMeta);
    DeploymentList deploymentList = new DeploymentList();
    deploymentList.setItems(asList(deployment));

    when(deploymentFilteredList.list()).thenReturn(deploymentList);
    LinkedHashMap<String, Integer> activeServiceCounts =
        kubernetesContainerService.getActiveServiceCountsWithLabels(KUBERNETES_CONFIG, emptyMap());
    assertThat(activeServiceCounts.get("controller")).isEqualTo(2);

    when(deploymentFilteredList.list()).thenReturn(new DeploymentList());
    activeServiceCounts = kubernetesContainerService.getActiveServiceCountsWithLabels(KUBERNETES_CONFIG, emptyMap());
    assertThat(activeServiceCounts).isEmpty();
  }
}
