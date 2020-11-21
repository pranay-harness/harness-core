package software.wings.service;

import static io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient.START_DATE;

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
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.api.lambda.AwsLambdaDeploymentInfo;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.key.AwsLambdaInstanceKey;
import software.wings.service.impl.instance.InstanceSyncTestConstants;
import software.wings.service.intfc.instance.ServerlessInstanceService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AwsLambdaInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock ServerlessInstanceService serverlessInstanceService;
  @Mock PerpetualTaskService perpetualTaskService;
  public static final String FUNCTION_NAME = "functionName";
  public static final String QUALIFIER = "qualifier";
  public static final PerpetualTaskSchedule SCHEDULE = PerpetualTaskSchedule.newBuilder()
                                                           .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                                           .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                                           .build();

  @InjectMocks @Inject private AwsLambdaInstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void createPerpetualTasks() {
    doReturn(getServerlessInstances()).when(serverlessInstanceService).list(anyString(), anyString());
    doReturn("perpetual-task-id")
        .when(perpetualTaskService)
        .createTask(eq(PerpetualTaskType.AWS_LAMBDA_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID), any(),
            any(), eq(false), eq(""));

    perpetualTaskCreator.createPerpetualTasks(getAwsLambdaInfraMapping());

    ArgumentCaptor<PerpetualTaskClientContext> captor = ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    verify(perpetualTaskService, times(3))
        .createTask(eq(PerpetualTaskType.AWS_LAMBDA_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID),
            captor.capture(), eq(SCHEDULE), eq(false), eq(""));

    assertThat(
        captor.getAllValues().stream().map(PerpetualTaskClientContext::getClientParams).map(x -> x.get(FUNCTION_NAME)))
        .containsExactlyInAnyOrder("function-1", "function-2", "function-3");

    assertThat(
        captor.getAllValues().stream().map(PerpetualTaskClientContext::getClientParams).map(x -> x.get(QUALIFIER)))
        .containsExactlyInAnyOrder("version-1", "version-2", "version-3");
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void createPerpetualTasksForNewDeployment() {
    Date now = new Date();
    List<PerpetualTaskRecord> existingRecords =
        asList(PerpetualTaskRecord.builder()
                   .clientContext(PerpetualTaskClientContext.builder()
                                      .clientParams(ImmutableMap.of(FUNCTION_NAME, "function-1", QUALIFIER, "version-1",
                                          START_DATE, String.valueOf(now.getTime())))
                                      .build())
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

    ArgumentCaptor<PerpetualTaskClientContext> captor = ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    verify(perpetualTaskService, times(1))
        .createTask(eq(PerpetualTaskType.AWS_LAMBDA_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID),
            captor.capture(), eq(SCHEDULE), eq(false), eq(""));

    assertThat(
        captor.getAllValues().stream().map(PerpetualTaskClientContext::getClientParams).map(x -> x.get(FUNCTION_NAME)))
        .containsExactlyInAnyOrder("function-2");
    assertThat(
        captor.getAllValues().stream().map(PerpetualTaskClientContext::getClientParams).map(x -> x.get(QUALIFIER)))
        .containsExactlyInAnyOrder("version-2");
  }

  private AwsLambdaInfraStructureMapping getAwsLambdaInfraMapping() {
    AwsLambdaInfraStructureMapping infraMapping = new AwsLambdaInfraStructureMapping();
    infraMapping.setAppId(InstanceSyncTestConstants.APP_ID);
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
