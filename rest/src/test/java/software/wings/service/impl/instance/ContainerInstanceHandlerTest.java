package software.wings.service.impl.instance;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ECS_CLUSTER;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP2;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.US_EAST;

import com.google.inject.Inject;

import de.danielbechler.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerDeploymentInfo;
import software.wings.beans.Application;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.dl.PageResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Set;

public class ContainerInstanceHandlerTest extends WingsBaseTest {
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private SecretManager secretManager;
  @Mock private EncryptedDataDetail encryptedDataDetail;
  @Mock private InstanceService instanceService;
  @Mock private SettingsService settingsService;
  @Mock private AwsHelperService awsHelperService;
  @Mock private AwsCodeDeployService awsCodeDeployService;
  @Mock private AppService appService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock private ContainerSync containerSync;
  @InjectMocks @Spy InstanceHelper instanceHelper;
  @InjectMocks @Spy ContainerInstanceHelper containerInstanceHelper;
  @Spy InstanceUtil instanceUtil;
  @InjectMocks @Inject ContainerInstanceHandler containerInstanceHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    doReturn(EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping()
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
                 .withAppId(APP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_ECS.getName())
                 .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
                 .withRegion(US_EAST)
                 .withServiceId(SERVICE_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    // catpure arg
    doReturn(true).when(instanceService).delete(anySet());
    // capture arg
    doReturn(Instance.builder().build()).when(instanceService).saveOrUpdate(any(Instance.class));

    doReturn(Application.Builder.anApplication().withName(APP_NAME).withUuid(APP_ID).withAccountId(ACCOUNT_ID).build())
        .when(appService)
        .get(anyString());

    doReturn(Environment.Builder.anEnvironment().withEnvironmentType(EnvironmentType.PROD).withName(ENV_NAME).build())
        .when(environmentService)
        .get(anyString(), anyString(), anyBoolean());

    doReturn(Service.Builder.aService().withName(SERVICE_NAME).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());
  }

  // 2 existing ECS instances,
  // expected 1 Delete, 1 Update
  @Test
  public void testSyncInstances() throws Exception {
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
            .build(),
        Instance.builder()
            .uuid(INSTANCE_2_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_ECS.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP2).build())
            .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId("taskARN:1").build())
            .instanceInfo(EcsContainerInfo.Builder.anEcsContainerInfo()
                              .withClusterName(ECS_CLUSTER)
                              .withServiceName("service_b_1")
                              .withStartedAt(0)
                              .withStartedBy("user1")
                              .withTaskArn("taskARN:1")
                              .withTaskDefinitionArn("taskDefinitionArn")
                              .build())
            .build()));

    doReturn(pageResponse).when(instanceService).list(any());

    ContainerSyncResponse instanceSyncResponse =
        doReturn(ContainerSyncResponse.builder().containerInfoList(asList()).build())
            .doReturn(ContainerSyncResponse.builder()
                          .containerInfoList(asList(EcsContainerInfo.Builder.anEcsContainerInfo()
                                                        .withClusterName(ECS_CLUSTER)
                                                        .withServiceName("service_b_1")
                                                        .withTaskArn("taskARN:1")
                                                        .withStartedAt(0)
                                                        .withStartedBy("user1")
                                                        .build()))
                          .build())
            .when(containerSync)
            .getInstances(any(), anyList());

    containerInstanceHandler.syncInstances(APP_ID, INFRA_MAPPING_ID);

    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceService).delete(captor.capture());
    Set idTobeDeleted = captor.getValue();
    assertEquals(1, idTobeDeleted.size());
    assertTrue(idTobeDeleted.contains(INSTANCE_1_ID));

    ArgumentCaptor<Instance> captorInstance = ArgumentCaptor.forClass(Instance.class);
    verify(instanceService, times(1)).saveOrUpdate(captorInstance.capture());

    List<Instance> capturedInstances = captorInstance.getAllValues();
    assertEquals("taskARN:1", capturedInstances.get(0).getContainerInstanceKey().getContainerId());
    assertEquals(InstanceType.ECS_CONTAINER_INSTANCE, capturedInstances.get(0).getInstanceType());
  }

  @Test
  public void testSyncInstances_2() throws Exception {
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
                              .withTaskArn("taskARN:1")
                              .withTaskDefinitionArn("taskDefinitionArn")
                              .build())
            .build()));

    doReturn(pageResponse).when(instanceService).list(any());

    ContainerSyncResponse instanceSyncResponse =
        doReturn(ContainerSyncResponse.builder().containerInfoList(asList()).build())
            .doReturn(ContainerSyncResponse.builder()
                          .containerInfoList(asList(EcsContainerInfo.Builder.anEcsContainerInfo()
                                                        .withClusterName(ECS_CLUSTER)
                                                        .withServiceName("service_b_2")
                                                        .withTaskArn("taskARN:2")
                                                        .withStartedAt(0)
                                                        .withStartedBy("user1")
                                                        .build()))
                          .build())
            .when(containerSync)
            .getInstances(any(), anyList());

    containerInstanceHandler.handleNewDeployment(
        ContainerDeploymentInfo.builder()
            .clusterName(ECS_CLUSTER)
            .containerSvcNameSet(Collections.setOf(asList("service_b_1", "service_b_2")))
            .accountId(ACCOUNT_ID)
            .infraMappingId(INFRA_MAPPING_ID)
            .workflowExecutionId("workfloeExecution_1")
            .stateExecutionInstanceId("stateExecutionInstanceId")
            .build());

    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceService).delete(captor.capture());
    Set idTobeDeleted = captor.getValue();
    assertEquals(1, idTobeDeleted.size());
    assertTrue(idTobeDeleted.contains(INSTANCE_1_ID));

    ArgumentCaptor<Instance> captorInstance = ArgumentCaptor.forClass(Instance.class);
    verify(instanceService, times(1)).saveOrUpdate(captorInstance.capture());

    List<Instance> capturedInstances = captorInstance.getAllValues();
    assertEquals("taskARN:2", capturedInstances.get(0).getContainerInstanceKey().getContainerId());
    assertEquals(InstanceType.ECS_CONTAINER_INSTANCE, capturedInstances.get(0).getInstanceType());
  }
}
