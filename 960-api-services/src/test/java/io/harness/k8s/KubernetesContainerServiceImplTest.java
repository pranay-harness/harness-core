/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.k8s.KubernetesConvention.ReleaseHistoryKeyName;
import static io.harness.k8s.model.KubernetesClusterAuthType.OIDC;
import static io.harness.k8s.model.KubernetesClusterAuthType.USER_PASSWORD;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.YOGESH;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.concurent.HTimeLimiterMocker;
import io.harness.container.ContainerInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.OidcGrantType;
import io.harness.k8s.oidc.OidcTokenRetriever;
import io.harness.rule.Owner;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.gson.reflect.TypeToken;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
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
import io.fabric8.kubernetes.api.model.SecretBuilder;
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
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1ListMetaBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodListBuilder;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1PodStatusBuilder;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.openapi.models.V1StatusBuilder;
import io.kubernetes.client.openapi.models.VersionInfo;
import io.kubernetes.client.openapi.models.VersionInfoBuilder;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import okhttp3.Call;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Created by brett on 2/10/17.
 */
@OwnedBy(CDP)
public class KubernetesContainerServiceImplTest extends CategoryTest {
  public static final String MASTER_URL = "masterUrl";
  public static final char[] USERNAME = "username".toCharArray();
  public static final char[] PASSWORD = "PASSWORD".toCharArray();
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
  @Mock private MixedOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> secrets;
  @Mock
  private MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>
      configMaps;
  @Mock
  private NonNamespaceOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>>
      namespacedServices;

  @Mock
  private NonNamespaceOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> namespacedSecrets;
  @Mock
  private NonNamespaceOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>
      namespacedConfigMaps;

  @Mock
  private RollableScalableResource<ReplicationController, DoneableReplicationController> scalableReplicationController;
  @Mock private Resource<Service, DoneableService> serviceResource;
  @Mock private Resource<Secret, DoneableSecret> secretResource;
  @Mock private Resource<ConfigMap, DoneableConfigMap> configMapResource;
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

  // ConfigMaps
  @Mock
  private MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>
      configMapOperations;

  // HPA
  @Mock
  private NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
      Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>> namespacedHpa;
  @Mock private Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler> horizontalPodAutoscalerResource;

  @Mock private ExtensionsAPIGroupClient extensionsAPIGroupClient;
  @Mock private AppsAPIGroupClient appsAPIGroupClient;

  @Mock private ApiClient k8sApiClient;
  @Mock private Call k8sApiCall;

  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private TimeLimiter timeLimiter;
  @Mock private Clock clock;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;
  @Spy @InjectMocks private OidcTokenRetriever oidcTokenRetriever;

  @InjectMocks private KubernetesContainerServiceImpl kubernetesContainerService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  ReplicationController replicationController;
  ReplicationControllerSpec spec;
  Secret releaseHistory = new SecretBuilder()
                              .withNewMetadata()
                              .withName("test-rn")
                              .withNamespace("default")
                              .endMetadata()
                              .withData(ImmutableMap.of(ReleaseHistoryKeyName, encodeBase64("test")))
                              .build();

  ConfigMap releaseHistoryConfigMap = new ConfigMapBuilder()
                                          .withNewMetadata()
                                          .withName("test-rn")
                                          .withNamespace("default")
                                          .endMetadata()
                                          .withData(ImmutableMap.of(ReleaseHistoryKeyName, "test"))
                                          .build();

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
    when(kubernetesClient.secrets()).thenReturn(secrets);
    when(kubernetesClient.configMaps()).thenReturn(configMaps);
    when(services.inNamespace("default")).thenReturn(namespacedServices);
    when(secrets.inNamespace("default")).thenReturn(namespacedSecrets);
    when(configMaps.inNamespace("default")).thenReturn(namespacedConfigMaps);
    when(replicationControllers.inNamespace("default")).thenReturn(namespacedReplicationControllers);
    when(services.inNamespace(anyString())).thenReturn(namespacedServices);
    when(namespacedServices.createOrReplaceWithNew()).thenReturn(new DoneableService(item -> item));
    when(namespacedReplicationControllers.withName(anyString())).thenReturn(scalableReplicationController);
    when(namespacedServices.withName(anyString())).thenReturn(serviceResource);
    when(namespacedSecrets.withName(anyString())).thenReturn(secretResource);
    when(namespacedSecrets.createOrReplace(any(Secret.class))).thenReturn(releaseHistory);
    when(namespacedConfigMaps.withName(anyString())).thenReturn(configMapResource);
    when(namespacedConfigMaps.createOrReplace(any(ConfigMap.class))).thenReturn(releaseHistoryConfigMap);
    doReturn(releaseHistory).when(secretResource).get();
    doReturn(releaseHistoryConfigMap).when(configMapResource).get();

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

    when(kubernetesHelperService.getApiClient(KUBERNETES_CONFIG)).thenReturn(k8sApiClient);
    when(k8sApiClient.buildCall(anyString(), anyString(), anyListOf(Pair.class), anyListOf(Pair.class), any(),
             anyMapOf(String.class, String.class), anyMapOf(String.class, String.class),
             anyMapOf(String.class, Object.class), any(String[].class), any(ApiCallback.class)))
        .thenReturn(k8sApiCall);
    when(k8sApiClient.escapeString(anyString())).thenAnswer(invocation -> invocation.getArgumentAt(0, String.class));
    when(k8sApiClient.parameterToPair(anyString(), any())).thenCallRealMethod();
    when(k8sApiClient.parameterToString(any())).thenCallRealMethod();

    replicationController = new ReplicationController();
    spec = new ReplicationControllerSpec();
    spec.setReplicas(8);
    replicationController.setSpec(spec);
    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(replicationController);

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

    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(deployment);
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");
    verify(scalableDeployment).delete();

    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(statefulSet);
    kubernetesContainerService.deleteController(KUBERNETES_CONFIG, "ctrl");
    verify(statefulSetResource).delete();

    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(daemonSet);
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
    List<ContainerInfo> containerInfos =
        kubernetesContainerService.setControllerPodCount(KUBERNETES_CONFIG, "foo", "bar", 0, 3, 10, null);

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
    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(null);

    try {
      kubernetesContainerService.getContainerInfosWhenReady(
          KUBERNETES_CONFIG, "controllerName", 0, 0, 0, asList(), false, null, false, 0L, "default");
      fail("Should not reach here.");
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage()).isEqualTo("Could not find a controller named controllerName");
    } catch (Exception ex) {
      fail("Should not reach here.");
    }

    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(replicationController);
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
    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(statefulSet);
    kubernetesContainerService.createOrReplaceController(KUBERNETES_CONFIG, statefulSet);
    verify(namespacedStatefulsets.withName(anyString())).patch(statefulSet);

    HTimeLimiterMocker.mockCallInterruptible(timeLimiter).thenReturn(null);
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
    Secret returnedSecret = kubernetesContainerService.getSecretFabric8(KUBERNETES_CONFIG, "");
    assertThat(returnedSecret).isNull();

    when(secretResource.get()).thenReturn(secret);
    kubernetesContainerService.getSecretFabric8(KUBERNETES_CONFIG, "secret");
    verify(secretResource).get();

    kubernetesContainerService.deleteSecretFabric8(KUBERNETES_CONFIG, "secret");
    verify(secretResource).delete();

    kubernetesContainerService.createOrReplaceSecretFabric8(KUBERNETES_CONFIG, secret);
    verify(namespacedSecrets).createOrReplace(secret);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testConfigMap() {
    when(configMapResource.get()).thenReturn(configMap);
    kubernetesContainerService.getConfigMapFabric8(KUBERNETES_CONFIG, "cm");
    verify(configMapResource).get();

    kubernetesContainerService.deleteConfigMapFabric8(KUBERNETES_CONFIG, "cm-delete");
    ArgumentCaptor<String> args = ArgumentCaptor.forClass(String.class);
    verify(namespacedConfigMaps, times(2)).withName(args.capture());
    assertThat(args.getValue()).isEqualTo("cm-delete");
    verify(configMapResource).delete();

    ObjectMeta objectMeta = new ObjectMeta();
    objectMeta.setName("cm");
    replicationController.setMetadata(objectMeta);
    configMap.setMetadata(objectMeta);
    kubernetesContainerService.createOrReplaceConfigMapFabric8(KUBERNETES_CONFIG, configMap);
    verify(namespacedConfigMaps).createOrReplace(configMap);

    when(kubernetesClient.configMaps()).thenThrow(new InvalidRequestException("test"));
    ConfigMap returnedConfigMap = kubernetesContainerService.getConfigMapFabric8(KUBERNETES_CONFIG, "");
    assertThat(returnedConfigMap).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetService() throws Exception {
    V1Service service = new V1ServiceBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), service));

    V1Service result = kubernetesContainerService.getService(KUBERNETES_CONFIG, "service");
    assertThat(result).isEqualTo(service);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetServiceExceptioon() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Service.class).getType()))
        .thenThrow(new ApiException(403, "", emptyMap(), "{error: \"unable to get service\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getService(KUBERNETES_CONFIG, "service"))
        .hasMessageContaining(
            "Unable to get default/Service/service. Code: 403, message: {error: \"unable to get service\"}");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetServiceWithNulls() throws Exception {
    assertThatCode(() -> kubernetesContainerService.getService(null, null)).doesNotThrowAnyException();
    assertThatCode(() -> kubernetesContainerService.getService(KUBERNETES_CONFIG, null)).doesNotThrowAnyException();

    verify(k8sApiClient, never()).execute(k8sApiCall, TypeToken.get(V1ServiceList.class).getType());
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

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetVersionAsString() throws Exception {
    VersionInfo versionInfo = new VersionInfoBuilder().withMajor("1").withMinor("16").build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(VersionInfo.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), versionInfo));

    String result = kubernetesContainerService.getVersionAsString(KUBERNETES_CONFIG);
    assertThat(result).isEqualTo("1.16");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetConfigFileContent() throws InterruptedException, ExecutionException, IOException {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    xxxxxxxx masterUrl\n"
        + "    insecure-skip-tls-verify: true\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    auth-provider:\n"
        + "      config:\n"
        + "        client-id: clientId\n"
        + "        client-secret: secret\n"
        + "        id-token: id_token\n"
        + "        refresh-token: refresh_token\n"
        + "        idp-issuer-url: url\n"
        + "      name: oidc\n";

    OpenIdOAuth2AccessToken accessToken = mock(OpenIdOAuth2AccessToken.class);
    doReturn("id_token").when(accessToken).getOpenIdToken();
    doReturn(3600).when(accessToken).getExpiresIn();
    doReturn("bearer").when(accessToken).getTokenType();
    doReturn("refresh_token").when(accessToken).getRefreshToken();

    doReturn(accessToken).when(oidcTokenRetriever).getAccessToken(any());

    // Test generating KubernetesConfig from KubernetesClusterConfig
    final KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                            .masterUrl("masterUrl")
                                            .namespace("namespace")
                                            .accountId("accId")
                                            .authType(OIDC)
                                            .oidcIdentityProviderUrl("url")
                                            .oidcUsername("user")
                                            .oidcGrantType(OidcGrantType.password)
                                            .oidcPassword("pwd".toCharArray())
                                            .oidcClientId("clientId".toCharArray())
                                            .oidcSecret("secret".toCharArray())
                                            .build();

    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(configFileContent).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetConfigFileContentForBasicAuth() {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    xxxxxxxx masterUrl\n"
        + "    insecure-skip-tls-verify: true\n"
        + "    \n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    \n"
        + "    \n"
        + "    password: password\n"
        + "    username: username\n"
        + "    ";

    KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                      .authType(USER_PASSWORD)
                                      .namespace("namespace")
                                      .masterUrl("masterUrl")
                                      .username("username".toCharArray())
                                      .password("password".toCharArray())
                                      .build();
    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetConfigFileWithCACert() {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    xxxxxxxx masterUrl\n"
        + "    \n"
        + "    certificate-authority-data: caCert\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    \n"
        + "    \n"
        + "    \n"
        + "    \n"
        + "    token: serviceAccountToken";

    KubernetesConfig kubeConfig = KubernetesConfig.builder()
                                      .authType(USER_PASSWORD)
                                      .namespace("namespace")
                                      .masterUrl("masterUrl")
                                      .caCert("caCert".toCharArray())
                                      .serviceAccountToken("serviceAccountToken".toCharArray())
                                      .build();
    String configFileContent = kubernetesContainerService.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetVersion() throws Exception {
    VersionInfo versionInfo = new VersionInfoBuilder().withMajor("1").withMinor("16").build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(VersionInfo.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), versionInfo));
    VersionInfo result = kubernetesContainerService.getVersion(KUBERNETES_CONFIG);
    assertThat(result).isEqualTo(versionInfo);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetVersionException() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(VersionInfo.class).getType()))
        .thenThrow(new ApiException(409, "Unable to get cluster version"));
    assertThatThrownBy(() -> kubernetesContainerService.getVersion(KUBERNETES_CONFIG))
        .hasMessage("Unable to retrieve k8s version. Code: 409, message: Unable to get cluster version");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetRunningPodsWithLabels() throws Exception {
    V1PodStatus runningStatus = new V1PodStatusBuilder().withPhase("Running").build();
    V1PodStatus terminatedStatus = new V1PodStatusBuilder().withPhase("Terminated").build();
    Map<String, String> labels = ImmutableMap.of("label1", "value1", "label2", "value2");
    V1PodList v1PodList = new V1PodListBuilder()
                              .addNewItem() // With running status
                              .withNewMetadata()
                              .withName("pod-1")
                              .endMetadata()
                              .withStatus(runningStatus)
                              .endItem()
                              .addNewItem() // With deletion timestamp
                              .withNewMetadata()
                              .withName("pod-2")
                              .withDeletionTimestamp(DateTime.now())
                              .endMetadata()
                              .withStatus(terminatedStatus)
                              .endItem()
                              .addNewItem() // Without status
                              .withNewMetadata()
                              .withName("pod-3")
                              .endMetadata()
                              .endItem()
                              .addNewItem() // With terminated status
                              .withNewMetadata()
                              .withName("pod-4")
                              .endMetadata()
                              .withStatus(terminatedStatus)
                              .endItem()
                              .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1PodList.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1PodList));

    List<V1Pod> result = kubernetesContainerService.getRunningPodsWithLabels(KUBERNETES_CONFIG, "default", labels);
    ArgumentCaptor<List<Pair>> queryParamsCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(k8sApiClient, times(1))
        .buildCall(anyString(), eq("GET"), queryParamsCaptor.capture(), anyListOf(Pair.class), any(),
            anyMapOf(String.class, String.class), anyMapOf(String.class, String.class),
            anyMapOf(String.class, Object.class), any(String[].class), any(ApiCallback.class));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMetadata().getName()).isEqualTo("pod-1");
    Optional<Pair> labelSelector =
        queryParamsCaptor.getValue().stream().filter(pair -> "labelSelector".equals(pair.getName())).findAny();
    assertThat(labelSelector).isPresent();
    assertThat(labelSelector.get().getValue()).isEqualTo("label1=value1,label2=value2");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetRunningPodsWithLabelsException() throws Exception {
    Map<String, String> labels = ImmutableMap.of("label1", "value1");
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1PodList.class).getType()))
        .thenThrow(new ApiException(401, emptyMap(), "{\"error\": \"unauthorized\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getRunningPodsWithLabels(KUBERNETES_CONFIG, "default", labels))
        .hasMessageContaining("Unable to get running pods. Code: 401, message: {\"error\": \"unauthorized\"}");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetConfigMap() throws Exception {
    V1ConfigMap configMap = new V1ConfigMapBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    V1ConfigMap result = kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(result).isEqualTo(configMap);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetNullWhenNoConfigMapExists() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    V1ConfigMap result = kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetConfigMapException() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(403, "", emptyMap(), "{error: \"cluster not found\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getConfigMap(KUBERNETES_CONFIG, "configmap"))
        .hasMessageContaining(
            "Failed to get default/ConfigMap/configmap. Code: 403, message: {error: \"cluster not found\"}");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetSecret() throws Exception {
    V1Secret secret = new V1SecretBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    V1Secret result = kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "secret");
    assertThat(result).isEqualTo(secret);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetNullWhenNoSecretExists() throws Exception {
    V1Secret secret = new V1SecretBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    V1Secret result = kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "secret");
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetSecretException() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenThrow(new ApiException(403, "", emptyMap(), "{error: \"cluster not found\"}"));

    assertThatThrownBy(() -> kubernetesContainerService.getSecret(KUBERNETES_CONFIG, "secret"))
        .hasMessageContaining(
            "Failed to get default/Secret/secret. Code: 403, message: {error: \"cluster not found\"}");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchReleaseHistoryFromSecrets() throws Exception {
    V1Secret v1Secret =
        new V1SecretBuilder().withData(ImmutableMap.of(ReleaseHistoryKeyName, "test".getBytes())).build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), v1Secret));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromSecrets(KUBERNETES_CONFIG, "secret");

    assertThat(releaseHistory).isEqualTo("test");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryFromSecrets() throws Exception {
    V1Secret secret = new V1SecretBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromSecrets(KUBERNETES_CONFIG, "secret");

    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryWhenNoSecrets() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromSecrets(KUBERNETES_CONFIG, "secret");
    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchReleaseHistoryFromConfigMap() throws Exception {
    V1ConfigMap configMap = new V1ConfigMapBuilder().withData(ImmutableMap.of(ReleaseHistoryKeyName, "test")).build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(releaseHistory).isEqualTo("test");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryFromConfigMap() throws Exception {
    V1ConfigMap configMap = new V1ConfigMapBuilder().build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldFetchEmptyReleaseHistoryWhenNoConfigMapExists() throws Exception {
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenThrow(new ApiException(404, null, null, null));

    String releaseHistory = kubernetesContainerService.fetchReleaseHistoryFromConfigMap(KUBERNETES_CONFIG, "configmap");
    assertThat(releaseHistory).isEmpty();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldUpdateReleaseHistoryInConfigMap() throws Exception {
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, "test"))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    Map<String, String> data = new HashMap<>();
    data.put(ReleaseHistoryKeyName, "version=1.0");
    configMap.setData(data);
    configMap.getMetadata().setNamespace("test");
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    V1ConfigMap result =
        kubernetesContainerService.saveReleaseHistoryInConfigMap(KUBERNETES_CONFIG, "release", "version=1.0");
    assertThat(result).isNotNull();
    assertThat(result.getData().get(ReleaseHistoryKeyName)).isEqualTo("version=1.0");
    assertThat(result.getMetadata().getNamespace()).isEqualTo("test");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldCreateReleaseHistoryInConfigMap() throws Exception {
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, "version=2.0"))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMapList.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), new V1ConfigMapList()));

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    V1ConfigMap result =
        kubernetesContainerService.saveReleaseHistoryInConfigMap(KUBERNETES_CONFIG, "release", "version=2.0");
    assertThat(result).isNotNull();
    assertThat(result.getData().get(ReleaseHistoryKeyName)).isEqualTo("version=2.0");
    assertThat(result.getMetadata().getNamespace()).isEqualTo("default");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldSaveReleaseHistoryInConfigMapWhenFalse() throws Exception {
    V1ConfigMap configMap =
        new V1ConfigMapBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, "version=2.0"))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), configMap));

    kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=2.0", false);

    verify(k8sApiClient, times(3)).execute(k8sApiCall, TypeToken.get(V1ConfigMap.class).getType());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldUpdateReleaseHistoryInSecret() throws Exception {
    V1Secret secret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, encodeBase64ToByteArray("test".getBytes())))
            .build();

    secret.getMetadata().setNamespace("test");
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    V1Secret result =
        kubernetesContainerService.saveReleaseHistoryInSecrets(KUBERNETES_CONFIG, "release", "version=1.0");
    assertThat(result).isNotNull();
    assertThat(new String(result.getData().get(ReleaseHistoryKeyName))).isEqualTo("version=1.0");
    assertThat(result.getMetadata().getNamespace()).isEqualTo("test");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldCreateReleaseHistoryInSecret() throws Exception {
    V1Secret secret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, encodeBase64ToByteArray("version=2.0".getBytes())))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    V1Secret result =
        kubernetesContainerService.saveReleaseHistoryInSecrets(KUBERNETES_CONFIG, "release", "version=2.0");
    assertThat(result).isNotNull();
    assertThat(new String(result.getData().get(ReleaseHistoryKeyName))).isEqualTo("version=2.0");
    assertThat(result.getMetadata().getNamespace()).isEqualTo("default");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldCreateReleaseHistoryInSecretWhenTrue() throws Exception {
    V1Secret secret =
        new V1SecretBuilder()
            .withMetadata(
                new V1ObjectMetaBuilder().withNamespace(KUBERNETES_CONFIG.getNamespace()).withName("release").build())
            .withData(ImmutableMap.of(ReleaseHistoryKeyName, encodeBase64ToByteArray("version=2.0".getBytes())))
            .build();

    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Secret.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), secret));

    kubernetesContainerService.saveReleaseHistory(KUBERNETES_CONFIG, "release", "version=2.0", true);

    verify(k8sApiClient, times(3)).execute(k8sApiCall, TypeToken.get(V1Secret.class).getType());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldDeleteSecret() throws Exception {
    V1Status status = new V1StatusBuilder().withMetadata(new V1ListMetaBuilder().build()).build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Status.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), status));

    kubernetesContainerService.deleteSecret(KUBERNETES_CONFIG, "release");

    verify(k8sApiClient, times(1)).execute(k8sApiCall, TypeToken.get(V1Status.class).getType());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldDeleteConfigMap() throws Exception {
    V1Status status = new V1StatusBuilder().withMetadata(new V1ListMetaBuilder().build()).build();
    when(k8sApiClient.execute(k8sApiCall, TypeToken.get(V1Status.class).getType()))
        .thenReturn(new ApiResponse<>(200, emptyMap(), status));

    kubernetesContainerService.deleteConfigMap(KUBERNETES_CONFIG, "release");

    verify(k8sApiClient, times(1)).execute(k8sApiCall, TypeToken.get(V1Status.class).getType());
  }
}
