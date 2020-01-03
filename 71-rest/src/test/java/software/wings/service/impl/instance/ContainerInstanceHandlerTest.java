package software.wings.service.impl.instance;

import static io.harness.rule.OwnerRule.ADWAIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.infrastructure.instance.InstanceType.KUBERNETES_CONTAINER_INSTANCE;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ECS_CLUSTER;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.KUBE_CLUSTER;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.US_EAST;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentSummary;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.Application;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo.Builder;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.instance.InstanceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ContainerInstanceHandlerTest extends WingsBaseTest {
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private InstanceService instanceService;
  @Mock private AppService appService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock private ContainerSync containerSync;
  @Mock private DeploymentService deploymentService;
  @InjectMocks @Inject ContainerInstanceHandler containerInstanceHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    // catpure arg
    doReturn(true).when(instanceService).delete(anySet());
    // capture arg
    doReturn(Instance.builder().build()).when(instanceService).save(any(Instance.class));

    doReturn(Application.Builder.anApplication().name(APP_NAME).uuid(APP_ID).accountId(ACCOUNT_ID).build())
        .when(appService)
        .get(anyString());

    doReturn(Environment.Builder.anEnvironment().environmentType(EnvironmentType.PROD).name(ENV_NAME).build())
        .when(environmentService)
        .get(anyString(), anyString(), anyBoolean());

    doReturn(Service.builder().name(SERVICE_NAME).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());
  }

  private InfrastructureMapping getInframapping(String inframappingType) {
    if (inframappingType.equals(InfrastructureMappingType.AWS_ECS.getName())) {
      return EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping()
          .withAppId(APP_ID)
          .withRegion(US_EAST)
          .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
          .withUuid(INFRA_MAPPING_ID)
          .withClusterName(ECS_CLUSTER)
          .withEnvId(ENV_ID)
          .withInfraMappingType(InfrastructureMappingType.AWS_ECS.getName())
          .withServiceId(SERVICE_ID)
          .withUuid(INFRA_MAPPING_ID)
          .withAccountId(ACCOUNT_ID)
          .build();
    } else {
      return GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping()
          .withAppId(APP_ID)
          .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
          .withUuid(INFRA_MAPPING_ID)
          .withClusterName("k")
          .withNamespace("default")
          .withEnvId(ENV_ID)
          .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
          .withServiceId(SERVICE_ID)
          .withUuid(INFRA_MAPPING_ID)
          .withAccountId(ACCOUNT_ID)
          .build();
    }
  }

  // expected 1 Delete
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSyncInstancesDelete_ECS() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.AWS_ECS.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(
        Instance.builder()
            .uuid(INSTANCE_1_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_ECS.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("taskARN:0").build())
            .instanceInfo(EcsContainerInfo.Builder.anEcsContainerInfo()
                              .withClusterName("ECSCluster")
                              .withServiceName("service_a_1")
                              .withStartedAt(0)
                              .withStartedBy("user1")
                              .withTaskArn("taskARN:0")
                              .withTaskDefinitionArn("taskDefinitionArn")
                              .build())
            .build()));

    ContainerSyncResponse containerSyncResponse =
        ContainerSyncResponse.builder().containerInfoList(Collections.emptyList()).build();

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(containerSyncResponse).when(containerSync).getInstances(any(), anyList());

    containerInstanceHandler.syncInstances(APP_ID, INFRA_MAPPING_ID);
    assertionsForDelete(INSTANCE_1_ID);
  }

  // expected  1 add
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSyncInstancesAdd_ECS() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.AWS_ECS.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(
        Instance.builder()
            .uuid(INSTANCE_1_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_ECS.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("taskARN:0").build())
            .lastWorkflowExecutionId("id")
            .instanceInfo(EcsContainerInfo.Builder.anEcsContainerInfo()
                              .withClusterName("ECSCluster")
                              .withServiceName("service_a_1")
                              .withStartedAt(0)
                              .withStartedBy("user1")
                              .withTaskArn("taskARN:0")
                              .withTaskDefinitionArn("taskDefinitionArn")
                              .build())
            .build()));

    ContainerSyncResponse containerSyncResponse = ContainerSyncResponse.builder()
                                                      .containerInfoList(asList(Builder.anEcsContainerInfo()
                                                                                    .withClusterName(ECS_CLUSTER)
                                                                                    .withServiceName("service_a_1")
                                                                                    .withTaskArn("taskARN:0")
                                                                                    .withStartedAt(0)
                                                                                    .withStartedBy("user1")
                                                                                    .build(),
                                                          Builder.anEcsContainerInfo()
                                                              .withClusterName(ECS_CLUSTER)
                                                              .withServiceName("service_a_1")
                                                              .withTaskArn("taskARN:2")
                                                              .withStartedAt(0)
                                                              .withStartedBy("user1")
                                                              .build()))
                                                      .build();

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(containerSyncResponse).when(containerSync).getInstances(any(), anyList());

    containerInstanceHandler.syncInstances(APP_ID, INFRA_MAPPING_ID);
    assertionsForSave("taskARN:2", InstanceType.ECS_CONTAINER_INSTANCE);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testNewDeployment_DeleteOlderInstance_ECS() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.AWS_ECS.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(
        Instance.builder()
            .uuid(INSTANCE_1_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_ECS.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("taskARN:0").build())
            .instanceInfo(EcsContainerInfo.Builder.anEcsContainerInfo()
                              .withClusterName("ECSCluster")
                              .withServiceName("service_b_1")
                              .withStartedAt(0)
                              .withStartedBy("user1")
                              .withTaskArn("taskARN:0")
                              .withTaskDefinitionArn("taskDefinitionArn")
                              .build())
            .build()));

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(ContainerSyncResponse.builder().containerInfoList(Collections.EMPTY_LIST).build())
        .when(containerSync)
        .getInstances(any(), anyList());
    OnDemandRollbackInfo onDemandRollbackInfo = OnDemandRollbackInfo.builder().onDemandRollback(false).build();

    containerInstanceHandler.handleNewDeployment(
        Arrays.asList(DeploymentSummary.builder()
                          .deploymentInfo(ContainerDeploymentInfoWithNames.builder()
                                              .clusterName(ECS_CLUSTER)
                                              .containerSvcName("service_b_1")
                                              .build())
                          .accountId(ACCOUNT_ID)
                          .infraMappingId(INFRA_MAPPING_ID)
                          .workflowExecutionId("workfloeExecution_1")
                          .stateExecutionInstanceId("stateExecutionInstanceId")
                          .build()),
        false, onDemandRollbackInfo);
    assertionsForDelete(INSTANCE_1_ID);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testNewDeployment_AddNewInstance_ECS() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.AWS_ECS.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.EMPTY_LIST);

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(ContainerSyncResponse.builder()
                 .containerInfoList(asList(Builder.anEcsContainerInfo()
                                               .withClusterName(ECS_CLUSTER)
                                               .withServiceName("service_a_1")
                                               .withTaskArn("taskARN:0")
                                               .withStartedAt(0)
                                               .withStartedBy("user1")
                                               .build()))
                 .build())
        .when(containerSync)
        .getInstances(any(), anyList());
    OnDemandRollbackInfo onDemandRollbackInfo = OnDemandRollbackInfo.builder().onDemandRollback(false).build();

    containerInstanceHandler.handleNewDeployment(
        Arrays.asList(DeploymentSummary.builder()
                          .deploymentInfo(ContainerDeploymentInfoWithNames.builder()
                                              .clusterName(ECS_CLUSTER)
                                              .containerSvcName("service_a_1")
                                              .build())
                          .accountId(ACCOUNT_ID)
                          .infraMappingId(INFRA_MAPPING_ID)
                          .workflowExecutionId("workfloeExecution_1")
                          .stateExecutionInstanceId("stateExecutionInstanceId")
                          .build()),
        false, onDemandRollbackInfo);

    assertionsForSave("taskARN:0", InstanceType.ECS_CONTAINER_INSTANCE);
  }

  private void testSyncInstances(PageResponse<Instance> pageResponse, ContainerSyncResponse containerSyncResponse,
      String containerId, InstanceType instanceType) throws Exception {
    doReturn(pageResponse).when(instanceService).list(any());

    doReturn(ContainerSyncResponse.builder().containerInfoList(asList()).build())
        .doReturn(containerSyncResponse)
        .when(containerSync)
        .getInstances(any(), anyList());

    containerInstanceHandler.syncInstances(APP_ID, INFRA_MAPPING_ID);
    // assertions(containerId, instanceType, false);
  }

  private void assertions(String containerId, String instanceId, InstanceType instanceType, boolean checkSaveOrUpdate)
      throws Exception {
    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceService).delete(captor.capture());
    Set idTobeDeleted = captor.getValue();
    // This asserts only 1 instance is deleted
    assertThat(idTobeDeleted).hasSize(1);
    assertThat(idTobeDeleted.contains(INSTANCE_1_ID) || idTobeDeleted.contains(INSTANCE_2_ID)).isTrue();

    if (checkSaveOrUpdate) {
      ArgumentCaptor<Instance> captorInstance = ArgumentCaptor.forClass(Instance.class);
      verify(instanceService, times(1)).save(captorInstance.capture());

      List<Instance> capturedInstances = captorInstance.getAllValues();
      assertThat(capturedInstances.get(0).getContainerInstanceKey().getContainerId()).isEqualTo(containerId);
      assertThat(capturedInstances.get(0).getInstanceType()).isEqualTo(instanceType);
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSyncInstances_DeleteInstance_Kubernetes() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.GCP_KUBERNETES.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(
        Instance.builder()
            .uuid(INSTANCE_1_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(KUBERNETES_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("pod:0").build())
            .instanceInfo(KubernetesContainerInfo.builder()
                              .clusterName(KUBE_CLUSTER)
                              .serviceName("service_a_0")
                              .controllerName("controllerName:0")
                              .podName("pod:0")
                              .build())
            .build()));

    ContainerSyncResponse containerSyncResponse =
        ContainerSyncResponse.builder().containerInfoList(Collections.EMPTY_LIST).build();

    doReturn(pageResponse).when(instanceService).list(any());

    doReturn(ContainerSyncResponse.builder().containerInfoList(asList()).build())
        .doReturn(containerSyncResponse)
        .when(containerSync)
        .getInstances(any(), anyList());

    containerInstanceHandler.syncInstances(APP_ID, INFRA_MAPPING_ID);
    assertionsForDelete(INSTANCE_1_ID);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSyncInstances_AddInstance_Kubernetes() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.GCP_KUBERNETES.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(
        Instance.builder()
            .uuid(INSTANCE_1_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(KUBERNETES_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("pod:0").build())
            .lastWorkflowExecutionId("id")
            .instanceInfo(KubernetesContainerInfo.builder()
                              .clusterName(KUBE_CLUSTER)
                              .serviceName("service_a_0")
                              .controllerName("controllerName:0")
                              .podName("pod:0")
                              .build())
            .build()));

    ContainerSyncResponse containerSyncResponse = ContainerSyncResponse.builder()
                                                      .containerInfoList(asList(KubernetesContainerInfo.builder()
                                                                                    .clusterName(KUBE_CLUSTER)
                                                                                    .controllerName("controllerName:0")
                                                                                    .podName("pod:0")
                                                                                    .serviceName("service_a_0")
                                                                                    .build(),
                                                          KubernetesContainerInfo.builder()
                                                              .clusterName(KUBE_CLUSTER)
                                                              .controllerName("controllerName:1")
                                                              .podName("pod:1")
                                                              .serviceName("service_a_0")
                                                              .build()))
                                                      .build();

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(containerSyncResponse).when(containerSync).getInstances(any(), anyList());

    containerInstanceHandler.syncInstances(APP_ID, INFRA_MAPPING_ID);
    assertionsForSave("pod:1", KUBERNETES_CONTAINER_INSTANCE);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testNewDeployment_DeleteOldInstances_Kubernetes() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.GCP_KUBERNETES.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(
        Instance.builder()
            .uuid(INSTANCE_1_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(KUBERNETES_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("pod:0").build())
            .instanceInfo(KubernetesContainerInfo.builder()
                              .clusterName(KUBE_CLUSTER)
                              .serviceName("service_a_0")
                              .controllerName("controllerName:0")
                              .namespace("default")
                              .podName("pod:0")
                              .build())
            .build()));

    doReturn(pageResponse).when(instanceService).list(any());

    doReturn(ContainerSyncResponse.builder().containerInfoList(asList()).build())
        .when(containerSync)
        .getInstances(any(), anyList());
    OnDemandRollbackInfo onDemandRollbackInfo = OnDemandRollbackInfo.builder().onDemandRollback(false).build();

    containerInstanceHandler.handleNewDeployment(
        Arrays.asList(DeploymentSummary.builder()
                          .deploymentInfo(ContainerDeploymentInfoWithNames.builder()
                                              .clusterName(KUBE_CLUSTER)
                                              .containerSvcName("controllerName:0")
                                              .namespace("default")
                                              .build())
                          .accountId(ACCOUNT_ID)
                          .infraMappingId(INFRA_MAPPING_ID)
                          .workflowExecutionId("workfloeExecution_1")
                          .stateExecutionInstanceId("stateExecutionInstanceId")
                          .build()),
        false, onDemandRollbackInfo);

    assertionsForDelete(INSTANCE_1_ID);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testNewDeployment_AddNewInstance_Kubernetes() throws Exception {
    doReturn(getInframapping(InfrastructureMappingType.GCP_KUBERNETES.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.EMPTY_LIST);

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(ContainerSyncResponse.builder()
                 .containerInfoList(asList(KubernetesContainerInfo.builder()
                                               .clusterName(KUBE_CLUSTER)
                                               .controllerName("controllerName:0")
                                               .podName("pod:1")
                                               .serviceName("service_a_1")
                                               .namespace("default")
                                               .build()))
                 .build())
        .when(containerSync)
        .getInstances(any(), anyList());
    OnDemandRollbackInfo onDemandRollbackInfo = OnDemandRollbackInfo.builder().onDemandRollback(false).build();

    containerInstanceHandler.handleNewDeployment(
        Arrays.asList(DeploymentSummary.builder()
                          .deploymentInfo(ContainerDeploymentInfoWithNames.builder()
                                              .clusterName(KUBE_CLUSTER)
                                              .containerSvcName("controllerName:0")
                                              .namespace("default")
                                              .build())
                          .accountId(ACCOUNT_ID)
                          .infraMappingId(INFRA_MAPPING_ID)
                          .workflowExecutionId("workfloeExecution_1")
                          .stateExecutionInstanceId("stateExecutionInstanceId")
                          .build()),
        false, onDemandRollbackInfo);

    assertionsForSave("pod:1", KUBERNETES_CONTAINER_INSTANCE);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testNewDeployment_Kubernetes_Rollback() throws Exception {
    doReturn(Optional.of(
                 DeploymentSummary.builder()
                     .deploymentInfo(ContainerDeploymentInfoWithNames.builder().containerSvcName("service_a_1").build())
                     .accountId(ACCOUNT_ID)
                     .infraMappingId(INFRA_MAPPING_ID)
                     .workflowExecutionId("workfloeExecution_1")
                     .stateExecutionInstanceId("stateExecutionInstanceId")
                     .artifactBuildNum("1")
                     .artifactName("old")
                     .build()))
        .when(deploymentService)
        .get(any(DeploymentSummary.class));

    doReturn(getInframapping(InfrastructureMappingType.GCP_KUBERNETES.name()))
        .when(infraMappingService)
        .get(anyString(), anyString());

    PageResponse<Instance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(
        Instance.builder()
            .uuid(INSTANCE_1_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(KUBERNETES_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("pod:0").build())
            .instanceInfo(KubernetesContainerInfo.builder()
                              .clusterName(KUBE_CLUSTER)
                              .serviceName("service_a_0")
                              .controllerName("controllerName:0")
                              .podName("pod:0")
                              .build())
            .build()));

    doReturn(pageResponse).when(instanceService).list(any());

    doReturn(ContainerSyncResponse.builder().containerInfoList(asList()).build())
        .doReturn(ContainerSyncResponse.builder()
                      .containerInfoList(asList(KubernetesContainerInfo.builder()
                                                    .clusterName(KUBE_CLUSTER)
                                                    .controllerName("controllerName:1")
                                                    .podName("pod:1")
                                                    .serviceName("service_a_1")
                                                    .build()))
                      .build())
        .when(containerSync)
        .getInstances(any(), anyList());
    OnDemandRollbackInfo onDemandRollbackInfo = OnDemandRollbackInfo.builder().onDemandRollback(false).build();

    containerInstanceHandler.handleNewDeployment(
        Arrays.asList(DeploymentSummary.builder()
                          .deploymentInfo(ContainerDeploymentInfoWithNames.builder()
                                              .clusterName(KUBE_CLUSTER)
                                              .containerSvcName("controllerName:0")
                                              .build())
                          .accountId(ACCOUNT_ID)
                          .infraMappingId(INFRA_MAPPING_ID)
                          .workflowExecutionId("workfloeExecution_1")
                          .stateExecutionInstanceId("stateExecutionInstanceId")
                          .build(),
            DeploymentSummary.builder()
                .deploymentInfo(ContainerDeploymentInfoWithNames.builder()
                                    .clusterName(KUBE_CLUSTER)
                                    .containerSvcName("controllerName:1")
                                    .build())
                .accountId(ACCOUNT_ID)
                .infraMappingId(INFRA_MAPPING_ID)
                .workflowExecutionId("workfloeExecution_1")
                .stateExecutionInstanceId("stateExecutionInstanceId")
                .build()),
        true, onDemandRollbackInfo);

    assertions_rollback("pod:1", KUBERNETES_CONTAINER_INSTANCE, true);
  }

  private void assertionsForDelete(String instanceId) throws Exception {
    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceService).delete(captor.capture());
    Set idTobeDeleted = captor.getValue();
    // This asserts only 1 instance is deleted
    assertThat(idTobeDeleted).hasSize(1);
    assertThat(idTobeDeleted.contains(instanceId)).isTrue();
  }

  private void assertions_rollback(String containerId, InstanceType instanceType, boolean checkSaveOrUpdate)
      throws Exception {
    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceService).delete(captor.capture());
    Set idTobeDeleted = captor.getValue();
    assertThat(idTobeDeleted).hasSize(1);
    assertThat(idTobeDeleted.contains(INSTANCE_1_ID)).isTrue();

    if (checkSaveOrUpdate) {
      ArgumentCaptor<Instance> captorInstance = ArgumentCaptor.forClass(Instance.class);
      verify(instanceService, times(1)).save(captorInstance.capture());

      List<Instance> capturedInstances = captorInstance.getAllValues();
      assertThat(capturedInstances.get(0).getContainerInstanceKey().getContainerId()).isEqualTo(containerId);
      assertThat(capturedInstances.get(0).getInstanceType()).isEqualTo(instanceType);
      assertThat(capturedInstances.get(0).getLastArtifactName()).isEqualTo("old");
      assertThat(capturedInstances.get(0).getLastArtifactBuildNum()).isEqualTo("1");
    }
  }

  private void assertionsForSave(String containerId, InstanceType instanceType) throws Exception {
    ArgumentCaptor<Instance> captorInstance = ArgumentCaptor.forClass(Instance.class);
    verify(instanceService, times(1)).save(captorInstance.capture());

    List<Instance> capturedInstances = captorInstance.getAllValues();
    assertThat(capturedInstances.get(0).getContainerInstanceKey().getContainerId()).isEqualTo(containerId);
    assertThat(capturedInstances.get(0).getInstanceType()).isEqualTo(instanceType);
  }
}
