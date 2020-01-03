package software.wings.service.impl.instance;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.beans.AmiDeploymentType.SPOTINST;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.EC2_INSTANCE_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.EC2_INSTANCE_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.EC2_INSTANCE_E_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ELASTIGROUP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ELASTIGROUP_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.HOST_NAME_IP2;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PRIVATE_DNS_1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PRIVATE_DNS_2;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PRIVATE_DNS_3;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PUBLIC_DNS_1;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PUBLIC_DNS_2;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PUBLIC_DNS_3;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.US_EAST;
import static wiremock.com.google.common.collect.Lists.newArrayList;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.api.SpotinstAmiDeploymentInfo;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.SpotinstAmiInstanceInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.service.impl.spotinst.SpotinstHelperServiceManager;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;
import java.util.Set;

public class SpotinstAmiInstanceHandlerTest extends WingsBaseTest {
  @Mock private SpotinstHelperServiceManager mockSpotinstHelperServiceManager;
  @Mock private InstanceService mockInstanceService;
  @Mock private AppService mockAppService;
  @Mock private EnvironmentService mockEnvironmentService;
  @Mock private ServiceResourceService mockServiceResourceService;

  @InjectMocks @Inject private SpotinstAmiInstanceHandler spotinstAmiInstanceHandler;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testSyncInstancesForElastigroup() {
    com.amazonaws.services.ec2.model.Instance ec2Instance1 = new com.amazonaws.services.ec2.model.Instance();
    ec2Instance1.setPrivateDnsName(PRIVATE_DNS_1);
    ec2Instance1.setPublicDnsName(PUBLIC_DNS_1);
    ec2Instance1.setInstanceId(EC2_INSTANCE_1_ID);

    com.amazonaws.services.ec2.model.Instance ec2Instance2 = new com.amazonaws.services.ec2.model.Instance();
    ec2Instance2.setPrivateDnsName(PRIVATE_DNS_2);
    ec2Instance2.setPublicDnsName(PUBLIC_DNS_2);
    ec2Instance2.setInstanceId(EC2_INSTANCE_2_ID);

    com.amazonaws.services.ec2.model.Instance ec2Instance3 = new com.amazonaws.services.ec2.model.Instance();
    ec2Instance3.setPrivateDnsName(PRIVATE_DNS_3);
    ec2Instance3.setPublicDnsName(PUBLIC_DNS_3);
    ec2Instance3.setInstanceId(EC2_INSTANCE_E_ID);

    List<Instance> currentInstancesInDb = newArrayList(
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
            .instanceInfo(SpotinstAmiInstanceInfo.builder()
                              .ec2Instance(ec2Instance1)
                              .elastigroupId(ELASTIGROUP_ID)
                              .hostPublicDns(PUBLIC_DNS_1)
                              .hostName(PRIVATE_DNS_1)
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
            .instanceInfo(SpotinstAmiInstanceInfo.builder()
                              .ec2Instance(ec2Instance2)
                              .elastigroupId(ELASTIGROUP_ID)
                              .hostPublicDns(PUBLIC_DNS_2)
                              .hostName(PRIVATE_DNS_2)
                              .build())
            .build());

    DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                              .deploymentInfo(SpotinstAmiDeploymentInfo.builder()
                                                                  .elastigroupId(ELASTIGROUP_ID)
                                                                  .elastigroupName(ELASTIGROUP_NAME)
                                                                  .build())
                                              .accountId(ACCOUNT_ID)
                                              .infraMappingId(INFRA_MAPPING_ID)
                                              .workflowExecutionId("workflowExecution_1")
                                              .stateExecutionInstanceId("stateExecutionInstanceId")
                                              .artifactBuildNum("1")
                                              .artifactName("new")
                                              .build();

    doReturn(newArrayList(ec2Instance2, ec2Instance3))
        .when(mockSpotinstHelperServiceManager)
        .listElastigroupInstances(any(), anyList(), any(), anyList(), anyString(), anyString(), anyString());

    doReturn(anApplication().name(APP_NAME).uuid(APP_ID).accountId(ACCOUNT_ID).build())
        .when(mockAppService)
        .get(anyString());
    doReturn(anEnvironment().environmentType(EnvironmentType.PROD).name(ENV_NAME).build())
        .when(mockEnvironmentService)
        .get(anyString(), anyString(), anyBoolean());
    doReturn(Service.builder().name(SERVICE_NAME).build())
        .when(mockServiceResourceService)
        .getWithDetails(anyString(), anyString());

    AwsAmiInfrastructureMapping infrastructureMapping =
        anAwsAmiInfrastructureMapping()
            .withUuid(INFRA_MAPPING_ID)
            .withAccountId(ACCOUNT_ID)
            .withAmiDeploymentType(SPOTINST)
            .withAppId(APP_ID)
            .withInfraMappingType(InfrastructureMappingType.AWS_AMI.getName())
            .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
            .withRegion(US_EAST)
            .withServiceId(SERVICE_ID)
            .build();

    spotinstAmiInstanceHandler.syncInstancesForElastigroup(ELASTIGROUP_ID, AwsConfig.builder().build(), emptyList(),
        SpotInstConfig.builder().build(), emptyList(), "us-east-1", currentInstancesInDb, false, APP_ID,
        deploymentSummary, infrastructureMapping);

    ArgumentCaptor<Set> captor = ArgumentCaptor.forClass(Set.class);
    verify(mockInstanceService).delete(captor.capture());
    Set idsTobeDeleted = captor.getValue();
    assertThat(idsTobeDeleted.size()).isEqualTo(1);
    assertThat(idsTobeDeleted.contains(INSTANCE_1_ID)).isTrue();

    ArgumentCaptor<Instance> captorInstance = ArgumentCaptor.forClass(Instance.class);
    verify(mockInstanceService).save(captorInstance.capture());
    Instance instance = captorInstance.getValue();
    assertThat(instance).isNotNull();
    InstanceInfo instanceInfo = instance.getInstanceInfo();
    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo instanceof SpotinstAmiInstanceInfo).isTrue();
    SpotinstAmiInstanceInfo spotinstAmiInstanceInfo = (SpotinstAmiInstanceInfo) instanceInfo;
    assertThat(spotinstAmiInstanceInfo.getEc2Instance().getInstanceId()).isEqualTo(EC2_INSTANCE_E_ID);
  }
}