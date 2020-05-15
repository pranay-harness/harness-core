package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.service.impl.instance.InstanceSyncTestConstants;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;

public class AwsAmiInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock private InstanceService instanceService;
  @Mock private AwsAmiInstanceSyncPerpetualTaskClient perpetualTaskClient;

  @InjectMocks @Inject private AwsAmiInstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void createPerpetualTasks() {
    doReturn(getAsgInstances()).when(instanceService).getInstancesForAppAndInframapping(anyString(), anyString());
    doReturn("perpetual-task-id")
        .when(perpetualTaskClient)
        .create(eq(InstanceSyncTestConstants.ACCOUNT_ID), any(AwsAmiInstanceSyncPerpetualTaskClientParams.class));

    final List<String> perpetualTaskIds = perpetualTaskCreator.createPerpetualTasks(getAmiInfraMapping());

    ArgumentCaptor<AwsAmiInstanceSyncPerpetualTaskClientParams> captor =
        ArgumentCaptor.forClass(AwsAmiInstanceSyncPerpetualTaskClientParams.class);
    verify(perpetualTaskClient, times(3)).create(eq(InstanceSyncTestConstants.ACCOUNT_ID), captor.capture());

    assertThat(captor.getAllValues().stream().map(AwsAmiInstanceSyncPerpetualTaskClientParams::getAsgName))
        .containsExactlyInAnyOrder("asg-1", "asg-2", "asg-3");
  }

  private AwsAmiInfrastructureMapping getAmiInfraMapping() {
    AwsAmiInfrastructureMapping infraMapping = new AwsAmiInfrastructureMapping();
    infraMapping.setAccountId(InstanceSyncTestConstants.ACCOUNT_ID);
    infraMapping.setUuid(InstanceSyncTestConstants.INFRA_MAPPING_ID);
    return infraMapping;
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void createPerpetualTasksForNewDeployment() {
    List<PerpetualTaskRecord> existingRecords =
        asList(PerpetualTaskRecord.builder()
                   .clientContext(new PerpetualTaskClientContext(
                       ImmutableMap.of(AwsAmiInstanceSyncPerpetualTaskClient.ASG_NAME, "asg-1")))
                   .build());

    final List<String> perpetualTaskIds = perpetualTaskCreator.createPerpetualTasksForNewDeployment(
        asList(DeploymentSummary.builder()
                   .appId(InstanceSyncTestConstants.APP_ID)
                   .accountId(InstanceSyncTestConstants.ACCOUNT_ID)
                   .infraMappingId(InstanceSyncTestConstants.INFRA_MAPPING_ID)
                   .deploymentInfo(AwsAutoScalingGroupDeploymentInfo.builder().autoScalingGroupName("asg-1").build())
                   .build(),
            DeploymentSummary.builder()
                .appId(InstanceSyncTestConstants.APP_ID)
                .accountId(InstanceSyncTestConstants.ACCOUNT_ID)
                .infraMappingId(InstanceSyncTestConstants.INFRA_MAPPING_ID)
                .deploymentInfo(AwsAutoScalingGroupDeploymentInfo.builder().autoScalingGroupName("asg-2").build())
                .build()),
        existingRecords, new AwsAmiInfrastructureMapping());

    ArgumentCaptor<AwsAmiInstanceSyncPerpetualTaskClientParams> captor =
        ArgumentCaptor.forClass(AwsAmiInstanceSyncPerpetualTaskClientParams.class);
    verify(perpetualTaskClient, times(1)).create(eq(InstanceSyncTestConstants.ACCOUNT_ID), captor.capture());

    assertThat(captor.getAllValues().stream().map(AwsAmiInstanceSyncPerpetualTaskClientParams::getAsgName))
        .containsExactlyInAnyOrder("asg-2");
  }

  private List<Instance> getAsgInstances() {
    return asList(Instance.builder()
                      .uuid("id-1")
                      .instanceInfo(AutoScalingGroupInstanceInfo.builder().autoScalingGroupName("asg-1").build())
                      .build(),
        Instance.builder()
            .uuid("id-2")
            .instanceInfo(AutoScalingGroupInstanceInfo.builder().autoScalingGroupName("asg-2").build())
            .build(),
        Instance.builder()
            .uuid("id-3")
            .instanceInfo(AutoScalingGroupInstanceInfo.builder().autoScalingGroupName("asg-3").build())
            .build(),
        Instance.builder().uuid("id-4").instanceInfo(Ec2InstanceInfo.builder().hostName("ip-1").build()).build(),
        Instance.builder()
            .uuid("id-5")
            .instanceInfo(AutoScalingGroupInstanceInfo.builder().autoScalingGroupName("asg-1").build())
            .build());
  }
}