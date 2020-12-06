package software.wings.service;

import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
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
import software.wings.beans.CustomInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import com.google.protobuf.util.Durations;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CustomDeploymentInstanceSyncPTCreatorTest extends WingsBaseTest {
  @Mock private PerpetualTaskService perpetualTaskService;
  @InjectMocks private CustomDeploymentInstanceSyncPTCreator perpetualTaskCreator;

  ArgumentCaptor<PerpetualTaskClientContext> captor = ArgumentCaptor.forClass(PerpetualTaskClientContext.class);

  private InfrastructureMapping infraMapping = buildInfraMapping();

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void createPerpetualTasks() {
    perpetualTaskCreator.createPerpetualTasks(infraMapping);

    verify(perpetualTaskService, times(1))
        .createTask(eq(PerpetualTaskType.CUSTOM_DEPLOYMENT_INSTANCE_SYNC), eq(ACCOUNT_ID), captor.capture(),
            eq(PerpetualTaskSchedule.newBuilder()
                    .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                    .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                    .build()),
            eq(false), anyString());

    verify(perpetualTaskService, never()).resetTask(anyString(), anyString(), any());
    assertionsForClientParams();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldCreateIfNoTaskExists() {
    perpetualTaskCreator.createPerpetualTasksForNewDeployment(
        Arrays.asList(DeploymentSummary.builder().build()), null, infraMapping);

    verify(perpetualTaskService, times(1))
        .createTask(eq(PerpetualTaskType.CUSTOM_DEPLOYMENT_INSTANCE_SYNC), eq(ACCOUNT_ID), captor.capture(),
            eq(PerpetualTaskSchedule.newBuilder()
                    .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                    .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                    .build()),
            eq(false), anyString());

    verify(perpetualTaskService, never()).resetTask(anyString(), anyString(), any());
    assertionsForClientParams();
  }

  private void assertionsForClientParams() {
    final Map<String, String> params = captor.getValue().getClientParams();

    assertThat(params.get(InstanceSyncConstants.HARNESS_ACCOUNT_ID)).isEqualTo(ACCOUNT_ID);
    assertThat(params.get(InstanceSyncConstants.HARNESS_APPLICATION_ID)).isEqualTo(APP_ID);
    assertThat(params.get(InstanceSyncConstants.HARNESS_ENV_ID)).isEqualTo(ENV_ID);
    assertThat(params.get(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID)).isEqualTo(INFRA_MAPPING_ID);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldResetTaskOnNewDeployment() {
    final List<String> taskIds =
        perpetualTaskCreator.createPerpetualTasksForNewDeployment(Arrays.asList(DeploymentSummary.builder().build()),
            Arrays.asList(PerpetualTaskRecord.builder().accountId(ACCOUNT_ID).uuid(UUID).build()), infraMapping);

    assertThat(taskIds).containsExactly(UUID);

    verify(perpetualTaskService, never()).createTask(any(), anyString(), any(), any(), anyBoolean(), anyString());

    verify(perpetualTaskService, times(1)).resetTask(ACCOUNT_ID, UUID, null);
  }

  private InfrastructureMapping buildInfraMapping() {
    CustomInfrastructureMapping infraMapping = CustomInfrastructureMapping.builder().build();
    infraMapping.setDeploymentTypeTemplateVersion("1");
    infraMapping.setCustomDeploymentTemplateId(TEMPLATE_ID);
    infraMapping.setUuid(INFRA_MAPPING_ID);
    infraMapping.setAccountId(ACCOUNT_ID);
    infraMapping.setAppId(APP_ID);
    infraMapping.setEnvId(ENV_ID);
    infraMapping.setServiceId(SERVICE_ID);
    infraMapping.setInfraMappingType(InfrastructureMappingType.CUSTOM.name());
    return infraMapping;
  }
}
