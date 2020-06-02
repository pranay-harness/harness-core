package software.wings.service;

import static io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient.FUNCTION_NAME;
import static io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient.QUALIFIER;
import static io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient.START_DATE;
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
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClientParams;
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
import software.wings.api.lambda.AwsLambdaDeploymentInfo;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.key.AwsLambdaInstanceKey;
import software.wings.service.impl.instance.InstanceSyncTestConstants;
import software.wings.service.intfc.instance.ServerlessInstanceService;

import java.util.Date;
import java.util.List;

public class AwsLambdaInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock ServerlessInstanceService serverlessInstanceService;
  @Mock AwsLambdaInstanceSyncPerpetualTaskClient client;

  @InjectMocks @Inject private AwsLambdaInstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void createPerpetualTasks() {
    doReturn(getServerlessInstances()).when(serverlessInstanceService).list(anyString(), anyString());
    doReturn("perpetual-task-id")
        .when(client)
        .create(eq(InstanceSyncTestConstants.ACCOUNT_ID), any(AwsLambdaInstanceSyncPerpetualTaskClientParams.class));

    perpetualTaskCreator.createPerpetualTasks(getAwsLambdaInfraMapping());

    ArgumentCaptor<AwsLambdaInstanceSyncPerpetualTaskClientParams> captor =
        ArgumentCaptor.forClass(AwsLambdaInstanceSyncPerpetualTaskClientParams.class);
    verify(client, times(3)).create(eq(InstanceSyncTestConstants.ACCOUNT_ID), captor.capture());

    assertThat(captor.getAllValues().stream().map(AwsLambdaInstanceSyncPerpetualTaskClientParams::getFunctionName))
        .containsExactlyInAnyOrder("function-1", "function-2", "function-3");

    assertThat(captor.getAllValues().stream().map(AwsLambdaInstanceSyncPerpetualTaskClientParams::getQualifier))
        .containsExactlyInAnyOrder("version-1", "version-2", "version-3");
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void createPerpetualTasksForNewDeployment() {
    Date now = new Date();
    List<PerpetualTaskRecord> existingRecords =
        asList(PerpetualTaskRecord.builder()
                   .clientContext(new PerpetualTaskClientContext(ImmutableMap.of(
                       FUNCTION_NAME, "function-1", QUALIFIER, "version-1", START_DATE, String.valueOf(now.getTime()))))
                   .build());

    perpetualTaskCreator.createPerpetualTasksForNewDeployment(
        asList(DeploymentSummary.builder()
                   .appId(InstanceSyncTestConstants.APP_ID)
                   .accountId(InstanceSyncTestConstants.ACCOUNT_ID)
                   .infraMappingId(InstanceSyncTestConstants.INFRA_MAPPING_ID)
                   .deployedAt(now.getTime())
                   .deploymentInfo(
                       AwsLambdaDeploymentInfo.builder().functionName("function-1").version("version-1").build())
                   .build(),
            DeploymentSummary.builder()
                .appId(InstanceSyncTestConstants.APP_ID)
                .accountId(InstanceSyncTestConstants.ACCOUNT_ID)
                .infraMappingId(InstanceSyncTestConstants.INFRA_MAPPING_ID)
                .deployedAt(now.getTime())
                .deploymentInfo(
                    AwsLambdaDeploymentInfo.builder().functionName("function-2").version("version-2").build())
                .build()),
        existingRecords, new AwsLambdaInfraStructureMapping());

    ArgumentCaptor<AwsLambdaInstanceSyncPerpetualTaskClientParams> captor =
        ArgumentCaptor.forClass(AwsLambdaInstanceSyncPerpetualTaskClientParams.class);
    verify(client, times(1)).create(eq(InstanceSyncTestConstants.ACCOUNT_ID), captor.capture());

    assertThat(captor.getAllValues().stream().map(AwsLambdaInstanceSyncPerpetualTaskClientParams::getFunctionName))
        .containsExactlyInAnyOrder("function-2");
    assertThat(captor.getAllValues().stream().map(AwsLambdaInstanceSyncPerpetualTaskClientParams::getQualifier))
        .containsExactlyInAnyOrder("version-2");
  }

  private AwsLambdaInfraStructureMapping getAwsLambdaInfraMapping() {
    AwsLambdaInfraStructureMapping infraMapping = new AwsLambdaInfraStructureMapping();
    infraMapping.setAccountId(InstanceSyncTestConstants.ACCOUNT_ID);
    infraMapping.setUuid(InstanceSyncTestConstants.INFRA_MAPPING_ID);
    return infraMapping;
  }

  private List<ServerlessInstance> getServerlessInstances() {
    return asList(
        ServerlessInstance.builder()
            .uuid("id-1")
            .lambdaInstanceKey(
                AwsLambdaInstanceKey.builder().functionName("function-1").functionVersion("version-1").build())
            .build(),
        ServerlessInstance.builder()
            .uuid("id-2")
            .lambdaInstanceKey(
                AwsLambdaInstanceKey.builder().functionName("function-2").functionVersion("version-2").build())
            .build(),
        ServerlessInstance.builder()
            .uuid("id-3")
            .lambdaInstanceKey(
                AwsLambdaInstanceKey.builder().functionName("function-3").functionVersion("version-3").build())
            .build(),
        ServerlessInstance.builder()
            .uuid("id-4")
            .lambdaInstanceKey(
                AwsLambdaInstanceKey.builder().functionName("function-1").functionVersion("version-1").build())
            .build());
  }
}
