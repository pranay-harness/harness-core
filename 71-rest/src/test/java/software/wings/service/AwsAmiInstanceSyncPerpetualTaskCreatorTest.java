package software.wings.service;

import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.service.impl.instance.InstanceSyncTestConstants;
import software.wings.service.intfc.instance.InstanceService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AwsAmiInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock private InstanceService instanceService;
  @Mock private PerpetualTaskService perpetualTaskService;

  @InjectMocks @Inject private AwsAmiInstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  public static final String ASG_NAME = "asgName";
  public static final PerpetualTaskSchedule SCHEDULE = PerpetualTaskSchedule.newBuilder()
                                                           .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                                           .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                                           .build();

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void createPerpetualTasks() {
    doReturn(getAsgInstances()).when(instanceService).getInstancesForAppAndInframapping(anyString(), anyString());
    doReturn("perpetual-task-id")
        .when(perpetualTaskService)
        .createTask(eq(PerpetualTaskType.AWS_AMI_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID), any(), any(),
            eq(false), eq(""));

    perpetualTaskCreator.createPerpetualTasks(getAmiInfraMapping());

    ArgumentCaptor<PerpetualTaskClientContext> captor = ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    verify(perpetualTaskService, times(3))
        .createTask(eq(PerpetualTaskType.AWS_AMI_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID),
            captor.capture(), eq(SCHEDULE), eq(false), eq(""));

    assertThat(
        captor.getAllValues().stream().map(PerpetualTaskClientContext::getClientParams).map(x -> x.get(ASG_NAME)))
        .containsExactlyInAnyOrder("asg-1", "asg-2", "asg-3");
  }

  private AwsAmiInfrastructureMapping getAmiInfraMapping() {
    AwsAmiInfrastructureMapping infraMapping = new AwsAmiInfrastructureMapping();

    infraMapping.setAppId(InstanceSyncTestConstants.APP_ID);
    infraMapping.setAccountId(InstanceSyncTestConstants.ACCOUNT_ID);
    infraMapping.setUuid(InstanceSyncTestConstants.INFRA_MAPPING_ID);
    return infraMapping;
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void createPerpetualTasksForNewDeployment() {
    List<PerpetualTaskRecord> existingRecords = asList(
        PerpetualTaskRecord.builder()
            .clientContext(PerpetualTaskClientContext.builder()
                               .clientParams(ImmutableMap.of(AwsAmiInstanceSyncPerpetualTaskClient.ASG_NAME, "asg-1"))
                               .build())
            .build());

    perpetualTaskCreator.createPerpetualTasksForNewDeployment(
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

    ArgumentCaptor<PerpetualTaskClientContext> captor = ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    verify(perpetualTaskService, times(1))
        .createTask(eq(PerpetualTaskType.AWS_AMI_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID),
            captor.capture(), eq(SCHEDULE), eq(false), eq(""));

    assertThat(
        captor.getAllValues().stream().map(PerpetualTaskClientContext::getClientParams).map(x -> x.get(ASG_NAME)))
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
