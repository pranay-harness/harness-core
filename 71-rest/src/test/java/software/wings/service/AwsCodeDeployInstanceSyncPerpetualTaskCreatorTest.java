package software.wings.service;

import static io.harness.rule.OwnerRule.ABOSII;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.AwsCodeDeployInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.AwsCodeDeployInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.beans.CodeDeployInfrastructureMapping;

import java.util.List;

public class AwsCodeDeployInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock AwsCodeDeployInstanceSyncPerpetualTaskClient perpetualTaskClient;

  @InjectMocks @Inject AwsCodeDeployInstanceSyncPerpetualTaskCreator taskCreator;

  @Before
  public void setup() {
    doReturn("task-id")
        .when(perpetualTaskClient)
        .create(anyString(), any(AwsCodeDeployInstanceSyncPerpetualTaskClientParams.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateTasks() {
    CodeDeployInfrastructureMapping infrastructureMapping = getInfrastructureMapping();

    List<String> taskIds = taskCreator.createPerpetualTasks(infrastructureMapping);

    ArgumentCaptor<AwsCodeDeployInstanceSyncPerpetualTaskClientParams> paramsCaptor =
        ArgumentCaptor.forClass(AwsCodeDeployInstanceSyncPerpetualTaskClientParams.class);
    verify(perpetualTaskClient, times(1)).create(eq(ACCOUNT_ID), paramsCaptor.capture());

    AwsCodeDeployInstanceSyncPerpetualTaskClientParams params = paramsCaptor.getValue();
    assertThat(taskIds).containsExactlyInAnyOrder("task-id");
    assertThat(params.getAppId()).isEqualTo(APP_ID);
    assertThat(params.getInframmapingId()).isEqualTo(INFRA_MAPPING_ID);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateTasksForNewDeployment() {
    List<DeploymentSummary> deploymentSummaries = singletonList(
        DeploymentSummary.builder().accountId(ACCOUNT_ID).appId(APP_ID).infraMappingId(INFRA_MAPPING_ID).build());
    List<PerpetualTaskRecord> existingTasks = emptyList();
    CodeDeployInfrastructureMapping infrastructureMapping = getInfrastructureMapping();

    List<String> taskIds =
        taskCreator.createPerpetualTasksForNewDeployment(deploymentSummaries, existingTasks, infrastructureMapping);

    ArgumentCaptor<AwsCodeDeployInstanceSyncPerpetualTaskClientParams> paramsCaptor =
        ArgumentCaptor.forClass(AwsCodeDeployInstanceSyncPerpetualTaskClientParams.class);
    verify(perpetualTaskClient, times(1)).create(eq(ACCOUNT_ID), paramsCaptor.capture());

    AwsCodeDeployInstanceSyncPerpetualTaskClientParams params = paramsCaptor.getValue();
    assertThat(taskIds).containsExactlyInAnyOrder("task-id");
    assertThat(params.getAppId()).isEqualTo(APP_ID);
    assertThat(params.getInframmapingId()).isEqualTo(INFRA_MAPPING_ID);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateTasksForNewDeploymentsWhenTaskExistsForAppId() {
    List<DeploymentSummary> deploymentSummaries = singletonList(
        DeploymentSummary.builder().accountId(ACCOUNT_ID).appId(APP_ID).infraMappingId(INFRA_MAPPING_ID).build());
    List<PerpetualTaskRecord> existingTasks = singletonList(
        PerpetualTaskRecord.builder()
            .clientContext(new PerpetualTaskClientContext(ImmutableMap.of(HARNESS_APPLICATION_ID, APP_ID)))
            .build());
    CodeDeployInfrastructureMapping infrastructureMapping = getInfrastructureMapping();

    List<String> taskIds =
        taskCreator.createPerpetualTasksForNewDeployment(deploymentSummaries, existingTasks, infrastructureMapping);

    verify(perpetualTaskClient, never())
        .create(eq(ACCOUNT_ID), any(AwsCodeDeployInstanceSyncPerpetualTaskClientParams.class));

    assertThat(taskIds).isEmpty();
  }

  private CodeDeployInfrastructureMapping getInfrastructureMapping() {
    return aCodeDeployInfrastructureMapping()
        .withAccountId(ACCOUNT_ID)
        .withAppId(APP_ID)
        .withUuid(INFRA_MAPPING_ID)
        .build();
  }
}