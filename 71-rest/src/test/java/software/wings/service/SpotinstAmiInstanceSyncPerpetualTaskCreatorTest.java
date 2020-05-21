package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.SpotinstAmiInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.SpotinstAmiDeploymentKey;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;

public class SpotinstAmiInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock InstanceService instanceService;
  @Mock SpotinstAmiInstanceSyncPerpetualTaskClient perpetualTaskClient;

  @InjectMocks @Inject SpotinstAmiInstanceSyncPerpetualTaskCreator taskCreator;

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasks() {
    doReturn(getAllInstances()).when(instanceService).getInstancesForAppAndInframapping(anyString(), anyString());
    doReturn("perpetual-task-id")
        .when(perpetualTaskClient)
        .create(eq(ACCOUNT_ID), any(SpotinstAmiInstanceSyncPerpetualTaskClientParams.class));

    taskCreator.createPerpetualTasks(getInfrastructureMapping());

    ArgumentCaptor<SpotinstAmiInstanceSyncPerpetualTaskClientParams> paramsCaptor =
        ArgumentCaptor.forClass(SpotinstAmiInstanceSyncPerpetualTaskClientParams.class);
    verify(perpetualTaskClient, times(2)).create(eq(ACCOUNT_ID), paramsCaptor.capture());

    assertThat(
        paramsCaptor.getAllValues().stream().map(SpotinstAmiInstanceSyncPerpetualTaskClientParams::getElastigroupId))
        .containsExactlyInAnyOrder("group-1", "group-2");
  }

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasksForNewDeployment() {
    List<PerpetualTaskRecord> existingPerpetualTasks =
        asList(PerpetualTaskRecord.builder()
                   .clientContext(new PerpetualTaskClientContext(
                       ImmutableMap.of(SpotinstAmiInstanceSyncPerpetualTaskClient.ELASTIGROUP_ID, "elastigroup-1")))
                   .build(),
            PerpetualTaskRecord.builder()
                .clientContext(new PerpetualTaskClientContext(
                    ImmutableMap.of(SpotinstAmiInstanceSyncPerpetualTaskClient.ELASTIGROUP_ID, "elastigroup-n")))
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

    ArgumentCaptor<SpotinstAmiInstanceSyncPerpetualTaskClientParams> paramsCaptor =
        ArgumentCaptor.forClass(SpotinstAmiInstanceSyncPerpetualTaskClientParams.class);
    verify(perpetualTaskClient, times(2)).create(eq(ACCOUNT_ID), paramsCaptor.capture());

    assertThat(
        paramsCaptor.getAllValues().stream().map(SpotinstAmiInstanceSyncPerpetualTaskClientParams::getElastigroupId))
        .containsExactlyInAnyOrder("elastigroup-2", "elastigroup-3");
  }

  private AwsAmiInfrastructureMapping getInfrastructureMapping() {
    return AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping()
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