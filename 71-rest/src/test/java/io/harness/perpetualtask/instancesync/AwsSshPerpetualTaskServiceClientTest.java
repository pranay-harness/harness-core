package io.harness.perpetualtask.instancesync;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.AwsSshPTClientParams;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPTParamsOuterClass.AwsSshInstanceSyncPTParams;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.impl.instance.InstanceSyncTestConstants;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AwsSshPerpetualTaskServiceClientTest extends WingsBaseTest {
  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private SecretManager secretManager;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private AwsUtils awsUtils;
  @Mock private SettingsService settingsService;

  @InjectMocks @Inject private AwsSshPerpetualTaskServiceClient client;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void create() {
    client.create(InstanceSyncTestConstants.ACCOUNT_ID,
        AwsSshPTClientParams.builder().appId(APP_ID).inframappingId(INFRA_MAPPING_ID).build());

    Mockito.verify(perpetualTaskService, Mockito.times(1))
        .createTask(eq(PerpetualTaskType.AWS_SSH_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID),
            eq(new PerpetualTaskClientContext(ImmutableMap.of(InstanceSyncConstants.HARNESS_APPLICATION_ID, APP_ID,
                InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, INFRA_MAPPING_ID))),
            eq(PerpetualTaskSchedule.newBuilder()
                    .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                    .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                    .build()),
            eq(false));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void reset() {
    client.reset(InstanceSyncTestConstants.ACCOUNT_ID, InstanceSyncTestConstants.PERPETUAL_TASK_ID);
    Mockito.verify(perpetualTaskService, Mockito.times(1))
        .resetTask(InstanceSyncTestConstants.ACCOUNT_ID, InstanceSyncTestConstants.PERPETUAL_TASK_ID);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void delete() {
    client.delete(InstanceSyncTestConstants.ACCOUNT_ID, InstanceSyncTestConstants.PERPETUAL_TASK_ID);
    Mockito.verify(perpetualTaskService, Mockito.times(1))
        .deleteTask(InstanceSyncTestConstants.ACCOUNT_ID, InstanceSyncTestConstants.PERPETUAL_TASK_ID);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getTaskParams() {
    AwsConfig awsConfig = AwsConfig.builder().tag("abc").build();
    prepareTaskData(awsConfig);

    final AwsSshInstanceSyncPTParams taskParams = (AwsSshInstanceSyncPTParams) client.getTaskParams(
        new PerpetualTaskClientContext(ImmutableMap.of(InstanceSyncConstants.HARNESS_APPLICATION_ID, APP_ID,
            InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, INFRA_MAPPING_ID)));

    Assertions.assertThat(taskParams.getRegion()).isEqualTo("us-east-1");
    Assertions.assertThat(taskParams.getAwsConfig()).isNotNull();
    Assertions.assertThat(taskParams.getFilter()).isNotNull();
    Assertions.assertThat(taskParams.getEncryptedData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getValidationTask() {
    AwsConfig awsConfig = AwsConfig.builder().tag("abc").build();
    prepareTaskData(awsConfig);

    final DelegateTask validationTask = client.getValidationTask(
        new PerpetualTaskClientContext(ImmutableMap.of(InstanceSyncConstants.HARNESS_APPLICATION_ID, APP_ID,
            InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, INFRA_MAPPING_ID)),
        InstanceSyncTestConstants.ACCOUNT_ID);

    Assertions.assertThat(validationTask)
        .isEqualTo(DelegateTask.builder()
                       .accountId(InstanceSyncTestConstants.ACCOUNT_ID)
                       .appId(GLOBAL_APP_ID)
                       .tags(singletonList("abc"))
                       .data(TaskData.builder()
                                 .async(false)
                                 .taskType(TaskType.AWS_EC2_TASK.name())
                                 .parameters(new Object[] {AwsEc2ListInstancesRequest.builder()
                                                               .awsConfig(awsConfig)
                                                               .encryptionDetails(new ArrayList<>())
                                                               .region("us-east-1")
                                                               .filters(new ArrayList<>())
                                                               .build()})
                                 .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                                 .build())
                       .build());
  }

  private void prepareTaskData(AwsConfig awsConfig) {
    AwsInfrastructureMapping infraMapping = new AwsInfrastructureMapping();
    infraMapping.setRegion("us-east-1");
    infraMapping.setServiceId(InstanceSyncTestConstants.SERVICE_ID);
    infraMapping.setComputeProviderSettingId(InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID);

    doReturn(infraMapping).when(infrastructureMappingService).get(APP_ID, INFRA_MAPPING_ID);
    doReturn(DeploymentType.SSH)
        .when(serviceResourceService)
        .getDeploymentType(infraMapping, null, infraMapping.getServiceId());
    doReturn(new ArrayList<>()).when(awsUtils).getAwsFilters(infraMapping, DeploymentType.SSH);
    doReturn(SettingAttribute.Builder.aSettingAttribute().withValue(awsConfig).build())
        .when(settingsService)
        .get(InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID);
    doReturn(new ArrayList<>()).when(secretManager).getEncryptionDetails(any(AwsConfig.class));
  }
}