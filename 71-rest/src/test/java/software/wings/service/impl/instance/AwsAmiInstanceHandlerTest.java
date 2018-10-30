package software.wings.service.impl.instance;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ASG_1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.AWS_ACCESS_KEY;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.AWS_ENCRYPTED_SECRET_KEY;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP2;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP3;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP4;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_3_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_4_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PRIVATE_DNS_1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PRIVATE_DNS_2;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PRIVATE_DNS_3;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PUBLIC_DNS_1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PUBLIC_DNS_2;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PUBLIC_DNS_3;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PUBLIC_DNS_4;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.US_EAST;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import io.harness.beans.PageResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.impl.AwsUtils;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AwsAmiInstanceHandlerTest extends WingsBaseTest {
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private SecretManager secretManager;
  @Mock private EncryptedDataDetail encryptedDataDetail;
  @Mock private InstanceService instanceService;
  @Mock private SettingsService settingsService;
  @Mock private AwsHelperService awsHelperService;
  @Mock private AwsCodeDeployService awsCodeDeployService;
  @Mock private AppService appService;
  @Mock private AwsUtils mockAwsUtils;
  @Mock private AwsEc2HelperServiceManager mockAwsEc2HelperServiceManager;
  @Mock private AwsAsgHelperServiceManager mockAwsAsgHelperServiceManager;

  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock DeploymentService deploymentService;
  @InjectMocks @Inject AwsAmiInstanceHandler awsAmiInstanceHandler;
  @InjectMocks @Spy InstanceHelper instanceHelper;
  @InjectMocks @Spy AwsInfrastructureProvider awsInfrastructureProvider;
  @Spy InstanceUtil instanceUtil;
  private com.amazonaws.services.ec2.model.Instance instance1;
  private com.amazonaws.services.ec2.model.Instance instance2;
  private com.amazonaws.services.ec2.model.Instance instance3;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    instance1 = new com.amazonaws.services.ec2.model.Instance();
    instance1.setPrivateDnsName(InstanceSyncTestConstants.PRIVATE_DNS_1);
    instance1.setPublicDnsName(PUBLIC_DNS_1);
    instance1.setInstanceId(INSTANCE_1_ID);

    instance2 = new com.amazonaws.services.ec2.model.Instance();
    instance2.setPrivateDnsName(PRIVATE_DNS_2);
    instance2.setPublicDnsName(PUBLIC_DNS_2);
    instance2.setInstanceId(INSTANCE_2_ID);

    instance3 = new com.amazonaws.services.ec2.model.Instance();
    instance3.setPrivateDnsName(PRIVATE_DNS_3);
    instance3.setPublicDnsName(PUBLIC_DNS_3);
    instance3.setInstanceId(INSTANCE_3_ID);

    doReturn(AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping()
                 .withUuid(INFRA_MAPPING_ID)
                 .withAccountId(ACCOUNT_ID)
                 .withAutoScalingGroupName(ASG_1)
                 .withAppId(APP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_AMI.getName())
                 .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
                 .withRegion(US_EAST)
                 .withServiceId(SERVICE_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    doReturn(asList(encryptedDataDetail)).when(secretManager).getEncryptionDetails(any(), any(), any());

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withValue(AwsConfig.builder()
                                .accessKey(AWS_ACCESS_KEY)
                                .accountId(ACCOUNT_ID)
                                .encryptedSecretKey(AWS_ENCRYPTED_SECRET_KEY)
                                .build())
                 .build())
        .when(settingsService)
        .get(anyString());

    // capture arg
    doReturn(true).when(instanceService).delete(anySet());
    // capture arg
    doReturn(Instance.builder().build()).when(instanceService).save(any(Instance.class));

    doReturn(Application.Builder.anApplication().withName("app").withUuid("app_1").withAccountId(ACCOUNT_ID).build())
        .when(appService)
        .get(anyString());

    doReturn(Environment.Builder.anEnvironment().withEnvironmentType(EnvironmentType.PROD).withName(ENV_NAME).build())
        .when(environmentService)
        .get(anyString(), anyString(), anyBoolean());

    doReturn(Service.builder().name(SERVICE_NAME).build()).when(serviceResourceService).get(anyString(), anyString());
  }

  // 3 existing instances, 1 EC2, 2 AMI,
  // expected EC2 Delete, 2 AMI Update
  @Test
  public void testSyncInstances_instanceSync() throws Exception {
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
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(AutoScalingGroupInstanceInfo.builder()
                              .autoScalingGroupName(ASG_1)
                              .hostId(HOST_NAME_IP1)
                              .ec2Instance(instance1)
                              .hostPublicDns(PUBLIC_DNS_1)
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
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP2).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(AutoScalingGroupInstanceInfo.builder()
                              .autoScalingGroupName(ASG_1)
                              .hostId(HOST_NAME_IP2)
                              .ec2Instance(instance2)
                              .hostPublicDns(PUBLIC_DNS_2)
                              .build())
            .build(),
        Instance.builder()
            .uuid(INSTANCE_3_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP3).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(Ec2InstanceInfo.builder().ec2Instance(instance3).hostPublicDns(PUBLIC_DNS_3).build())
            .build(),
        Instance.builder()
            .uuid(INSTANCE_4_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP4).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(Ec2InstanceInfo.builder()
                              .ec2Instance(null /*for NPE issue we saw in prod*/)
                              .hostPublicDns(PUBLIC_DNS_4)
                              .build())
            .build()));

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(new DescribeInstancesResult()).when(awsHelperService).describeEc2Instances(any(), any(), any(), any());
    doReturn(emptyList()).when(mockAwsEc2HelperServiceManager).listEc2Instances(any(), any(), any(), any());

    com.amazonaws.services.ec2.model.Instance ec2Instance1 = new com.amazonaws.services.ec2.model.Instance();
    ec2Instance1.setPrivateDnsName(PRIVATE_DNS_1);
    ec2Instance1.setPublicDnsName(PUBLIC_DNS_1);
    ec2Instance1.setInstanceId(INSTANCE_1_ID);

    // ec2Instance with empty privateDnsName
    com.amazonaws.services.ec2.model.Instance ec2Instance2 = new com.amazonaws.services.ec2.model.Instance();
    ec2Instance2.setInstanceId(INSTANCE_2_ID);

    doReturn(asList(ec2Instance1, ec2Instance2))
        .when(mockAwsAsgHelperServiceManager)
        .listAutoScalingGroupInstances(any(), anyList(), anyString(), anyString(), anyString());

    awsAmiInstanceHandler.syncInstances(APP_ID, INFRA_MAPPING_ID);

    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceService).delete(captor.capture());
    Set idTobeDeleted = captor.getValue();
    assertEquals(1, idTobeDeleted.size());
    assertTrue(idTobeDeleted.contains(instance3.getInstanceId()));
  }

  // 3 existing instances, 1 EC2, 2 AMI,
  // expected EC2 Delete, 2 AMI Update
  @Test
  public void testSyncInstances_Rollback() throws Exception {
    PageResponse<Instance> pageResponse = new PageResponse<>();

    doReturn(Optional.of(
                 DeploymentSummary.builder()
                     .deploymentInfo(
                         AwsAutoScalingGroupDeploymentInfo.builder().autoScalingGroupName("autoScalingGroup").build())
                     .accountId(ACCOUNT_ID)
                     .infraMappingId(INFRA_MAPPING_ID)
                     .workflowExecutionId("workfloeExecution_1")
                     .stateExecutionInstanceId("stateExecutionInstanceId")
                     .artifactBuildNum("1")
                     .artifactName("old")
                     .build()))
        .when(deploymentService)
        .get(any(DeploymentSummary.class));

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
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP1).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(AutoScalingGroupInstanceInfo.builder()
                              .autoScalingGroupName(ASG_1)
                              .hostId(HOST_NAME_IP1)
                              .ec2Instance(instance1)
                              .hostPublicDns(PUBLIC_DNS_1)
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
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP2).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(AutoScalingGroupInstanceInfo.builder()
                              .autoScalingGroupName(ASG_1)
                              .hostId(HOST_NAME_IP2)
                              .ec2Instance(instance2)
                              .hostPublicDns(PUBLIC_DNS_2)
                              .build())
            .build(),
        Instance.builder()
            .uuid(INSTANCE_3_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP3).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(Ec2InstanceInfo.builder().ec2Instance(instance3).hostPublicDns(PUBLIC_DNS_3).build())
            .build(),
        Instance.builder()
            .uuid(INSTANCE_4_ID)
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .computeProviderId(COMPUTE_PROVIDER_NAME)
            .appName(APP_NAME)
            .envId(ENV_ID)
            .envName(ENV_NAME)
            .envType(EnvironmentType.PROD)
            .infraMappingId(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .hostInstanceKey(HostInstanceKey.builder().infraMappingId(INFRA_MAPPING_ID).hostName(HOST_NAME_IP4).build())
            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
            .containerInstanceKey(ContainerInstanceKey.builder().containerId(CONTAINER_ID).build())
            .instanceInfo(Ec2InstanceInfo.builder()
                              .ec2Instance(null /*for NPE issue we saw in prod*/)
                              .hostPublicDns(PUBLIC_DNS_4)
                              .build())
            .build()));

    doReturn(pageResponse).when(instanceService).list(any());
    doReturn(new DescribeInstancesResult()).when(awsHelperService).describeEc2Instances(any(), any(), any(), any());
    doReturn(emptyList()).when(mockAwsEc2HelperServiceManager).listEc2Instances(any(), any(), any(), any());

    com.amazonaws.services.ec2.model.Instance ec2Instance1 = new com.amazonaws.services.ec2.model.Instance();
    ec2Instance1.setPrivateDnsName(PRIVATE_DNS_1);
    ec2Instance1.setPublicDnsName(PUBLIC_DNS_1);
    ec2Instance1.setInstanceId(INSTANCE_1_ID);

    // ec2Instance with empty privateDnsName
    com.amazonaws.services.ec2.model.Instance ec2Instance2 = new com.amazonaws.services.ec2.model.Instance();
    ec2Instance2.setInstanceId(INSTANCE_2_ID);

    doReturn(asList(ec2Instance1, ec2Instance2))
        .when(mockAwsAsgHelperServiceManager)
        .listAutoScalingGroupInstances(any(), anyList(), anyString(), anyString(), anyString());

    awsAmiInstanceHandler.handleNewDeployment(
        Arrays.asList(
            DeploymentSummary.builder()
                .deploymentInfo(
                    AwsAutoScalingGroupDeploymentInfo.builder().autoScalingGroupName("autoScalingGroup").build())
                .infraMappingId(INFRA_MAPPING_ID)
                .accountId(ACCOUNT_ID)
                .artifactName("New")
                .artifactBuildNum("2")
                .stateExecutionInstanceId("stateExecutionInstanceId")
                .workflowExecutionId("workflowExecutionId")
                .workflowId("workflowId")
                .build()),
        true);

    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceService).delete(captor.capture());
    Set idTobeDeleted = captor.getValue();
    assertEquals(1, idTobeDeleted.size());
    assertTrue(idTobeDeleted.contains(instance3.getInstanceId()));

    ArgumentCaptor<Instance> captorInstance = ArgumentCaptor.forClass(Instance.class);
    verify(instanceService, times(2)).save(captorInstance.capture());
    List<Instance> instanceList = captorInstance.getAllValues();
    assertNotNull(instanceList.get(0));
    assertEquals("1", instanceList.get(0).getLastArtifactBuildNum());
    assertEquals("old", instanceList.get(0).getLastArtifactName());
    assertNotNull(instanceList.get(1));
    assertEquals("1", instanceList.get(1).getLastArtifactBuildNum());
    assertEquals("old", instanceList.get(1).getLastArtifactName());
  }
}