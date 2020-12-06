package software.wings.service;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;
import static software.wings.service.SpotinstAmiInstanceSyncPerpetualTaskCreator.ELASTIGROUP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.SpotinstAmiInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.SpotinstAmiDeploymentKey;
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

public class SpotinstAmiInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock InstanceService instanceService;
  @Mock PerpetualTaskService perpetualTaskService;

  @InjectMocks @Inject SpotinstAmiInstanceSyncPerpetualTaskCreator taskCreator;

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasks() {
    doReturn(getAllInstances()).when(instanceService).getInstancesForAppAndInframapping(anyString(), anyString());
    doReturn("perpetual-task-id")
        .when(perpetualTaskService)
        .createTask(eq(PerpetualTaskType.SPOT_INST_AMI_INSTANCE_SYNC), eq(ACCOUNT_ID),
            any(PerpetualTaskClientContext.class), any(PerpetualTaskSchedule.class), eq(false), eq(""));

    taskCreator.createPerpetualTasks(getInfrastructureMapping());

    ArgumentCaptor<PerpetualTaskClientContext> clientContextCaptor =
        ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    ArgumentCaptor<PerpetualTaskSchedule> scheduleCaptor = ArgumentCaptor.forClass(PerpetualTaskSchedule.class);

    verify(perpetualTaskService, times(2))
        .createTask(eq(PerpetualTaskType.SPOT_INST_AMI_INSTANCE_SYNC), eq(ACCOUNT_ID), clientContextCaptor.capture(),
            scheduleCaptor.capture(), eq(false), eq(""));

    assertThat(clientContextCaptor.getAllValues().stream().map(
                   clientContext -> clientContext.getClientParams().get(ELASTIGROUP_ID)))
        .containsExactlyInAnyOrder("group-1", "group-2");

    clientContextCaptor.getAllValues().forEach(clientContext -> {
      assertThat(clientContext.getClientParams().get(HARNESS_APPLICATION_ID)).isEqualTo(APP_ID);
      assertThat(clientContext.getClientParams().get(INFRASTRUCTURE_MAPPING_ID)).isEqualTo(INFRA_MAPPING_ID);
    });

    scheduleCaptor.getAllValues().forEach(schedule -> {
      assertThat(schedule.getInterval()).isEqualTo(Durations.fromMinutes(INTERVAL_MINUTES));
      assertThat(schedule.getTimeout()).isEqualTo(Durations.fromSeconds(TIMEOUT_SECONDS));
    });
  }

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasksForNewDeployment() {
    List<PerpetualTaskRecord> existingPerpetualTasks =
        asList(PerpetualTaskRecord.builder()
                   .clientContext(PerpetualTaskClientContext.builder()
                                      .clientParams(ImmutableMap.of(
                                          SpotinstAmiInstanceSyncPerpetualTaskClient.ELASTIGROUP_ID, "elastigroup-1"))
                                      .build())
                   .build(),
            PerpetualTaskRecord.builder()
                .clientContext(PerpetualTaskClientContext.builder()
                                   .clientParams(ImmutableMap.of(
                                       SpotinstAmiInstanceSyncPerpetualTaskClient.ELASTIGROUP_ID, "elastigroup-n"))
                                   .build())
                .build());

    List<DeploymentSummary> summaries =
        asList(DeploymentSummary.builder()
                   .appId(APP_ID)
                   .accountId(ACCOUNT_ID)
                   .infraMappingId(INFRA_MAPPING_ID)
                   .spotinstAmiDeploymentKey(SpotinstAmiDeploymentKey.builder().elastigroupId("elastigroup-1").build())
                   .build(),
            DeploymentSummary.builder()
                .appId(APP_ID)
                .accountId(ACCOUNT_ID)
                .infraMappingId(INFRA_MAPPING_ID)
                .spotinstAmiDeploymentKey(SpotinstAmiDeploymentKey.builder().elastigroupId("elastigroup-2").build())
                .build(),
            DeploymentSummary.builder()
                .appId(APP_ID)
                .accountId(ACCOUNT_ID)
                .infraMappingId(INFRA_MAPPING_ID)
                .spotinstAmiDeploymentKey(SpotinstAmiDeploymentKey.builder().elastigroupId("elastigroup-3").build())
                .build());

    taskCreator.createPerpetualTasksForNewDeployment(
        summaries, existingPerpetualTasks, new AwsAmiInfrastructureMapping());

    ArgumentCaptor<PerpetualTaskClientContext> clientContextCaptor =
        ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    ArgumentCaptor<PerpetualTaskSchedule> scheduleCaptor = ArgumentCaptor.forClass(PerpetualTaskSchedule.class);

    verify(perpetualTaskService, times(2))
        .createTask(eq(PerpetualTaskType.SPOT_INST_AMI_INSTANCE_SYNC), eq(ACCOUNT_ID), clientContextCaptor.capture(),
            scheduleCaptor.capture(), eq(false), eq(""));

    assertThat(clientContextCaptor.getAllValues().stream().map(
                   clientContext -> clientContext.getClientParams().get(ELASTIGROUP_ID)))
        .containsExactlyInAnyOrder("elastigroup-2", "elastigroup-3");

    clientContextCaptor.getAllValues().forEach(clientContext -> {
      assertThat(clientContext.getClientParams().get(HARNESS_APPLICATION_ID)).isEqualTo(APP_ID);
      assertThat(clientContext.getClientParams().get(INFRASTRUCTURE_MAPPING_ID)).isEqualTo(INFRA_MAPPING_ID);
    });

    scheduleCaptor.getAllValues().forEach(schedule -> {
      assertThat(schedule.getInterval()).isEqualTo(Durations.fromMinutes(INTERVAL_MINUTES));
      assertThat(schedule.getTimeout()).isEqualTo(Durations.fromSeconds(TIMEOUT_SECONDS));
    });
  }

  private AwsAmiInfrastructureMapping getInfrastructureMapping() {
    return AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping()
        .withAppId(APP_ID)
        .withUuid(INFRA_MAPPING_ID)
        .withAccountId(ACCOUNT_ID)
        .build();
  }

  private List<Instance> getAllInstances() {
    return asList(Instance.builder()
                      .uuid("uuid-1")
                      .instanceInfo(SpotinstAmiInstanceInfo.builder().elastigroupId("group-1").build())
                      .build(),
        Instance.builder()
            .uuid("uuid-2")
            .instanceInfo(SpotinstAmiInstanceInfo.builder().elastigroupId("group-2").build())
            .build(),
        Instance.builder().uuid("uuid-3").instanceInfo(Ec2InstanceInfo.builder().build()).build(),
        Instance.builder().uuid("uuid-4").build());
  }
}
